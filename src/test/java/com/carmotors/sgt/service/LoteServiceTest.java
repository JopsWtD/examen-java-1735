package com.carmotors.sgt.service;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.carmotors.sgt.exception.DatoDuplicadoException;
import com.carmotors.sgt.exception.EntidadNoEncontradaException;
import com.carmotors.sgt.exception.ValidacionException;
import com.carmotors.sgt.model.Lote;
import com.carmotors.sgt.model.LotePorVencer;
import com.carmotors.sgt.model.Proveedor;
import com.carmotors.sgt.model.ProveedorRepuesto;
import com.carmotors.sgt.model.Repuesto;
import com.carmotors.sgt.dao.LoteDAO;
import com.carmotors.sgt.dao.ProveedorDAO;
import com.carmotors.sgt.dao.ProveedorRepuestoDAO;
import com.carmotors.sgt.dao.RepuestoDAO;

class LoteServiceTest {

    private LoteService service;
    private LoteDAOMock loteDAO;
    private ProveedorDAOMock proveedorDAO;
    private ProveedorRepuestoDAOMock proveedorRepuestoDAO;
    private RepuestoDAOMock repuestoDAO;

    @BeforeEach
    void setUp() {
        loteDAO = new LoteDAOMock();
        proveedorDAO = new ProveedorDAOMock();
        proveedorDAO.agregar(new Proveedor(1, "Importadora Sur", "5712345678", "ventas@importadora.com", "Calle 5", true));
        proveedorDAO.agregar(new Proveedor(2, "Baterías Centro", "5712345679", "baterias@centro.com", "Calle 7", false));

        proveedorRepuestoDAO = new ProveedorRepuestoDAOMock();
        proveedorRepuestoDAO.agregar(new ProveedorRepuesto(1, 10, java.math.BigDecimal.valueOf(55000), 4, true));

        repuestoDAO = new RepuestoDAOMock();
        repuestoDAO.agregar(new Repuesto(10, 1, "Filtro de aire", "Bosch", java.math.BigDecimal.valueOf(45000), 0, 5, true));
        repuestoDAO.agregar(new Repuesto(11, 1, "Bujía", "NGK", java.math.BigDecimal.valueOf(15000), 0, 2, false));

        service = new LoteService(loteDAO, proveedorDAO, proveedorRepuestoDAO, repuestoDAO);
    }

    @Test
    @DisplayName("Debe registrar un lote válido con cantidad inicial igual a actual")
    void registrarLoteValido() {
        Lote lote = new Lote();
        lote.setCodigoLote("LOT-TEST-001");
        lote.setProveedorId(1);
        lote.setRepuestoId(10);
        lote.setFechaIngreso(LocalDate.of(2026, 11, 1));
        lote.setFechaCaducidad(LocalDate.of(2027, 11, 1));
        lote.setCantidadInicial(25);

        Lote guardado = service.registrarLote(lote);

        assertNotNull(guardado);
        assertEquals(1, guardado.getId());
        assertEquals(25, guardado.getCantidadInicial());
        assertEquals(25, guardado.getCantidadActual());
    }

    @Test
    @DisplayName("Debe rechazar un código de lote duplicado")
    void rechazarCodigoDuplicado() {
        loteDAO.agregar(new Lote(1, "LOT-TEST-001", 1, 10, LocalDate.of(2026, 10, 1), LocalDate.of(2027, 10, 1), 10, 10));

        Lote lote = new Lote();
        lote.setCodigoLote("LOT-TEST-001");
        lote.setProveedorId(1);
        lote.setRepuestoId(10);
        lote.setFechaIngreso(LocalDate.of(2026, 11, 1));
        lote.setFechaCaducidad(LocalDate.of(2027, 11, 1));
        lote.setCantidadInicial(12);

        assertThrows(DatoDuplicadoException.class, () -> service.registrarLote(lote));
    }

    @Test
    @DisplayName("Debe rechazar una cantidad inicial menor o igual a cero")
    void rechazarCantidadInicialInvalida() {
        Lote lote = new Lote();
        lote.setCodigoLote("LOT-TEST-002");
        lote.setProveedorId(1);
        lote.setRepuestoId(10);
        lote.setFechaIngreso(LocalDate.of(2026, 11, 1));
        lote.setFechaCaducidad(LocalDate.of(2027, 11, 1));
        lote.setCantidadInicial(0);

        assertThrows(ValidacionException.class, () -> service.registrarLote(lote));
    }

