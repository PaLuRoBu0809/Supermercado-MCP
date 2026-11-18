package com.udea.supermercado.model;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

@Entity
@Table(name = "pedido")
public class Pedido {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "cliente_id")
    private Cliente cliente;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(length = 15)
    private EstadoPedido estado = EstadoPedido.BORRADOR;

    @NotBlank
    @Size(max = 200)
    private String direccionEntrega;

    @NotNull
    private LocalDate fechaEntrega;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(length = 10)
    private Franja franja;

    @NotNull
    @Column(precision = 12, scale = 2)
    private BigDecimal total = BigDecimal.ZERO;

    @NotNull
    private LocalDateTime creadoEn = LocalDateTime.now();

    @Version
    private Long version;

    @OneToMany(mappedBy = "pedido", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<DetallePedido> detalles = new ArrayList<>();

    protected Pedido() {
    }

    public Pedido(Cliente cliente, String direccionEntrega, LocalDate fechaEntrega, Franja franja) {
        this.cliente = cliente;
        this.direccionEntrega = direccionEntrega;
        this.fechaEntrega = fechaEntrega;
        this.franja = franja;
    }

    public Integer getId() {
        return id;
    }

    public Cliente getCliente() {
        return cliente;
    }

    public EstadoPedido getEstado() {
        return estado;
    }

    public void setEstado(EstadoPedido estado) {
        this.estado = estado;
    }

    public String getDireccionEntrega() {
        return direccionEntrega;
    }

    public LocalDate getFechaEntrega() {
        return fechaEntrega;
    }

    public Franja getFranja() {
        return franja;
    }

    public BigDecimal getTotal() {
        return total;
    }

    public void setTotal(BigDecimal total) {
        this.total = total;
    }

    public LocalDateTime getCreadoEn() {
        return creadoEn;
    }

    public List<DetallePedido> getDetalles() {
        return detalles;
    }

    public void agregarDetalle(DetallePedido detalle) {
        detalles.add(detalle);
    }

    public void quitarDetalle(DetallePedido detalle) {
        detalles.remove(detalle);
    }
}