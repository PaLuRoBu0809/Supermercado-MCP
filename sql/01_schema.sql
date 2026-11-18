CREATE DATABASE IF NOT EXISTS supermercado
  CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci;
USE supermercado;

CREATE TABLE IF NOT EXISTS categoria (
  id     INT AUTO_INCREMENT PRIMARY KEY,
  nombre VARCHAR(60) NOT NULL UNIQUE
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS producto (
  id           INT AUTO_INCREMENT PRIMARY KEY,
  nombre       VARCHAR(120)  NOT NULL,
  categoria_id INT           NOT NULL,
  precio       DECIMAL(10,2) NOT NULL,
  unidad       VARCHAR(20)   NOT NULL DEFAULT 'unidad',
  stock        INT           NOT NULL DEFAULT 0,
  activo       TINYINT(1)    NOT NULL DEFAULT 1,
  version      BIGINT        NOT NULL DEFAULT 0,
  CONSTRAINT fk_producto_categoria FOREIGN KEY (categoria_id) REFERENCES categoria(id),
  CONSTRAINT ck_producto_precio    CHECK (precio > 0),
  CONSTRAINT ck_producto_stock     CHECK (stock >= 0)
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS cliente (
  id        INT AUTO_INCREMENT PRIMARY KEY,
  nombre    VARCHAR(100) NOT NULL,
  telefono  VARCHAR(20)  NOT NULL UNIQUE,
  direccion VARCHAR(200) NOT NULL,
  version   BIGINT       NOT NULL DEFAULT 0
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS pedido (
  id                INT AUTO_INCREMENT PRIMARY KEY,
  cliente_id        INT           NOT NULL,
  estado            VARCHAR(15)   NOT NULL DEFAULT 'BORRADOR',
  direccion_entrega VARCHAR(200)  NOT NULL,
  fecha_entrega     DATE          NOT NULL,
  franja            VARCHAR(10)   NOT NULL,
  total             DECIMAL(12,2) NOT NULL DEFAULT 0,
  creado_en         DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP,
  version           BIGINT        NOT NULL DEFAULT 0,
  CONSTRAINT fk_pedido_cliente FOREIGN KEY (cliente_id) REFERENCES cliente(id),
  CONSTRAINT ck_pedido_estado  CHECK (estado IN ('BORRADOR','CONFIRMADO','EN_CAMINO','ENTREGADO','CANCELADO')),
  CONSTRAINT ck_pedido_franja  CHECK (franja IN ('MANANA','TARDE','NOCHE'))
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS detalle_pedido (
  id              INT AUTO_INCREMENT PRIMARY KEY,
  pedido_id       INT           NOT NULL,
  producto_id     INT           NOT NULL,
  cantidad        INT           NOT NULL,
  precio_unitario DECIMAL(10,2) NOT NULL,
  CONSTRAINT fk_detalle_pedido   FOREIGN KEY (pedido_id)   REFERENCES pedido(id),
  CONSTRAINT fk_detalle_producto FOREIGN KEY (producto_id) REFERENCES producto(id),
  CONSTRAINT uq_detalle_pedido_producto UNIQUE (pedido_id, producto_id),
  CONSTRAINT ck_detalle_cantidad CHECK (cantidad > 0)
) ENGINE=InnoDB;