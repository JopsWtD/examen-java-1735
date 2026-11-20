package com.carmotors.sgt.service;

import com.carmotors.sgt.exception.DatoDuplicadoException;
import com.carmotors.sgt.exception.EntidadNoEncontradaException;
import com.carmotors.sgt.exception.ValidacionException;
import com.carmotors.sgt.model.Cliente;
import com.carmotors.sgt.dao.ClienteDAO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Pruebas unitarias en memoria para {@link ClienteService} (RF-17, RN-13, RN-17).
 */
class ClienteServiceTest {

    private ClienteDAOEnMemoria clienteRepository;
    private ClienteService clienteService;

    @BeforeEach
    void configurar() {
        clienteRepository = new ClienteDAOEnMemoria();
        clienteService = new ClienteService(clienteRepository);
    }

    @Test
    @DisplayName("Debe registrar un cliente válido normalizando datos y activo en true")
    void registrarClienteValido() {
        Cliente nuevo = new Cliente(0, "  María Rojas  ", " 1094567801 ", "  ", " Maria.Rojas@Correo.com ", false);
        Cliente guardado = clienteService.registrarCliente(nuevo);

        assertEquals(1, guardado.getId());
        assertEquals("María Rojas", guardado.getNombre());
        assertEquals("1094567801", guardado.getDocumentoIdentidad());
        assertNull(guardado.getTelefono());
        assertEquals("maria.rojas@correo.com", guardado.getEmail());
        assertTrue(guardado.isActivo());
        assertEquals(1, clienteService.listarTodos().size());
    }

    @Test
    @DisplayName("Debe rechazar documento duplicado con DatoDuplicadoException")
    void rechazarDocumentoDuplicado() {
        clienteService.registrarCliente(new Cliente(0, "María Rojas", "1094567801", null, "maria@correo.com", true));

        Cliente repetido = new Cliente(0, "Otro Cliente", "1094567801", null, "otro@correo.com", true);
        assertThrows(DatoDuplicadoException.class, () -> clienteService.registrarCliente(repetido));
    }

    @Test
    @DisplayName("Debe rechazar email duplicado sin distinguir mayúsculas con DatoDuplicadoException")
    void rechazarEmailDuplicado() {
        clienteService.registrarCliente(new Cliente(0, "María Rojas", "1094567801", null, "maria@correo.com", true));

        Cliente repetido = new Cliente(0, "Otro Cliente", "1094567802", null, "MARIA@correo.com", true);
        assertThrows(DatoDuplicadoException.class, () -> clienteService.registrarCliente(repetido));
    }

    @Test
    @DisplayName("Debe rechazar email con formato inválido con ValidacionException")
    void rechazarEmailInvalido() {
        Cliente invalido = new Cliente(0, "Carlos Peña", "1090123402", null, "carlos.correo.com", true);
        assertThrows(ValidacionException.class, () -> clienteService.registrarCliente(invalido));
    }

    @Test
    @DisplayName("Debe rechazar nombre, documento o email vacíos con ValidacionException")
    void rechazarCamposObligatoriosVacios() {
        Cliente sinNombre = new Cliente(0, "   ", "1090123402", null, "carlos@correo.com", true);
        assertThrows(ValidacionException.class, () -> clienteService.registrarCliente(sinNombre));

        Cliente sinDocumento = new Cliente(0, "Carlos Peña", " ", null, "carlos@correo.com", true);
        assertThrows(ValidacionException.class, () -> clienteService.registrarCliente(sinDocumento));

        Cliente sinEmail = new Cliente(0, "Carlos Peña", "1090123402", null, null, true);
        assertThrows(ValidacionException.class, () -> clienteService.registrarCliente(sinEmail));

        assertEquals(0, clienteService.listarTodos().size());
    }

    @Test
    @DisplayName("Debe actualizar datos de contacto conservando su propio documento y email")
    void actualizarClienteExistente() {
        Cliente creado = clienteService.registrarCliente(
                new Cliente(0, "Luisa Díaz", "1093456703", null, "luisa@correo.com", true));

        Cliente cambios = new Cliente(creado.getId(), "Luisa Marcela Díaz", "1093456703",
                "3125551103", "luisa@correo.com", true);
        Cliente actualizado = clienteService.actualizarCliente(cambios);

        assertEquals("Luisa Marcela Díaz", actualizado.getNombre());
        assertEquals("3125551103", actualizado.getTelefono());
        assertEquals("luisa@correo.com", clienteService.buscarPorId(creado.getId()).getEmail());
    }

