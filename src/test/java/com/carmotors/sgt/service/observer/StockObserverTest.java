package com.carmotors.sgt.service.observer;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.carmotors.sgt.model.Lote;
import com.carmotors.sgt.model.Proveedor;
import com.carmotors.sgt.model.ProveedorRepuesto;
import com.carmotors.sgt.model.Repuesto;
import com.carmotors.sgt.model.StockRepuesto;
import com.carmotors.sgt.dao.LoteDAO;
import com.carmotors.sgt.dao.ProveedorDAO;
import com.carmotors.sgt.dao.ProveedorRepuestoDAO;
import com.carmotors.sgt.dao.RepuestoDAO;
import com.carmotors.sgt.service.InventarioService;

class StockObserverTest {

    private InventarioService inventarioService;
    private RepuestoDAOTest repuestoDAO;
    private LoteDAOTest loteDAO;
    private ProveedorRepuestoDAOTest proveedorRepuestoDAO;
    private ProveedorDAOTest proveedorDAO;

    @BeforeEach
    void setUp() {
        repuestoDAO = new RepuestoDAOTest();
        loteDAO = new LoteDAOTest();
        proveedorRepuestoDAO = new ProveedorRepuestoDAOTest();
        proveedorDAO = new ProveedorDAOTest();

        inventarioService = new InventarioService(repuestoDAO, loteDAO, proveedorRepuestoDAO, proveedorDAO);
    }

    @Test
    @DisplayName("Debe notificar a los observadores cuando el disponible es menor o igual al stock mínimo en repuesto activo")
    void notificarCuandoDisponibleMenorOIgualMinimo() {
        Repuesto repuesto = new Repuesto(1, 1, "Filtro de Aceite", "Bosch", BigDecimal.valueOf(30000), 0, 5, true);
        repuestoDAO.agregar(repuesto);

        Proveedor proveedor = new Proveedor(10, "AutoPartes Ltda", "12345", "ventas@autopartes.com", "Calle 10", true);
        proveedorDAO.agregar(proveedor);

        ProveedorRepuesto asociacion = new ProveedorRepuesto(10, 1, BigDecimal.valueOf(20000), 2, true);
        proveedorRepuestoDAO.agregar(asociacion);

        // Lote que genera disponible = 3 (menor que mínimo = 5)
        loteDAO.fijarStock(1, 3, 3);

        StockObserverEspia espia = new StockObserverEspia();
        inventarioService.agregarObserver(espia);

        StockRepuesto resultado = inventarioService.evaluarYNotificarStockBajo(1);

        assertNotNull(resultado);
        assertEquals(1, espia.alertas.size());
        StockRepuesto notificado = espia.alertas.get(0);
        assertEquals(1, notificado.getRepuesto().getId());
        assertEquals(3, notificado.getDisponible());
        assertEquals(5, notificado.getStockMinimo());
        assertEquals("AutoPartes Ltda", notificado.getProveedorHabitual());
    }

    @Test
    @DisplayName("No debe notificar si el disponible supera el stock mínimo")
    void noNotificarCuandoDisponibleSuperaMinimo() {
        Repuesto repuesto = new Repuesto(2, 1, "Bujía NGK", "NGK", BigDecimal.valueOf(15000), 0, 4, true);
        repuestoDAO.agregar(repuesto);

        // Lote con disponible = 10 (mayor que mínimo = 4)
        loteDAO.fijarStock(2, 10, 10);

        StockObserverEspia espia = new StockObserverEspia();
        inventarioService.agregarObserver(espia);

        inventarioService.evaluarYNotificarStockBajo(2);

        assertTrue(espia.alertas.isEmpty(), "No debe disparar alerta si el disponible supera el mínimo.");
    }

    @Test
    @DisplayName("No debe notificar si el repuesto está inactivo aunque el stock sea bajo")
    void noNotificarCuandoRepuestoInactivo() {
        Repuesto inactivo = new Repuesto(3, 1, "Correa Descontinuada", "Gates", BigDecimal.valueOf(45000), 0, 5, false);
        repuestoDAO.agregar(inactivo);

        // Stock bajo pero repuesto inactivo
        loteDAO.fijarStock(3, 1, 1);

        StockObserverEspia espia = new StockObserverEspia();
        inventarioService.agregarObserver(espia);

        inventarioService.evaluarYNotificarStockBajo(3);

        assertTrue(espia.alertas.isEmpty(), "No debe disparar alerta para repuestos inactivos.");
    }

