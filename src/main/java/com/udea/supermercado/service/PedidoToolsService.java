package com.udea.supermercado.service;

import java.math.BigDecimal;
import java.text.Normalizer;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeParseException;
import java.time.format.TextStyle;
import java.util.Locale;
import java.util.Optional;

import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.udea.supermercado.dto.PedidoDto;
import com.udea.supermercado.exception.ReglaNegocioException;
import com.udea.supermercado.model.DetallePedido;
import com.udea.supermercado.model.EstadoPedido;
import com.udea.supermercado.model.Franja;
import com.udea.supermercado.model.Pedido;
import com.udea.supermercado.model.Producto;
import com.udea.supermercado.repository.ClienteRepository;
import com.udea.supermercado.repository.PedidoRepository;
import com.udea.supermercado.repository.ProductoRepository;

/** Herramientas MCP de ESCRITURA sobre pedidos en estado BORRADOR (armar el carrito). */
@Service
@Transactional
public class PedidoToolsService {

    static final ZoneId ZONA = ZoneId.of("America/Bogota");
    static final int MAX_DIAS_ADELANTE = 14;
    static final int MAX_CANTIDAD_POR_PRODUCTO = 50;

    private final PedidoRepository pedidos;
    private final ClienteRepository clientes;
    private final ProductoRepository productos;

    public PedidoToolsService(PedidoRepository pedidos, ClienteRepository clientes, ProductoRepository productos) {
        this.pedidos = pedidos;
        this.clientes = clientes;
        this.productos = productos;
    }

    @Tool(description = """
            Devuelve la fecha de hoy (zona horaria de Colombia) con el dia de la semana, en formato AAAA-MM-DD. \
            Usala cuando el cliente diga 'manana', 'el viernes' o 'la proxima semana' para calcular la fecha exacta de entrega; \
            no supongas la fecha por tu cuenta.""")
    @Transactional(readOnly = true)
    public String obtenerFechaActual() {
        LocalDate hoy = LocalDate.now(ZONA);
        return hoy + " (" + hoy.getDayOfWeek().getDisplayName(TextStyle.FULL, Locale.forLanguageTag("es-CO")) + ")";
    }

    @Tool(description = """
            Crea un pedido nuevo en estado BORRADOR (carrito) para un cliente ya registrado. Aun no descuenta stock ni compromete nada. \
            Requiere el id del cliente (de buscarClientePorTelefono o registrarCliente), la fecha de entrega y la franja horaria. \
            La entrega debe ser desde manana hasta 14 dias adelante. Un cliente solo puede tener un borrador a la vez: \
            si ya tiene uno, falla indicando su id para que sigas con ese. Despues agrega productos con agregarProducto. \
            Devuelve el pedido con su id.""")
    public PedidoDto crearPedidoBorrador(
            @ToolParam(description = "Id del cliente") Integer clienteId,
            @ToolParam(description = "Fecha de entrega en formato AAAA-MM-DD, por ejemplo 2026-11-20") String fechaEntrega,
            @ToolParam(description = "Franja de entrega: MANANA, TARDE o NOCHE") String franja,
            @ToolParam(description = "Direccion de entrega. Dejar vacio para usar la direccion registrada del cliente", required = false) String direccionEntrega) {
        if (clienteId == null) {
            throw new ReglaNegocioException("Falta el id del cliente. Usa buscarClientePorTelefono para obtenerlo.");
        }
        var cliente = clientes.findById(clienteId).orElseThrow(() -> new ReglaNegocioException(
                "No existe un cliente con id " + clienteId + ". Usa buscarClientePorTelefono o registrarCliente."));
        LocalDate fecha = parsearFecha(fechaEntrega);
        Franja fr = parsearFranja(franja);
        String dir = direccionEntrega == null || direccionEntrega.isBlank() ? cliente.getDireccion()
                : direccionEntrega.trim();
        if (dir.length() > 200) {
            throw new ReglaNegocioException("La direccion es demasiado larga (maximo 200 caracteres).");
        }
        Optional<Pedido> abierto = pedidos.findByClienteIdOrderByIdDesc(clienteId).stream()
                .filter(p -> p.getEstado() == EstadoPedido.BORRADOR).findFirst();
        if (abierto.isPresent()) {
            throw new ReglaNegocioException("El cliente ya tiene el pedido borrador " + abierto.get().getId()
                    + ". Sigue agregando productos a ese pedido con agregarProducto.");
        }
        return PedidoDto.de(pedidos.saveAndFlush(new Pedido(cliente, dir, fecha, fr)));
    }