    @Test
    @DisplayName("Debe rechazar al actualizar un documento o email que pertenece a otro cliente")
    void rechazarActualizacionConDatosDeOtroCliente() {
        clienteService.registrarCliente(new Cliente(0, "Jorge Castillo", "88245604", null, "jorge@correo.com", true));
        Cliente segundo = clienteService.registrarCliente(
                new Cliente(0, "Sandra Vega", "60345205", null, "sandra@correo.com", true));

        Cliente conDocAjeno = new Cliente(segundo.getId(), "Sandra Vega", "88245604", null, "sandra@correo.com", true);
        assertThrows(DatoDuplicadoException.class, () -> clienteService.actualizarCliente(conDocAjeno));

        Cliente conEmailAjeno = new Cliente(segundo.getId(), "Sandra Vega", "60345205", null, "jorge@correo.com", true);
        assertThrows(DatoDuplicadoException.class, () -> clienteService.actualizarCliente(conEmailAjeno));
    }

    @Test
    @DisplayName("Debe desactivar lógicamente sin eliminar el registro y permitir reactivarlo")
    void desactivarYReactivarClienteLogicamente() {
        Cliente creado = clienteService.registrarCliente(
                new Cliente(0, "Pablo Ortiz", "1091234507", null, "pablo@correo.com", true));

        Cliente desactivado = clienteService.desactivarCliente(creado.getId());
        assertFalse(desactivado.isActivo());
        assertEquals(0, clienteService.listarActivos().size());
        assertEquals(1, clienteService.listarTodos().size());

        Cliente reactivado = clienteService.reactivarCliente(creado.getId());
        assertTrue(reactivado.isActivo());
        assertEquals(1, clienteService.listarActivos().size());
    }

    @Test
    @DisplayName("Debe lanzar EntidadNoEncontradaException al buscar o actualizar un cliente inexistente")
    void clienteInexistenteLanzaExcepcion() {
        assertThrows(EntidadNoEncontradaException.class, () -> clienteService.buscarPorId(999));
        assertThrows(EntidadNoEncontradaException.class, () -> clienteService.buscarPorDocumento("000000"));
        assertThrows(EntidadNoEncontradaException.class, () -> clienteService.desactivarCliente(999));

        Cliente fantasma = new Cliente(999, "Nadie", "123", null, "nadie@correo.com", true);
        assertThrows(EntidadNoEncontradaException.class, () -> clienteService.actualizarCliente(fantasma));
    }

    private static class ClienteDAOEnMemoria implements ClienteDAO {

        private final List<Cliente> datos = new ArrayList<>();
        private int secuenciaId = 1;

        @Override
        public List<Cliente> listarTodos() {
            return new ArrayList<>(datos);
        }

        @Override
        public List<Cliente> listarActivos() {
            List<Cliente> activos = new ArrayList<>();
            for (Cliente cliente : datos) {
                if (cliente.isActivo()) {
                    activos.add(cliente);
                }
            }
            return activos;
        }

        @Override
        public Cliente buscarPorId(int id) {
            for (Cliente cliente : datos) {
                if (cliente.getId() == id) {
                    return copiar(cliente);
                }
            }
            return null;
        }

        @Override
        public Cliente buscarPorDocumento(String documento) {
            for (Cliente cliente : datos) {
                if (cliente.getDocumentoIdentidad().equals(documento)) {
                    return copiar(cliente);
                }
            }
            return null;
        }

        @Override
        public Cliente buscarPorEmail(String email) {
            for (Cliente cliente : datos) {
                if (cliente.getEmail().equalsIgnoreCase(email)) {
                    return copiar(cliente);
                }
            }
            return null;
        }

        @Override
        public int guardar(Cliente cliente) {
            int nuevoId = secuenciaId++;
            Cliente copia = copiar(cliente);
            copia.setId(nuevoId);
            datos.add(copia);
            return nuevoId;
        }

        @Override
        public boolean actualizar(Cliente cliente) {
            for (int i = 0; i < datos.size(); i++) {
                if (datos.get(i).getId() == cliente.getId()) {
                    datos.set(i, copiar(cliente));
                    return true;
                }
            }
            return false;
        }

        @Override
        public boolean actualizarEstadoActivo(int id, boolean activo) {
            for (Cliente cliente : datos) {
                if (cliente.getId() == id) {
                    cliente.setActivo(activo);
                    return true;
                }
            }
            return false;
        }

        private Cliente copiar(Cliente origen) {
            return new Cliente(
                    origen.getId(),
                    origen.getNombre(),
                    origen.getDocumentoIdentidad(),
                    origen.getTelefono(),
                    origen.getEmail(),
                    origen.isActivo()
            );
        }
    }
}
