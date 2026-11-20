package com.carmotors.sgt.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.carmotors.sgt.exception.DatoDuplicadoException;
import com.carmotors.sgt.exception.EntidadNoEncontradaException;
import com.carmotors.sgt.exception.ValidacionException;
import com.carmotors.sgt.model.ComparacionProveedor;
import com.carmotors.sgt.model.EvaluacionProveedor;
import com.carmotors.sgt.model.Proveedor;
import com.carmotors.sgt.model.ProveedorRepuesto;
import com.carmotors.sgt.model.Repuesto;
import com.carmotors.sgt.dao.EvaluacionProveedorDAO;
import com.carmotors.sgt.dao.ProveedorDAO;
import com.carmotors.sgt.dao.ProveedorRepuestoDAO;
import com.carmotors.sgt.dao.RepuestoDAO;

class ProveedorRepuestoServiceTest {

    private ProveedorRepuestoService service;
    private ProveedorRepuestoDAOMock catalogoDAO;
    private EvaluacionProveedorDAOMock evaluacionDAO;

    @BeforeEach
    void setUp() {
        ProveedorDAOMock proveedorDAO = new ProveedorDAOMock();
        proveedorDAO.agregar(new Proveedor(1, "AutoPartes del Norte", "5775550101", "ventas@autopartesnorte.com", "Calle 1", true));
        proveedorDAO.agregar(new Proveedor(2, "Frenos y Más", "5775550102", "pedidos@frenosymas.com", "Calle 2", true));
        proveedorDAO.agregar(new Proveedor(3, "Lubricantes Andinos", "5775550103", "comercial@lubriandinos.com", "Calle 3", false));

        RepuestoDAOMock repuestoDAO = new RepuestoDAOMock();
        repuestoDAO.agregar(new Repuesto(1, 4, "Aceite sintético 10W-40 (litro)", "Mobil", new BigDecimal("38000.00"), 0, 10, true));
        repuestoDAO.agregar(new Repuesto(2, 4, "Filtro de aceite", "Fram", new BigDecimal("25000.00"), 0, 5, true));

        catalogoDAO = new ProveedorRepuestoDAOMock();
        evaluacionDAO = new EvaluacionProveedorDAOMock();

        // La transacción JDBC real no se puede ejecutar en memoria: se reemplaza solo la apertura
        // de la conexión y se reutilizan los mismos pasos de desmarcar/marcar sobre el DAO en memoria.
        service = new ProveedorRepuestoService(catalogoDAO, proveedorDAO, repuestoDAO, evaluacionDAO) {
            @Override
            protected void ejecutarCambioHabitual(int proveedorId, int repuestoId) {
                aplicarCambioHabitual(catalogoDAO, proveedorId, repuestoId);
            }
        };
    }

    @Test
    @DisplayName("Asocia un repuesto a un proveedor activo con costo a dos decimales y sin marca de habitual")
    void asociarValido() {
        ProveedorRepuesto creada = service.asociar(nuevaAsociacion(1, 1, "26000.5", 3, true));

        assertEquals(new BigDecimal("26000.50"), creada.getCosto());
        assertEquals(3, creada.getTiempoEntregaDias());
        assertFalse(creada.isEsHabitual());
        ProveedorRepuesto guardada = catalogoDAO.buscarPorProveedorYRepuesto(1, 1);
        assertNotNull(guardada);
        assertEquals(new BigDecimal("26000.50"), guardada.getCosto());
    }

    @Test
    @DisplayName("Acepta tiempo de entrega cero (entrega inmediata)")
    void asociarConTiempoCero() {
        ProveedorRepuesto creada = service.asociar(nuevaAsociacion(1, 2, "15000", 0, false));

        assertEquals(0, creada.getTiempoEntregaDias());
    }

    @Test
    @DisplayName("Rechaza proveedor o repuesto inexistente")
    void rechazarProveedorORepuestoInexistente() {
        assertThrows(EntidadNoEncontradaException.class,
                () -> service.asociar(nuevaAsociacion(99, 1, "1000", 1, false)));
        assertThrows(EntidadNoEncontradaException.class,
                () -> service.asociar(nuevaAsociacion(1, 99, "1000", 1, false)));
        assertTrue(catalogoDAO.datos.isEmpty());
    }

