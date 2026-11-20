package com.carmotors.sgt.service;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

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
import com.carmotors.sgt.dao.impl.ProveedorDAOImpl;
import com.carmotors.sgt.dao.impl.ProveedorRepuestoDAOImpl;
import com.carmotors.sgt.dao.impl.RepuestoDAOImpl;
import com.carmotors.sgt.dao.impl.RepuestoTipoDAOImpl;
import com.carmotors.sgt.util.InputValidator;

public class RepuestoService {

    private static final int MAX_LONGITUD_NOMBRE = 100;
    private static final int MAX_LONGITUD_MARCA = 50;

    private final RepuestoDAO repuestoDAO;
    private final RepuestoTipoDAO repuestoTipoDAO;
    private final ProveedorRepuestoDAO proveedorRepuestoDAO;
    private final ProveedorDAO proveedorDAO;

    public RepuestoService() {
        this(new RepuestoDAOImpl(), new RepuestoTipoDAOImpl(), new ProveedorRepuestoDAOImpl(), new ProveedorDAOImpl());
    }

    public RepuestoService(RepuestoDAO repuestoDAO) {
        this(repuestoDAO, new RepuestoTipoDAOImpl(), new ProveedorRepuestoDAOImpl(), new ProveedorDAOImpl());
    }

    public RepuestoService(RepuestoDAO repuestoDAO, RepuestoTipoDAO repuestoTipoDAO) {
        this(repuestoDAO, repuestoTipoDAO, new ProveedorRepuestoDAOImpl(), new ProveedorDAOImpl());
    }

    public RepuestoService(RepuestoDAO repuestoDAO, RepuestoTipoDAO repuestoTipoDAO,
                           ProveedorRepuestoDAO proveedorRepuestoDAO, ProveedorDAO proveedorDAO) {
        if (repuestoDAO == null) {
            throw new ValidacionException("El DAO de repuestos es obligatorio.");
        }
        if (repuestoTipoDAO == null) {
            throw new ValidacionException("El DAO de tipos de repuesto es obligatorio.");
        }
        if (proveedorRepuestoDAO == null) {
            throw new ValidacionException("El DAO del catálogo proveedor-repuesto es obligatorio.");
        }
        if (proveedorDAO == null) {
            throw new ValidacionException("El DAO de proveedores es obligatorio.");
        }
        this.repuestoDAO = repuestoDAO;
        this.repuestoTipoDAO = repuestoTipoDAO;
        this.proveedorRepuestoDAO = proveedorRepuestoDAO;
        this.proveedorDAO = proveedorDAO;
    }

    public List<Repuesto> listarTodos() {
        return repuestoDAO.listarTodos();
    }

    public List<Repuesto> listarActivos() {
        return repuestoDAO.listarActivos();
    }

    public List<RepuestoTipo> listarTipos() {
        return repuestoTipoDAO.listarTodos();
    }

    public String obtenerNombreTipo(int tipoId) {
        if (tipoId <= 0) {
            return "Sin tipo";
        }
        RepuestoTipo tipo = repuestoTipoDAO.buscarPorId(tipoId);
        return tipo == null ? "Sin tipo" : tipo.getNombre();
    }

    public Repuesto buscarPorId(int id) {
        if (id <= 0) {
            throw new ValidacionException("El ID del repuesto debe ser un número positivo.");
        }

        Repuesto repuesto = repuestoDAO.buscarPorId(id);
        if (repuesto == null) {
            throw new EntidadNoEncontradaException("No se encontró un repuesto con el ID: " + id);
        }
        return repuesto;
    }

