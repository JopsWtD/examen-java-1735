package com.carmotors.sgt.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.carmotors.sgt.exception.EntidadNoEncontradaException;
import com.carmotors.sgt.exception.ValidacionException;
import com.carmotors.sgt.model.Cliente;
import com.carmotors.sgt.model.DetalleTecnico;
import com.carmotors.sgt.model.Factura;
import com.carmotors.sgt.model.FiltroOrdenServicio;
import com.carmotors.sgt.model.HistorialCliente;
import com.carmotors.sgt.model.OrdenServicioDetalle;
import com.carmotors.sgt.model.ResumenOrdenHistorial;
import com.carmotors.sgt.model.ResumenOrdenServicio;
import com.carmotors.sgt.model.Vehiculo;
import com.carmotors.sgt.dao.ClienteDAO;
import com.carmotors.sgt.dao.ConsultaOrdenServicioDAO;
import com.carmotors.sgt.dao.FacturaDAO;
import com.carmotors.sgt.dao.OrdenServicioDetalleDAO;
import com.carmotors.sgt.dao.VehiculoDAO;

/**
 * Pruebas del historial de servicio del cliente (T7.4, RF-20, RN-17, RNF-11, HU-10)
 * con DAOs en memoria.
 */
class HistorialClienteServiceTest {

    private static final int CLIENTE_ACTIVO = 1;
    private static final int CLIENTE_INACTIVO = 2;
    private static final int CLIENTE_SIN_DATOS = 3;

    private HistorialClienteService service;
    private ClienteDAOMock clienteDAO;
    private VehiculoDAOMock vehiculoDAO;
    private ConsultaOrdenDAOMock consultaDAO;
    private DetalleDAOMock detalleDAO;
    private FacturaDAOMock facturaDAO;

    @BeforeEach
    void setUp() {
        clienteDAO = new ClienteDAOMock();
        clienteDAO.agregar(new Cliente(CLIENTE_ACTIVO, "María Rojas", "1094567801", "3105551101",
                "maria@correo.com", true));
        clienteDAO.agregar(new Cliente(CLIENTE_INACTIVO, "Pablo Ortiz", "1091234507", "3165551107",
                "pablo@correo.com", false));
        clienteDAO.agregar(new Cliente(CLIENTE_SIN_DATOS, "Ana Ruiz", "1098888000", "3001112233",
                "ana@correo.com", true));

        vehiculoDAO = new VehiculoDAOMock();
        vehiculoDAO.agregar(new Vehiculo(10, CLIENTE_ACTIVO, "ABC123", "Chevrolet", "Spark GT"));
        vehiculoDAO.agregar(new Vehiculo(11, CLIENTE_ACTIVO, "XYZ789", "Mazda", "2"));
        vehiculoDAO.agregar(new Vehiculo(20, CLIENTE_INACTIVO, "OLD999", "Renault", "Logan"));

        consultaDAO = new ConsultaOrdenDAOMock();
        consultaDAO.agregar(new ResumenOrdenServicio(101, 5, "ABC123",
                LocalDateTime.of(2026, 5, 1, 8, 30)));
        consultaDAO.agregar(new ResumenOrdenServicio(102, 4, "ABC123",
                LocalDateTime.of(2026, 6, 1, 9, 0)));
        consultaDAO.agregar(new ResumenOrdenServicio(103, 5, "XYZ789",
                LocalDateTime.of(2026, 6, 1, 10, 0)));

        detalleDAO = new DetalleDAOMock();
        detalleDAO.agregar(new OrdenServicioDetalle(1001, 101, 1, "Cambio de aceite",
                new BigDecimal("60000.00"), null));
        detalleDAO.agregar(new OrdenServicioDetalle(1002, 102, 2, "Revisión de frenos",
                new BigDecimal("90000.00"), null));
        detalleDAO.agregar(new OrdenServicioDetalle(1003, 102, 3, "Alineación",
                new BigDecimal("45000.00"), null));

        facturaDAO = new FacturaDAOMock();
        facturaDAO.agregar(factura(500, 102, "F-2026-0001", new BigDecimal("233750.00")));

        service = new HistorialClienteService(clienteDAO, vehiculoDAO, consultaDAO, detalleDAO, facturaDAO);
    }

