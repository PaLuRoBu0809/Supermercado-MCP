package com.udea.supermercado.service;

import java.util.List;
import java.util.Locale;

import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.udea.supermercado.dto.ClienteDto;
import com.udea.supermercado.dto.ProductoDto;
import com.udea.supermercado.exception.ReglaNegocioException;
import com.udea.supermercado.repository.ClienteRepository;
import com.udea.supermercado.repository.ProductoRepository;

/**
 * Herramientas MCP de CONSULTA (no modifican datos): catalogo y clientes.
 * Las descripciones de @Tool y @ToolParam son lo que lee el modelo para decidir cuando y como llamarlas.
 */
@Service
@Transactional(readOnly = true)
public class CatalogoToolsService {

    private final ProductoRepository productos;
    private final ClienteRepository clientes;

    public CatalogoToolsService(ProductoRepository productos, ClienteRepository clientes) {
        this.productos = productos;
        this.clientes = clientes;
    }

    @Tool(description = """
            Busca productos del supermercado por fragmento del nombre o de la categoria (no distingue mayusculas ni tildes). \
            Devuelve para cada producto: id, nombre, categoria, precio en pesos colombianos (COP), unidad de venta \
            ('unidad' o 'kg') y stock disponible. Solo incluye productos que se venden actualmente; si el stock es 0 \
            el producto existe pero esta agotado. Si no hay coincidencias devuelve una lista vacia: en ese caso \
            prueba con otra palabra o pregunta al cliente que desea. Usala SIEMPRE para conocer el id y el precio reales \
            antes de agregar algo a un pedido; nunca inventes ids ni precios.""")
    public List<ProductoDto> buscarProductos(
            @ToolParam(description = "Palabra o fragmento a buscar, por ejemplo 'leche', 'cafe' o 'bebidas'") String texto) {
        String limpio = texto == null ? "" : texto.replaceAll("[%_]", "").trim().toLowerCase(Locale.ROOT);
        if (limpio.isEmpty()) {
            throw new ReglaNegocioException(
                    "Indica que producto o categoria buscar (por ejemplo: leche, cafe, bebidas).");
        }
        return productos.buscarActivos("%" + limpio + "%").stream().map(ProductoDto::de).toList();
    }

    @Tool(description = """
            Consulta un producto por su id: nombre, categoria, precio en COP, unidad de venta y stock actual. \
            Sirve para confirmar precio o disponibilidad de un producto cuyo id ya se conoce. \
            Si no se conoce el id, usa primero buscarProductos.""")
    public ProductoDto consultarProducto(
            @ToolParam(description = "Id numerico del producto, obtenido con buscarProductos") Integer productoId) {
        if (productoId == null) {
            throw new ReglaNegocioException("Falta el id del producto. Usa buscarProductos para encontrarlo.");
        }
        var producto = productos.findConCategoriaById(productoId).orElseThrow(() -> new ReglaNegocioException(
                "No existe un producto con id " + productoId + ". Usa buscarProductos para encontrar el id correcto."));
        if (!producto.getActivo()) {
            throw new ReglaNegocioException(
                    "El producto " + productoId + " (" + producto.getNombre() + ") ya no se vende. Ofrece una alternativa con buscarProductos.");
        }
        return ProductoDto.de(producto);
    }

    @Tool(description = """
            Busca un cliente registrado por su numero de telefono (10 digitos; se ignoran espacios, guiones y el prefijo +57). \
            Devuelve id, nombre, telefono y direccion. Es el primer paso para identificar a quien hace el pedido. \
            Si no existe, falla con un mensaje: en ese caso pide al cliente su nombre y direccion para registrarlo.""")
    public ClienteDto buscarClientePorTelefono(
            @ToolParam(description = "Telefono celular del cliente, por ejemplo 3001112233") String telefono) {
        String digitos = telefono == null ? "" : telefono.replaceAll("\\D", "");
        if (digitos.length() == 12 && digitos.startsWith("57")) {
            digitos = digitos.substring(2);
        }
        if (digitos.length() != 10) {
            throw new ReglaNegocioException(
                    "El telefono debe tener 10 digitos (por ejemplo 3001112233). Pidele al cliente que lo confirme.");
        }
        String numero = digitos;
        return clientes.findByTelefono(numero).map(ClienteDto::de).orElseThrow(() -> new ReglaNegocioException(
                "No hay un cliente registrado con el telefono " + numero
                        + ". Pide al cliente su nombre y direccion para registrarlo."));
    }
}