    @Test
    @DisplayName("Debe rechazar caducidad que no sea estrictamente posterior a la fecha de ingreso")
    void rechazarCaducidadInvalida() {
        Lote lote = new Lote();
        lote.setCodigoLote("LOT-TEST-003");
        lote.setProveedorId(1);
        lote.setRepuestoId(10);
        lote.setFechaIngreso(LocalDate.of(2026, 11, 1));
        lote.setFechaCaducidad(LocalDate.of(2026, 11, 1));
        lote.setCantidadInicial(8);

        assertThrows(ValidacionException.class, () -> service.registrarLote(lote));
    }

    @Test
    @DisplayName("Debe rechazar proveedor no asociado al repuesto o inactivo")
    void rechazarProveedorInvalido() {
        Lote lote = new Lote();
        lote.setCodigoLote("LOT-TEST-004");
        lote.setProveedorId(2);
        lote.setRepuestoId(10);
        lote.setFechaIngreso(LocalDate.of(2026, 11, 1));
        lote.setCantidadInicial(8);

        assertThrows(ValidacionException.class, () -> service.registrarLote(lote));
    }

    @Test
    @DisplayName("Debe rechazar un proveedor inexistente")
    void rechazarProveedorInexistente() {
        Lote lote = new Lote();
        lote.setCodigoLote("LOT-TEST-005");
        lote.setProveedorId(99);
        lote.setRepuestoId(10);
        lote.setFechaIngreso(LocalDate.of(2026, 11, 1));
        lote.setCantidadInicial(8);

        assertThrows(EntidadNoEncontradaException.class, () -> service.registrarLote(lote));
    }

    @Test
    @DisplayName("Debe rechazar un repuesto inexistente")
    void rechazarRepuestoInexistente() {
        Lote lote = new Lote();
        lote.setCodigoLote("LOT-TEST-006");
        lote.setProveedorId(1);
        lote.setRepuestoId(99);
        lote.setFechaIngreso(LocalDate.of(2026, 11, 1));
        lote.setCantidadInicial(8);

        assertThrows(EntidadNoEncontradaException.class, () -> service.registrarLote(lote));
    }

    @Test
    @DisplayName("Debe rechazar un repuesto inactivo")
    void rechazarRepuestoInactivo() {
        proveedorRepuestoDAO.agregar(new ProveedorRepuesto(1, 11, java.math.BigDecimal.valueOf(14000), 3, true));

        Lote lote = new Lote();
        lote.setCodigoLote("LOT-TEST-007");
        lote.setProveedorId(1);
        lote.setRepuestoId(11);
        lote.setFechaIngreso(LocalDate.of(2026, 11, 2));
        lote.setCantidadInicial(5);

        assertThrows(ValidacionException.class, () -> service.registrarLote(lote));
    }

    @Test
    @DisplayName("Debe consultar trazabilidad por código sin distinguir mayúsculas")
    void buscarPorCodigo() {
        loteDAO.agregar(new Lote(1, "LOT-TRACE-001", 1, 10,
                LocalDate.of(2026, 11, 1), LocalDate.of(2027, 11, 1), 20, 14));

        Lote encontrado = service.buscarPorCodigo("lot-trace-001");

        assertNotNull(encontrado);
        assertEquals("LOT-TRACE-001", encontrado.getCodigoLote());
        assertEquals(20, encontrado.getCantidadInicial());
        assertEquals(14, encontrado.getCantidadActual());
        assertNull(service.buscarPorCodigo("LOT-INEXISTENTE"));
    }

