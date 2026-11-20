package com.carmotors.sgt.service;

import com.carmotors.sgt.exception.DatoDuplicadoException;
import com.carmotors.sgt.exception.EntidadNoEncontradaException;
import com.carmotors.sgt.exception.ValidacionException;
import com.carmotors.sgt.model.Especialidad;
import com.carmotors.sgt.model.MantenimientoTipo;
import com.carmotors.sgt.model.ServicioTipo;
import com.carmotors.sgt.dao.EspecialidadDAO;
import com.carmotors.sgt.dao.MantenimientoTipoDAO;
import com.carmotors.sgt.dao.ServicioTipoDAO;
import com.carmotors.sgt.util.InputValidator;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

/**
 * Capa de lógica de negocio para el catálogo de tipos de servicio ({@code servicio_tipos}).
 * Aplica validación de precio no negativo con {@link BigDecimal} (HALF_UP, escala 2)
 * y la regla de intervalo en meses según tipo Preventivo o Correctivo.
 */
public class ServicioTipoService {

    private static final int MAX_LONGITUD_NOMBRE = 100;
    private static final int MAX_INTERVALO_MESES = 120;

    private final ServicioTipoDAO servicioTipoDAO;
    private final MantenimientoTipoDAO mantenimientoTipoDAO;
    private final EspecialidadDAO especialidadDAO;

    public ServicioTipoService(
            ServicioTipoDAO servicioTipoDAO,
            MantenimientoTipoDAO mantenimientoTipoDAO,
            EspecialidadDAO especialidadDAO) {
        if (servicioTipoDAO == null) {
            throw new IllegalArgumentException("El repositorio de tipos de servicio es obligatorio.");
        }
        if (mantenimientoTipoDAO == null) {
            throw new IllegalArgumentException("El repositorio de tipos de mantenimiento es obligatorio.");
        }
        if (especialidadDAO == null) {
            throw new IllegalArgumentException("El repositorio de especialidades es obligatorio.");
        }
        this.servicioTipoDAO = servicioTipoDAO;
        this.mantenimientoTipoDAO = mantenimientoTipoDAO;
        this.especialidadDAO = especialidadDAO;
    }

    public List<ServicioTipo> listarTodos() {
        return servicioTipoDAO.listarTodos();
    }

    public List<MantenimientoTipo> listarTiposMantenimiento() {
        return mantenimientoTipoDAO.listarTodos();
    }

    public List<Especialidad> listarEspecialidades() {
        return especialidadDAO.listarTodos();
    }

    public MantenimientoTipo buscarTipoMantenimientoPorId(int mantenimientoTipoId) {
        return mantenimientoTipoDAO.buscarPorId(mantenimientoTipoId);
    }

    public Especialidad buscarEspecialidadPorId(int especialidadId) {
        return especialidadDAO.buscarPorId(especialidadId);
    }

    public ServicioTipo buscarPorId(int id) {
        if (id <= 0) {
            throw new ValidacionException("El ID del tipo de servicio debe ser un número positivo.");
        }
        ServicioTipo servicioTipo = servicioTipoDAO.buscarPorId(id);
        if (servicioTipo == null) {
            throw new EntidadNoEncontradaException("No se encontró un tipo de servicio con el ID: " + id);
        }
        return servicioTipo;
    }

    public ServicioTipo registrarServicioTipo(ServicioTipo servicioTipo) {
        validarYNormalizar(servicioTipo);

        ServicioTipo existente = servicioTipoDAO.buscarPorNombre(servicioTipo.getNombre());
        if (existente != null) {
            throw new DatoDuplicadoException(
                    "Ya existe un tipo de servicio registrado con el nombre: " + servicioTipo.getNombre());
        }

        int idGenerado = servicioTipoDAO.guardar(servicioTipo);
        servicioTipo.setId(idGenerado);
        return servicioTipo;
    }

    public ServicioTipo actualizarServicioTipo(ServicioTipo servicioTipo) {
        if (servicioTipo == null || servicioTipo.getId() <= 0) {
            throw new ValidacionException("Debe indicar un tipo de servicio válido con ID mayor que cero.");
        }
        ServicioTipo actual = servicioTipoDAO.buscarPorId(servicioTipo.getId());
        if (actual == null) {
            throw new EntidadNoEncontradaException("No existe un tipo de servicio con el ID: " + servicioTipo.getId());
        }

        validarYNormalizar(servicioTipo);

        ServicioTipo conMismoNombre = servicioTipoDAO.buscarPorNombre(servicioTipo.getNombre());
        if (conMismoNombre != null && conMismoNombre.getId() != servicioTipo.getId()) {
            throw new DatoDuplicadoException(
                    "Ya existe otro tipo de servicio con el nombre: " + servicioTipo.getNombre());
        }

        boolean actualizado = servicioTipoDAO.actualizar(servicioTipo);
        if (!actualizado) {
            throw new ValidacionException("No se pudo actualizar el tipo de servicio con ID: " + servicioTipo.getId());
        }
        return servicioTipo;
    }