    @Test
    @DisplayName("Debe dejar de notificar tras desuscribir un observador con eliminarObserver")
    void desuscribirObserver() {
        Repuesto repuesto = new Repuesto(4, 1, "Pastillas Freno", "Brembo", BigDecimal.valueOf(80000), 0, 6, true);
        repuestoDAO.agregar(repuesto);
        loteDAO.fijarStock(4, 2, 2);

        StockObserverEspia espia = new StockObserverEspia();
        inventarioService.agregarObserver(espia);
        assertEquals(1, inventarioService.getObservers().size());

        inventarioService.eliminarObserver(espia);
        assertEquals(0, inventarioService.getObservers().size());

        inventarioService.evaluarYNotificarStockBajo(4);

        assertTrue(espia.alertas.isEmpty(), "El observador desuscrito no debe recibir alertas.");
    }

    @Test
    @DisplayName("Debe ignorar observador nulo y evitar suscripciones duplicadas")
    void ignorarNuloYDuplicados() {
        StockObserverEspia espia = new StockObserverEspia();
        inventarioService.agregarObserver(null);
        assertEquals(0, inventarioService.getObservers().size());

        inventarioService.agregarObserver(espia);
        inventarioService.agregarObserver(espia); // Intento duplicado
        assertEquals(1, inventarioService.getObservers().size());

        inventarioService.eliminarObserver(null);
        assertEquals(1, inventarioService.getObservers().size());
    }

    @Test
    @DisplayName("Un fallo en un observador no debe impedir la notificación a los demás observadores")
    void falloEnUnObserverNoAfectaOtros() {
        Repuesto repuesto = new Repuesto(5, 1, "Amortiguador", "Monroe", BigDecimal.valueOf(120000), 0, 2, true);
        repuestoDAO.agregar(repuesto);
        loteDAO.fijarStock(5, 1, 1);

        StockObserver observadorFallido = stock -> {
            throw new RuntimeException("Error simulado en observador");
        };
        StockObserverEspia espiaValido = new StockObserverEspia();

        inventarioService.agregarObserver(observadorFallido);
        inventarioService.agregarObserver(espiaValido);

        StockRepuesto resultado = inventarioService.evaluarYNotificarStockBajo(5);

        assertNotNull(resultado);
        assertEquals(1, espiaValido.alertas.size(), "El observador válido debe haber recibido la alerta.");
    }

    @Test
    @DisplayName("Un observador que se desuscribe durante la notificación no debe interrumpir a los demás")
    void desuscribirDuranteNotificacionNoInterrumpe() {
        Repuesto repuesto = new Repuesto(7, 1, "Bomba de agua", "ACDelco", BigDecimal.valueOf(95000), 0, 3, true);
        repuestoDAO.agregar(repuesto);
        loteDAO.fijarStock(7, 1, 1);

        StockObserverEspia espiaValido = new StockObserverEspia();
        inventarioService.agregarObserver(new ObserverQueSeDesuscribe());
        inventarioService.agregarObserver(espiaValido);

        StockRepuesto resultado = inventarioService.evaluarYNotificarStockBajo(7);

        assertNotNull(resultado);
        assertEquals(1, espiaValido.alertas.size(),
                "La copia defensiva debe permitir notificar al resto de observadores.");
        assertEquals(1, inventarioService.getObservers().size(),
                "El observador que se desuscribió no debe permanecer en la lista.");
    }

    @Test
    @DisplayName("AlertaStockBajoObserver debe imprimir formato completo de alerta por consola")
    void alertaStockBajoObserverImprimeFormatoCorrecto() {
        Repuesto repuesto = new Repuesto(6, 1, "Líquido de frenos", "Wagner", BigDecimal.valueOf(25000), 2, 5, true);
        StockRepuesto stock = new StockRepuesto(repuesto, 4, 4, 2, 2, 0, 5, "Frenos del Valle");

        PrintStream originalOut = System.out;
        ByteArrayOutputStream salida = new ByteArrayOutputStream();
        try {
            System.setOut(new PrintStream(salida));
            AlertaStockBajoObserver observer = new AlertaStockBajoObserver();
            observer.onStockBajo(stock);
            observer.onStockBajo(null); // Seguridad con nulos
        } finally {
            System.setOut(originalOut);
        }

        String texto = salida.toString();
        assertTrue(texto.contains("ALERTA DE STOCK BAJO — RN-22"));
        assertTrue(texto.contains("Líquido de frenos"));
        assertTrue(texto.contains("Frenos del Valle"));
        assertTrue(texto.contains("Disponible:         2"));
    }

