package com.carmotors.sgt.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import com.carmotors.sgt.exception.EntidadNoEncontradaException;
import com.carmotors.sgt.exception.ValidacionException;
import com.carmotors.sgt.model.EvaluacionProveedor;
import com.carmotors.sgt.model.Proveedor;
import com.carmotors.sgt.model.RankingProveedor;
import com.carmotors.sgt.dao.EvaluacionProveedorDAO;
import com.carmotors.sgt.dao.ProveedorDAO;
import com.carmotors.sgt.dao.impl.EvaluacionProveedorDAOImpl;
import com.carmotors.sgt.dao.impl.ProveedorDAOImpl;
import com.carmotors.sgt.util.InputValidator;

public class EvaluacionProveedorService {

    private static final int MIN_CALIFICACION = 1;
    private static final int MAX_CALIFICACION = 5;
    private static final int MAX_LONGITUD_COMENTARIO = 255;

    private final EvaluacionProveedorDAO evaluacionDAO;
    private final ProveedorDAO proveedorDAO;

    public EvaluacionProveedorService() {
        this(new EvaluacionProveedorDAOImpl(), new ProveedorDAOImpl());
    }

    public EvaluacionProveedorService(EvaluacionProveedorDAO evaluacionDAO, ProveedorDAO proveedorDAO) {
        if (evaluacionDAO == null) {
            throw new ValidacionException("El DAO de evaluaciones es obligatorio.");
        }
        if (proveedorDAO == null) {
            throw new ValidacionException("El DAO de proveedores es obligatorio.");
        }
        this.evaluacionDAO = evaluacionDAO;
        this.proveedorDAO = proveedorDAO;
    }

    public List<EvaluacionProveedor> listarTodas() {
        return evaluacionDAO.listarTodos();
    }

    public List<EvaluacionProveedor> listarPorProveedor(int proveedorId) {
        validarProveedorExistente(proveedorId);
        return evaluacionDAO.listarPorProveedor(proveedorId);
    }

    public EvaluacionProveedor registrarEvaluacion(EvaluacionProveedor evaluacion) {
        validarEvaluacion(evaluacion);

        Proveedor proveedor = validarProveedorExistente(evaluacion.getProveedorId());
        if (!proveedor.isActivo()) {
            throw new ValidacionException("El proveedor está inactivo y no puede recibir evaluaciones.");
        }

        if (evaluacion.getFecha() == null) {
            evaluacion.setFecha(LocalDate.now());
        } else if (evaluacion.getFecha().isAfter(LocalDate.now())) {
            throw new ValidacionException("La fecha de evaluación no puede ser futura.");
        }

        int idGenerado = evaluacionDAO.guardar(evaluacion);
        evaluacion.setId(idGenerado);
        return evaluacion;
    }

    public BigDecimal obtenerPromedioProveedor(int proveedorId) {
        validarProveedorExistente(proveedorId);
        List<EvaluacionProveedor> historial = evaluacionDAO.listarPorProveedor(proveedorId);
        if (historial == null || historial.isEmpty()) {
            return BigDecimal.ZERO.setScale(1, RoundingMode.HALF_UP);
        }

        BigDecimal acumulado = BigDecimal.ZERO;
        for (EvaluacionProveedor evaluacion : historial) {
            acumulado = acumulado.add(evaluacion.getPuntaje());
        }
        BigDecimal promedio = acumulado.divide(BigDecimal.valueOf(historial.size()), 1, RoundingMode.HALF_UP);
        return promedio;
    }

