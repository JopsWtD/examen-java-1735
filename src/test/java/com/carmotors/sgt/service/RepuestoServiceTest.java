package com.carmotors.sgt.service;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.carmotors.sgt.exception.DatoDuplicadoException;
import com.carmotors.sgt.exception.EntidadNoEncontradaException;
import com.carmotors.sgt.exception.ValidacionException;
import com.carmotors.sgt.model.Proveedor;
import com.carmotors.sgt.model.ProveedorRepuesto;
import com.carmotors.sgt.model.Repuesto;
import com.carmotors.sgt.model.RepuestoTipo;
import com.carmotors.sgt.dao.ProveedorDAO;
import com.carmotors.sgt.dao.ProveedorRepuestoDAO;
import com.carmotors.sgt.dao.RepuestoDAO;
import com.carmotors.sgt.dao.RepuestoTipoDAO;

class RepuestoServiceTest {

    private RepuestoDAOMock repuestoDAO;
    private RepuestoTipoDAOEnMemoria tipoRepository;
    private ProveedorRepuestoDAOMock proveedorRepuestoDAO;
    private ProveedorDAOMock proveedorDAO;
    private RepuestoService service;

    @BeforeEach
    void setUp() {
        repuestoDAO = new RepuestoDAOMock();
        tipoRepository = new RepuestoTipoDAOEnMemoria();
        tipoRepository.agregar(new RepuestoTipo(1, "Filtro"));
        tipoRepository.agregar(new RepuestoTipo(2, "Batería"));
        tipoRepository.agregar(new RepuestoTipo(3, "Mecánico"));
        proveedorRepuestoDAO = new ProveedorRepuestoDAOMock();
        proveedorDAO = new ProveedorDAOMock();
        service = new RepuestoService(repuestoDAO, tipoRepository, proveedorRepuestoDAO, proveedorDAO);
    }

    @Test
    @DisplayName("Debe crear un repuesto válido normalizando precio a escala 2 y valores por defecto")
    void crearRepuestoValidoNormalizaPrecioYValoresPorDefecto() {
        Repuesto repuesto = new Repuesto();
        repuesto.setRepuestoTipoId(1);
        repuesto.setNombre("  Filtro de aceite  ");
        repuesto.setMarca("  Bosch  ");
        repuesto.setPrecioVenta(new BigDecimal("15000.556"));
        repuesto.setStockMinimo(5);

        Repuesto creado = service.crearRepuesto(repuesto);

        assertEquals(1, creado.getId());
        assertEquals("Filtro de aceite", creado.getNombre());
        assertEquals("Bosch", creado.getMarca());
        assertEquals(new BigDecimal("15000.56"), creado.getPrecioVenta());
        assertEquals(2, creado.getPrecioVenta().scale());
        assertEquals(0, creado.getCantidadReservada());
        assertTrue(creado.isActivo());
    }

    @Test
    @DisplayName("Debe rechazar precios negativos, stock mínimo negativo, nombre o marca vacíos y tipo inexistente")
    void rechazaValidacionesDeDominio() {
        Repuesto precioInvalido = new Repuesto();
        precioInvalido.setRepuestoTipoId(1);
        precioInvalido.setNombre("Filtro");
        precioInvalido.setMarca("Bosch");
        precioInvalido.setPrecioVenta(new BigDecimal("0.00"));
        assertThrows(ValidacionException.class, () -> service.crearRepuesto(precioInvalido));

        Repuesto stockInvalido = new Repuesto();
        stockInvalido.setRepuestoTipoId(1);
        stockInvalido.setNombre("Filtro");
        stockInvalido.setMarca("Bosch");
        stockInvalido.setPrecioVenta(new BigDecimal("1000.00"));
        stockInvalido.setStockMinimo(-1);
        assertThrows(ValidacionException.class, () -> service.crearRepuesto(stockInvalido));

        Repuesto nombreVacio = new Repuesto();
        nombreVacio.setRepuestoTipoId(1);
        nombreVacio.setNombre("   ");
        nombreVacio.setMarca("Bosch");
        nombreVacio.setPrecioVenta(new BigDecimal("1000.00"));
        assertThrows(ValidacionException.class, () -> service.crearRepuesto(nombreVacio));

        Repuesto marcaVacia = new Repuesto();
        marcaVacia.setRepuestoTipoId(1);
        marcaVacia.setNombre("Filtro");
        marcaVacia.setMarca("   ");
        marcaVacia.setPrecioVenta(new BigDecimal("1000.00"));
        assertThrows(ValidacionException.class, () -> service.crearRepuesto(marcaVacia));

        Repuesto tipoInvalido = new Repuesto();
        tipoInvalido.setRepuestoTipoId(99);
        tipoInvalido.setNombre("Filtro");
        tipoInvalido.setMarca("Bosch");
        tipoInvalido.setPrecioVenta(new BigDecimal("1000.00"));
        assertThrows(ValidacionException.class, () -> service.crearRepuesto(tipoInvalido));
    }