    @Test
    @DisplayName("Debe listar lotes por caducidad, ingreso e identificador")
    void listarPorRepuestoOrdenado() {
        loteDAO.agregar(new Lote(1, "LOT-SIN-CADUCIDAD", 1, 10,
                LocalDate.of(2026, 1, 1), null, 30, 30));
        loteDAO.agregar(new Lote(2, "LOT-CADUCIDAD-TARDIA-A", 1, 10,
                LocalDate.of(2026, 2, 1), LocalDate.of(2027, 5, 1), 20, 20));
        loteDAO.agregar(new Lote(3, "LOT-CADUCIDAD-TARDIA-B", 1, 10,
                LocalDate.of(2026, 2, 1), LocalDate.of(2027, 5, 1), 18, 18));
        loteDAO.agregar(new Lote(4, "LOT-CADUCIDAD-CERCANA-B", 1, 10,
                LocalDate.of(2026, 3, 1), LocalDate.of(2027, 3, 1), 15, 10));
        loteDAO.agregar(new Lote(5, "LOT-CADUCIDAD-CERCANA-A", 1, 10,
                LocalDate.of(2026, 2, 15), LocalDate.of(2027, 3, 1), 12, 12));

        List<Lote> lotes = service.listarPorRepuesto(10);

        assertEquals(5, lotes.size());
        assertEquals("LOT-CADUCIDAD-CERCANA-A", lotes.get(0).getCodigoLote());
        assertEquals("LOT-CADUCIDAD-CERCANA-B", lotes.get(1).getCodigoLote());
        assertEquals("LOT-CADUCIDAD-TARDIA-A", lotes.get(2).getCodigoLote());
        assertEquals("LOT-CADUCIDAD-TARDIA-B", lotes.get(3).getCodigoLote());
        assertEquals("LOT-SIN-CADUCIDAD", lotes.get(4).getCodigoLote());
    }

    @Test
    @DisplayName("Debe rechazar trazabilidad de un repuesto inexistente")
    void rechazarListadoRepuestoInexistente() {
        assertThrows(EntidadNoEncontradaException.class, () -> service.listarPorRepuesto(99));
    }

    @Test
    @DisplayName("Debe excluir lotes sin caducidad, agotados y con caducidad posterior al límite")
    void generarReporteLotesPorVencer_excluyeLotesAgotadosSinCaducidadYFuturosLejanos() {
        LocalDate hoy = LocalDate.of(2026, 11, 15);

        // Lote sin caducidad (null)
        loteDAO.agregar(new Lote(1, "LOT-SIN-CAD", 1, 10, hoy.minusDays(10), null, 20, 20));
        // Lote agotado (cantidadActual = 0)
        loteDAO.agregar(new Lote(2, "LOT-AGOTADO", 1, 10, hoy.minusDays(30), hoy.plusDays(5), 10, 0));
        // Lote con caducidad posterior al horizonte (hoy + 60 con N = 30)
        loteDAO.agregar(new Lote(3, "LOT-LEJANO", 1, 10, hoy.minusDays(5), hoy.plusDays(60), 15, 15));

        List<LotePorVencer> reporte = service.generarReporteLotesPorVencer(30, hoy);

        assertTrue(reporte.isEmpty(), "Ningún lote sin caducidad, agotado o posterior a 30 días debe incluirse.");
    }

    @Test
    @DisplayName("Debe clasificar correctamente lotes VENCIDO y POR VENCER con cálculo de días y nombres")
    void generarReporteLotesPorVencer_clasificaVencidoYPorVencerCorrectamente() {
        LocalDate hoy = LocalDate.of(2026, 11, 15);

        // Lote vencido hace 5 días
        loteDAO.agregar(new Lote(1, "LOT-VENCIDO", 1, 10, hoy.minusDays(60), hoy.minusDays(5), 10, 4));
        // Lote que vence hoy
        loteDAO.agregar(new Lote(2, "LOT-HOY", 1, 10, hoy.minusDays(30), hoy, 20, 15));
        // Lote por vencer en 12 días
        loteDAO.agregar(new Lote(3, "LOT-PROXIMO", 1, 10, hoy.minusDays(20), hoy.plusDays(12), 30, 25));

        List<LotePorVencer> reporte = service.generarReporteLotesPorVencer(30, hoy);

        assertEquals(3, reporte.size());

        // Ordenado por caducidad ascendente:
        // 1. LOT-VENCIDO
        LotePorVencer itemVencido = reporte.get(0);
        assertEquals("LOT-VENCIDO", itemVencido.getLote().getCodigoLote());
        assertEquals("VENCIDO", itemVencido.getEstado());
        assertEquals(-5, itemVencido.getDiasRestantes());
        assertEquals("Filtro de aire", itemVencido.getNombreRepuesto());
        assertEquals("Importadora Sur", itemVencido.getNombreProveedor());

        // 2. LOT-HOY
        LotePorVencer itemHoy = reporte.get(1);
        assertEquals("LOT-HOY", itemHoy.getLote().getCodigoLote());
        assertEquals("POR VENCER", itemHoy.getEstado());
        assertEquals(0, itemHoy.getDiasRestantes());

        // 3. LOT-PROXIMO
        LotePorVencer itemProximo = reporte.get(2);
        assertEquals("LOT-PROXIMO", itemProximo.getLote().getCodigoLote());
        assertEquals("POR VENCER", itemProximo.getEstado());
        assertEquals(12, itemProximo.getDiasRestantes());
    }