    public void eliminarServicioTipo(int id) {
        buscarPorId(id);
        boolean eliminado = servicioTipoDAO.eliminar(id);
        if (!eliminado) {
            throw new ValidacionException("No se pudo eliminar el tipo de servicio con ID: " + id);
        }
    }

    private void validarYNormalizar(ServicioTipo servicioTipo) {
        if (servicioTipo == null) {
            throw new ValidacionException("Los datos del tipo de servicio no pueden ser nulos.");
        }

        String nombre = InputValidator.normalizarEspacios(servicioTipo.getNombre());
        if (nombre.isEmpty()) {
            throw new ValidacionException("El nombre del tipo de servicio es obligatorio.");
        }
        if (nombre.length() > MAX_LONGITUD_NOMBRE) {
            throw new ValidacionException(
                    "El nombre del tipo de servicio no puede superar los " + MAX_LONGITUD_NOMBRE + " caracteres.");
        }
        if (nombre.length() < 3 || !InputValidator.contieneLetra(nombre)) {
            throw new ValidacionException(
                    "El nombre del tipo de servicio debe tener al menos 3 caracteres e incluir letras.");
        }
        servicioTipo.setNombre(nombre);

        if (servicioTipo.getMantenimientoTipoId() <= 0) {
            throw new ValidacionException("Debe seleccionar un tipo de mantenimiento válido.");
        }
        MantenimientoTipo mantenimientoTipo =
                mantenimientoTipoDAO.buscarPorId(servicioTipo.getMantenimientoTipoId());
        if (mantenimientoTipo == null) {
            throw new EntidadNoEncontradaException(
                    "El tipo de mantenimiento seleccionado (ID " + servicioTipo.getMantenimientoTipoId() + ") no existe.");
        }

        if (servicioTipo.getEspecialidadId() <= 0) {
            throw new ValidacionException("Debe seleccionar una especialidad válida.");
        }
        Especialidad especialidad = especialidadDAO.buscarPorId(servicioTipo.getEspecialidadId());
        if (especialidad == null) {
            throw new EntidadNoEncontradaException(
                    "La especialidad seleccionada (ID " + servicioTipo.getEspecialidadId() + ") no existe.");
        }

        BigDecimal precio = servicioTipo.getPrecioManoObraBase();
        if (!InputValidator.esBigDecimalNoNegativo(precio)) {
            throw new ValidacionException("El precio base de mano de obra debe ser mayor o igual a 0.00.");
        }
        BigDecimal precioNormalizado = precio.setScale(2, RoundingMode.HALF_UP);
        if (precioNormalizado.compareTo(InputValidator.MAX_MONTO_DECIMAL_12_2) > 0) {
            throw new ValidacionException("El precio base de mano de obra supera el máximo permitido ("
                    + InputValidator.MAX_MONTO_DECIMAL_12_2 + ").");
        }
        servicioTipo.setPrecioManoObraBase(precioNormalizado);

        validarIntervaloSegunMantenimiento(mantenimientoTipo, servicioTipo.getIntervaloMeses());
    }

    private void validarIntervaloSegunMantenimiento(MantenimientoTipo tipo, Integer intervaloMeses) {
        String nombreTipo = InputValidator.normalizarTexto(tipo.getNombre());
        if (nombreTipo.equalsIgnoreCase("Preventivo")) {
            if (intervaloMeses == null || intervaloMeses <= 0) {
                throw new ValidacionException(
                        "Un servicio de mantenimiento Preventivo exige un intervalo en meses mayor que cero.");
            }
            if (intervaloMeses > MAX_INTERVALO_MESES) {
                throw new ValidacionException(
                        "El intervalo en meses para mantenimiento Preventivo no puede superar "
                                + MAX_INTERVALO_MESES + " meses.");
            }
            return;
        }
        if (nombreTipo.equalsIgnoreCase("Correctivo") && intervaloMeses != null) {
            throw new ValidacionException(
                    "Un servicio de mantenimiento Correctivo no debe tener intervalo en meses (debe ser nulo).");
        }
    }
}
