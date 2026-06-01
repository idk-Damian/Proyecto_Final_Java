/*
 * DAO: Operaciones de Empleado
 * Esqueleto para que los compañeros completen
 */
package miproyectoequipo.dao;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import miproyectoequipo.modelo.Empleado;
import miproyectoequipo.modelo.Empleado.TipoContrato;
import miproyectoequipo.modelo.EmpleadoTiempoCompleto;
import miproyectoequipo.modelo.EmpleadoTiempoParcial;

/**
 * DAO para la tabla empleados.
 * Maneja CRUD de empleados (tiempo completo y parcial).
 * 
 * @author Vladimir
 */
public class EmpleadoDAO {

    /**
     * Inserta un nuevo empleado.
     * @param empleado a insertar
     * @return true si se insertó correctamente
     */
    public boolean insertar(Empleado empleado) {
        String sql = "INSERT INTO empleados (cedula, nombre, apellido, cargo, tipo_contrato) VALUES (?, ?, ?, ?, ?)";
        try (Connection conn = ConexionDB.getInstancia().getConexion();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, empleado.getCedula());
            ps.setString(2, empleado.getNombre());
            ps.setString(3, empleado.getApellido());
            ps.setString(4, empleado.getCargo());
            ps.setString(5, empleado.getTipoContrato().name());
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            System.err.println("[EmpleadoDAO] Error al insertar: " + e.getMessage());
            return false;
        }
    }

    /**
     * Busca un empleado por su cédula.
     * @param cedula del empleado
     * @return Empleado encontrado o null
     */
    public Empleado buscarPorCedula(String cedula) {
        String sql = "SELECT * FROM empleados WHERE cedula = ?";
        try (Connection conn = ConexionDB.getInstancia().getConexion();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, cedula);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                return mapearEmpleado(rs);
            }
        } catch (SQLException e) {
            System.err.println("[EmpleadoDAO] Error al buscar: " + e.getMessage());
        }
        return null;
    }

    /**
     * Busca un empleado por su ID interno.
     * @param id del empleado
     * @return Empleado encontrado o null
     */
    public Empleado buscarPorId(int id) {
        String sql = "SELECT * FROM empleados WHERE id = ?";
        try (Connection conn = ConexionDB.getInstancia().getConexion();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, id);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                return mapearEmpleado(rs);
            }
        } catch (SQLException e) {
            System.err.println("[EmpleadoDAO] Error al buscar por id: " + e.getMessage());
        }
        return null;
    }

    /**
     * Modifica la información de un empleado.
     * @param empleado con datos actualizados
     * @return true si se modificó
     */
    public boolean modificar(Empleado empleado) {
        String sql = "UPDATE empleados SET nombre = ?, apellido = ?, cargo = ?, tipo_contrato = ?, activo = ? WHERE cedula = ?";
        try (Connection conn = ConexionDB.getInstancia().getConexion();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, empleado.getNombre());
            ps.setString(2, empleado.getApellido());
            ps.setString(3, empleado.getCargo());
            ps.setString(4, empleado.getTipoContrato().name());
            ps.setInt(5, empleado.isActivo() ? 1 : 0);
            ps.setString(6, empleado.getCedula());
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            System.err.println("[EmpleadoDAO] Error al modificar: " + e.getMessage());
            return false;
        }
    }

    /**
     * Elimina (desactiva) un empleado.
     * @param cedula del empleado
     * @return true si se eliminó
     */
    public boolean eliminar(String cedula) {
        String sqlEliminarHuella = "DELETE FROM huellas_digitales WHERE cedula_empleado = ?";
        String sqlEliminarEmpleado = "DELETE FROM empleados WHERE cedula = ?";

        try (Connection conn = ConexionDB.getInstancia().getConexion()) {

            conn.setAutoCommit(false);

            try (
                PreparedStatement psHuella = conn.prepareStatement(sqlEliminarHuella);
                PreparedStatement psEmpleado = conn.prepareStatement(sqlEliminarEmpleado)
            ) {
                psHuella.setString(1, cedula);
                psHuella.executeUpdate();

                psEmpleado.setString(1, cedula);
                int filasEmpleado = psEmpleado.executeUpdate();

                conn.commit();

                return filasEmpleado > 0;

            } catch (SQLException e) {
                conn.rollback();
                System.err.println("Error al eliminar empleado: " + e.getMessage());
                return false;
            } finally {
                conn.setAutoCommit(true);
            }

        } catch (SQLException e) {
            System.err.println("Error de conexión al eliminar empleado: " + e.getMessage());
            return false;
        }
    }

    /**
     * Lista todos los empleados activos.
     * @return lista de empleados
     */
    public List<Empleado> listarTodos() {
        List<Empleado> lista = new ArrayList<>();
        String sql = "SELECT * FROM empleados WHERE activo = 1 ORDER BY apellido, nombre";
        try (Connection conn = ConexionDB.getInstancia().getConexion();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                lista.add(mapearEmpleado(rs));
            }
        } catch (SQLException e) {
            System.err.println("[EmpleadoDAO] Error al listar: " + e.getMessage());
        }
        return lista;
    }

    /**
     * Mapea un ResultSet al tipo correcto de Empleado (TC o TP).
     */
    private Empleado mapearEmpleado(ResultSet rs) throws SQLException {
        TipoContrato tipo = TipoContrato.valueOf(rs.getString("tipo_contrato"));
        Empleado emp;
        if (tipo == TipoContrato.TIEMPO_COMPLETO) {
            emp = new EmpleadoTiempoCompleto(
                rs.getString("cedula"), rs.getString("nombre"),
                rs.getString("apellido"), rs.getString("cargo")
            );
        } else {
            emp = new EmpleadoTiempoParcial(
                rs.getString("cedula"), rs.getString("nombre"),
                rs.getString("apellido"), rs.getString("cargo")
            );
        }
        emp.setId(rs.getInt("id"));
        emp.setActivo(rs.getBoolean("activo"));
        return emp;
    }
}