    private class ObserverQueSeDesuscribe implements StockObserver {
        @Override
        public void onStockBajo(StockRepuesto stock) {
            inventarioService.eliminarObserver(this);
        }
    }

    private static class StockObserverEspia implements StockObserver {
        private final List<StockRepuesto> alertas = new ArrayList<>();

        @Override
        public void onStockBajo(StockRepuesto stock) {
            alertas.add(stock);
        }
    }

    private static class RepuestoDAOTest implements RepuestoDAO {
        private final List<Repuesto> datos = new ArrayList<>();

        void agregar(Repuesto r) {
            datos.add(r);
        }

        @Override
        public List<Repuesto> listarTodos() {
            return datos;
        }

        @Override
        public List<Repuesto> listarActivos() {
            List<Repuesto> activos = new ArrayList<>();
            for (Repuesto r : datos) {
                if (r.isActivo()) activos.add(r);
            }
            return activos;
        }

        @Override
        public Repuesto buscarPorId(int id) {
            for (Repuesto r : datos) {
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

    private static class LoteDAOTest implements LoteDAO {
        private int stockFisico = 0;
        private int stockOperativo = 0;

        void fijarStock(int repuestoId, int fisico, int operativo) {
            this.stockFisico = fisico;
            this.stockOperativo = operativo;
        }

        @Override
        public Lote buscarPorCodigo(String codigoLote) {
            return null;
        }

        @Override
        public List<Lote> listarPorRepuesto(int repuestoId) {
            return new ArrayList<>();
        }

        @Override
        public int guardar(Lote lote) {
            return 0;
        }

        @Override
        public int sumarStockFisico(int repuestoId) {
            return stockFisico;
        }

        @Override
        public int sumarStockOperativo(int repuestoId, LocalDate fechaReferencia) {
            return stockOperativo;
        }

        @Override
        public List<Lote> listarPorVencer(LocalDate fechaLimite) {
            return new ArrayList<>();
        }

        @Override
        public boolean descontarUnidades(int loteId, int cantidad) {
            return false;
        }
    }

    private static class ProveedorRepuestoDAOTest implements ProveedorRepuestoDAO {
        private final List<ProveedorRepuesto> datos = new ArrayList<>();

        void agregar(ProveedorRepuesto pr) {
            datos.add(pr);
        }

        @Override
        public List<ProveedorRepuesto> listarPorProveedor(int proveedorId) {
            return datos;
        }

        @Override
        public List<ProveedorRepuesto> listarPorRepuesto(int repuestoId) {
            List<ProveedorRepuesto> res = new ArrayList<>();
            for (ProveedorRepuesto pr : datos) {
                if (pr.getRepuestoId() == repuestoId) res.add(pr);
            }
            return res;
        }

        @Override
        public ProveedorRepuesto buscarPorProveedorYRepuesto(int proveedorId, int repuestoId) {
            for (ProveedorRepuesto pr : datos) {
                if (pr.getProveedorId() == proveedorId && pr.getRepuestoId() == repuestoId) return pr;
            }
            return null;
        }

        @Override
        public boolean guardar(ProveedorRepuesto proveedorRepuesto) {
            return false;
        }

        @Override
        public boolean actualizarCondiciones(ProveedorRepuesto proveedorRepuesto) {
            return false;
        }

        @Override
        public boolean eliminar(int proveedorId, int repuestoId) {
            return false;
        }

        @Override
        public int desmarcarHabitual(int repuestoId) {
            return 0;
        }

        @Override
        public boolean marcarHabitual(int proveedorId, int repuestoId) {
            return false;
        }
    }

    private static class ProveedorDAOTest implements ProveedorDAO {
        private final List<Proveedor> datos = new ArrayList<>();

        void agregar(Proveedor p) {
            datos.add(p);
        }

        @Override
        public List<Proveedor> listarTodos() {
            return datos;
        }

        @Override
        public List<Proveedor> listarActivos() {
            return datos;
        }

        @Override
        public Proveedor buscarPorId(int id) {
            for (Proveedor p : datos) {
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
}
