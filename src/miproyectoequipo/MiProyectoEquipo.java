
package miproyectoequipo;

import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import miproyectoequipo.dao.ConexionDB;
import miproyectoequipo.vista.LoginFrame;

public class MiProyectoEquipo {

    public static void main(String[] args) {

        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception e) {

        }

        System.out.println("=== Sistema de Registro de Empleados ===");
        System.out.println("Inicializando base de datos...");
        ConexionDB.getInstancia().inicializarTablas();

        SwingUtilities.invokeLater(() -> {
            LoginFrame login = new LoginFrame();
            login.setVisible(true);
        });
    }
}