    @Test
    @DisplayName("Debe rechazar días menor o igual a cero")
    void generarReporteLotesPorVencer_diasInvalido_lanzaValidacionException() {
        LocalDate hoy = LocalDate.of(2026, 11, 15);
        assertThrows(ValidacionException.class, () -> service.generarReporteLotesPorVencer(0, hoy));
        assertThrows(ValidacionException.class, () -> service.generarReporteLotesPorVencer(-5, hoy));
        assertThrows(ValidacionException.class, () -> service.generarReporteLotesPorVencer(0));
    }

    @Test
    @DisplayName("Debe rechazar fecha de referencia nula")
    void generarReporteLotesPorVencer_fechaReferenciaNula_lanzaValidacionException() {
        assertThrows(ValidacionException.class, () -> service.generarReporteLotesPorVencer(30, null));
    }

    @Test
    @DisplayName("Debe resolver fallback cuando proveedor o repuesto no existen")
    void generarReporteLotesPorVencer_resuelveNombresDesconocidosFallback() {
        LocalDate hoy = LocalDate.of(2026, 11, 15);
        loteDAO.agregar(new Lote(1, "LOT-HUERFANO", 999, 888, hoy.minusDays(10), hoy.plusDays(5), 10, 5));

        List<LotePorVencer> reporte = service.generarReporteLotesPorVencer(30, hoy);

        assertEquals(1, reporte.size());
        assertEquals("Proveedor #999", reporte.get(0).getNombreProveedor());
        assertEquals("Repuesto #888", reporte.get(0).getNombreRepuesto());
    }

    @Test
    @DisplayName("Debe usar sobrecarga por defecto con 30 días")
    void generarReporteLotesPorVencer_sobrecargaPorDefecto() {
        LocalDate hoy = LocalDate.now();
        loteDAO.agregar(new Lote(1, "LOT-DEF-1", 1, 10, hoy.minusDays(10), hoy.plusDays(10), 10, 8));
        loteDAO.agregar(new Lote(2, "LOT-DEF-2", 1, 10, hoy.minusDays(10), hoy.plusDays(40), 10, 8));

        List<LotePorVencer> reporte = service.generarReporteLotesPorVencer();

        assertEquals(1, reporte.size());
        assertEquals("LOT-DEF-1", reporte.get(0).getLote().getCodigoLote());
    }

    private static class LoteDAOMock implements LoteDAO {
        private final List<Lote> datos = new ArrayList<>();
        private int nextId = 1;

        void agregar(Lote lote) {
            lote.setId(nextId++);
            datos.add(lote);
        }

        @Override
        public Lote buscarPorCodigo(String codigo) {
            for (Lote lote : datos) {
                if (lote.getCodigoLote().equalsIgnoreCase(codigo)) {
                    return lote;
                }
            }
            return null;
        }

        @Override
        public List<Lote> listarPorRepuesto(int repuestoId) {
            List<Lote> resultado = new ArrayList<>();
            for (Lote lote : datos) {
                if (lote.getRepuestoId() == repuestoId) {
                    resultado.add(lote);
                }
            }
            return resultado;
        }

        @Override
        public int guardar(Lote lote) {
            lote.setId(nextId++);
            datos.add(lote);
            return lote.getId();
        }

        @Override
        public int sumarStockFisico(int repuestoId) {
            int total = 0;
            for (Lote lote : datos) {
                if (lote.getRepuestoId() == repuestoId) {
                    total += lote.getCantidadActual();
                }
            }
            return total;
        }

        @Override
        public int sumarStockOperativo(int repuestoId, LocalDate fechaReferencia) {
            int total = 0;
            for (Lote lote : datos) {
                boolean vigente = lote.getFechaCaducidad() == null || !lote.getFechaCaducidad().isBefore(fechaReferencia);
                if (lote.getRepuestoId() == repuestoId && vigente) {
                    total += lote.getCantidadActual();
                }
            }
            return total;
        }

        @Override
        public List<Lote> listarPorVencer(LocalDate fechaLimite) {
            List<Lote> resultado = new ArrayList<>();
            for (Lote lote : datos) {
                if (lote.getCantidadActual() > 0 && lote.getFechaCaducidad() != null
                        && !lote.getFechaCaducidad().isAfter(fechaLimite)) {
                    resultado.add(lote);
                }
            }
            return resultado;
        }

