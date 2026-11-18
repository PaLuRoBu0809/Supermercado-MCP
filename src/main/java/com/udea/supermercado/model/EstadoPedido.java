package com.udea.supermercado.model;

/** Ciclo de vida de un pedido. Debe coincidir con el CHECK ck_pedido_estado de la BD. */
public enum EstadoPedido {
    BORRADOR, CONFIRMADO, EN_CAMINO, ENTREGADO, CANCELADO
}