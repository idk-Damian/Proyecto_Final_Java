
package miproyectoequipo.vista;

import java.awt.*;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.image.BufferedImage;
import java.io.File;
import javax.imageio.ImageIO;
import javax.swing.*;
import miproyectoequipo.dao.HuellaDAO;
import miproyectoequipo.huella.HuellaListener;
import miproyectoequipo.huella.ZKFingerprintManager;

public class ValidacionHuellaDialog extends JDialog implements HuellaListener {

    private static final Color COLOR_UTA = new Color(122, 0, 30);
    private static final Color COLOR_UTA_OSCURO = new Color(90, 0, 22);
    private static final Color COLOR_EXITO = new Color(0, 128, 0);
    private static final Color COLOR_ERROR = new Color(178, 0, 0);

    private final ZKFingerprintManager manager;
    private final String cedulaEsperada;
    private final String nombreEmpleado;

    private JLabel lblEstado;
    private JLabel lblImagenHuella;
    private boolean validado = false;
    private boolean cerrado = false;

    public ValidacionHuellaDialog(JFrame parent, ZKFingerprintManager manager, String cedulaEsperada, String nombreEmpleado) {
        super(parent, "Validación por Huella", true);
        this.manager = manager;
        this.cedulaEsperada = cedulaEsperada;
        this.nombreEmpleado = nombreEmpleado;

        setSize(440, 260);
        setLocationRelativeTo(parent);
        setLayout(new BorderLayout());

        JLabel banner = new JLabel("Validación por Huella Dactilar", SwingConstants.CENTER);
        banner.setOpaque(true);
        banner.setBackground(COLOR_UTA);
        banner.setForeground(Color.WHITE);
        banner.setFont(new Font("Segoe UI", Font.BOLD, 16));
        banner.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createMatteBorder(0, 0, 3, 0, COLOR_UTA_OSCURO),
            BorderFactory.createEmptyBorder(12, 10, 12, 10)));
        add(banner, BorderLayout.NORTH);

        JPanel centro = new JPanel();
        centro.setLayout(new BoxLayout(centro, BoxLayout.Y_AXIS));
        centro.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));

        JLabel lblEmp = new JLabel("Empleado: " + nombreEmpleado, SwingConstants.CENTER);
        lblEmp.setFont(new Font("Segoe UI", Font.BOLD, 14));
        lblEmp.setAlignmentX(Component.CENTER_ALIGNMENT);

        lblImagenHuella = new JLabel("(Huella)", SwingConstants.CENTER);
        lblImagenHuella.setPreferredSize(new Dimension(160, 180));
        lblImagenHuella.setMaximumSize(new Dimension(160, 180));
        lblImagenHuella.setHorizontalAlignment(SwingConstants.CENTER);
        lblImagenHuella.setForeground(new Color(90, 90, 90));
        lblImagenHuella.setBorder(BorderFactory.createLineBorder(new Color(190, 190, 190)));
        lblImagenHuella.setAlignmentX(Component.CENTER_ALIGNMENT);

        lblEstado = new JLabel("Conectando al lector...", SwingConstants.CENTER);
        lblEstado.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        lblEstado.setAlignmentX(Component.CENTER_ALIGNMENT);

        centro.add(lblEmp);
        centro.add(Box.createRigidArea(new Dimension(0, 12)));
        centro.add(lblImagenHuella);
        centro.add(Box.createRigidArea(new Dimension(0, 12)));
        centro.add(lblEstado);
        add(centro, BorderLayout.CENTER);

        JButton btnCancelar = new JButton("Cancelar");
        btnCancelar.addActionListener(e -> dispose());
        JPanel sur = new JPanel(new FlowLayout(FlowLayout.CENTER));
        sur.add(btnCancelar);
        add(sur, BorderLayout.SOUTH);

        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                finalizar();
            }
        });

        manager.setListener(this);

        new Thread(() -> {
            if (!manager.isActivo()) {
                boolean ok = manager.iniciar();
                if (!ok) {
                    SwingUtilities.invokeLater(() -> mostrar("No se pudo conectar el lector. Cancele e intente otro método.", COLOR_ERROR));
                    return;
                }
            }
            manager.cargarHuellasEnCache(new HuellaDAO().obtenerTodasHuellas());
            SwingUtilities.invokeLater(() -> mostrar("Coloque su dedo en el lector...", COLOR_UTA));
        }, "ValidacionHuellaInit").start();
    }

    public boolean isValidado() {
        return validado;
    }

    private void mostrar(String texto, Color color) {
        lblEstado.setText(texto);
        lblEstado.setForeground(color);
    }

    private void finalizar() {
        if (cerrado) return;
        cerrado = true;

        manager.setListener(null);
    }

    @Override
    public void dispose() {
        finalizar();
        super.dispose();
    }

    @Override
    public void onIdentificacionResultado(String cedula, int score) {
        if (cedula != null && cedula.equals(cedulaEsperada)) {
            validado = true;
            mostrar("Identidad confirmada por huella.", COLOR_EXITO);
            Timer t = new Timer(900, e -> dispose());
            t.setRepeats(false);
            t.start();
        } else if (cedula != null) {
            mostrar("La huella pertenece a otra persona. Intente de nuevo.", COLOR_ERROR);
        } else {
            mostrar("Huella no reconocida. Intente de nuevo.", COLOR_ERROR);
        }
    }

    @Override
    public void onDispositivoListo(int ancho, int alto) {
        mostrar("Coloque su dedo en el lector...", COLOR_UTA);
    }

    @Override
    public void onDispositivoError(String mensaje) {
        mostrar(mensaje, COLOR_ERROR);
    }

    @Override
    public void onCapturaExitosa(byte[] imagen, byte[] template, String base64) {

        try {
            manager.guardarImagenBMP(imagen, "temp_fingerprint.bmp");
            BufferedImage img = ImageIO.read(new File("temp_fingerprint.bmp"));
            if (img != null) {
                Image scaled = img.getScaledInstance(150, 170, Image.SCALE_SMOOTH);
                lblImagenHuella.setText(null);
                lblImagenHuella.setIcon(new ImageIcon(scaled));
            }
        } catch (Exception e) {

        }
    }

    @Override
    public void onCapturaError(String mensaje) {
        mostrar(mensaje, COLOR_ERROR);
    }

    @Override
    public void onProgresoEnroll(int capturaActual, int totalCapturas) {

    }

    @Override
    public void onEnrollCompleto(String templateBase64) {

    }
}