    @Test
    @DisplayName("Rechaza asociar un proveedor inactivo")
    void rechazarProveedorInactivo() {
        ValidacionException ex = assertThrows(ValidacionException.class,
                () -> service.asociar(nuevaAsociacion(3, 1, "24000", 2, false)));

        assertTrue(ex.getMessage().contains("inactivo"));
        assertTrue(catalogoDAO.datos.isEmpty());
    }

    @Test
    @DisplayName("Rechaza costo nulo, cero, negativo o que redondea a cero")
    void rechazarCostoNoPositivo() {
        assertThrows(ValidacionException.class, () -> service.asociar(nuevaAsociacion(1, 1, null, 1, false)));
        assertThrows(ValidacionException.class, () -> service.asociar(nuevaAsociacion(1, 1, "0", 1, false)));
        assertThrows(ValidacionException.class, () -> service.asociar(nuevaAsociacion(1, 1, "-500", 1, false)));
        assertThrows(ValidacionException.class, () -> service.asociar(nuevaAsociacion(1, 1, "0.004", 1, false)));
        assertTrue(catalogoDAO.datos.isEmpty());
    }

    @Test
    @DisplayName("Rechaza tiempo de entrega negativo")
    void rechazarTiempoNegativo() {
        assertThrows(ValidacionException.class, () -> service.asociar(nuevaAsociacion(1, 1, "1000", -1, false)));
        assertTrue(catalogoDAO.datos.isEmpty());
    }

    @Test
    @DisplayName("Rechaza repetir el par proveedor-repuesto")
    void rechazarParDuplicado() {
        service.asociar(nuevaAsociacion(1, 1, "26000", 3, false));

        assertThrows(DatoDuplicadoException.class,
                () -> service.asociar(nuevaAsociacion(1, 1, "25000", 2, false)));
        assertEquals(1, catalogoDAO.datos.size());
    }

    @Test
    @DisplayName("Marca un habitual cuando el repuesto no tenía ninguno")
    void marcarHabitualSinAnterior() {
        service.asociar(nuevaAsociacion(1, 1, "26000", 3, false));
        service.asociar(nuevaAsociacion(2, 1, "25500", 4, false));

        ProveedorRepuesto marcada = service.marcarHabitual(2, 1);

        assertTrue(marcada.isEsHabitual());
        assertTrue(catalogoDAO.buscarPorProveedorYRepuesto(2, 1).isEsHabitual());
        assertFalse(catalogoDAO.buscarPorProveedorYRepuesto(1, 1).isEsHabitual());
        assertEquals(1, contarHabituales(1));
    }

    @Test
    @DisplayName("Al cambiar de habitual, el anterior queda desmarcado")
    void cambiarHabitualDesmarcaAnterior() {
        catalogoDAO.datos.add(new ProveedorRepuesto(3, 1, new BigDecimal("24000.00"), 2, false));
        service.asociar(nuevaAsociacion(1, 1, "26000", 3, false));
        service.asociar(nuevaAsociacion(2, 1, "25500", 4, false));
        service.marcarHabitual(1, 1);

        service.marcarHabitual(2, 1);

        assertFalse(catalogoDAO.buscarPorProveedorYRepuesto(1, 1).isEsHabitual());
        assertTrue(catalogoDAO.buscarPorProveedorYRepuesto(2, 1).isEsHabitual());
        assertEquals(1, contarHabituales(1));
    }

    @Test
    @DisplayName("El cambio de habitual de un repuesto no afecta a los de otro repuesto")
    void cambiarHabitualNoAfectaOtroRepuesto() {
        service.asociar(nuevaAsociacion(1, 1, "26000", 3, false));
        service.asociar(nuevaAsociacion(1, 2, "15000", 2, false));
        service.asociar(nuevaAsociacion(2, 1, "25500", 4, false));
        service.marcarHabitual(1, 2);

        service.marcarHabitual(2, 1);

        assertTrue(catalogoDAO.buscarPorProveedorYRepuesto(1, 2).isEsHabitual());
    }

    @Test
    @DisplayName("Rechaza marcar como habitual a un proveedor inactivo y no cambia el habitual actual")
    void rechazarHabitualInactivo() {
        catalogoDAO.datos.add(new ProveedorRepuesto(1, 1, new BigDecimal("26000.00"), 3, true));
        catalogoDAO.datos.add(new ProveedorRepuesto(3, 1, new BigDecimal("24000.00"), 2, false));

        assertThrows(ValidacionException.class, () -> service.marcarHabitual(3, 1));

        assertTrue(catalogoDAO.buscarPorProveedorYRepuesto(1, 1).isEsHabitual());
        assertFalse(catalogoDAO.buscarPorProveedorYRepuesto(3, 1).isEsHabitual());
    }

