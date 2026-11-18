package com.udea.supermercado.dto;

import java.math.BigDecimal;

import com.udea.supermercado.model.Producto;

/** Vista de un producto para el modelo: solo datos simples (nunca la entidad JPA). */
public record ProductoDto(Integer id, String nombre, String categoria, BigDecimal precio, String unidad, Integer stock) {

    public static ProductoDto de(Producto p) {
        return new ProductoDto(p.getId(), p.getNombre(), p.getCategoria().getNombre(), p.getPrecio(), p.getUnidad(),
                p.getStock());
    }
}