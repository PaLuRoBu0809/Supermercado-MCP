package com.udea.supermercado.config;

import org.springframework.ai.tool.ToolCallbackProvider;
import org.springframework.ai.tool.method.MethodToolCallbackProvider;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.udea.supermercado.service.CatalogoToolsService;
import com.udea.supermercado.service.ClienteToolsService;
import com.udea.supermercado.service.PedidoToolsService;

/**
 * Publica como herramientas MCP los metodos @Tool de los servicios.
 * Al agregar un servicio de herramientas nuevo, basta con sumarlo a toolObjects(...).
 */
@Configuration
public class McpToolsConfig {

    @Bean
    public ToolCallbackProvider supermercadoTools(CatalogoToolsService catalogo, ClienteToolsService clientes,
            PedidoToolsService pedidos) {
        return MethodToolCallbackProvider.builder().toolObjects(catalogo, clientes, pedidos).build();
    }
}