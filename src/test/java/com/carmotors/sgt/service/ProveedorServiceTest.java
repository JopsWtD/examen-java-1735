package com.carmotors.sgt.service;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.Test;

import com.carmotors.sgt.exception.DatoDuplicadoException;
import com.carmotors.sgt.exception.EntidadNoEncontradaException;
import com.carmotors.sgt.exception.ValidacionException;
import com.carmotors.sgt.model.Proveedor;
import com.carmotors.sgt.model.ProveedorRepuesto;
import com.carmotors.sgt.dao.ProveedorDAO;
import com.carmotors.sgt.dao.ProveedorRepuestoDAO;

class ProveedorServiceTest {

    @Test
    void crearProveedorConNombreVacioDebeLanzarExcepcion() {
        ProveedorService service = new ProveedorService(new ProveedorDAOMock());
        Proveedor proveedor = new Proveedor();
        proveedor.setNombre(" ");
        proveedor.setEmail("proveedor@dominio.com");

        ValidacionException ex = assertThrows(ValidacionException.class,
                () -> service.crearProveedor(proveedor));

        assertTrue(ex.getMessage().toLowerCase().contains("nombre"));
    }

    @Test
    void crearProveedorConEmailInvalidoDebeLanzarExcepcion() {
        ProveedorService service = new ProveedorService(new ProveedorDAOMock());
        Proveedor proveedor = new Proveedor();
        proveedor.setNombre("Distribuidora Uno");
        proveedor.setEmail("correo-no-valido");

        ValidacionException ex = assertThrows(ValidacionException.class,
                () -> service.crearProveedor(proveedor));

        assertTrue(ex.getMessage().toLowerCase().contains("email"));
    }

    @Test
    void crearProveedorSinEmailDebeGuardarEmailNullYPermitirMultiples() {
        ProveedorDAOMock dao = new ProveedorDAOMock();
        ProveedorService service = new ProveedorService(dao);

        Proveedor primero = new Proveedor();
        primero.setNombre("Distribuidora Uno");
        primero.setTelefono("3001234567");
        primero.setEmail("");
        primero.setDireccion("Calle 1");

        Proveedor segundo = new Proveedor();
        segundo.setNombre("Distribuidora Dos");
        segundo.setTelefono("3007654321");
        segundo.setEmail(null);
        segundo.setDireccion("Calle 2");

        Proveedor creadoUno = service.crearProveedor(primero);
        Proveedor creadoDos = service.crearProveedor(segundo);

        assertNull(creadoUno.getEmail());
        assertNull(creadoDos.getEmail());
        assertEquals(2, dao.getDatos().size());
    }

    @Test
    void crearProveedorConEmailDuplicadoDebeLanzarExcepcion() {
        ProveedorDAOMock dao = new ProveedorDAOMock();
        ProveedorService service = new ProveedorService(dao);

        Proveedor existente = new Proveedor(1, "Distribuidora Uno", "3001234567", "ventas@dominio.com", "Calle 1", true);
        dao.getDatos().add(existente);

        Proveedor nuevo = new Proveedor();
        nuevo.setNombre("Distribuidora Dos");
        nuevo.setTelefono("3007654321");
        nuevo.setEmail("ventas@dominio.com");
        nuevo.setDireccion("Calle 2");

        assertThrows(DatoDuplicadoException.class, () -> service.crearProveedor(nuevo));
    }

    @Test
    void actualizarProveedorConEmailDuplicadoDebeLanzarExcepcion() {
        ProveedorDAOMock dao = new ProveedorDAOMock();
        ProveedorService service = new ProveedorService(dao);

        Proveedor primero = new Proveedor(1, "Distribuidora Uno", "3001234567", "ventas@dominio.com", "Calle 1", true);
        Proveedor segundo = new Proveedor(2, "Distribuidora Dos", "3007654321", "compras@dominio.com", "Calle 2", true);
        dao.getDatos().add(primero);
        dao.getDatos().add(segundo);

        Proveedor actualizado = new Proveedor(1, "Distribuidora Uno", "3001234567", "compras@dominio.com", "Calle 1", true);

        assertThrows(DatoDuplicadoException.class, () -> service.actualizarProveedor(actualizado));
    }

    @Test
    void buscarProveedorInexistenteDebeLanzarExcepcion() {
        ProveedorService service = new ProveedorService(new ProveedorDAOMock());

        assertThrows(EntidadNoEncontradaException.class, () -> service.buscarPorId(999));
    }

    @Test
    void actualizarProveedorInexistenteDebeLanzarExcepcion() {
        ProveedorService service = new ProveedorService(new ProveedorDAOMock());
        Proveedor proveedor = new Proveedor(999, "No existe", "3001234567", "a@b.com", "Dir", true);

        assertThrows(EntidadNoEncontradaException.class, () -> service.actualizarProveedor(proveedor));
    }

    @Test
    void desactivarProveedorDebeMarcarActivoComoFalse() {
        ProveedorDAOMock dao = new ProveedorDAOMock();
        ProveedorService service = new ProveedorService(dao);
        Proveedor proveedor = new Proveedor(1, "Distribuidora Uno", "3001234567", "ventas@dominio.com", "Calle 1", true);
        dao.getDatos().add(proveedor);

        service.desactivarProveedor(1);

        assertFalse(dao.getDatos().get(0).isActivo());
    }

