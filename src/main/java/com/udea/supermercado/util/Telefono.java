package com.udea.supermercado.util;

import com.udea.supermercado.exception.ReglaNegocioException;

/** Normaliza telefonos celulares colombianos: deja solo los 10 digitos (ignora espacios, guiones y el prefijo 57). */
public final class Telefono {

    private Telefono() {
    }

    public static String normalizar(String telefono) {
        String digitos = telefono == null ? "" : telefono.replaceAll("\\D", "");
        if (digitos.length() == 12 && digitos.startsWith("57")) {
            digitos = digitos.substring(2);
        }
        if (digitos.length() != 10) {
            throw new ReglaNegocioException(
                    "El telefono debe tener 10 digitos (por ejemplo 3001112233). Pidele al cliente que lo confirme.");
        }
        return digitos;
    }
}