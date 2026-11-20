package com.carmotors.sgt.service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.carmotors.sgt.exception.EntidadNoEncontradaException;
import com.carmotors.sgt.exception.ValidacionException;
import com.carmotors.sgt.model.Lote;
import com.carmotors.sgt.model.Proveedor;
import com.carmotors.sgt.model.ProveedorRepuesto;
import com.carmotors.sgt.model.Repuesto;
import com.carmotors.sgt.model.StockRepuesto;
import com.carmotors.sgt.dao.LoteDAO;
import com.carmotors.sgt.dao.ProveedorDAO;
import com.carmotors.sgt.dao.ProveedorRepuestoDAO;
import com.carmotors.sgt.dao.RepuestoDAO;

class InventarioServiceTest {

    private RepuestoDAOMock repuestoDAO;
    private LoteDAOMock loteDAO;
    private ProveedorRepuestoDAOMock proveedorRepuestoDAO;
    private ProveedorDAOMock proveedorDAO;
    private InventarioService service;
    private LocalDate hoy;

    @BeforeEach
    void setUp() {
        repuestoDAO = new RepuestoDAOMock();
        loteDAO = new LoteDAOMock();
        proveedorRepuestoDAO = new ProveedorRepuestoDAOMock();
        proveedorDAO = new ProveedorDAOMock();
        service = new InventarioService(repuestoDAO, loteDAO, proveedorRepuestoDAO, proveedorDAO);
        hoy = LocalDate.now();
    }

    @Test
    @DisplayName("Repuesto sin lotes: todas las métricas en cero")
    void repuestoSinLotes() {
        repuestoDAO.agregar(repuesto(1, 0, 5, true));

        StockRepuesto stock = service.calcularStock(1);

        assertEquals(0, stock.getStockFisico());
        assertEquals(0, stock.getStockOperativo());
        assertEquals(0, stock.getReservado());
        assertEquals(0, stock.getDisponible());
        assertEquals(0, stock.getDeficit());
        assertEquals(5, stock.getStockMinimo());
        assertEquals(1, stock.getRepuesto().getId());
    }

    @Test
    @DisplayName("Solo lotes vigentes: físico igual a operativo")
    void soloLotesVigentes() {
        repuestoDAO.agregar(repuesto(1, 0, 2, true));
        loteDAO.agregar(lote(1, hoy.plusDays(30), 10));
        loteDAO.agregar(lote(1, hoy.plusYears(1), 6));

        StockRepuesto stock = service.calcularStock(1);

        assertEquals(16, stock.getStockFisico());
        assertEquals(16, stock.getStockOperativo());
        assertEquals(16, stock.getDisponible());
    }

    @Test
    @DisplayName("Lotes sin fecha de caducidad cuentan como vigentes")
    void lotesSinCaducidad() {
        repuestoDAO.agregar(repuesto(1, 0, 0, true));
        loteDAO.agregar(lote(1, null, 7));
        loteDAO.agregar(lote(1, null, 3));

        StockRepuesto stock = service.calcularStock(1);

        assertEquals(10, stock.getStockFisico());
        assertEquals(10, stock.getStockOperativo());
    }

    @Test
    @DisplayName("Lote que vence hoy es vigente y lote vencido ayer solo suma al físico")
    void loteVenceHoyYLoteVencidoAyer() {
        repuestoDAO.agregar(repuesto(1, 0, 0, true));
        loteDAO.agregar(lote(1, hoy, 8));
        loteDAO.agregar(lote(1, hoy.minusDays(1), 5));

        StockRepuesto stock = service.calcularStock(1);

        assertEquals(13, stock.getStockFisico());
        assertEquals(8, stock.getStockOperativo());
    }

    @Test
    @DisplayName("Disponible descuenta las reservas del stock operativo")
    void disponibleConReservas() {
        repuestoDAO.agregar(repuesto(1, 4, 10, true));
        loteDAO.agregar(lote(1, hoy.plusDays(60), 32));
        loteDAO.agregar(lote(1, null, 24));

        StockRepuesto stock = service.calcularStock(1);

        assertEquals(56, stock.getStockOperativo());
        assertEquals(4, stock.getReservado());
        assertEquals(52, stock.getDisponible());
        assertEquals(0, stock.getDeficit());
    }

