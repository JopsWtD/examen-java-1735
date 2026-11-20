package com.carmotors.sgt.service;

import com.carmotors.sgt.exception.DatoDuplicadoException;
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
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Pruebas unitarias en memoria para {@link VehiculoService} (RF-18, RN-13, RN-17, HU-09).
 */
class VehiculoServiceTest {

    private VehiculoDAOEnMemoria vehiculoRepository;
    private ClienteDAOEnMemoria clienteRepository;
    private VehiculoService vehiculoService;

    @BeforeEach
    void setUp() {
        vehiculoRepository = new VehiculoDAOEnMemoria();
        clienteRepository = new ClienteDAOEnMemoria();
        vehiculoService = new VehiculoService(vehiculoRepository, clienteRepository);

        // Cliente activo ID = 1
        clienteRepository.guardar(new Cliente(1, "Carlos Gómez", "10101010", "3001234567", "carlos@correo.com", true));
        // Cliente inactivo ID = 2 (RN-17)
        clienteRepository.guardar(new Cliente(2, "Ana López", "20202020", "3007654321", "ana@correo.com", false));
    }

    @Test
    @DisplayName("Debe registrar un vehículo válido normalizando su placa a mayúsculas")
    void registrarVehiculoValido() {
        Vehiculo nuevo = new Vehiculo(1, "abc-123", "  Toyota  ", "  Corolla  ");
        Vehiculo registrado = vehiculoService.registrarVehiculo(nuevo);

        assertNotNull(registrado);
        assertEquals(1, registrado.getId());
        assertEquals(1, registrado.getClienteId());
        assertEquals("ABC123", registrado.getPlaca());
        assertEquals("Toyota", registrado.getMarca());
        assertEquals("Corolla", registrado.getModelo());
        assertEquals(1, vehiculoService.listarTodos().size());
    }

    @Test
    @DisplayName("Debe rechazar el registro si el vehículo es nulo")
    void registrarVehiculoNulo() {
        assertThrows(ValidacionException.class, () -> vehiculoService.registrarVehiculo(null));
    }

    @Test
    @DisplayName("Debe rechazar clienteId menor o igual a cero")
    void registrarClienteIdInvalido() {
        Vehiculo v1 = new Vehiculo(0, "ABC123", "Toyota", "Corolla");
        assertThrows(ValidacionException.class, () -> vehiculoService.registrarVehiculo(v1));

        Vehiculo v2 = new Vehiculo(-5, "ABC123", "Toyota", "Corolla");
        assertThrows(ValidacionException.class, () -> vehiculoService.registrarVehiculo(v2));
    }

    @Test
    @DisplayName("Debe rechazar el registro si el cliente no existe")
    void registrarClienteNoExiste() {
        Vehiculo vehiculo = new Vehiculo(999, "ABC123", "Toyota", "Corolla");
        assertThrows(EntidadNoEncontradaException.class, () -> vehiculoService.registrarVehiculo(vehiculo));
    }

    @Test
    @DisplayName("RN-17: Debe rechazar el registro de un vehículo a un cliente inactivo")
    void registrarVehiculoClienteInactivo() {
        Vehiculo vehiculo = new Vehiculo(2, "XYZ789", "Mazda", "3");
        ValidacionException ex = assertThrows(
                ValidacionException.class,
                () -> vehiculoService.registrarVehiculo(vehiculo)
        );
        assertTrue(ex.getMessage().contains("cliente inactivo"));
    }

    @Test
    @DisplayName("RN-13: Debe rechazar placa duplicada sin distinguir guiones ni minúsculas")
    void registrarPlacaDuplicada() {
        vehiculoService.registrarVehiculo(new Vehiculo(1, "ABC123", "Toyota", "Corolla"));

        Vehiculo duplicado = new Vehiculo(1, "abc-123", "Honda", "Civic");
        DatoDuplicadoException ex = assertThrows(
                DatoDuplicadoException.class,
                () -> vehiculoService.registrarVehiculo(duplicado)
        );
        assertTrue(ex.getMessage().contains("ABC123"));
    }

    @Test
    @DisplayName("Debe rechazar placas con formato inválido")
    void registrarPlacaInvalida() {
        Vehiculo placaCorta = new Vehiculo(1, "AB1", "Ford", "Fiesta");
        assertThrows(ValidacionException.class, () -> vehiculoService.registrarVehiculo(placaCorta));

        Vehiculo placaLarga = new Vehiculo(1, "ABCDEFGHIJKLM", "Ford", "Fiesta");
        assertThrows(ValidacionException.class, () -> vehiculoService.registrarVehiculo(placaLarga));
    }

    @Test
    @DisplayName("Debe rechazar marca o modelo vacíos o que excedan 50 caracteres")
    void registrarMarcaModeloInvalidos() {
        Vehiculo sinMarca = new Vehiculo(1, "ABC123", "   ", "Corolla");
        assertThrows(ValidacionException.class, () -> vehiculoService.registrarVehiculo(sinMarca));

        Vehiculo sinModelo = new Vehiculo(1, "ABC123", "Toyota", "  ");
        assertThrows(ValidacionException.class, () -> vehiculoService.registrarVehiculo(sinModelo));

        String textoLargo = "A".repeat(51);
        Vehiculo marcaLarga = new Vehiculo(1, "ABC123", textoLargo, "Corolla");
        assertThrows(ValidacionException.class, () -> vehiculoService.registrarVehiculo(marcaLarga));

        Vehiculo modeloLargo = new Vehiculo(1, "ABC123", "Toyota", textoLargo);
        assertThrows(ValidacionException.class, () -> vehiculoService.registrarVehiculo(modeloLargo));
    }

