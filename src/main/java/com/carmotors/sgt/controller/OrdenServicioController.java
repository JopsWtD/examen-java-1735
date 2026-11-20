package com.carmotors.sgt.controller;

import com.carmotors.sgt.exception.CarMotorsException;
import com.carmotors.sgt.model.ConsultaOrdenServicio;
import com.carmotors.sgt.model.FiltroOrdenServicio;
import com.carmotors.sgt.model.ResumenOrdenServicio;
import com.carmotors.sgt.model.DetalleRepuesto;
import com.carmotors.sgt.model.LineaOrdenSolicitada;
import com.carmotors.sgt.model.Especialidad;
import com.carmotors.sgt.model.DetalleTecnico;
import com.carmotors.sgt.model.OrdenServicio;
import com.carmotors.sgt.model.ResultadoCancelarOrden;
import com.carmotors.sgt.model.ResultadoCompletarOrden;
import com.carmotors.sgt.model.ServicioTipo;
import com.carmotors.sgt.model.Tecnico;
import com.carmotors.sgt.model.OrdenServicioDetalle;
import com.carmotors.sgt.service.OrdenServicioService;
import com.carmotors.sgt.service.facade.TallerFacade;
import com.carmotors.sgt.service.chain.SugerenciaTecnico;

import java.math.BigDecimal;
import java.util.List;

/**
 * Controlador de órdenes de servicio (RNF-04): delega en {@link OrdenServicioService}.
 */
public class OrdenServicioController {

    private final OrdenServicioService ordenServicioService;
    private final TallerFacade tallerFacade;

    public OrdenServicioController(OrdenServicioService ordenServicioService) {
        this(ordenServicioService, null);
    }

    public OrdenServicioController(OrdenServicioService ordenServicioService, TallerFacade tallerFacade) {
        if (ordenServicioService == null) {
            throw new IllegalArgumentException("El servicio de órdenes de servicio es obligatorio.");
        }
        this.ordenServicioService = ordenServicioService;
        this.tallerFacade = tallerFacade;
    }

    public List<ResumenOrdenServicio> consultarOrdenes(FiltroOrdenServicio filtro) {
        return ordenServicioService.consultarOrdenes(filtro);
    }

    public ConsultaOrdenServicio consultarDetalle(int ordenId) {
        return ordenServicioService.consultarDetalle(ordenId);
    }

    public OrdenServicio crearOrden(int vehiculoId, String descripcionProblema, List<OrdenServicioDetalle> lineas) {
        return ordenServicioService.crearOrden(vehiculoId, descripcionProblema, lineas);
    }

    public OrdenServicio crearOrdenIntegral(int vehiculoId, String descripcion, List<LineaOrdenSolicitada> lineas) {
        return obtenerFachada().crearOrdenIntegral(vehiculoId, descripcion, lineas);
    }

    public List<Tecnico> listarTecnicosActivos() {
        return obtenerFachada().listarTecnicosActivos();
    }

    public Tecnico buscarTecnico(int tecnicoId) {
        return obtenerFachada().buscarTecnico(tecnicoId);
    }

    public String obtenerNombreEspecialidad(int especialidadId) {
        Especialidad especialidad = obtenerFachada().buscarEspecialidad(especialidadId);
        return especialidad == null ? "Desconocida" : especialidad.getNombre();
    }

    private TallerFacade obtenerFachada() {
        if (tallerFacade == null) {
            throw new CarMotorsException("La fachada del taller no está configurada.");
        }
        return tallerFacade;
    }

    public List<OrdenServicioDetalle> listarLineas(int ordenId) {
        return ordenServicioService.listarLineas(ordenId);
    }

    public boolean requiereTecnicoParaAgregar(int ordenId) {
        return ordenServicioService.requiereTecnicoParaAgregar(ordenId);
    }

    public boolean especialidadCoincide(ServicioTipo tipo, Tecnico tecnico) {
        return ordenServicioService.especialidadCoincide(tipo, tecnico);
    }

    public String agregarLinea(int ordenId, OrdenServicioDetalle linea, Integer tecnicoId,
                               boolean confirmarOtraEspecialidad) {
        try {
            int detalleId = ordenServicioService.agregarLinea(ordenId, linea, tecnicoId, confirmarOtraEspecialidad);
            return "Línea de servicio agregada. ID: " + detalleId;
        } catch (CarMotorsException e) {
            return "No se pudo agregar la línea: " + e.getMessage();
        }
    }

    public String quitarLinea(int ordenId, int detalleId) {
        try {
            ordenServicioService.quitarLinea(ordenId, detalleId);
            return "Línea de servicio retirada.";
        } catch (CarMotorsException e) {
            return "No se pudo retirar la línea: " + e.getMessage();
        }
    }

