/*
 * DAO: Operaciones de Usuario
 * CRUD + autenticación por cédula/contraseña
 */
package miproyectoequipo.dao;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import miproyectoequipo.modelo.Usuario;
import miproyectoequipo.modelo.Usuario.Perfil;

/**
 * DAO para la tabla usuarios.
 * Maneja CRUD y autenticación.
 * 
 * @author Vladimir
 */
public class UsuarioDAO {

    /**
     * Inserta un nuevo usuario.
     * @param usuario a insertar
     * @return true si se insertó correctamente
     */
    public boolean insertar(Usuario usuario) {
        String sql = "INSERT INTO usuarios (cedula, nombre, contrasena, perfil, activo) VALUES (?, ?, ?, ?, ?)";
        try (Connection conn = ConexionDB.getInstancia().getConexion();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, usuario.getCedula());
            ps.setString(2, usuario.getNombre());
            ps.setString(3, usuario.getContrasena());
            ps.setString(4, usuario.getPerfil().name());
            ps.setBoolean(5, usuario.isActivo());
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            System.err.println("[UsuarioDAO] Error al insertar: " + e.getMessage());
            return false;
        }
    }

    /**
     * Busca un usuario por su cédula.
     * @param cedula del usuario
     * @return Usuario encontrado o null
     */
    public Usuario buscarPorCedula(String cedula) {
        String sql = "SELECT * FROM usuarios WHERE cedula = ?";
        try (Connection conn = ConexionDB.getInstancia().getConexion();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, cedula);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                return mapearUsuario(rs);
            }
        } catch (SQLException e) {
            System.err.println("[UsuarioDAO] Error al buscar: " + e.getMessage());
        }
        return null;
    }

    /**
     * Autenticación por cédula y contraseña (fallback sin lector).
     * @param cedula del usuario
     * @param contrasena del usuario
     * @return Usuario autenticado o null
     */
    public Usuario autenticar(String cedula, String contrasena) {
        String sql = "SELECT * FROM usuarios WHERE cedula = ? AND contrasena = ? AND activo = 1";
        try (Connection conn = ConexionDB.getInstancia().getConexion();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, cedula);
            ps.setString(2, contrasena);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                return mapearUsuario(rs);
            }
        } catch (SQLException e) {
            System.err.println("[UsuarioDAO] Error al autenticar: " + e.getMessage());
        }
        return null;
    }

    /**
     * Lista todos los usuarios activos.
     * @return lista de usuarios
     */
    public List<Usuario> listarTodos() {
        List<Usuario> lista = new ArrayList<>();
        String sql = "SELECT * FROM usuarios WHERE activo = 1 ORDER BY nombre";
        try (Connection conn = ConexionDB.getInstancia().getConexion();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                lista.add(mapearUsuario(rs));
            }
        } catch (SQLException e) {
            System.err.println("[UsuarioDAO] Error al listar: " + e.getMessage());
        }
        return lista;
    }

    /**
     * Mapea un ResultSet a un objeto Usuario.
     */
    private Usuario mapearUsuario(ResultSet rs) throws SQLException {
        Usuario u = new Usuario();
        u.setId(rs.getInt("id"));
        u.setCedula(rs.getString("cedula"));
        u.setNombre(rs.getString("nombre"));
        u.setContrasena(rs.getString("contrasena"));
        u.setPerfil(Perfil.valueOf(rs.getString("perfil")));
        u.setActivo(rs.getBoolean("activo"));
        return u;
    }
}