    private Factura factura(int id, int ordenId, String numero, BigDecimal totalFinal) {
        Factura factura = new Factura();
        factura.setId(id);
        factura.setOrdenId(ordenId);
        factura.setNumeroFactura(numero);
        factura.setTotalFinal(totalFinal);
        return factura;
    }

    @Test
    @DisplayName("Ordena las órdenes de la más reciente a la más antigua con desempate por ID (RNF-11)")
    void ordenaOrdenesDescendente() {
        HistorialCliente historial = service.consultarPorId(CLIENTE_ACTIVO);

        assertNotNull(historial);
        assertEquals(2, historial.getVehiculos().size());
        assertEquals(3, historial.getOrdenes().size());
        assertEquals(103, historial.getOrdenes().get(0).getOrdenId());
        assertEquals(102, historial.getOrdenes().get(1).getOrdenId());
        assertEquals(101, historial.getOrdenes().get(2).getOrdenId());
    }

    @Test
    @DisplayName("Adjunta las líneas de servicio de cada orden")
    void adjuntaLineasDeCadaOrden() {
        HistorialCliente historial = service.consultarPorId(CLIENTE_ACTIVO);

        ResumenOrdenHistorial orden102 = historial.getOrdenes().get(1);
        assertEquals(102, orden102.getOrdenId());
        assertEquals(2, orden102.getLineas().size());
        assertEquals("Revisión de frenos", orden102.getLineas().get(0).getNombreServicioSnapshot());
        assertEquals(1, historial.getOrdenes().get(2).getLineas().size());
    }

    @Test
    @DisplayName("Una orden facturada trae el total_final histórico y el número de factura (RF-20)")
    void ordenFacturadaTraeTotalYNumero() {
        HistorialCliente historial = service.consultarPorId(CLIENTE_ACTIVO);

        ResumenOrdenHistorial orden102 = historial.getOrdenes().get(1);
        assertTrue(orden102.isFacturada());
        assertEquals(new BigDecimal("233750.00"), orden102.getTotalFacturado());
        assertEquals("F-2026-0001", orden102.getNumeroFactura());
        assertEquals("Facturado", orden102.getEstado());
    }

    @Test
    @DisplayName("Una orden sin factura deja el total en null y no inventa valores (RF-20)")
    void ordenSinFacturaNoTieneTotal() {
        HistorialCliente historial = service.consultarPorId(CLIENTE_ACTIVO);

        ResumenOrdenHistorial orden101 = historial.getOrdenes().get(2);
        assertFalse(orden101.isFacturada());
        assertNull(orden101.getTotalFacturado());
        assertNull(orden101.getNumeroFactura());
    }

    @Test
    @DisplayName("El historial de un cliente inactivo sigue siendo consultable (RN-17)")
    void clienteInactivoEsConsultable() {
        consultaDAO.agregar(new ResumenOrdenServicio(201, 6, "OLD999",
                LocalDateTime.of(2026, 4, 15, 7, 0)));

        HistorialCliente historial = service.consultarPorId(CLIENTE_INACTIVO);

        assertNotNull(historial);
        assertFalse(historial.getCliente().isActivo());
        assertEquals(1, historial.getVehiculos().size());
        assertEquals(1, historial.getOrdenes().size());
        assertNull(historial.getOrdenes().get(0).getTotalFacturado());
    }

    @Test
    @DisplayName("Un cliente sin vehículos ni órdenes devuelve listas vacías, sin excepción")
    void clienteSinDatosDevuelveListasVacias() {
        HistorialCliente historial = service.consultarPorId(CLIENTE_SIN_DATOS);

        assertNotNull(historial.getCliente());
        assertTrue(historial.getVehiculos().isEmpty());
        assertTrue(historial.getOrdenes().isEmpty());
    }

