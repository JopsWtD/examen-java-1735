package com.carmotors.sgt.dao;

import java.util.List;

import com.carmotors.sgt.model.ProveedorRepuesto;

/**
 * Acceso a datos del catálogo proveedor-repuesto (tabla {@code proveedor_repuestos}).
 * Solo persiste y consulta; las reglas de RF-29 y RN-18 viven en el servicio.
 */
public interface ProveedorRepuestoDAO {

    List<ProveedorRepuesto> listarPorProveedor(int proveedorId);

    List<ProveedorRepuesto> listarPorRepuesto(int repuestoId);

    ProveedorRepuesto buscarPorProveedorYRepuesto(int proveedorId, int repuestoId);

    boolean guardar(ProveedorRepuesto proveedorRepuesto);

    boolean actualizarCondiciones(ProveedorRepuesto proveedorRepuesto);

    boolean eliminar(int proveedorId, int repuestoId);

    /**
     * Quita la marca de habitual a todos los proveedores del repuesto.
     *
     * @return cantidad de filas desmarcadas (0 si no había habitual)
     */
    int desmarcarHabitual(int repuestoId);

    /**
     * Quita la marca de habitual en todos los repuestos donde el proveedor dado era habitual (RF-28, RN-18).
     *
     * @param proveedorId identificador del proveedor desactivado
     * @return cantidad de filas desmarcadas
     */
    default int desmarcarHabitualesDeProveedor(int proveedorId) {
        return 0;
    }

    boolean marcarHabitual(int proveedorId, int repuestoId);
}
