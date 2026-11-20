package com.carmotors.sgt.service;

import com.carmotors.sgt.exception.DatoDuplicadoException;
import com.carmotors.sgt.exception.EntidadNoEncontradaException;
import com.carmotors.sgt.exception.ValidacionException;
import com.carmotors.sgt.model.CargaLineaTecnico;
import com.carmotors.sgt.model.Especialidad;
import com.carmotors.sgt.model.Tecnico;
import com.carmotors.sgt.dao.EspecialidadDAO;
import com.carmotors.sgt.dao.TecnicoDAO;
import com.carmotors.sgt.util.InputValidator;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Capa de lógica de negocio para la gestión de técnicos y su especialidad (RF-33, RN-17).
 */
public class TecnicoService {

    private static final int MAX_LONGITUD_NOMBRE = 100;
    private static final int MAX_LONGITUD_DOCUMENTO = 30;
    private static final int MAX_LONGITUD_TELEFONO = 20;

    private final TecnicoDAO tecnicoDAO;
    private final EspecialidadDAO especialidadDAO;

    public TecnicoService(TecnicoDAO tecnicoDAO, EspecialidadDAO especialidadDAO) {
        if (tecnicoDAO == null) {
            throw new IllegalArgumentException("El repositorio de técnicos es obligatorio.");
        }
        if (especialidadDAO == null) {
            throw new IllegalArgumentException("El repositorio de especialidades es obligatorio.");
        }
        this.tecnicoDAO = tecnicoDAO;
        this.especialidadDAO = especialidadDAO;
    }

    public List<Tecnico> listarTodos() {
        return tecnicoDAO.listarTodos();
    }

    public List<Tecnico> listarActivos() {
        return tecnicoDAO.listarActivos();
    }

    public List<Especialidad> listarEspecialidades() {
        return especialidadDAO.listarTodos();
    }

    public Especialidad buscarEspecialidadPorId(int especialidadId) {
        return especialidadDAO.buscarPorId(especialidadId);
    }

    public Tecnico buscarPorId(int id) {
        if (id <= 0) {
            throw new ValidacionException("El ID del técnico debe ser un número positivo.");
        }
        Tecnico tecnico = tecnicoDAO.buscarPorId(id);
        if (tecnico == null) {
            throw new EntidadNoEncontradaException("No se encontró un técnico con el ID: " + id);
        }
        return tecnico;
    }

    public Tecnico buscarPorDocumento(String documento) {
        String docNormalizado = InputValidator.normalizarTexto(documento);
        if (docNormalizado.isEmpty()) {
            throw new ValidacionException("El documento de identidad no puede estar vacío.");
        }
        Tecnico tecnico = tecnicoDAO.buscarPorDocumento(docNormalizado);
        if (tecnico == null) {
            throw new EntidadNoEncontradaException("No se encontró un técnico con el documento: " + docNormalizado);
        }
        return tecnico;
    }

    public Tecnico registrarTecnico(Tecnico tecnico) {
        validarYNormalizarDatos(tecnico);

        Tecnico existente = tecnicoDAO.buscarPorDocumento(tecnico.getDocumentoIdentidad());
        if (existente != null) {
            throw new DatoDuplicadoException(
                    "Ya existe un técnico registrado con el documento: " + tecnico.getDocumentoIdentidad());
        }

        tecnico.setActivo(true);
        int idGenerado = tecnicoDAO.guardar(tecnico);
        tecnico.setId(idGenerado);
        return tecnico;
    }

    public Tecnico actualizarTecnico(Tecnico tecnico) {
        if (tecnico == null || tecnico.getId() <= 0) {
            throw new ValidacionException("Debe indicar un técnico válido con ID mayor que cero.");
        }
        Tecnico actual = tecnicoDAO.buscarPorId(tecnico.getId());
        if (actual == null) {
            throw new EntidadNoEncontradaException("No existe un técnico con el ID: " + tecnico.getId());
        }

        validarYNormalizarDatos(tecnico);

        Tecnico conMismoDoc = tecnicoDAO.buscarPorDocumento(tecnico.getDocumentoIdentidad());
        if (conMismoDoc != null && conMismoDoc.getId() != tecnico.getId()) {
            throw new DatoDuplicadoException(
                    "Ya existe otro técnico registrado con el documento: " + tecnico.getDocumentoIdentidad());
        }

        // RN-17: La actualización de datos nunca debe alterar el estado activo/inactivo.
        tecnico.setActivo(actual.isActivo());

        boolean actualizado = tecnicoDAO.actualizar(tecnico);
        if (!actualizado) {
            throw new ValidacionException("No se pudo actualizar el técnico con ID: " + tecnico.getId());
        }
        return tecnico;
    }

    /**
     * Desactiva lógicamente a un técnico para impedir nuevas asignaciones sin borrar su historial (RN-17).
     */
    public Tecnico desactivarTecnico(int id) {
        Tecnico tecnico = buscarPorId(id);
        if (!tecnico.isActivo()) {
            return tecnico;
        }
        boolean actualizado = tecnicoDAO.actualizarEstadoActivo(id, false);
        if (!actualizado) {
            throw new ValidacionException("No se pudo desactivar el técnico con ID: " + id);
        }
        tecnico.setActivo(false);
        return tecnico;
    }