    @Test
    @DisplayName("Debe rechazar nombres duplicados al crear y al actualizar")
    void rechazaRepuestosDuplicados() {
        Repuesto original = new Repuesto();
        original.setRepuestoTipoId(1);
        original.setNombre("Filtro de aceite");
        original.setMarca("Bosch");
        original.setPrecioVenta(new BigDecimal("5000.00"));
        original.setStockMinimo(2);
        service.crearRepuesto(original);

        Repuesto duplicado = new Repuesto();
        duplicado.setRepuestoTipoId(2);
        duplicado.setNombre("  filtro de aceite  ");
        duplicado.setMarca("Atlas");
        duplicado.setPrecioVenta(new BigDecimal("5200.00"));
        duplicado.setStockMinimo(2);
        assertThrows(DatoDuplicadoException.class, () -> service.crearRepuesto(duplicado));

        Repuesto otro = new Repuesto();
        otro.setRepuestoTipoId(2);
        otro.setNombre("Batería de prueba");
        otro.setMarca("Moura");
        otro.setPrecioVenta(new BigDecimal("3200.00"));
        otro.setStockMinimo(1);
        Repuesto segundo = service.crearRepuesto(otro);

        Repuesto actualizado = new Repuesto();
        actualizado.setId(segundo.getId());
        actualizado.setRepuestoTipoId(2);
        actualizado.setNombre("Filtro de aceite");
        actualizado.setMarca("Bosch");
        actualizado.setPrecioVenta(new BigDecimal("6000.00"));
        actualizado.setStockMinimo(4);
        assertThrows(DatoDuplicadoException.class, () -> service.actualizarRepuesto(actualizado));
    }

    @Test
    @DisplayName("Debe preservar cantidadReservada y activo al actualizar y lanzar entidad no encontrada si no existe")
    void actualizarConservaValoresYBuscaInexistentes() {
        Repuesto original = new Repuesto();
        original.setRepuestoTipoId(1);
        original.setNombre("Filtro de aceite");
        original.setMarca("Bosch");
        original.setPrecioVenta(new BigDecimal("5000.00"));
        original.setCantidadReservada(3);
        original.setStockMinimo(2);
        original.setActivo(true);
        Repuesto creado = service.crearRepuesto(original);

        Repuesto cambios = new Repuesto();
        cambios.setId(creado.getId());
        cambios.setRepuestoTipoId(2);
        cambios.setNombre("Filtro de aire");
        cambios.setMarca("Mann");
        cambios.setPrecioVenta(new BigDecimal("6000.50"));
        cambios.setStockMinimo(10);
        cambios.setCantidadReservada(77);
        cambios.setActivo(false);

        Repuesto actualizado = service.actualizarRepuesto(cambios);

        assertEquals(0, actualizado.getCantidadReservada());
        assertTrue(actualizado.isActivo());
        assertEquals("Filtro de aire", actualizado.getNombre());

        assertThrows(EntidadNoEncontradaException.class, () -> service.buscarPorId(999));
        assertThrows(EntidadNoEncontradaException.class, () -> service.actualizarRepuesto(new Repuesto(999, 1, "X", "Y", new BigDecimal("10.00"), 0, 1, true)));
        assertThrows(EntidadNoEncontradaException.class, () -> service.desactivarRepuesto(999));
        assertThrows(EntidadNoEncontradaException.class, () -> service.reactivarRepuesto(999));
    }

