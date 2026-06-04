/*
 * DAO: Operaciones de Asistencia
 * Esqueleto para que los compañeros completen
 */
package miproyectoequipo.dao;

import java.sql.*;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import miproyectoequipo.modelo.RegistroAsistencia;

/**
 * DAO para la tabla asistencias.
 * Registra y consulta asistencias diarias.
 * 
 * @author Vladimir
 */
public class AsistenciaDAO {

    /**
     * Registra o actualiza la asistencia de un empleado para hoy.
     * Valida que no haya duplicados.
     * 
     * @param registro datos de asistencia
     * @return true si se registró correctamente
     */
    public boolean registrarAsistencia(RegistroAsistencia registro) {
        // Verificar si ya existe registro para hoy
        RegistroAsistencia existente = buscarPorCedulaYFecha(registro.getCedulaEmpleado(), registro.getFecha());
        
        if (existente != null) {
            return actualizarAsistencia(registro);
        }

        String sql = "INSERT INTO asistencias (cedula_empleado, fecha, hora_entrada_manana, hora_salida_manana, " +
                     "hora_entrada_tarde, hora_salida_tarde, minutos_atraso) VALUES (?, ?, ?, ?, ?, ?, ?)";
        try (Connection conn = ConexionDB.getInstancia().getConexion();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, registro.getCedulaEmpleado());
            ps.setDate(2, Date.valueOf(registro.getFecha()));
            ps.setTime(3, registro.getHoraEntradaManana() != null ? Time.valueOf(registro.getHoraEntradaManana()) : null);
            ps.setTime(4, registro.getHoraSalidaManana() != null ? Time.valueOf(registro.getHoraSalidaManana()) : null);
            ps.setTime(5, registro.getHoraEntradaTarde() != null ? Time.valueOf(registro.getHoraEntradaTarde()) : null);
            ps.setTime(6, registro.getHoraSalidaTarde() != null ? Time.valueOf(registro.getHoraSalidaTarde()) : null);
            ps.setInt(7, registro.calcularMinutosAtraso());
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            System.err.println("[AsistenciaDAO] Error al registrar: " + e.getMessage());
            return false;
        }
    }

    /**
     * Actualiza un registro de asistencia existente.
     */
    private boolean actualizarAsistencia(RegistroAsistencia registro) {
        String sql = "UPDATE asistencias SET hora_entrada_manana = COALESCE(?, hora_entrada_manana), " +
                     "hora_salida_manana = COALESCE(?, hora_salida_manana), " +
                     "hora_entrada_tarde = COALESCE(?, hora_entrada_tarde), " +
                     "hora_salida_tarde = COALESCE(?, hora_salida_tarde), " +
                     "minutos_atraso = ? " +
                     "WHERE cedula_empleado = ? AND fecha = ?";
        try (Connection conn = ConexionDB.getInstancia().getConexion();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setTime(1, registro.getHoraEntradaManana() != null ? Time.valueOf(registro.getHoraEntradaManana()) : null);
            ps.setTime(2, registro.getHoraSalidaManana() != null ? Time.valueOf(registro.getHoraSalidaManana()) : null);
            ps.setTime(3, registro.getHoraEntradaTarde() != null ? Time.valueOf(registro.getHoraEntradaTarde()) : null);
            ps.setTime(4, registro.getHoraSalidaTarde() != null ? Time.valueOf(registro.getHoraSalidaTarde()) : null);
            ps.setInt(5, registro.calcularMinutosAtraso());
            ps.setString(6, registro.getCedulaEmpleado());
            ps.setDate(7, Date.valueOf(registro.getFecha()));
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            System.err.println("[AsistenciaDAO] Error al actualizar: " + e.getMessage());
            return false;
        }
    }

    /**
     * Busca el registro de asistencia de un empleado en una fecha.
     * @param cedula del empleado
     * @param fecha a buscar
     * @return RegistroAsistencia o null
     */
    public RegistroAsistencia buscarPorCedulaYFecha(String cedula, LocalDate fecha) {
        String sql = "SELECT * FROM asistencias WHERE cedula_empleado = ? AND fecha = ?";
        try (Connection conn = ConexionDB.getInstancia().getConexion();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, cedula);
            ps.setDate(2, Date.valueOf(fecha));
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                return mapearAsistencia(rs);
            }
        } catch (SQLException e) {
            System.err.println("[AsistenciaDAO] Error al buscar: " + e.getMessage());
        }
        return null;
    }

    /**
     * Lista asistencias de un empleado en un rango de fechas.
     * @param cedula del empleado
     * @param desde fecha inicio
     * @param hasta fecha fin
     * @return lista de registros
     */
    public List<RegistroAsistencia> listarPorCedulaYRango(String cedula, LocalDate desde, LocalDate hasta) {
        List<RegistroAsistencia> lista = new ArrayList<>();
        String sql = "SELECT * FROM asistencias WHERE cedula_empleado = ? AND fecha BETWEEN ? AND ? ORDER BY fecha";
        try (Connection conn = ConexionDB.getInstancia().getConexion();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, cedula);
            ps.setDate(2, Date.valueOf(desde));
            ps.setDate(3, Date.valueOf(hasta));
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                lista.add(mapearAsistencia(rs));
            }
        } catch (SQLException e) {
            System.err.println("[AsistenciaDAO] Error al listar: " + e.getMessage());
        }
        return lista;
    }

    /**
     * Lista asistencias de todos los empleados en un rango de fechas.
     * @param desde fecha inicio
     * @param hasta fecha fin
     * @return lista de registros
     */
    public List<RegistroAsistencia> listarTodosPorRango(LocalDate desde, LocalDate hasta) {
        List<RegistroAsistencia> lista = new ArrayList<>();
        String sql = "SELECT * FROM asistencias WHERE fecha BETWEEN ? AND ? ORDER BY fecha, cedula_empleado";
        try (Connection conn = ConexionDB.getInstancia().getConexion();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setDate(1, Date.valueOf(desde));
            ps.setDate(2, Date.valueOf(hasta));
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                lista.add(mapearAsistencia(rs));
            }
        } catch (SQLException e) {
            System.err.println("[AsistenciaDAO] Error al listar todos por rango: " + e.getMessage());
        }
        return lista;
    }

    /**
     * Mapea un ResultSet a RegistroAsistencia.
     */
    private RegistroAsistencia mapearAsistencia(ResultSet rs) throws SQLException {
        RegistroAsistencia reg = new RegistroAsistencia();
        reg.setId(rs.getInt("id"));
        reg.setCedulaEmpleado(rs.getString("cedula_empleado"));
        reg.setFecha(rs.getDate("fecha").toLocalDate());

        Time t;
        t = rs.getTime("hora_entrada_manana");
        if (t != null) reg.setHoraEntradaManana(t.toLocalTime());
        t = rs.getTime("hora_salida_manana");
        if (t != null) reg.setHoraSalidaManana(t.toLocalTime());
        t = rs.getTime("hora_entrada_tarde");
        if (t != null) reg.setHoraEntradaTarde(t.toLocalTime());
        t = rs.getTime("hora_salida_tarde");
        if (t != null) reg.setHoraSalidaTarde(t.toLocalTime());

        reg.setMinutosAtraso(rs.getInt("minutos_atraso"));
        return reg;
    }
}