    @Test
    @DisplayName("Déficit cuando las reservas superan el stock operativo")
    void deficitPorLotesVencidos() {
        repuestoDAO.agregar(repuesto(1, 6, 0, true));
        loteDAO.agregar(lote(1, hoy.plusDays(10), 4));
        loteDAO.agregar(lote(1, hoy.minusDays(3), 5));

        StockRepuesto stock = service.calcularStock(1);

        assertEquals(9, stock.getStockFisico());
        assertEquals(4, stock.getStockOperativo());
        assertEquals(0, stock.getDisponible());
        assertEquals(2, stock.getDeficit());
    }

    @Test
    @DisplayName("Repuesto inexistente lanza EntidadNoEncontradaException")
    void repuestoInexistente() {
        assertThrows(EntidadNoEncontradaException.class, () -> service.calcularStock(99));
    }

    @Test
    @DisplayName("ID inválido lanza ValidacionException")
    void idInvalido() {
        assertThrows(ValidacionException.class, () -> service.calcularStock(0));
        assertThrows(ValidacionException.class, () -> service.calcularStock(-3));
    }

    @Test
    @DisplayName("Métricas de todos los repuestos activos excluyen los inactivos")
    void metricasDeRepuestosActivos() {
        repuestoDAO.agregar(repuesto(1, 1, 2, true));
        repuestoDAO.agregar(repuesto(2, 0, 2, false));
        repuestoDAO.agregar(repuesto(3, 0, 4, true));
        loteDAO.agregar(lote(1, null, 5));
        loteDAO.agregar(lote(2, null, 9));
        loteDAO.agregar(lote(3, hoy.minusDays(1), 2));

        List<StockRepuesto> metricas = service.calcularStockActivos();

        assertEquals(2, metricas.size());
        assertEquals(1, metricas.get(0).getRepuesto().getId());
        assertEquals(4, metricas.get(0).getDisponible());
        assertEquals(3, metricas.get(1).getRepuesto().getId());
        assertEquals(2, metricas.get(1).getStockFisico());
        assertEquals(0, metricas.get(1).getStockOperativo());
        for (StockRepuesto stock : metricas) {
            assertTrue(stock.getRepuesto().isActivo());
        }
    }

    @Test
    @DisplayName("Reporte de stock bajo incluye menor o igual al mínimo con proveedor habitual o No definido")
    void reporteStockBajoIncluyeBordeYProveedorHabitual() {
        proveedorDAO.agregar(new Proveedor(1, "Frenos y Más", "5775550102", "ventas@frenos.com", "Cll 10", true));
        proveedorDAO.agregar(new Proveedor(2, "Proveedor Inactivo", "5775550109", "inactivo@prov.com", "Cll 11", false));

        repuestoDAO.agregar(repuesto(1, 0, 5, true));   // disponible 3 < 5, con habitual activo
        repuestoDAO.agregar(repuesto(2, 1, 4, true));   // disponible 4 == 4 (borde), sin habitual
        repuestoDAO.agregar(repuesto(3, 0, 2, true));   // disponible 6 > 2 (excluido)
        repuestoDAO.agregar(repuesto(4, 0, 10, false)); // inactivo aunque bajo mínimo (excluido)
        repuestoDAO.agregar(repuesto(5, 0, 3, true));   // disponible 1 < 3, habitual inactivo -> No definido

        loteDAO.agregar(lote(1, hoy.plusDays(30), 3));
        loteDAO.agregar(lote(2, hoy.plusDays(30), 5));
        loteDAO.agregar(lote(3, hoy.plusDays(30), 6));
        loteDAO.agregar(lote(4, hoy.plusDays(30), 1));
        loteDAO.agregar(lote(5, hoy.plusDays(30), 1));

        proveedorRepuestoDAO.agregar(new ProveedorRepuesto(1, 1, new BigDecimal("140000.00"), 4, true));
        proveedorRepuestoDAO.agregar(new ProveedorRepuesto(2, 5, new BigDecimal("50000.00"), 2, true));

        List<StockRepuesto> reporte = service.generarReporteStockBajo();

        assertEquals(3, reporte.size());
        assertEquals(5, reporte.get(0).getRepuesto().getId());
        assertEquals(1, reporte.get(0).getDisponible());
        assertEquals("No definido", reporte.get(0).getProveedorHabitual());

        assertEquals(1, reporte.get(1).getRepuesto().getId());
        assertEquals(3, reporte.get(1).getDisponible());
        assertEquals("Frenos y Más", reporte.get(1).getProveedorHabitual());

        assertEquals(2, reporte.get(2).getRepuesto().getId());
        assertEquals(4, reporte.get(2).getDisponible());
        assertEquals("No definido", reporte.get(2).getProveedorHabitual());
    }

