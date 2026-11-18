package com.udea.supermercado.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.udea.supermercado.model.Producto;

import jakarta.persistence.LockModeType;

public interface ProductoRepository extends JpaRepository<Producto, Integer> {

    /** Busca productos ACTIVOS por fragmento del nombre o de la categoria (el patron llega ya con %). */
    @Query("""
            select p from Producto p join fetch p.categoria c
            where p.activo = true
              and (lower(p.nombre) like :patron or lower(c.nombre) like :patron)
            order by p.nombre
            """)
    List<Producto> buscarActivos(@Param("patron") String patron);

    @Query("select p from Producto p join fetch p.categoria where p.id = :id")
    Optional<Producto> findConCategoriaById(@Param("id") Integer id);

    /** Lee el producto bloqueando la fila (SELECT ... FOR UPDATE) para descontar stock sin carreras. */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select p from Producto p where p.id = :id")
    Optional<Producto> findByIdParaActualizar(@Param("id") Integer id);
}