    public Tecnico reactivarTecnico(int id) {
        Tecnico tecnico = buscarPorId(id);
        if (tecnico.isActivo()) {
            return tecnico;
        }
        boolean actualizado = tecnicoDAO.actualizarEstadoActivo(id, true);
        if (!actualizado) {
            throw new ValidacionException("No se pudo reactivar el técnico con ID: " + id);
        }
        tecnico.setActivo(true);
        return tecnico;
    }

    /**
     * Calcula la carga de trabajo actual de un técnico como el total de líneas
     * asignadas en órdenes con estado Pendiente o En Proceso (RF-34, RN-21).
     *
     * @param tecnicoId identificador del técnico
     * @return cantidad de líneas activas asignadas
     */
    public int obtenerCargaTrabajo(int tecnicoId) {
        return consultarCargaTrabajo(tecnicoId).size();
    }

    /**
     * Consulta las líneas de trabajo activas asignadas a un técnico en órdenes
     * con estado Pendiente o En Proceso (RF-34, RN-21, HU-16).
     * Permite consultar técnicos activos e inactivos que aún tengan historial vigente.
     *
     * @param tecnicoId identificador del técnico
     * @return lista de líneas activas asignadas
     */
    public List<CargaLineaTecnico> consultarCargaTrabajo(int tecnicoId) {
        buscarPorId(tecnicoId);
        List<CargaLineaTecnico> lineas = tecnicoDAO.listarCargaPorTecnico(tecnicoId);
        return lineas != null ? lineas : new ArrayList<>();
    }

    /**
     * Obtiene el mapa de carga activa por técnico ({@code tecnicoId -> cantidadLineas})
     * para los eslabones de sugerencia de técnicos (RN-20, RN-21).
     *
     * @return mapa con la carga activa de cada técnico que tiene asignaciones vigentes
     */
    public Map<Integer, Integer> obtenerCargasActivasPorTecnico() {
        Map<Integer, Integer> cargas = tecnicoDAO.contarCargaActivaPorTecnico();
        return cargas != null ? cargas : new HashMap<>();
    }

    private void validarYNormalizarDatos(Tecnico tecnico) {
        if (tecnico == null) {
            throw new ValidacionException("Los datos del técnico no pueden ser nulos.");
        }

        String nombre = InputValidator.normalizarEspacios(tecnico.getNombre());
        if (nombre.isEmpty()) {
            throw new ValidacionException("El nombre del técnico es obligatorio.");
        }
        if (nombre.length() > MAX_LONGITUD_NOMBRE) {
            throw new ValidacionException(
                    "El nombre del técnico no puede superar los " + MAX_LONGITUD_NOMBRE + " caracteres.");
        }
        if (!InputValidator.esNombrePersonaValido(nombre)) {
            throw new ValidacionException(
                    "El nombre del técnico debe incluir nombre y apellido (2 a 4 palabras, solo letras, sin números).");
        }
        tecnico.setNombre(nombre);

        String documento = InputValidator.normalizarTexto(tecnico.getDocumentoIdentidad());
        if (documento.isEmpty()) {
            throw new ValidacionException("El documento de identidad del técnico es obligatorio.");
        }
        if (documento.length() > MAX_LONGITUD_DOCUMENTO) {
            throw new ValidacionException(
                    "El documento de identidad no puede superar los " + MAX_LONGITUD_DOCUMENTO + " caracteres.");
        }
        if (!InputValidator.esDocumentoNumericoValido(documento)) {
            throw new ValidacionException(
                    "El documento de identidad del técnico debe contener entre 6 y 12 dígitos numéricos.");
        }
        tecnico.setDocumentoIdentidad(documento);

        if (tecnico.getEspecialidadId() <= 0) {
            throw new ValidacionException("Debe seleccionar una especialidad válida.");
        }
        Especialidad especialidad = especialidadDAO.buscarPorId(tecnico.getEspecialidadId());
        if (especialidad == null) {
            throw new EntidadNoEncontradaException(
                    "La especialidad seleccionada (ID " + tecnico.getEspecialidadId() + ") no existe.");
        }

        String telefono = InputValidator.normalizarTexto(tecnico.getTelefono());
        if (!telefono.isEmpty()) {
            if (telefono.length() > MAX_LONGITUD_TELEFONO) {
                throw new ValidacionException(
                        "El teléfono del técnico no puede superar los " + MAX_LONGITUD_TELEFONO + " caracteres.");
            }
            if (!InputValidator.esTelefonoValido(telefono)) {
                throw new ValidacionException(
                        "El teléfono del técnico debe contener entre 7 y 15 dígitos numéricos.");
            }
        }
        tecnico.setTelefono(telefono.isEmpty() ? null : telefono);
    }
}