    @Test
    @DisplayName("Rechaza marcar habitual si el proveedor no ofrece el repuesto o ya es el habitual")
    void rechazarHabitualSinAsociacionOYaHabitual() {
        assertThrows(EntidadNoEncontradaException.class, () -> service.marcarHabitual(1, 1));

        service.asociar(nuevaAsociacion(1, 1, "26000", 3, false));
        service.marcarHabitual(1, 1);
        assertThrows(ValidacionException.class, () -> service.marcarHabitual(1, 1));
        assertEquals(1, contarHabituales(1));
    }

    @Test
    @DisplayName("Actualiza costo y tiempo de entrega validando sus reglas")
    void actualizarCondiciones() {
        service.asociar(nuevaAsociacion(1, 1, "26000", 3, false));

        ProveedorRepuesto actualizada = service.actualizarCondiciones(1, 1, new BigDecimal("27000.555"), 5);

        assertEquals(new BigDecimal("27000.56"), actualizada.getCosto());
        assertEquals(5, catalogoDAO.buscarPorProveedorYRepuesto(1, 1).getTiempoEntregaDias());
        assertThrows(ValidacionException.class,
                () -> service.actualizarCondiciones(1, 1, BigDecimal.ZERO, 5));
        assertThrows(ValidacionException.class,
                () -> service.actualizarCondiciones(1, 1, new BigDecimal("1000"), -2));
        assertEquals(new BigDecimal("27000.56"), catalogoDAO.buscarPorProveedorYRepuesto(1, 1).getCosto());
        assertThrows(EntidadNoEncontradaException.class,
                () -> service.actualizarCondiciones(2, 1, new BigDecimal("1000"), 1));
    }

    @Test
    @DisplayName("Quita una asociación existente y rechaza una inexistente")
    void quitarAsociacion() {
        service.asociar(nuevaAsociacion(1, 1, "26000", 3, false));

        service.quitarAsociacion(1, 1);

        assertNull(catalogoDAO.buscarPorProveedorYRepuesto(1, 1));
        assertThrows(EntidadNoEncontradaException.class, () -> service.quitarAsociacion(1, 1));
    }

    @Test
    @DisplayName("Lista el catálogo por proveedor y por repuesto")
    void listarCatalogo() {
        service.asociar(nuevaAsociacion(1, 1, "26000", 3, false));
        service.asociar(nuevaAsociacion(1, 2, "15000", 2, false));
        service.asociar(nuevaAsociacion(2, 1, "25500", 4, false));

        assertEquals(2, service.listarPorProveedor(1).size());
        assertEquals(2, service.listarPorRepuesto(1).size());
        assertThrows(EntidadNoEncontradaException.class, () -> service.listarPorProveedor(99));
        assertThrows(EntidadNoEncontradaException.class, () -> service.listarPorRepuesto(99));
    }

    private ProveedorRepuesto nuevaAsociacion(int proveedorId, int repuestoId, String costo,
                                              int tiempoEntregaDias, boolean esHabitual) {
        BigDecimal valor = costo == null ? null : new BigDecimal(costo);
        return new ProveedorRepuesto(proveedorId, repuestoId, valor, tiempoEntregaDias, esHabitual);
    }

    private int contarHabituales(int repuestoId) {
        int total = 0;
        for (ProveedorRepuesto item : catalogoDAO.listarPorRepuesto(repuestoId)) {
            if (item.isEsHabitual()) {
                total++;
            }
        }
        return total;
    }

    private static class ProveedorRepuestoDAOMock implements ProveedorRepuestoDAO {
        private final List<ProveedorRepuesto> datos = new ArrayList<>();

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
            datos.add(new ProveedorRepuesto(proveedorRepuesto.getProveedorId(), proveedorRepuesto.getRepuestoId(),
                    proveedorRepuesto.getCosto(), proveedorRepuesto.getTiempoEntregaDias(),
                    proveedorRepuesto.isEsHabitual()));
            return true;
        }