    public String actualizarCostoManoObra(int ordenId, int detalleId, BigDecimal nuevoCosto) {
        try {
            OrdenServicioDetalle actualizada = ordenServicioService.actualizarCostoManoObra(
                    ordenId, detalleId, nuevoCosto);
            return "Mano de obra de la línea " + actualizada.getId()
                    + " actualizada a $" + actualizada.getCostoManoObra().toPlainString() + ".";
        } catch (CarMotorsException e) {
            return "No se pudo ajustar la mano de obra: " + e.getMessage();
        }
    }

    public String obtenerNombreEstado(int estadoId) {
        return ordenServicioService.obtenerNombreEstado(estadoId);
    }

    public void validarOrdenEditable(int ordenId) {
        ordenServicioService.validarOrdenEditable(ordenId);
    }

    public SugerenciaTecnico sugerirTecnicoParaLinea(int ordenId, int detalleId) {
        return ordenServicioService.sugerirTecnicoParaLinea(ordenId, detalleId);
    }

    public SugerenciaTecnico sugerirTecnicoParaTipoServicio(int ordenId, int servicioTipoId) {
        return ordenServicioService.sugerirTecnicoParaTipoServicio(ordenId, servicioTipoId);
    }

    public boolean requiereConfirmacionEspecialidad(int ordenId, int detalleId, int tecnicoId) {
        return ordenServicioService.requiereConfirmacionEspecialidad(ordenId, detalleId, tecnicoId);
    }

    public List<DetalleTecnico> listarTecnicosDeLinea(int ordenId, int detalleId) {
        return ordenServicioService.listarTecnicosDeLinea(ordenId, detalleId);
    }

    public String asignarTecnicoALinea(
            int ordenId,
            int detalleId,
            int tecnicoId,
            BigDecimal horasTrabajadas,
            boolean confirmarOtraEspecialidad) {
        try {
            DetalleTecnico asignacion = ordenServicioService.asignarTecnicoALinea(
                    ordenId, detalleId, tecnicoId, horasTrabajadas, confirmarOtraEspecialidad);
            return "Técnico asignado a la línea " + detalleId
                    + " (horas registradas: " + asignacion.getHorasTrabajadas() + " h).";
        } catch (CarMotorsException e) {
            return "No se pudo asignar el técnico: " + e.getMessage();
        }
    }

    public String registrarHorasTrabajadas(
            int ordenId,
            int detalleId,
            int tecnicoId,
            BigDecimal horasTrabajadas) {
        try {
            DetalleTecnico actualizada = ordenServicioService.registrarHorasTrabajadas(
                    ordenId, detalleId, tecnicoId, horasTrabajadas);
            return "Horas trabajadas registradas: " + actualizada.getHorasTrabajadas() + " h.";
        } catch (CarMotorsException e) {
            return "No se pudieron registrar las horas: " + e.getMessage();
        }
    }

    public DetalleRepuesto reservarRepuesto(int detalleId, int repuestoId, int cantidad) {
        return ordenServicioService.reservarRepuesto(detalleId, repuestoId, cantidad);
    }

    public List<DetalleRepuesto> listarRepuestosDeLinea(int detalleId) {
        return ordenServicioService.listarRepuestosDeLinea(detalleId);
    }

    public String retirarRepuesto(int detalleId, int repuestoId) {
        try {
            DetalleRepuesto retirado = ordenServicioService.retirarRepuesto(detalleId, repuestoId);
            return "Repuesto '" + retirado.getNombreRepuestoSnapshot() + "' retirado de la línea "
                    + detalleId + " (unidades liberadas: " + retirado.getCantidadAsignada() + ").";
        } catch (CarMotorsException e) {
            return "No se pudo retirar el repuesto: " + e.getMessage();
        }
    }

    public int consultarDisponible(int repuestoId) {
        return ordenServicioService.consultarDisponible(repuestoId);
    }

    public OrdenServicio pasarAEnProceso(int ordenId) {
        return ordenServicioService.pasarAEnProceso(ordenId);
    }

    public ResultadoCompletarOrden completarOrden(int ordenId) {
        return tallerFacade == null ? ordenServicioService.completarOrden(ordenId)
                : tallerFacade.completarOrden(ordenId);
    }

    public ResultadoCancelarOrden cancelarOrden(int ordenId) {
        return ordenServicioService.cancelarOrden(ordenId);
    }

    public OrdenServicio entregarOrden(int ordenId) {
        return ordenServicioService.entregarOrden(ordenId);
    }
}