    /**
     * Genera el ranking de proveedores (RF-32, RN-16). El puntaje de cada proveedor es el
     * promedio del puntaje de sus evaluaciones con un decimal. Orden: promedio descendente y,
     * en empate, nombre ascendente. Los proveedores sin evaluaciones van al final ("Sin evaluar"),
     * ordenados por nombre. Se incluyen proveedores activos e inactivos, porque sus evaluaciones
     * forman parte del historial (RN-17).
     */
    public List<RankingProveedor> generarRanking() {
        List<Proveedor> proveedores = proveedorDAO.listarTodos();
        List<EvaluacionProveedor> evaluaciones = evaluacionDAO.listarTodos();
        if (proveedores == null) {
            proveedores = new ArrayList<>();
        }
        if (evaluaciones == null) {
            evaluaciones = new ArrayList<>();
        }

        Map<Integer, List<EvaluacionProveedor>> evaluacionesPorProveedor = evaluaciones.stream()
                .collect(Collectors.groupingBy(evaluacion -> evaluacion.getProveedorId()));

        Comparator<RankingProveedor> porNombre = (a, b) ->
                a.getProveedor().getNombre().compareToIgnoreCase(b.getProveedor().getNombre());
        Comparator<RankingProveedor> porPromedioDescendente = (a, b) ->
                b.getPromedio().compareTo(a.getPromedio());

        List<RankingProveedor> evaluados = proveedores.stream()
                .filter(proveedor -> evaluacionesPorProveedor.containsKey(proveedor.getId()))
                .map(proveedor -> crearFilaEvaluada(proveedor, evaluacionesPorProveedor.get(proveedor.getId())))
                .sorted(porPromedioDescendente.thenComparing(porNombre))
                .collect(Collectors.toList());

        List<RankingProveedor> sinEvaluar = proveedores.stream()
                .filter(proveedor -> !evaluacionesPorProveedor.containsKey(proveedor.getId()))
                .map(proveedor -> new RankingProveedor(proveedor, null, 0))
                .sorted(porNombre)
                .collect(Collectors.toList());

        List<RankingProveedor> ranking = new ArrayList<>(evaluados);
        ranking.addAll(sinEvaluar);
        return ranking;
    }

    private RankingProveedor crearFilaEvaluada(Proveedor proveedor, List<EvaluacionProveedor> evaluaciones) {
        BigDecimal acumulado = evaluaciones.stream()
                .map(evaluacion -> evaluacion.getPuntaje())
                .reduce(BigDecimal.ZERO, (suma, puntaje) -> suma.add(puntaje));
        BigDecimal promedio = acumulado.divide(BigDecimal.valueOf(evaluaciones.size()), 1, RoundingMode.HALF_UP);
        return new RankingProveedor(proveedor, promedio, evaluaciones.size());
    }

    private void validarEvaluacion(EvaluacionProveedor evaluacion) {
        if (evaluacion == null) {
            throw new ValidacionException("La evaluación no puede ser nula.");
        }

        if (evaluacion.getProveedorId() <= 0) {
            throw new ValidacionException("Debe indicar un proveedor válido para la evaluación.");
        }

        validarRango(evaluacion.getCalidad(), "calidad");
        validarRango(evaluacion.getPuntualidad(), "puntualidad");

        String comentario = normalizarComentario(evaluacion.getComentario());
        if (comentario != null && comentario.length() > MAX_LONGITUD_COMENTARIO) {
            throw new ValidacionException("El comentario no puede superar los " + MAX_LONGITUD_COMENTARIO + " caracteres.");
        }
        evaluacion.setComentario(comentario);
    }

    private void validarRango(int valor, String campo) {
        if (valor < MIN_CALIFICACION || valor > MAX_CALIFICACION) {
            throw new ValidacionException("La calificación de " + campo + " debe estar entre " + MIN_CALIFICACION + " y " + MAX_CALIFICACION + ".");
        }
    }

    private Proveedor validarProveedorExistente(int proveedorId) {
        Proveedor proveedor = proveedorDAO.buscarPorId(proveedorId);
        if (proveedor == null) {
            throw new EntidadNoEncontradaException("No existe un proveedor con el ID: " + proveedorId);
        }
        return proveedor;
    }

    private String normalizarComentario(String comentario) {
        String limpio = InputValidator.normalizarTexto(comentario);
        return limpio.isEmpty() ? null : limpio;
    }
}