        @Override
        public boolean actualizarCondiciones(ProveedorRepuesto proveedorRepuesto) {
            ProveedorRepuesto actual = buscarPorProveedorYRepuesto(
                    proveedorRepuesto.getProveedorId(), proveedorRepuesto.getRepuestoId());
            if (actual == null) {
                return false;
            }
            actual.setCosto(proveedorRepuesto.getCosto());
            actual.setTiempoEntregaDias(proveedorRepuesto.getTiempoEntregaDias());
            return true;
        }

        @Override
        public boolean eliminar(int proveedorId, int repuestoId) {
            ProveedorRepuesto actual = buscarPorProveedorYRepuesto(proveedorId, repuestoId);
            return actual != null && datos.remove(actual);
        }

        @Override
        public int desmarcarHabitual(int repuestoId) {
            int filas = 0;
            for (ProveedorRepuesto item : datos) {
                if (item.getRepuestoId() == repuestoId && item.isEsHabitual()) {
                    item.setEsHabitual(false);
                    filas++;
                }
            }
            return filas;
        }

        @Override
        public boolean marcarHabitual(int proveedorId, int repuestoId) {
            ProveedorRepuesto actual = buscarPorProveedorYRepuesto(proveedorId, repuestoId);
            if (actual == null) {
                return false;
            }
            actual.setEsHabitual(true);
            return true;
        }
    }

    private static class ProveedorDAOMock implements ProveedorDAO {
        private final List<Proveedor> datos = new ArrayList<>();

        void agregar(Proveedor proveedor) {
            datos.add(proveedor);
        }

        @Override
        public List<Proveedor> listarTodos() { return new ArrayList<>(datos); }

        @Override
        public List<Proveedor> listarActivos() { return new ArrayList<>(datos); }

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
        public Proveedor buscarPorEmail(String email) { return null; }

        @Override
        public int guardar(Proveedor proveedor) { return 0; }

        @Override
        public boolean actualizar(Proveedor proveedor) { return false; }

        @Override
        public boolean desactivar(int id) { return false; }

        @Override
        public boolean reactivar(int id) { return false; }
    }

    private static class RepuestoDAOMock implements RepuestoDAO {
        private final List<Repuesto> datos = new ArrayList<>();

        void agregar(Repuesto repuesto) {
            datos.add(repuesto);
        }

        @Override
        public List<Repuesto> listarTodos() { return new ArrayList<>(datos); }

        @Override
        public List<Repuesto> listarActivos() { return new ArrayList<>(datos); }

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
        public Repuesto buscarPorNombre(String nombre) { return null; }

        @Override
        public int guardar(Repuesto repuesto) { return 0; }

        @Override
        public boolean actualizar(Repuesto repuesto) { return false; }

        @Override
        public boolean desactivar(int id) { return false; }

        @Override
        public boolean reactivar(int id) { return false; }

        @Override
        public boolean descontarReserva(int repuestoId, int cantidad) { return false; }

        @Override
        public boolean aumentarReservado(int repuestoId, int cantidad) {
            return false;
        }
    }

    // --- Pruebas para T3.2: Comparación de proveedores por repuesto (RF-30, RN-16, RN-17, RNF-11) ---

    @Test
    @DisplayName("T3.2: Compara proveedores activos ordenándolos por costo ascendente")
    void compararProveedoresOrdenadosPorCosto() {
        // Proveedor 1 con costo $35000.00
        service.asociar(nuevaAsociacion(1, 1, "35000.00", 3, false));
        // Proveedor 2 con costo $28000.00
        service.asociar(nuevaAsociacion(2, 1, "28000.00", 5, false));

        List<ComparacionProveedor> comparacion = service.compararProveedoresPorRepuesto(1);

        assertNotNull(comparacion);
        assertEquals(2, comparacion.size());
        // El de menor costo ($28000.00, Proveedor 2) debe ser el primero
        assertEquals(2, comparacion.get(0).getProveedorId());
        assertEquals("Frenos y Más", comparacion.get(0).getNombreProveedor());
        assertEquals(new BigDecimal("28000.00"), comparacion.get(0).getCosto());

        // El de mayor costo ($35000.00, Proveedor 1) debe ser el segundo
        assertEquals(1, comparacion.get(1).getProveedorId());
        assertEquals("AutoPartes del Norte", comparacion.get(1).getNombreProveedor());
        assertEquals(new BigDecimal("35000.00"), comparacion.get(1).getCosto());
    }

    @Test
    @DisplayName("T3.2: Excluye proveedores inactivos aunque estén asociados al repuesto (RN-17)")
    void compararProveedoresExcluyeInactivos() {
        // Asociamos proveedor activo 1
        service.asociar(nuevaAsociacion(1, 1, "30000.00", 2, false));
        // Asociamos directamente en el mock al proveedor 3 (que está inactivo)
        catalogoDAO.guardar(nuevaAsociacion(3, 1, "20000.00", 1, false));

        List<ComparacionProveedor> comparacion = service.compararProveedoresPorRepuesto(1);

        assertEquals(1, comparacion.size());
        assertEquals(1, comparacion.get(0).getProveedorId());
    }

    @Test
    @DisplayName("T3.2: Calcula promedio de evaluaciones con 1 decimal o null si no tiene evaluaciones")
    void compararProveedoresPromedioEvaluacionYFormato() {
        // Proveedor 1 con costo 30000.00 y 2 evaluaciones
        service.asociar(nuevaAsociacion(1, 1, "30000.00", 2, false));
        evaluacionDAO.agregar(new EvaluacionProveedor(1, 1, null, 5, 5, "Excelente")); // Puntaje 5.0
        evaluacionDAO.agregar(new EvaluacionProveedor(2, 1, null, 4, 4, "Bueno"));     // Puntaje 4.0
        // Promedio esperado: (5.0 + 4.0) / 2 = 4.5

        // Proveedor 2 con costo 40000.00 sin evaluaciones
        service.asociar(nuevaAsociacion(2, 1, "40000.00", 4, false));

        List<ComparacionProveedor> comparacion = service.compararProveedoresPorRepuesto(1);

        assertEquals(2, comparacion.size());

        ComparacionProveedor compProv1 = comparacion.get(0);
        assertEquals(1, compProv1.getProveedorId());
        assertEquals(new BigDecimal("4.5"), compProv1.getPromedioEvaluacion());
        assertEquals(2, compProv1.getCantidadEvaluaciones());

        ComparacionProveedor compProv2 = comparacion.get(1);
        assertEquals(2, compProv2.getProveedorId());
        assertNull(compProv2.getPromedioEvaluacion());
        assertEquals(0, compProv2.getCantidadEvaluaciones());
    }

    @Test
    @DisplayName("T3.2: Retorna lista vacía si el repuesto no tiene proveedores activos")
    void compararProveedoresRepuestoSinProveedoresActivos() {
        List<ComparacionProveedor> comparacion = service.compararProveedoresPorRepuesto(2);
        assertNotNull(comparacion);
        assertTrue(comparacion.isEmpty());
    }

    @Test
    @DisplayName("T3.2: Lanza EntidadNoEncontradaException si el repuesto no existe")
    void compararProveedoresRepuestoInexistente() {
        assertThrows(EntidadNoEncontradaException.class,
                () -> service.compararProveedoresPorRepuesto(999));
    }

    @Test
    @DisplayName("T3.2: Lanza ValidacionException si el id del repuesto es menor o igual a cero")
    void compararProveedoresRepuestoIdInvalido() {
        assertThrows(ValidacionException.class,
                () -> service.compararProveedoresPorRepuesto(0));
        assertThrows(ValidacionException.class,
                () -> service.compararProveedoresPorRepuesto(-5));
    }

    private static class EvaluacionProveedorDAOMock implements EvaluacionProveedorDAO {
        private final List<EvaluacionProveedor> datos = new ArrayList<>();

        void agregar(EvaluacionProveedor evaluacion) {
            datos.add(evaluacion);
        }

        @Override
        public List<EvaluacionProveedor> listarTodos() {
            return new ArrayList<>(datos);
        }

        @Override
        public List<EvaluacionProveedor> listarPorProveedor(int proveedorId) {
            List<EvaluacionProveedor> resultado = new ArrayList<>();
            for (EvaluacionProveedor ev : datos) {
                if (ev.getProveedorId() == proveedorId) {
                    resultado.add(ev);
                }
            }
            return resultado;
        }

        @Override
        public EvaluacionProveedor buscarPorId(int id) {
            for (EvaluacionProveedor ev : datos) {
                if (ev.getId() == id) {
                    return ev;
                }
            }
            return null;
        }

        @Override
        public int guardar(EvaluacionProveedor evaluacion) {
            datos.add(evaluacion);
            return evaluacion.getId();
        }
    }
}
