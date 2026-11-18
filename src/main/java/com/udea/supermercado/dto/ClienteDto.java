package com.udea.supermercado.dto;

import com.udea.supermercado.model.Cliente;

public record ClienteDto(Integer id, String nombre, String telefono, String direccion) {

    public static ClienteDto de(Cliente c) {
        return new ClienteDto(c.getId(), c.getNombre(), c.getTelefono(), c.getDireccion());
    }
}