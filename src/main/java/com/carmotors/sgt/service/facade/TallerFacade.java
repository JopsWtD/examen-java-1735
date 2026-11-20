package com.carmotors.sgt.service.facade;

import com.carmotors.sgt.exception.ValidacionException;
import com.carmotors.sgt.model.Especialidad;
import com.carmotors.sgt.model.LineaOrdenSolicitada;
import com.carmotors.sgt.model.OrdenServicio;
import com.carmotors.sgt.model.ResultadoCompletarOrden;
import com.carmotors.sgt.model.Tecnico;
import com.carmotors.sgt.service.InventarioService;
import com.carmotors.sgt.service.OrdenServicioService;
import com.carmotors.sgt.service.TecnicoService;

import java.util.List;

/** Punto de entrada para los flujos de órdenes que coordinan el taller. */
public class TallerFacade {
    private final OrdenServicioService ordenes;
    private final InventarioService inventario;
    private final TecnicoService tecnicos;

    public TallerFacade(OrdenServicioService ordenes, InventarioService inventario, TecnicoService tecnicos) {
        if (ordenes == null) {
            throw new ValidacionException("El servicio de órdenes de servicio es obligatorio.");
        }
        if (inventario == null) {
            throw new ValidacionException("El servicio de inventario es obligatorio.");
        }
        if (tecnicos == null) {
            throw new ValidacionException("El servicio de técnicos es obligatorio.");
        }
        this.ordenes = ordenes;
        this.inventario = inventario;
        this.tecnicos = tecnicos;
    }

    public OrdenServicio crearOrdenIntegral(int vehiculoId, String descripcion, List<LineaOrdenSolicitada> lineas) {
        return ordenes.crearOrdenIntegral(vehiculoId, descripcion, lineas);
    }

    public ResultadoCompletarOrden completarOrden(int ordenId) {
        ResultadoCompletarOrden resultado = ordenes.completarOrdenSinNotificar(ordenId);
        inventario.notificarStockBajoTrasCompletar(resultado);
        return resultado;
    }

    public List<Tecnico> listarTecnicosActivos() {
        return tecnicos.listarActivos();
    }

    public Especialidad buscarEspecialidad(int especialidadId) {
        return tecnicos.buscarEspecialidadPorId(especialidadId);
    }

    public Tecnico buscarTecnico(int tecnicoId) {
        return tecnicos.buscarPorId(tecnicoId);
    }
}
