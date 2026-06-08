
package miproyectoequipo.dao;

import java.sql.*;
import java.util.HashMap;
import java.util.Map;
import miproyectoequipo.modelo.HuellaDigital;

public class HuellaDAO {

    public boolean guardarHuella(String cedula, String templateBase64) {

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
