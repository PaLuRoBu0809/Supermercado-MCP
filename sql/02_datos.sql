SET NAMES utf8mb4;
USE supermercado;

INSERT INTO categoria (nombre) VALUES
  ('Lácteos y huevos'),
  ('Frutas y verduras'),
  ('Granos y despensa'),
  ('Bebidas'),
  ('Aseo del hogar');

INSERT INTO producto (nombre, categoria_id, precio, unidad, stock, activo) VALUES
  ('Leche entera 1 L',               1,  4200, 'unidad',  50, 1),
  ('Huevos AA x30',                  1, 18500, 'unidad',  20, 1),
  ('Queso campesino 500 g',          1, 11800, 'unidad',  15, 1),
  ('Yogur natural 1 L',              1,  7500, 'unidad',  25, 1),
  ('Banano',                         2,  3200, 'kg',      60, 1),
  ('Tomate chonto',                  2,  4800, 'kg',      40, 1),
  ('Papa pastusa',                   2,  3500, 'kg',      80, 1),
  ('Cebolla cabezona',               2,  3900, 'kg',      45, 1),
  ('Aguacate Hass',                  2, 12500, 'kg',       3, 1),
  ('Arroz 500 g',                    3,  2600, 'unidad', 100, 1),
  ('Frijol cargamanto 500 g',        3,  7800, 'unidad',  30, 1),
  ('Aceite de girasol 1 L',          3, 14900, 'unidad',  35, 1),
  ('Pasta espagueti 250 g',          3,  2400, 'unidad',  70, 1),
  ('Café molido 250 g',              3,  9800, 'unidad',  40, 1),
  ('Azúcar 1 kg',                    3,  4600, 'unidad',  55, 1),
  ('Galletas de avena (descontinuado)', 3, 3800, 'unidad', 10, 0),
  ('Agua sin gas 600 ml',            4,  2200, 'unidad', 120, 1),
  ('Gaseosa cola 1.5 L',             4,  5900, 'unidad',  45, 1),
  ('Jugo de naranja 1 L',            4,  6800, 'unidad',  30, 1),
  ('Detergente líquido 1 L',         5, 12900, 'unidad',  25, 1),
  ('Papel higiénico x12',            5, 19900, 'unidad',  30, 1),
  ('Jabón de manos 500 ml',          5,  8700, 'unidad',  20, 1);

INSERT INTO cliente (nombre, telefono, direccion) VALUES
  ('Ana Gómez',   '3001112233', 'Calle 10 # 20-30, Medellín'),
  ('Carlos Ruiz', '3004445566', 'Carrera 45 # 12-08, Bello');