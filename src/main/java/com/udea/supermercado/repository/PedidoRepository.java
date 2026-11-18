package com.udea.supermercado.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.udea.supermercado.model.Pedido;

import jakarta.persistence.LockModeType;

public interface PedidoRepository extends JpaRepository<Pedido, Integer> {

    List<Pedido> findByClienteIdOrderByIdDesc(Integer clienteId);

    /** Lee el pedido bloqueando la fila: confirmar y cancelar no deben ejecutarse a la vez sobre el mismo pedido. */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select p from Pedido p where p.id = :id")
    Optional<Pedido> findByIdParaActualizar(@Param("id") Integer id);
}