    @Test
    @DisplayName("Rechaza un cliente inexistente por ID y por documento")
    void rechazaClienteInexistente() {
        assertThrows(EntidadNoEncontradaException.class, () -> service.consultarPorId(999));
        assertThrows(EntidadNoEncontradaException.class, () -> service.consultarPorDocumento("00000000"));
    }

    @Test
    @DisplayName("Rechaza un ID no positivo y un documento vacío")
    void rechazaEntradasInvalidas() {
        assertThrows(ValidacionException.class, () -> service.consultarPorId(0));
        assertThrows(ValidacionException.class, () -> service.consultarPorId(-4));
        assertThrows(ValidacionException.class, () -> service.consultarPorDocumento("   "));
        assertThrows(ValidacionException.class, () -> service.consultarPorDocumento(null));
    }

    @Test
    @DisplayName("Consulta por documento normalizando espacios y encuentra al cliente")
    void consultaPorDocumentoNormalizado() {
        HistorialCliente historial = service.consultarPorDocumento("  1094567801  ");

        assertNotNull(historial);
        assertEquals(CLIENTE_ACTIVO, historial.getCliente().getId());
        assertEquals("1094567801", historial.getCliente().getDocumentoIdentidad());
        assertEquals(3, historial.getOrdenes().size());
    }

    // ------------------------------------------------------------------
    // DAOs en memoria
    // ------------------------------------------------------------------

    private static class ClienteDAOMock implements ClienteDAO {
        private final List<Cliente> datos = new ArrayList<>();

        void agregar(Cliente cliente) {
            datos.add(cliente);
        }