    @Test
    @DisplayName("Debe desactivar y reactivar un repuesto sin borrar su registro")
    void desactivarYReactivarRepuesto() {
        Repuesto repuesto = new Repuesto();
        repuesto.setRepuestoTipoId(1);
        repuesto.setNombre("Filtro de aceite");
        repuesto.setMarca("Bosch");
        repuesto.setPrecioVenta(new BigDecimal("5000.00"));
        repuesto.setStockMinimo(2);
        Repuesto creado = service.crearRepuesto(repuesto);

        Repuesto desactivado = service.desactivarRepuesto(creado.getId());
        assertTrue(!desactivado.isActivo());

        Repuesto reactivado = service.reactivarRepuesto(creado.getId());
        assertTrue(reactivado.isActivo());
    }

    @Test
    @DisplayName("Debe rechazar precio que redondea a 0.00, precio fuera de DECIMAL(12,2) y textos numéricos")
    void rechazarPrecioRedondeadoACeroYTextosNumericos() {
        Repuesto precioRedondeadoCero = new Repuesto(0, 1, "Bujía", "NGK", new BigDecimal("0.004"), 0, 1, true);
        assertThrows(ValidacionException.class, () -> service.crearRepuesto(precioRedondeadoCero));

        Repuesto precioDesbordado = new Repuesto(0, 1, "Bujía", "NGK", new BigDecimal("10000000000.00"), 0, 1, true);
        assertThrows(ValidacionException.class, () -> service.crearRepuesto(precioDesbordado));

        Repuesto nombreNumerico = new Repuesto(0, 1, "0", "NGK", new BigDecimal("25000.00"), 0, 1, true);
        assertThrows(ValidacionException.class, () -> service.crearRepuesto(nombreNumerico));
    }

    @Test
    @DisplayName("Debe filtrar repuestos por nombre, tipo, marca y proveedor de forma insensible a mayúsculas y tildes")
    void filtraRepuestosPorCadaCriterioYCombinaciones() {
        Repuesto r1 = crearRepuestoBase(1, "Filtro de aceite sintético", "Bosch");
        Repuesto r2 = crearRepuestoBase(2, "Batería 12V 45Ah", "Varta");
        Repuesto r3 = crearRepuestoBase(3, "Pastillas de freno delanteras", "Bosch");

        proveedorDAO.agregar(new Proveedor(1, "AutoPartes del Norte", "5775550101", "ventas@norte.com", "Av 5", true));
        proveedorDAO.agregar(new Proveedor(2, "Frenos y Más", "5775550102", "ventas@frenos.com", "Cll 10", true));

        proveedorRepuestoDAO.agregar(new ProveedorRepuesto(1, r1.getId(), new BigDecimal("20000.00"), 2, true));
        proveedorRepuestoDAO.agregar(new ProveedorRepuesto(2, r3.getId(), new BigDecimal("78000.00"), 3, true));
        proveedorRepuestoDAO.agregar(new ProveedorRepuesto(1, r3.getId(), new BigDecimal("85000.00"), 4, false));

        List<Repuesto> porNombre = service.buscarPorFiltros("SINTETICO", null, "", null);
        assertEquals(1, porNombre.size());
        assertEquals(r1.getId(), porNombre.get(0).getId());

        List<Repuesto> porTipo = service.buscarPorFiltros(null, "bateria", null, null);
        assertEquals(1, porTipo.size());
        assertEquals(r2.getId(), porTipo.get(0).getId());

        List<Repuesto> porMarca = service.buscarPorFiltros("", "", "bOsCh", "");
        assertEquals(2, porMarca.size());

        List<Repuesto> porProveedor = service.buscarPorFiltros(null, null, null, "frenos y mas");
        assertEquals(1, porProveedor.size());
        assertEquals(r3.getId(), porProveedor.get(0).getId());

        List<Repuesto> combinado = service.buscarPorFiltros("pastillas", "mecanico", "BOSCH", "AutoPartes");
        assertEquals(1, combinado.size());
        assertEquals(r3.getId(), combinado.get(0).getId());

        List<Repuesto> sinFiltros = service.buscarPorFiltros(null, "", "  ", null);
        assertEquals(3, sinFiltros.size());

        List<Repuesto> sinCoincidencias = service.buscarPorFiltros("amortiguador", null, null, "Frenos");
        assertTrue(sinCoincidencias.isEmpty());

        List<Repuesto> proveedorInexistente = service.buscarPorFiltros(null, null, null, "NoExiste");
        assertTrue(proveedorInexistente.isEmpty());
    }

