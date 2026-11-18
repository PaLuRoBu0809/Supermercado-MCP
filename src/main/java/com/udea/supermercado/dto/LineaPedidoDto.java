package com.udea.supermercado.dto;

import java.math.BigDecimal;

import com.udea.supermercado.model.DetallePedido;

public record LineaPedidoDto(Integer productoId, String nombre, Integer cantidad, String unidad,
        BigDecimal precioUnitario, BigDecimal subtotal) {

    public static LineaPedidoDto de(DetallePedido d) {
        return new LineaPedidoDto(d.getProducto().getId(), d.getProducto().getNombre(), d.getCantidad(),
                d.getProducto().getUnidad(), d.getPrecioUnitario(), d.getSubtotal());
    }
}