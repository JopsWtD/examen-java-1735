package com.carmotors.sgt.service;

import com.carmotors.sgt.exception.EntidadNoEncontradaException;
import com.carmotors.sgt.exception.ValidacionException;
import com.carmotors.sgt.model.Cliente;
import com.carmotors.sgt.model.Vehiculo;
import com.carmotors.sgt.dao.ClienteDAO;
import com.carmotors.sgt.dao.VehiculoDAO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Pruebas unitarias para las búsquedas exactas con normalización de clientes y vehículos (RF-19, RN-13, RN-17, HU-09).
 */
class BusquedaClienteVehiculoTest {

    private ClienteDAOEnMemoria clienteRepository;
    private VehiculoDAOEnMemoria vehiculoRepository;
    private ClienteService clienteService;
    private VehiculoService vehiculoService;

    @BeforeEach
    void setUp() {
        clienteRepository = new ClienteDAOEnMemoria();
        vehiculoRepository = new VehiculoDAOEnMemoria();

        clienteService = new ClienteService(clienteRepository);
        vehiculoService = new VehiculoService(vehiculoRepository, clienteRepository);

        // Cliente activo ID = 1
        clienteRepository.guardar(new Cliente(1, "Mario Bros", "1094567801", "3001112233", "mario@correo.com", true));
        // Cliente inactivo ID = 2
        clienteRepository.guardar(new Cliente(2, "Luigi Bros", "1094567802", "3002223344", "luigi@correo.com", false));

        // Vehículo activo de cliente activo
        vehiculoRepository.guardar(new Vehiculo(1, 1, "ABC123", "Toyota", "Corolla"));
        // Vehículo de cliente inactivo (histórico)
        vehiculoRepository.guardar(new Vehiculo(2, 2, "XYZ789", "Nissan", "Sentra"));
    }

    // --- Búsqueda de Cliente por Documento (RF-19, RN-17, HU-09) ---

    @Test
    @DisplayName("Debe buscar cliente activo por documento normalizando espacios")
    void buscarClienteActivoPorDocumentoConEspacios() {
        Cliente encontrado = clienteService.buscarPorDocumento("   1094567801   ");

        assertNotNull(encontrado);
        assertEquals(1, encontrado.getId());
        assertEquals("1094567801", encontrado.getDocumentoIdentidad());
        assertEquals("Mario Bros", encontrado.getNombre());
        assertTrue(encontrado.isActivo());
    }

    @Test
    @DisplayName("Debe permitir buscar y encontrar clientes inactivos por documento para preservar historial (HU-09)")
    void buscarClienteInactivoPorDocumento() {
        Cliente encontrado = clienteService.buscarPorDocumento("1094567802");

        assertNotNull(encontrado);
        assertEquals(2, encontrado.getId());
        assertEquals("1094567802", encontrado.getDocumentoIdentidad());
        assertFalse(encontrado.isActivo());
    }

    @Test
    @DisplayName("Debe lanzar ValidacionException si el documento está vacío o solo contiene espacios")
    void buscarClienteDocumentoVacio() {
        assertThrows(ValidacionException.class, () -> clienteService.buscarPorDocumento(""));
        assertThrows(ValidacionException.class, () -> clienteService.buscarPorDocumento("   "));
        assertThrows(ValidacionException.class, () -> clienteService.buscarPorDocumento(null));
    }

    @Test
    @DisplayName("Debe lanzar EntidadNoEncontradaException si el documento no existe en BD")
    void buscarClienteDocumentoInexistente() {
        EntidadNoEncontradaException ex = assertThrows(
                EntidadNoEncontradaException.class,
                () -> clienteService.buscarPorDocumento("9999999999")
        );
        assertTrue(ex.getMessage().contains("9999999999"));
    }

    // --- Búsqueda de Vehículo por Placa (RF-19, RN-13, HU-09) ---

    @Test
    @DisplayName("Debe buscar vehículo por placa normalizando minúsculas, espacios y guiones (RN-13)")
    void buscarVehiculoPlacaConGuionYMinusculas() {
        Vehiculo v1 = vehiculoService.buscarPorPlaca("  abc-123  ");
        assertNotNull(v1);
        assertEquals("ABC123", v1.getPlaca());
        assertEquals("Toyota", v1.getMarca());

        Vehiculo v2 = vehiculoService.buscarPorPlaca("xyz 789");
        assertNotNull(v2);
        assertEquals("XYZ789", v2.getPlaca());
        assertEquals("Nissan", v2.getMarca());
    }

