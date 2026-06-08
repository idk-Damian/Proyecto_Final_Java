
package miproyectoequipo.dao;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import miproyectoequipo.modelo.Usuario;
import miproyectoequipo.modelo.Usuario.Perfil;

public class UsuarioDAO {

    public boolean insertar(Usuario usuario) {
        String sql = "INSERT INTO usuarios (cedula, nombre, usuario, email, contrasena, perfil, activo) VALUES (?, ?, ?, ?, ?, ?, ?)";
        try (Connection conn = ConexionDB.getInstancia().getConexion();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, usuario.getCedula());
            ps.setString(2, usuario.getNombre());
            ps.setString(3, usuario.getUsuario());
            ps.setString(4, usuario.getEmail());
            ps.setString(5, usuario.getContrasena());
            ps.setString(6, usuario.getPerfil().name());
            ps.setBoolean(7, usuario.isActivo());
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            System.err.println("[UsuarioDAO] Error al insertar: " + e.getMessage());
            return false;
        }
    }

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

    public Usuario autenticar(String identificador, String contrasena) {
        String sql = "SELECT * FROM usuarios WHERE (cedula = ? OR email = ? OR usuario = ?) "
                   + "AND contrasena = ? AND activo = 1";
        try (Connection conn = ConexionDB.getInstancia().getConexion();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, identificador);
            ps.setString(2, identificador);
            ps.setString(3, identificador);
            ps.setString(4, contrasena);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                return mapearUsuario(rs);
            }
        } catch (SQLException e) {
            System.err.println("[UsuarioDAO] Error al autenticar: " + e.getMessage());
        }
        return null;
    }

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

    private Usuario mapearUsuario(ResultSet rs) throws SQLException {
        Usuario u = new Usuario();
        u.setId(rs.getInt("id"));
        u.setCedula(rs.getString("cedula"));
        u.setNombre(rs.getString("nombre"));
        u.setUsuario(rs.getString("usuario"));
        u.setEmail(rs.getString("email"));
        u.setContrasena(rs.getString("contrasena"));
        u.setPerfil(Perfil.valueOf(rs.getString("perfil")));
        u.setActivo(rs.getBoolean("activo"));
        return u;
    }
}
