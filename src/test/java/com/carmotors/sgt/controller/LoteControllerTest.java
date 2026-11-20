package com.carmotors.sgt.controller;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.carmotors.sgt.model.Lote;
import com.carmotors.sgt.model.LotePorVencer;
import com.carmotors.sgt.model.Proveedor;
import com.carmotors.sgt.model.ProveedorRepuesto;
import com.carmotors.sgt.model.Repuesto;
import com.carmotors.sgt.dao.LoteDAO;
import com.carmotors.sgt.dao.ProveedorDAO;
import com.carmotors.sgt.dao.ProveedorRepuestoDAO;
import com.carmotors.sgt.dao.RepuestoDAO;
import com.carmotors.sgt.service.LoteService;

class LoteControllerTest {

    private LoteController controller;
    private LoteDAOTest loteDAO;

    @BeforeEach
    void setUp() {
        loteDAO = new LoteDAOTest();
        ProveedorDAOTest proveedorDAO = new ProveedorDAOTest();
        proveedorDAO.agregar(new Proveedor(1, "Importadora Sur", "1234567", "sur@correo.com", "Calle 1", true));

        ProveedorRepuestoDAOTest catalogoDAO = new ProveedorRepuestoDAOTest();
        catalogoDAO.agregar(new ProveedorRepuesto(1, 10, java.math.BigDecimal.valueOf(50000), 3, true));

        RepuestoDAOTest repuestoDAO = new RepuestoDAOTest();
        repuestoDAO.agregar(new Repuesto(10, 1, "Filtro", "Bosch", java.math.BigDecimal.valueOf(40000), 0, 5, true));

        LoteService service = new LoteService(loteDAO, proveedorDAO, catalogoDAO, repuestoDAO);
        controller = new LoteController(service);
    }

    @Test
    @DisplayName("Debe lanzar excepción si el servicio es nulo en constructor")
    void constructorNulo() {
        assertThrows(IllegalArgumentException.class, () -> new LoteController(null));
    }

    @Test
    @DisplayName("Debe delegar búsqueda por código y listado por repuesto")
    void delegarConsultas() {
        Lote lote = new Lote(1, "LOT-001", 1, 10, LocalDate.now().minusDays(5), LocalDate.now().plusDays(20), 10, 10);
        loteDAO.agregar(lote);

        Lote encontrado = controller.buscarPorCodigo("LOT-001");
        assertNotNull(encontrado);
        assertEquals("LOT-001", encontrado.getCodigoLote());

        List<Lote> listados = controller.listarPorRepuesto(10);
        assertEquals(1, listados.size());
    }

    @Test
    @DisplayName("Debe delegar generación de reporte de lotes por vencer")
    void delegarReporteLotesPorVencer() {
        LocalDate hoy = LocalDate.now();
        loteDAO.agregar(new Lote(1, "LOT-REP-1", 1, 10, hoy.minusDays(10), hoy.plusDays(15), 10, 8));

        List<LotePorVencer> reporteDefecto = controller.generarReporteLotesPorVencer();
        assertEquals(1, reporteDefecto.size());

        List<LotePorVencer> reporteConDias = controller.generarReporteLotesPorVencer(20);
        assertEquals(1, reporteConDias.size());
        assertEquals("LOT-REP-1", reporteConDias.get(0).getLote().getCodigoLote());
    }

    private static class LoteDAOTest implements LoteDAO {
        private final List<Lote> lotes = new ArrayList<>();

        void agregar(Lote lote) {
            lotes.add(lote);
        }

        @Override
        public Lote buscarPorCodigo(String codigoLote) {
            for (Lote l : lotes) {
                if (l.getCodigoLote().equalsIgnoreCase(codigoLote)) {
                    return l;
                }
            }
            return null;
        }

        @Override
        public List<Lote> listarPorRepuesto(int repuestoId) {
            List<Lote> res = new ArrayList<>();
            for (Lote l : lotes) {
                if (l.getRepuestoId() == repuestoId) {
                    res.add(l);
                }
            }
            return res;
        }

        @Override
        public int guardar(Lote lote) {
            lote.setId(lotes.size() + 1);
            lotes.add(lote);
            return lote.getId();
        }

        @Override
        public int sumarStockFisico(int repuestoId) {
            return 0;
        }

        @Override
        public int sumarStockOperativo(int repuestoId, LocalDate fechaReferencia) {
            return 0;
        }

        @Override
        public List<Lote> listarPorVencer(LocalDate fechaLimite) {
            List<Lote> res = new ArrayList<>();
            for (Lote l : lotes) {
                if (l.getCantidadActual() > 0 && l.getFechaCaducidad() != null
                        && !l.getFechaCaducidad().isAfter(fechaLimite)) {
                    res.add(l);
                }
            }
            return res;
        }

        @Override
        public boolean descontarUnidades(int loteId, int cantidad) {
            return false;
        }
    }

    private static class ProveedorDAOTest implements ProveedorDAO {
        private final List<Proveedor> proveedores = new ArrayList<>();

        void agregar(Proveedor p) {
            proveedores.add(p);
        }

        @Override
        public List<Proveedor> listarTodos() {
            return proveedores;
        }

        @Override
        public List<Proveedor> listarActivos() {
            return proveedores;
        }

        @Override
        public Proveedor buscarPorId(int id) {
            for (Proveedor p : proveedores) {
                if (p.getId() == id) return p;
            }
            return null;
        }

        @Override
        public Proveedor buscarPorEmail(String email) {
            return null;
        }

        @Override
        public int guardar(Proveedor proveedor) {
            return 0;
        }

        @Override
        public boolean actualizar(Proveedor proveedor) {
            return false;
        }

        @Override
        public boolean desactivar(int id) {
            return false;
        }

        @Override
        public boolean reactivar(int id) {
            return false;
        }
    }

    private static class RepuestoDAOTest implements RepuestoDAO {
        private final List<Repuesto> repuestos = new ArrayList<>();

        void agregar(Repuesto r) {
            repuestos.add(r);
        }

        @Override
        public List<Repuesto> listarTodos() {
            return repuestos;
        }

        @Override
        public List<Repuesto> listarActivos() {
            return repuestos;
        }

        @Override
        public Repuesto buscarPorId(int id) {
            for (Repuesto r : repuestos) {
                if (r.getId() == id) return r;
            }
            return null;
        }

        @Override
        public Repuesto buscarPorNombre(String nombre) {
            return null;
        }

        @Override
        public int guardar(Repuesto repuesto) {
            return 0;
        }

        @Override
        public boolean actualizar(Repuesto repuesto) {
            return false;
        }

        @Override
        public boolean desactivar(int id) {
            return false;
        }

        @Override
        public boolean reactivar(int id) {
            return false;
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

    private static class ProveedorRepuestoDAOTest implements ProveedorRepuestoDAO {
        private final List<ProveedorRepuesto> lista = new ArrayList<>();

        void agregar(ProveedorRepuesto pr) {
            lista.add(pr);
        }

        @Override
        public List<ProveedorRepuesto> listarPorProveedor(int proveedorId) {
            return lista;
        }

        @Override
        public List<ProveedorRepuesto> listarPorRepuesto(int repuestoId) {
            return lista;
        }

        @Override
        public ProveedorRepuesto buscarPorProveedorYRepuesto(int proveedorId, int repuestoId) {
            for (ProveedorRepuesto pr : lista) {
                if (pr.getProveedorId() == proveedorId && pr.getRepuestoId() == repuestoId) {
                    return pr;
                }
            }
            return null;
        }

        @Override
        public boolean guardar(ProveedorRepuesto proveedorRepuesto) {
            return true;
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