    @Test
    @DisplayName("Reporte de stock bajo retorna lista vacía cuando todos superan su mínimo")
    void reporteStockBajoVacioSiTodosSuperanMinimo() {
        repuestoDAO.agregar(repuesto(1, 0, 3, true));
        loteDAO.agregar(lote(1, hoy.plusDays(20), 10));

        List<StockRepuesto> reporte = service.generarReporteStockBajo();

        assertTrue(reporte.isEmpty());
    }

    private Repuesto repuesto(int id, int reservada, int minimo, boolean activo) {
        return new Repuesto(id, 1, "Repuesto " + id, "Marca", new BigDecimal("1000.00"), reservada, minimo, activo);
    }

    private Lote lote(int repuestoId, LocalDate caducidad, int cantidadActual) {
        Lote lote = new Lote();
        lote.setRepuestoId(repuestoId);
        lote.setProveedorId(1);
        lote.setFechaIngreso(hoy.minusYears(2));
        lote.setFechaCaducidad(caducidad);
        lote.setCantidadInicial(cantidadActual);
        lote.setCantidadActual(cantidadActual);
        return lote;
    }

    private static class LoteDAOMock implements LoteDAO {
        private final List<Lote> datos = new ArrayList<>();
        private int siguienteId = 1;

        void agregar(Lote lote) {
            lote.setId(siguienteId);
            lote.setCodigoLote("LOT-" + siguienteId);
            siguienteId++;
            datos.add(lote);
        }

        @Override
        public Lote buscarPorCodigo(String codigoLote) {
            for (Lote lote : datos) {
                if (lote.getCodigoLote().equalsIgnoreCase(codigoLote)) {
                    return lote;
                }
            }
            return null;
        }

        @Override
        public int guardar(Lote lote) {
            agregar(lote);
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
            for (Repuesto repuesto : datos) {
                if (repuesto.getNombre().equalsIgnoreCase(nombre)) {
                    return repuesto;
                }
            }
            return null;
        }

        @Override
        public int guardar(Repuesto repuesto) {
            datos.add(repuesto);
            return repuesto.getId();
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
            datos.add(proveedor);
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

    private static class ProveedorRepuestoDAOMock implements ProveedorRepuestoDAO {
        private final List<ProveedorRepuesto> datos = new ArrayList<>();

        void agregar(ProveedorRepuesto asociacion) {
            datos.add(asociacion);
        }

        @Override
        public List<ProveedorRepuesto> listarPorProveedor(int proveedorId) {
            List<ProveedorRepuesto> resultado = new ArrayList<>();
            for (ProveedorRepuesto item : datos) {
                if (item.getProveedorId() == proveedorId) {
                    resultado.add(item);
                }
            }
            return resultado;
        }

        @Override
        public List<ProveedorRepuesto> listarPorRepuesto(int repuestoId) {
            List<ProveedorRepuesto> resultado = new ArrayList<>();
            for (ProveedorRepuesto item : datos) {
                if (item.getRepuestoId() == repuestoId) {
                    resultado.add(item);
                }
            }
            return resultado;
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
