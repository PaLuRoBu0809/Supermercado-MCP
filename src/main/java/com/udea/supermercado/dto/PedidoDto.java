package com.udea.supermercado.dto;

import java.math.BigDecimal;
import java.util.List;

import com.udea.supermercado.model.Pedido;

public record PedidoDto(Integer id, Integer clienteId, String cliente, String estado, String direccionEntrega,
        String fechaEntrega, String franja, BigDecimal total, List<LineaPedidoDto> productos) {

    /** Debe llamarse dentro de la transaccion: recorre relaciones LAZY (cliente, detalles, productos). */
    public static PedidoDto de(Pedido p) {
        return new PedidoDto(p.getId(), p.getCliente().getId(), p.getCliente().getNombre(), p.getEstado().name(),
                p.getDireccionEntrega(), p.getFechaEntrega().toString(), p.getFranja().name(), p.getTotal(),
                p.getDetalles().stream().map(LineaPedidoDto::de).toList());
    }
}