        @Override
        public List<Cliente> listarTodos() {
            return datos;
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
                    return cliente;
                }
            }
            return null;
        }

        @Override
        public Cliente buscarPorDocumento(String documento) {
            for (Cliente cliente : datos) {
                if (cliente.getDocumentoIdentidad().equals(documento)) {
                    return cliente;
                }
            }
            return null;
        }

        @Override
        public Cliente buscarPorEmail(String email) {
            return null;
        }

        @Override
        public int guardar(Cliente cliente) {
            throw new UnsupportedOperationException("No se usa en estas pruebas.");
        }

        @Override
        public boolean actualizar(Cliente cliente) {
            throw new UnsupportedOperationException("No se usa en estas pruebas.");
        }

        @Override
        public boolean actualizarEstadoActivo(int id, boolean activo) {
            throw new UnsupportedOperationException("No se usa en estas pruebas.");
        }
    }

    private static class VehiculoDAOMock implements VehiculoDAO {
        private final List<Vehiculo> datos = new ArrayList<>();

        void agregar(Vehiculo vehiculo) {
            datos.add(vehiculo);
        }

        @Override
        public List<Vehiculo> listarPorCliente(int clienteId) {
            List<Vehiculo> resultado = new ArrayList<>();
            for (Vehiculo vehiculo : datos) {
                if (vehiculo.getClienteId() == clienteId) {
                    resultado.add(vehiculo);
                }
            }
            return resultado;
        }

        @Override
        public int guardar(Vehiculo vehiculo) {
            throw new UnsupportedOperationException("No se usa en estas pruebas.");
        }

        @Override
        public Vehiculo buscarPorId(int id) {
            return null;
        }

        @Override
        public Vehiculo buscarPorPlaca(String placa) {
            return null;
        }

        @Override
        public List<Vehiculo> listarTodos() {
            return datos;
        }
    }

    private static class ConsultaOrdenDAOMock implements ConsultaOrdenServicioDAO {
        private final List<ResumenOrdenServicio> datos = new ArrayList<>();

        void agregar(ResumenOrdenServicio orden) {
            datos.add(orden);
        }

        @Override
        public List<ResumenOrdenServicio> listar(FiltroOrdenServicio filtro) {
            return datos;
        }

        @Override
        public List<ResumenOrdenServicio> listarPorCliente(int clienteId) {
            List<ResumenOrdenServicio> resultado = new ArrayList<>();
            for (ResumenOrdenServicio orden : datos) {
                if (clienteId == CLIENTE_ACTIVO && (orden.getId() == 101 || orden.getId() == 102
                        || orden.getId() == 103)) {
                    resultado.add(orden);
                } else if (clienteId == CLIENTE_INACTIVO && orden.getId() == 201) {
                    resultado.add(orden);
                }
            }
            return resultado;
        }

        @Override
        public void cargarDatosFacturados(int ordenId, Cliente cliente, Vehiculo vehiculo) {
            throw new UnsupportedOperationException("No se usa en estas pruebas.");
        }
    }

    private static class DetalleDAOMock implements OrdenServicioDetalleDAO {
        private final List<OrdenServicioDetalle> datos = new ArrayList<>();

        void agregar(OrdenServicioDetalle detalle) {
            datos.add(detalle);
        }

        @Override
        public List<OrdenServicioDetalle> listarPorOrden(int ordenId) {
            List<OrdenServicioDetalle> resultado = new ArrayList<>();
            for (OrdenServicioDetalle detalle : datos) {
                if (detalle.getOrdenId() == ordenId) {
                    resultado.add(detalle);
                }
            }
            return resultado;
        }

        @Override
        public int guardar(OrdenServicioDetalle detalle) {
            throw new UnsupportedOperationException("No se usa en estas pruebas.");
        }

        @Override
        public List<OrdenServicioDetalle> listarPorOrdenParaActualizar(int ordenId) {
            return listarPorOrden(ordenId);
        }

        @Override
        public void asignarTecnico(int detalleId, int tecnicoId) {
            throw new UnsupportedOperationException("No se usa en estas pruebas.");
        }

        @Override
        public void asignarTecnico(int detalleId, int tecnicoId, BigDecimal horasTrabajadas) {
            throw new UnsupportedOperationException("No se usa en estas pruebas.");
        }

        @Override
        public boolean existeTecnicoAsignado(int detalleId, int tecnicoId) {
            return false;
        }

        @Override
        public List<DetalleTecnico> listarTecnicosAsignados(int detalleId) {
            return new ArrayList<>();
        }

        @Override
        public boolean actualizarHorasTrabajadas(int detalleId, int tecnicoId, BigDecimal horasTrabajadas) {
            return false;
        }

        @Override
        public boolean tieneHorasTrabajadas(int detalleId) {
            return false;
        }

        @Override
        public Map<Integer, Integer> listarReservasParaActualizar(int detalleId) {
            return new HashMap<>();
        }

        @Override
        public void eliminar(int ordenId, int detalleId) {
            throw new UnsupportedOperationException("No se usa en estas pruebas.");
        }

        @Override
        public OrdenServicioDetalle buscarPorId(int id) {
            for (OrdenServicioDetalle detalle : datos) {
                if (detalle.getId() == id) {
                    return detalle;
                }
            }
            return null;
        }
    }

    private static class FacturaDAOMock implements FacturaDAO {
        private final Map<Integer, Factura> porOrden = new HashMap<>();

        void agregar(Factura factura) {
            porOrden.put(factura.getOrdenId(), factura);
        }

        @Override
        public Factura buscarPorOrden(int ordenId) {
            return porOrden.get(ordenId);
        }

        @Override
        public int guardar(Factura factura) {
            throw new UnsupportedOperationException("No se usa en estas pruebas.");
        }

        @Override
        public Factura buscarPorId(int id) {
            return null;
        }

        @Override
        public Factura buscarPorNumero(String numeroFactura) {
            return null;
        }

        @Override
        public List<Factura> listarTodas() {
            return new ArrayList<>(porOrden.values());
        }

        @Override
        public String obtenerUltimoNumeroDelAnio(int anio) {
            return null;
        }

        @Override
        public void marcarArchivoGenerado(int facturaId) {
            throw new UnsupportedOperationException("No se usa en estas pruebas.");
        }
    }
}