    @Test
    void desactivarProveedorDebeDesmarcarCondicionDeProveedorHabitual() {
        ProveedorDAOMock dao = new ProveedorDAOMock();
        ProveedorRepuestoDAOMock catalogoDao = new ProveedorRepuestoDAOMock();
        catalogoDao.habitualesPorProveedor.add(1);
        ProveedorService service = new ProveedorService(dao, catalogoDao, false);
        dao.getDatos().add(new Proveedor(1, "Distribuidora Uno", "3001234567", "ventas@dominio.com", "Calle 1", true));

        service.desactivarProveedor(1);

        assertFalse(dao.getDatos().get(0).isActivo());
        assertFalse(catalogoDao.habitualesPorProveedor.contains(1));
        assertEquals(1, catalogoDao.llamadasDesmarcarProveedor);
    }

    @Test
    void actualizarProveedorInactivoNoDebeReactivarloSilenciosamente() {
        ProveedorDAOMock dao = new ProveedorDAOMock();
        ProveedorService service = new ProveedorService(dao);
        dao.getDatos().add(new Proveedor(1, "Distribuidora Inactiva", "3001234567", "old@dominio.com", "Calle 1", false));

        Proveedor cambios = new Proveedor();
        cambios.setId(1);
        cambios.setNombre("Distribuidora Renovada");
        cambios.setTelefono("3009998877");
        cambios.setEmail("new@dominio.com");
        cambios.setDireccion("Calle 20");
        cambios.setActivo(true);

        Proveedor actualizado = service.actualizarProveedor(cambios);

        assertFalse(actualizado.isActivo());
        assertFalse(dao.getDatos().get(0).isActivo());
    }

    @Test
    void reactivarProveedorInexistenteDebeLanzarExcepcion() {
        ProveedorService service = new ProveedorService(new ProveedorDAOMock());

        assertThrows(EntidadNoEncontradaException.class, () -> service.reactivarProveedor(999));
    }

    @Test
    void desactivarProveedorInexistenteDebeLanzarExcepcion() {
        ProveedorService service = new ProveedorService(new ProveedorDAOMock());

        assertThrows(EntidadNoEncontradaException.class, () -> service.desactivarProveedor(999));
    }

    @Test
    void crearProveedorDebeForzarEstadoActivoTrueYNulosEnOpcionalesVacios() {
        ProveedorDAOMock dao = new ProveedorDAOMock();
        ProveedorService service = new ProveedorService(dao);
        Proveedor proveedor = new Proveedor();
        proveedor.setNombre("  Repuestos Centro  ");
        proveedor.setTelefono("   ");
        proveedor.setEmail("");
        proveedor.setDireccion("   ");
        proveedor.setActivo(false);

        Proveedor creado = service.crearProveedor(proveedor);

        assertTrue(creado.isActivo());
        assertNull(creado.getTelefono());
        assertNull(creado.getEmail());
        assertNull(creado.getDireccion());
    }

    @Test
    void crearProveedorConCamposQueSuperanLongitudMaximaDebeLanzarExcepcion() {
        ProveedorService service = new ProveedorService(new ProveedorDAOMock());
        Proveedor proveedor = new Proveedor();
        proveedor.setNombre("Distribuidora Límite");
        proveedor.setTelefono("123456789012345678901");

        ValidacionException ex = assertThrows(ValidacionException.class,
                () -> service.crearProveedor(proveedor));

        assertTrue(ex.getMessage().toLowerCase().contains("teléfono"));
    }

    private static class ProveedorRepuestoDAOMock implements ProveedorRepuestoDAO {
        private final List<Integer> habitualesPorProveedor = new ArrayList<>();
        private int llamadasDesmarcarProveedor = 0;

        @Override
        public List<ProveedorRepuesto> listarPorProveedor(int proveedorId) {
            return new ArrayList<>();
        }

        @Override
        public List<ProveedorRepuesto> listarPorRepuesto(int repuestoId) {
            return new ArrayList<>();
        }

        @Override
        public ProveedorRepuesto buscarPorProveedorYRepuesto(int proveedorId, int repuestoId) {
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
        public int desmarcarHabitualesDeProveedor(int proveedorId) {
            llamadasDesmarcarProveedor++;
            int removidos = 0;
            while (habitualesPorProveedor.remove(Integer.valueOf(proveedorId))) {
                removidos++;
            }
            return removidos;
        }

        @Override
        public boolean marcarHabitual(int proveedorId, int repuestoId) {
            return false;
        }
    }

    private static class ProveedorDAOMock implements ProveedorDAO {
        private final List<Proveedor> datos = new ArrayList<>();

        @Override
        public List<Proveedor> listarTodos() {
            return datos;
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
            for (Proveedor proveedor : datos) {
                if (proveedor.getEmail() != null && proveedor.getEmail().equalsIgnoreCase(email)) {
                    return proveedor;
                }
            }
            return null;
        }

        @Override
        public int guardar(Proveedor proveedor) {
            if (proveedor.getId() == 0) {
                proveedor.setId(datos.size() + 1);
            }
            for (int i = 0; i < datos.size(); i++) {
                if (datos.get(i).getId() == proveedor.getId()) {
                    datos.remove(i);
                    break;
                }
            }
            datos.add(proveedor);
            return proveedor.getId();
        }

        @Override
        public boolean actualizar(Proveedor proveedor) {
            return guardar(proveedor) > 0;
        }

        @Override
        public boolean desactivar(int id) {
            Proveedor proveedor = buscarPorId(id);
            if (proveedor == null) {
                return false;
            }
            proveedor.setActivo(false);
            return true;
        }

        @Override
        public boolean reactivar(int id) {
            Proveedor proveedor = buscarPorId(id);
            if (proveedor == null) {
                return false;
            }
            proveedor.setActivo(true);
            return true;
        }

        public List<Proveedor> getDatos() {
            return datos;
        }
    }
}
