/*
 * DAO: Operaciones de Huella Digital
 * Guardar, buscar y listar templates de huellas
 */
package miproyectoequipo.dao;

import java.sql.*;
import java.util.HashMap;
import java.util.Map;
import miproyectoequipo.modelo.HuellaDigital;

/**
 * DAO para la tabla huellas_digitales.
 * Gestiona el almacenamiento y recuperación de templates biométricos.
 * 
 * @author Vladimir
 */
public class HuellaDAO {

    /**
     * Guarda o actualiza la huella digital de un empleado.
     * Si ya existe una huella para esa cédula, la reemplaza.
     * 
     * @param cedula del empleado
     * @param templateBase64 template en formato Base64
     * @return true si se guardó correctamente
     */
    public boolean guardarHuella(String cedula, String templateBase64) {
        // Primero intentar actualizar
        String sqlUpdate = "UPDATE huellas_digitales SET template_base64 = ?, fecha_registro = GETDATE() WHERE cedula_empleado = ?";
        try (Connection conn = ConexionDB.getInstancia().getConexion();
             PreparedStatement ps = conn.prepareStatement(sqlUpdate)) {
            ps.setString(1, templateBase64);
            ps.setString(2, cedula);
            if (ps.executeUpdate() > 0) {
                System.out.println("[HuellaDAO] Huella actualizada para cédula: " + cedula);
                return true;
            }
        } catch (SQLException e) {
            System.err.println("[HuellaDAO] Error al actualizar: " + e.getMessage());
        }

        // Si no existía, insertar nueva
        String sqlInsert = "INSERT INTO huellas_digitales (cedula_empleado, template_base64) VALUES (?, ?)";
        try (Connection conn = ConexionDB.getInstancia().getConexion();
             PreparedStatement ps = conn.prepareStatement(sqlInsert)) {
            ps.setString(1, cedula);
            ps.setString(2, templateBase64);
            boolean ok = ps.executeUpdate() > 0;
            if (ok) {
                System.out.println("[HuellaDAO] Huella registrada para cédula: " + cedula);
            }
            return ok;
        } catch (SQLException e) {
            System.err.println("[HuellaDAO] Error al insertar: " + e.getMessage());
            return false;
        }
    }

    /**
     * Obtiene el template Base64 de la huella de un empleado.
     * 
     * @param cedula del empleado
     * @return template en Base64 o null si no existe
     */
    public String obtenerHuella(String cedula) {
        String sql = "SELECT template_base64 FROM huellas_digitales WHERE cedula_empleado = ?";
        try (Connection conn = ConexionDB.getInstancia().getConexion();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, cedula);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                return rs.getString("template_base64");
            }
        } catch (SQLException e) {
            System.err.println("[HuellaDAO] Error al obtener huella: " + e.getMessage());
        }
        return null;
    }

    /**
     * Obtiene todas las huellas registradas.
     * Usado para cargar el cache de identificación 1:N del SDK.
     * 
     * @return Map donde key=cédula, value=templateBase64
     */
    public Map<String, String> obtenerTodasHuellas() {
        Map<String, String> huellas = new HashMap<>();
        String sql = "SELECT cedula_empleado, template_base64 FROM huellas_digitales";
        try (Connection conn = ConexionDB.getInstancia().getConexion();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                huellas.put(rs.getString("cedula_empleado"), rs.getString("template_base64"));
            }
        } catch (SQLException e) {
            System.err.println("[HuellaDAO] Error al listar huellas: " + e.getMessage());
        }
        System.out.println("[HuellaDAO] Cargadas " + huellas.size() + " huellas de la BD.");
        return huellas;
    }

    /**
     * Elimina la huella digital de un empleado.
     * 
     * @param cedula del empleado
     * @return true si se eliminó
     */
    public boolean eliminarHuella(String cedula) {
        String sql = "DELETE FROM huellas_digitales WHERE cedula_empleado = ?";
        try (Connection conn = ConexionDB.getInstancia().getConexion();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, cedula);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            System.err.println("[HuellaDAO] Error al eliminar: " + e.getMessage());
            return false;
        }
    }

    /**
     * Verifica si un empleado tiene huella registrada.
     * 
     * @param cedula del empleado
     * @return true si tiene huella
     */
    public boolean tieneHuella(String cedula) {
        String sql = "SELECT COUNT(*) FROM huellas_digitales WHERE cedula_empleado = ?";
        try (Connection conn = ConexionDB.getInstancia().getConexion();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, cedula);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                return rs.getInt(1) > 0;
            }
        } catch (SQLException e) {
            System.err.println("[HuellaDAO] Error al verificar: " + e.getMessage());
        }
        return false;
    }
}
