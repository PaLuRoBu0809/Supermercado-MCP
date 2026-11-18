package com.udea.supermercado.exception;

/**
 * Error de regla de negocio: el mensaje es lo que lee el modelo (y, a traves de el, el cliente).
 * Convencion del proyecto: mensaje corto, en español, que diga QUE paso y QUE hacer a continuacion.
 * Nunca incluye nombres de clases Java ni trazas.
 */
public class ReglaNegocioException extends RuntimeException {

    public ReglaNegocioException(String mensaje) {
        super(mensaje);
    }
}