    /**
     * Busca y filtra repuestos por nombre, tipo, marca o proveedor (RF-04, RNF-11).
     * Los criterios son opcionales, combinables e insensibles a mayúsculas y tildes.
     */
    public List<Repuesto> buscarPorFiltros(String nombre, String tipo, String marca, String proveedor) {
        String filtroNombre = InputValidator.normalizarParaBusqueda(nombre);
        String filtroTipo = InputValidator.normalizarParaBusqueda(tipo);
        String filtroMarca = InputValidator.normalizarParaBusqueda(marca);
        String filtroProveedor = InputValidator.normalizarParaBusqueda(proveedor);

        List<Repuesto> catalogo = repuestoDAO.listarTodos();
        Map<Integer, String> tiposPorId = construirMapaTipos(filtroTipo);
        Set<Integer> repuestoIdsProveedor = obtenerRepuestoIdsPorProveedor(filtroProveedor);

        return catalogo.stream()
                .filter(repuesto -> coincideTexto(repuesto.getNombre(), filtroNombre))
                .filter(repuesto -> filtroTipo.isEmpty()
                        || coincideTexto(tiposPorId.get(repuesto.getRepuestoTipoId()), filtroTipo))
                .filter(repuesto -> coincideTexto(repuesto.getMarca(), filtroMarca))
                .filter(repuesto -> filtroProveedor.isEmpty()
                        || repuestoIdsProveedor.contains(repuesto.getId()))
                .collect(Collectors.toList());
    }

    private Map<Integer, String> construirMapaTipos(String filtroTipo) {
        Map<Integer, String> tiposPorId = new HashMap<>();
        if (filtroTipo.isEmpty()) {
            return tiposPorId;
        }
        for (RepuestoTipo repuestoTipo : repuestoTipoDAO.listarTodos()) {
            tiposPorId.put(repuestoTipo.getId(), repuestoTipo.getNombre());
        }
        return tiposPorId;
    }

    private Set<Integer> obtenerRepuestoIdsPorProveedor(String filtroProveedor) {
        Set<Integer> repuestoIds = new HashSet<>();
        if (filtroProveedor.isEmpty()) {
            return repuestoIds;
        }
        List<Proveedor> proveedoresCoincidentes = proveedorDAO.listarTodos().stream()
                .filter(proveedor -> coincideTexto(proveedor.getNombre(), filtroProveedor))
                .collect(Collectors.toList());
        for (Proveedor proveedor : proveedoresCoincidentes) {
            for (ProveedorRepuesto asociacion : proveedorRepuestoDAO.listarPorProveedor(proveedor.getId())) {
                repuestoIds.add(asociacion.getRepuestoId());
            }
        }
        return repuestoIds;
    }

    private boolean coincideTexto(String valor, String filtroNormalizado) {
        if (filtroNormalizado.isEmpty()) {
            return true;
        }
        return InputValidator.normalizarParaBusqueda(valor).contains(filtroNormalizado);
    }

    public Repuesto crearRepuesto(Repuesto repuesto) {
        validarRepuesto(repuesto);

        Repuesto existente = repuestoDAO.buscarPorNombre(repuesto.getNombre());
        if (existente != null) {
            throw new DatoDuplicadoException("Ya existe un repuesto registrado con ese nombre.");
        }

        repuesto.setCantidadReservada(0);
        repuesto.setActivo(true);

        int idGenerado = repuestoDAO.guardar(repuesto);
        repuesto.setId(idGenerado);
        return repuesto;
    }

    public Repuesto actualizarRepuesto(Repuesto repuesto) {
        if (repuesto == null || repuesto.getId() <= 0) {
            throw new ValidacionException("Debe indicar el identificador del repuesto a actualizar.");
        }

        Repuesto actual = repuestoDAO.buscarPorId(repuesto.getId());
        if (actual == null) {
            throw new EntidadNoEncontradaException("No existe un repuesto con el ID: " + repuesto.getId());
        }

        validarRepuesto(repuesto);

        Repuesto existente = repuestoDAO.buscarPorNombre(repuesto.getNombre());
        if (existente != null && existente.getId() != repuesto.getId()) {
            throw new DatoDuplicadoException("Ya existe otro repuesto registrado con ese nombre.");
        }

        repuesto.setCantidadReservada(actual.getCantidadReservada());
        repuesto.setActivo(actual.isActivo());

        boolean actualizado = repuestoDAO.actualizar(repuesto);
        if (!actualizado) {
            throw new ValidacionException("No se pudo actualizar el repuesto indicado.");
        }
        return repuesto;
    }