    @Tool(description = """
            Agrega un producto a un pedido en BORRADOR. Si el producto ya estaba en el pedido, suma la cantidad. \
            Verifica que el producto se venda y que haya stock suficiente (el stock se descuenta de verdad solo al confirmar). \
            Usa el id real de buscarProductos; nunca inventes ids. La cantidad es un numero entero entre 1 y 50. \
            Devuelve el pedido completo con sus productos y el total actualizado.""")
    public PedidoDto agregarProducto(
            @ToolParam(description = "Id del pedido borrador") Integer pedidoId,
            @ToolParam(description = "Id del producto, obtenido con buscarProductos") Integer productoId,
            @ToolParam(description = "Cantidad a agregar (entero entre 1 y 50)") Integer cantidad) {
        if (pedidoId == null || productoId == null) {
            throw new ReglaNegocioException("Faltan datos: se necesitan pedidoId y productoId.");
        }
        if (cantidad == null || cantidad < 1 || cantidad > MAX_CANTIDAD_POR_PRODUCTO) {
            throw new ReglaNegocioException("La cantidad debe ser un numero entero entre 1 y "
                    + MAX_CANTIDAD_POR_PRODUCTO + ". Pregunta al cliente cuantas unidades quiere.");
        }
        Pedido pedido = cargarBorrador(pedidoId);
        Producto producto = productos.findConCategoriaById(productoId).orElseThrow(() -> new ReglaNegocioException(
                "No existe un producto con id " + productoId + ". Usa buscarProductos para encontrar el id correcto."));
        if (!producto.getActivo()) {
            throw new ReglaNegocioException("El producto " + productoId + " (" + producto.getNombre()
                    + ") ya no se vende. Ofrece una alternativa con buscarProductos.");
        }
        Optional<DetallePedido> existente = buscarLinea(pedido, productoId);
        int yaEnPedido = existente.map(DetallePedido::getCantidad).orElse(0);
        int nueva = yaEnPedido + cantidad;
        if (producto.getStock() <= 0) {
            throw new ReglaNegocioException(
                    producto.getNombre() + " esta agotado. Ofrece una alternativa con buscarProductos.");
        }
        if (nueva > producto.getStock()) {
            String yaTiene = yaEnPedido > 0 ? " y el pedido ya tiene " + yaEnPedido : "";
            throw new ReglaNegocioException("Solo hay " + producto.getStock() + " unidades de " + producto.getNombre()
                    + yaTiene + ". Ofrece esa cantidad como maximo o una alternativa con buscarProductos.");
        }
        if (existente.isPresent()) {
            existente.get().setCantidad(nueva);
        } else {
            pedido.agregarDetalle(new DetallePedido(pedido, producto, cantidad, producto.getPrecio()));
        }
        recalcularTotal(pedido);
        return PedidoDto.de(pedidos.saveAndFlush(pedido));
    }