        @Override
        public boolean descontarUnidades(int loteId, int cantidad) {
            return false;
        }
    }

    private static class ProveedorDAOMock implements ProveedorDAO {
        private final List<Proveedor> datos = new ArrayList<>();

        void agregar(Proveedor proveedor) {
            datos.add(proveedor);
        }

        @Override
        public List<Proveedor> listarTodos() {
            return new ArrayList<>(datos);
        }

        @Override
        public List<Proveedor> listarActivos() {
            List<Proveedor> activos = new ArrayList<>();
            for (Proveedor proveedor : datos) {
                if (proveedor.isActivo()) {
                    activos.add(proveedor);
                }
            }
            return activos;
        }

        @Override
        public Proveedor buscarPorId(int id) {
            for (Proveedor proveedor : datos) {
                if (proveedor.getId() == id) {
                    return proveedor;
                }
            }
            return null;
        }

        @Override
        public Proveedor buscarPorEmail(String email) {
            return null;
        }

        @Override
        public int guardar(Proveedor proveedor) {
            return proveedor.getId();
        }

        @Override
        public boolean actualizar(Proveedor proveedor) {
            return true;
        }

        @Override
        public boolean desactivar(int id) {
            return true;
        }

        @Override
        public boolean reactivar(int id) {
            return true;
        }
    }

    private static class RepuestoDAOMock implements RepuestoDAO {
        private final List<Repuesto> datos = new ArrayList<>();

        void agregar(Repuesto repuesto) {
            datos.add(repuesto);
        }

        @Override
        public List<Repuesto> listarTodos() {
            return new ArrayList<>(datos);
        }

        @Override
        public List<Repuesto> listarActivos() {
            List<Repuesto> activos = new ArrayList<>();
            for (Repuesto repuesto : datos) {
                if (repuesto.isActivo()) {
                    activos.add(repuesto);
                }
            }
            return activos;
        }

        @Override
        public Repuesto buscarPorId(int id) {
            for (Repuesto repuesto : datos) {
                if (repuesto.getId() == id) {
                    return repuesto;
                }
            }
            return null;
        }

        @Override
        public Repuesto buscarPorNombre(String nombre) {
            return null;
        }

        @Override
        public int guardar(Repuesto repuesto) {
            repuesto.setId(datos.size() + 1);
            datos.add(repuesto);
            return repuesto.getId();
        }

        @Override
        public boolean actualizar(Repuesto repuesto) {
            return true;
        }

        @Override
        public boolean desactivar(int id) {
            return true;
        }

        @Override
        public boolean reactivar(int id) {
            return true;
        }

        @Override
        public boolean descontarReserva(int repuestoId, int cantidad) {
            return false;
        }

        @Override
        public boolean aumentarReservado(int repuestoId, int cantidad) {
            return false;
        }
    }

    private static class ProveedorRepuestoDAOMock implements ProveedorRepuestoDAO {
        private final List<ProveedorRepuesto> datos = new ArrayList<>();

        void agregar(ProveedorRepuesto proveedorRepuesto) {
            datos.add(proveedorRepuesto);
        }

        @Override
        public List<ProveedorRepuesto> listarPorProveedor(int proveedorId) {
            return new ArrayList<>(datos);
        }

        @Override
        public List<ProveedorRepuesto> listarPorRepuesto(int repuestoId) {
            return new ArrayList<>(datos);
        }

        @Override
        public ProveedorRepuesto buscarPorProveedorYRepuesto(int proveedorId, int repuestoId) {
            for (ProveedorRepuesto item : datos) {
                if (item.getProveedorId() == proveedorId && item.getRepuestoId() == repuestoId) {
                    return item;
                }
            }
            return null;
        }

        @Override
        public boolean guardar(ProveedorRepuesto proveedorRepuesto) {
            return datos.add(proveedorRepuesto);
        }

        @Override
        public boolean actualizarCondiciones(ProveedorRepuesto proveedorRepuesto) {
            return true;
        }

        @Override
        public boolean eliminar(int proveedorId, int repuestoId) {
            return true;
        }

        @Override
        public int desmarcarHabitual(int repuestoId) {
            return 0;
        }

        @Override
        public boolean marcarHabitual(int proveedorId, int repuestoId) {
            return true;
        }
    }
}