    public Repuesto desactivarRepuesto(int id) {
        Repuesto repuesto = buscarPorId(id);
        if (!repuesto.isActivo()) {
            return repuesto;
        }

        boolean actualizado = repuestoDAO.desactivar(id);
        if (!actualizado) {
            throw new ValidacionException("No se pudo desactivar el repuesto.");
        }
        repuesto.setActivo(false);
        return repuesto;
    }

    public Repuesto reactivarRepuesto(int id) {
        Repuesto repuesto = buscarPorId(id);
        if (repuesto.isActivo()) {
            return repuesto;
        }

        boolean actualizado = repuestoDAO.reactivar(id);
        if (!actualizado) {
            throw new ValidacionException("No se pudo reactivar el repuesto.");
        }
        repuesto.setActivo(true);
        return repuesto;
    }

    private void validarRepuesto(Repuesto repuesto) {
        if (repuesto == null) {
            throw new ValidacionException("El repuesto no puede ser nulo.");
        }

        if (repuesto.getRepuestoTipoId() <= 0 || repuestoTipoDAO.buscarPorId(repuesto.getRepuestoTipoId()) == null) {
            throw new ValidacionException("Debe seleccionar un tipo de repuesto válido.");
        }

        String nombre = InputValidator.normalizarEspacios(repuesto.getNombre());
        if (nombre.isEmpty()) {
            throw new ValidacionException("El nombre del repuesto es obligatorio.");
        }
        if (nombre.length() > MAX_LONGITUD_NOMBRE) {
            throw new ValidacionException("El nombre del repuesto no puede superar los " + MAX_LONGITUD_NOMBRE + " caracteres.");
        }
        if (nombre.length() < 2 || !InputValidator.contieneLetra(nombre)) {
            throw new ValidacionException("El nombre del repuesto debe tener al menos 2 caracteres e incluir letras.");
        }
        repuesto.setNombre(nombre);

        String marca = InputValidator.normalizarEspacios(repuesto.getMarca());
        if (marca.isEmpty()) {
            throw new ValidacionException("La marca del repuesto es obligatoria.");
        }
        if (marca.length() > MAX_LONGITUD_MARCA) {
            throw new ValidacionException("La marca del repuesto no puede superar los " + MAX_LONGITUD_MARCA + " caracteres.");
        }
        if (marca.length() < 2 || !InputValidator.contieneLetra(marca)) {
            throw new ValidacionException("La marca del repuesto debe tener al menos 2 caracteres e incluir letras.");
        }
        repuesto.setMarca(marca);

        if (repuesto.getPrecioVenta() == null) {
            throw new ValidacionException("El precio de venta debe ser un valor positivo.");
        }
        BigDecimal precioNormalizado = normalizarPrecio(repuesto.getPrecioVenta());
        if (!InputValidator.esBigDecimalPositivo(precioNormalizado)) {
            throw new ValidacionException("El precio de venta debe ser un valor positivo.");
        }
        if (precioNormalizado.compareTo(InputValidator.MAX_MONTO_DECIMAL_12_2) > 0) {
            throw new ValidacionException("El precio de venta supera el máximo permitido ("
                    + InputValidator.MAX_MONTO_DECIMAL_12_2 + ").");
        }
        repuesto.setPrecioVenta(precioNormalizado);

        if (repuesto.getStockMinimo() < 0) {
            throw new ValidacionException("El stock mínimo no puede ser negativo.");
        }

        if (repuesto.getCantidadReservada() < 0) {
            throw new ValidacionException("La cantidad reservada no puede ser negativa.");
        }
    }

    private BigDecimal normalizarPrecio(BigDecimal precio) {
        return precio.setScale(2, InputValidator.REDONDEO_MONETARIO);
    }
}