    @Tool(description = """
            Quita un producto de un pedido en BORRADOR. Si no se indica cantidad, quita el producto completo; \
            si se indica una cantidad menor a la que hay en el pedido, solo la reduce. \
            Devuelve el pedido completo con el total actualizado.""")
    public PedidoDto quitarProducto(
            @ToolParam(description = "Id del pedido borrador") Integer pedidoId,
            @ToolParam(description = "Id del producto a quitar") Integer productoId,
            @ToolParam(description = "Cantidad a quitar. Dejar vacio para quitar el producto completo", required = false) Integer cantidad) {
        if (pedidoId == null || productoId == null) {
            throw new ReglaNegocioException("Faltan datos: se necesitan pedidoId y productoId.");
        }
        if (cantidad != null && cantidad < 1) {
            throw new ReglaNegocioException("La cantidad a quitar debe ser un entero mayor o igual a 1.");
        }
        Pedido pedido = cargarBorrador(pedidoId);
        DetallePedido linea = buscarLinea(pedido, productoId).orElseThrow(() -> new ReglaNegocioException(
                "El producto " + productoId + " no esta en el pedido " + pedidoId
                        + ". Revisa los productos del pedido antes de quitar."));
        if (cantidad == null || cantidad >= linea.getCantidad()) {
            pedido.quitarDetalle(linea);
        } else {
            linea.setCantidad(linea.getCantidad() - cantidad);
        }
        recalcularTotal(pedido);
        return PedidoDto.de(pedidos.saveAndFlush(pedido));
    }

    // ---------- utilidades privadas ----------

    /** Carga el pedido bloqueando la fila (dos cambios simultaneos al mismo pedido se atienden uno tras otro). */
    private Pedido cargarBorrador(Integer pedidoId) {
        Pedido pedido = pedidos.findByIdParaActualizar(pedidoId).orElseThrow(() -> new ReglaNegocioException(
                "No existe el pedido " + pedidoId + ". Usa el id que devolvio crearPedidoBorrador."));
        if (pedido.getEstado() != EstadoPedido.BORRADOR) {
            throw new ReglaNegocioException("El pedido " + pedidoId + " esta " + pedido.getEstado()
                    + " y ya no se puede modificar. Solo los pedidos en BORRADOR admiten cambios.");
        }
        return pedido;
    }

    private Optional<DetallePedido> buscarLinea(Pedido pedido, Integer productoId) {
        return pedido.getDetalles().stream().filter(d -> d.getProducto().getId().equals(productoId)).findFirst();
    }

    private void recalcularTotal(Pedido pedido) {
        pedido.setTotal(pedido.getDetalles().stream().map(DetallePedido::getSubtotal).reduce(BigDecimal.ZERO,
                BigDecimal::add));
    }

    private LocalDate parsearFecha(String texto) {
        LocalDate fecha;
        try {
            fecha = LocalDate.parse(texto == null ? "" : texto.trim());
        } catch (DateTimeParseException e) {
            throw new ReglaNegocioException(
                    "La fecha debe tener formato AAAA-MM-DD (por ejemplo 2026-11-20). Usa obtenerFechaActual si necesitas calcularla.");
        }
        LocalDate hoy = LocalDate.now(ZONA);
        if (fecha.isBefore(hoy.plusDays(1)) || fecha.isAfter(hoy.plusDays(MAX_DIAS_ADELANTE))) {
            throw new ReglaNegocioException("La fecha de entrega debe estar entre " + hoy.plusDays(1) + " y "
                    + hoy.plusDays(MAX_DIAS_ADELANTE) + " (desde manana hasta " + MAX_DIAS_ADELANTE
                    + " dias adelante). Pidele al cliente otra fecha.");
        }
        return fecha;
    }

    private Franja parsearFranja(String texto) {
        // Acepta variantes como MANANA, manana o Tarde: quita tildes y pasa a mayusculas.
        String limpio = texto == null ? ""
                : Normalizer.normalize(texto.trim(), Normalizer.Form.NFD).replaceAll("\\p{M}", "").toUpperCase(Locale.ROOT);
        try {
            return Franja.valueOf(limpio);
        } catch (IllegalArgumentException e) {
            throw new ReglaNegocioException("La franja debe ser MANANA, TARDE o NOCHE. Preguntale al cliente cual prefiere.");
        }
    }
}