    @Test
    @DisplayName("Debe buscar vehículo por ID exitosamente")
    void buscarPorIdExitoso() {
        Vehiculo creado = vehiculoService.registrarVehiculo(new Vehiculo(1, "ABC123", "Toyota", "Corolla"));
        Vehiculo encontrado = vehiculoService.buscarPorId(creado.getId());

        assertNotNull(encontrado);
        assertEquals("ABC123", encontrado.getPlaca());
    }

    @Test
    @DisplayName("Debe lanzar excepción si el ID no existe o es inválido")
    void buscarPorIdInvalido() {
        assertThrows(ValidacionException.class, () -> vehiculoService.buscarPorId(0));
        assertThrows(ValidacionException.class, () -> vehiculoService.buscarPorId(-1));
        assertThrows(EntidadNoEncontradaException.class, () -> vehiculoService.buscarPorId(999));
    }

    @Test
    @DisplayName("Debe buscar vehículo por placa normalizada exitosamente")
    void buscarPorPlacaExitosa() {
        vehiculoService.registrarVehiculo(new Vehiculo(1, "ABC123", "Toyota", "Corolla"));

        Vehiculo encontrado = vehiculoService.buscarPorPlaca("abc-123");
        assertNotNull(encontrado);
        assertEquals("ABC123", encontrado.getPlaca());
    }

    @Test
    @DisplayName("Debe lanzar excepción si la placa no existe o está vacía")
    void buscarPorPlacaInvalida() {
        assertThrows(ValidacionException.class, () -> vehiculoService.buscarPorPlaca("   "));
        assertThrows(EntidadNoEncontradaException.class, () -> vehiculoService.buscarPorPlaca("XYZ999"));
    }

    @Test
    @DisplayName("HU-09: Debe listar los vehículos de un cliente activo")
    void listarVehiculosClienteActivo() {
        vehiculoService.registrarVehiculo(new Vehiculo(1, "ABC123", "Toyota", "Corolla"));
        vehiculoService.registrarVehiculo(new Vehiculo(1, "XYZ789", "Chevrolet", "Spark"));

        List<Vehiculo> lista = vehiculoService.listarPorCliente(1);
        assertEquals(2, lista.size());
        assertEquals("ABC123", lista.get(0).getPlaca());
        assertEquals("XYZ789", lista.get(1).getPlaca());
    }

    @Test
    @DisplayName("HU-09: Debe permitir consultar vehículos de un cliente inactivo para preservar su historial")
    void listarVehiculosClienteInactivoPermitido() {
        // Asignamos directamente en repo un vehículo al cliente inactivo ID 2 para simular historial previo
        vehiculoRepository.guardar(new Vehiculo(2, "OLD123", "Nissan", "Sentra"));

        List<Vehiculo> lista = vehiculoService.listarPorCliente(2);
        assertEquals(1, lista.size());
        assertEquals("OLD123", lista.get(0).getPlaca());
    }

    @Test
    @DisplayName("Debe lanzar excepción al listar vehículos de un cliente que no existe")
    void listarVehiculosClienteNoExiste() {
        assertThrows(EntidadNoEncontradaException.class, () -> vehiculoService.listarPorCliente(999));
    }

    @Test
    @DisplayName("Debe retornar lista vacía si el cliente no tiene vehículos")
    void listarVehiculosClienteSinVehiculos() {
        List<Vehiculo> lista = vehiculoService.listarPorCliente(1);
        assertTrue(lista.isEmpty());
    }

    // --- Repositorios en memoria para pruebas desacopladas de BD ---

    private static class VehiculoDAOEnMemoria implements VehiculoDAO {
        private final List<Vehiculo> datos = new ArrayList<>();
        private int secuenciaId = 1;

        @Override
        public int guardar(Vehiculo vehiculo) {
            int nuevoId = secuenciaId++;
            Vehiculo copia = new Vehiculo(nuevoId, vehiculo.getClienteId(),
                    vehiculo.getPlaca(), vehiculo.getMarca(), vehiculo.getModelo());
            datos.add(copia);
            return nuevoId;
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
            List<Vehiculo> resultado = new ArrayList<>();
            for (Vehiculo v : datos) {
                if (v.getClienteId() == clienteId) {
                    resultado.add(new Vehiculo(v.getId(), v.getClienteId(), v.getPlaca(), v.getMarca(), v.getModelo()));
                }
            }
            return resultado;
        }

        @Override
        public List<Vehiculo> listarTodos() {
            List<Vehiculo> resultado = new ArrayList<>();
            for (Vehiculo v : datos) {
                resultado.add(new Vehiculo(v.getId(), v.getClienteId(), v.getPlaca(), v.getMarca(), v.getModelo()));
            }
            return resultado;
        }
    }

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
                    return c;
                }
            }
            return null;
        }

        @Override
        public Cliente buscarPorEmail(String email) {
            for (Cliente c : datos) {
                if (c.getEmail().equalsIgnoreCase(email)) {
                    return c;
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
            for (Cliente c : datos) {
                if (c.getId() == id) {
                    c.setActivo(activo);
                    return true;
                }
            }
            return false;
        }
    }
}