    private Repuesto crearRepuestoBase(int tipoId, String nombre, String marca) {
        Repuesto repuesto = new Repuesto();
        repuesto.setRepuestoTipoId(tipoId);
        repuesto.setNombre(nombre);
        repuesto.setMarca(marca);
        repuesto.setPrecioVenta(new BigDecimal("50000.00"));
        repuesto.setStockMinimo(2);
        return service.crearRepuesto(repuesto);
    }

    private static class RepuestoTipoDAOEnMemoria implements RepuestoTipoDAO {
        private final List<RepuestoTipo> datos = new ArrayList<>();

        void agregar(RepuestoTipo tipo) {
            datos.add(tipo);
        }

        @Override
        public List<RepuestoTipo> listarTodos() {
            return new ArrayList<>(datos);
        }

        @Override
        public RepuestoTipo buscarPorId(int id) {
            for (RepuestoTipo tipo : datos) {
                if (tipo.getId() == id) {
                    return tipo;
                }
            }
            return null;
        }
    }

    private static class RepuestoDAOMock implements RepuestoDAO {
        private final List<Repuesto> datos = new ArrayList<>();
        private int siguienteId = 1;

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
            if (nombre == null) {
                return null;
            }
            for (Repuesto repuesto : datos) {
                if (repuesto.getNombre() != null && repuesto.getNombre().equalsIgnoreCase(nombre)) {
                    return repuesto;
                }
            }
            return null;
        }

        @Override
        public int guardar(Repuesto repuesto) {
            int id = siguienteId++;
            Repuesto copia = new Repuesto(
                    id,
                    repuesto.getRepuestoTipoId(),
                    repuesto.getNombre(),
                    repuesto.getMarca(),
                    repuesto.getPrecioVenta(),
                    repuesto.getCantidadReservada(),
                    repuesto.getStockMinimo(),
                    repuesto.isActivo());
            datos.add(copia);
            return id;
        }

        @Override
        public boolean actualizar(Repuesto repuesto) {
            for (int i = 0; i < datos.size(); i++) {
                if (datos.get(i).getId() == repuesto.getId()) {
                    datos.set(i, new Repuesto(
                            repuesto.getId(),
                            repuesto.getRepuestoTipoId(),
                            repuesto.getNombre(),
                            repuesto.getMarca(),
                            repuesto.getPrecioVenta(),
                            datos.get(i).getCantidadReservada(),
                            repuesto.getStockMinimo(),
                            datos.get(i).isActivo()));
                    return true;
                }
            }
            return false;
        }

        @Override
        public boolean desactivar(int id) {
            for (Repuesto repuesto : datos) {
                if (repuesto.getId() == id) {
                    repuesto.setActivo(false);
                    return true;
                }
            }
            return false;
        }

        @Override
        public boolean reactivar(int id) {
            for (Repuesto repuesto : datos) {
                if (repuesto.getId() == id) {
                    repuesto.setActivo(true);
                    return true;
                }
            }
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