    @Test
    @DisplayName("Debe lanzar ValidacionException si la placa tiene formato inválido o está vacía")
    void buscarVehiculoPlacaFormatoInvalido() {
        assertThrows(ValidacionException.class, () -> vehiculoService.buscarPorPlaca(""));
        assertThrows(ValidacionException.class, () -> vehiculoService.buscarPorPlaca("   "));
        assertThrows(ValidacionException.class, () -> vehiculoService.buscarPorPlaca("A"));
        assertThrows(ValidacionException.class, () -> vehiculoService.buscarPorPlaca("1234"));
        assertThrows(ValidacionException.class, () -> vehiculoService.buscarPorPlaca("TOOLONNGGG123"));
    }

    @Test
    @DisplayName("Debe lanzar EntidadNoEncontradaException si la placa es válida pero no existe")
    void buscarVehiculoPlacaInexistente() {
        EntidadNoEncontradaException ex = assertThrows(
                EntidadNoEncontradaException.class,
                () -> vehiculoService.buscarPorPlaca("KTM999")
        );
        assertTrue(ex.getMessage().contains("KTM999"));
    }

    @Test
    @DisplayName("Debe asociar y permitir consultar al cliente propietario del vehículo encontrado")
    void asociarClientePropietarioDeVehiculo() {
        Vehiculo vehiculo = vehiculoService.buscarPorPlaca("abc123");
        assertNotNull(vehiculo);

        Cliente propietario = clienteService.buscarPorId(vehiculo.getClienteId());
        assertNotNull(propietario);
        assertEquals(1, propietario.getId());
        assertEquals("Mario Bros", propietario.getNombre());
        assertTrue(propietario.isActivo());
    }

    // --- Repositorios en memoria para pruebas desacopladas ---

    private static class ClienteDAOEnMemoria implements ClienteDAO {
        private final List<Cliente> datos = new ArrayList<>();

        @Override
        public List<Cliente> listarTodos() {
            return new ArrayList<>(datos);
        }

        @Override
        public List<Cliente> listarActivos() {
            List<Cliente> activos = new ArrayList<>();
            for (Cliente c : datos) {
                if (c.isActivo()) {
                    activos.add(c);
                }
            }
            return activos;
        }

        @Override
        public Cliente buscarPorId(int id) {
            for (Cliente c : datos) {
                if (c.getId() == id) {
                    return new Cliente(c.getId(), c.getNombre(), c.getDocumentoIdentidad(),
                            c.getTelefono(), c.getEmail(), c.isActivo());
                }
            }
            return null;
        }

        @Override
        public Cliente buscarPorDocumento(String documento) {
            for (Cliente c : datos) {
                if (c.getDocumentoIdentidad().equals(documento)) {
                    return new Cliente(c.getId(), c.getNombre(), c.getDocumentoIdentidad(),
                            c.getTelefono(), c.getEmail(), c.isActivo());
                }
            }
            return null;
        }

        @Override
        public Cliente buscarPorEmail(String email) {
            for (Cliente c : datos) {
                if (c.getEmail().equalsIgnoreCase(email)) {
                    return new Cliente(c.getId(), c.getNombre(), c.getDocumentoIdentidad(),
                            c.getTelefono(), c.getEmail(), c.isActivo());
                }
            }
            return null;
        }

        @Override
        public int guardar(Cliente cliente) {
            datos.add(cliente);
            return cliente.getId();
        }

        @Override
        public boolean actualizar(Cliente cliente) {
            return true;
        }

        @Override
        public boolean actualizarEstadoActivo(int id, boolean activo) {
            return true;
        }
    }

    private static class VehiculoDAOEnMemoria implements VehiculoDAO {
        private final List<Vehiculo> datos = new ArrayList<>();

        @Override
        public int guardar(Vehiculo vehiculo) {
            datos.add(vehiculo);
            return vehiculo.getId();
        }

        @Override
        public Vehiculo buscarPorId(int id) {
            for (Vehiculo v : datos) {
                if (v.getId() == id) {
                    return new Vehiculo(v.getId(), v.getClienteId(), v.getPlaca(), v.getMarca(), v.getModelo());
                }
            }
            return null;
        }

        @Override
        public Vehiculo buscarPorPlaca(String placa) {
            for (Vehiculo v : datos) {
                if (v.getPlaca().equalsIgnoreCase(placa)) {
                    return new Vehiculo(v.getId(), v.getClienteId(), v.getPlaca(), v.getMarca(), v.getModelo());
                }
            }
            return null;
        }

        @Override
        public List<Vehiculo> listarPorCliente(int clienteId) {
            List<Vehiculo> list = new ArrayList<>();
            for (Vehiculo v : datos) {
                if (v.getClienteId() == clienteId) {
                    list.add(new Vehiculo(v.getId(), v.getClienteId(), v.getPlaca(), v.getMarca(), v.getModelo()));
                }
            }
            return list;
        }

        @Override
        public List<Vehiculo> listarTodos() {
            return new ArrayList<>(datos);
        }
    }
}
