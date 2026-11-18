package com.udea.supermercado.service;

import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.udea.supermercado.dto.ClienteDto;
import com.udea.supermercado.exception.ReglaNegocioException;
import com.udea.supermercado.model.Cliente;
import com.udea.supermercado.repository.ClienteRepository;
import com.udea.supermercado.util.Telefono;

/** Herramienta MCP de ESCRITURA para clientes. */
@Service
@Transactional
public class ClienteToolsService {

    private final ClienteRepository clientes;

    public ClienteToolsService(ClienteRepository clientes) {
        this.clientes = clientes;
    }

    @Tool(description = """
            Registra un cliente nuevo. Usala SOLO cuando buscarClientePorTelefono indique que no existe un cliente con ese telefono \
            y el cliente ya te haya dado su nombre y su direccion de entrega. Devuelve el cliente creado con su id. \
            Si el telefono ya esta registrado falla: en ese caso usa buscarClientePorTelefono. No inventes ni completes datos.""")
    public ClienteDto registrarCliente(
            @ToolParam(description = "Nombre completo del cliente") String nombre,
            @ToolParam(description = "Telefono celular de 10 digitos, por ejemplo 3001112233") String telefono,
            @ToolParam(description = "Direccion de entrega habitual, con barrio o municipio") String direccion) {
        String nom = nombre == null ? "" : nombre.trim();
        String dir = direccion == null ? "" : direccion.trim();
        if (nom.isEmpty()) {
            throw new ReglaNegocioException("Falta el nombre del cliente. Preguntaselo antes de registrarlo.");
        }
        if (nom.length() > 100) {
            throw new ReglaNegocioException("El nombre es demasiado largo (maximo 100 caracteres).");
        }
        if (dir.isEmpty()) {
            throw new ReglaNegocioException("Falta la direccion del cliente. Preguntasela antes de registrarlo.");
        }
        if (dir.length() > 200) {
            throw new ReglaNegocioException("La direccion es demasiado larga (maximo 200 caracteres).");
        }
        String tel = Telefono.normalizar(telefono);
        if (clientes.existsByTelefono(tel)) {
            throw new ReglaNegocioException("Ya hay un cliente registrado con el telefono " + tel
                    + ". Usa buscarClientePorTelefono para obtener sus datos.");
        }
        try {
            return ClienteDto.de(clientes.saveAndFlush(new Cliente(nom, tel, dir)));
        } catch (DataIntegrityViolationException e) {
            // Dos solicitudes simultaneas con el mismo telefono: la restriccion UNIQUE de la BD decide quien gana.
            throw new ReglaNegocioException("Ya hay un cliente registrado con el telefono " + tel
                    + ". Usa buscarClientePorTelefono para obtener sus datos.");
        }
    }
}