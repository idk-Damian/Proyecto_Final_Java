/*
 * Vista: Pantalla de Login con autenticación por huella dactilar
 * Permite login por huella ZKTeco o por cédula/contraseña (fallback)
 */
package miproyectoequipo.vista;

import java.awt.*;
import java.awt.event.*;
import java.awt.geom.RoundRectangle2D;
import java.awt.image.BufferedImage;
import java.io.File;
import java.util.Map;
import javax.imageio.ImageIO;
import javax.swing.*;
import javax.swing.border.*;
import miproyectoequipo.dao.ConexionDB;
import miproyectoequipo.dao.HuellaDAO;
import miproyectoequipo.dao.UsuarioDAO;
import miproyectoequipo.huella.HuellaListener;
import miproyectoequipo.huella.ZKFingerprintManager;
import miproyectoequipo.modelo.Usuario;

/**
 * Pantalla de inicio de sesión.
 * Modo principal: autenticación por huella dactilar (ZKTeco ZK9500).
 * Modo alternativo: cédula + contraseña (si no hay lector conectado).
 *
 * @author Vladimir
 */
public class LoginFrame extends JFrame implements HuellaListener {

    // Colores del tema
    private static final Color COLOR_FONDO = new Color(15, 23, 42);
    private static final Color COLOR_PANEL = new Color(30, 41, 59);
    private static final Color COLOR_PANEL_CLARO = new Color(51, 65, 85);
    private static final Color COLOR_ACENTO = new Color(56, 189, 248);
    private static final Color COLOR_ACENTO_HOVER = new Color(14, 165, 233);
    private static final Color COLOR_EXITO = new Color(34, 197, 94);
    private static final Color COLOR_ERROR = new Color(239, 68, 68);
    private static final Color COLOR_TEXTO = new Color(226, 232, 240);
    private static final Color COLOR_TEXTO_SEC = new Color(148, 163, 184);
    private static final Color COLOR_INPUT_BG = new Color(15, 23, 42);
    private static final Color COLOR_INPUT_BORDER = new Color(71, 85, 105);

    // Componentes
    private JLabel lblEstadoHuella;
    private JLabel lblImagenHuella;
    private JLabel lblMensaje;
    private JTextField txtCedula;
    private JPasswordField txtContrasena;
    private JButton btnConectarLector;
    private JButton btnLoginManual;
    private JPanel panelHuella;
    private JPanel panelManual;
    private CardLayout cardLayout;
    private JPanel panelCentral;

    // Lógica
    private ZKFingerprintManager fingerprintManager;
    private UsuarioDAO usuarioDAO;
    private HuellaDAO huellaDAO;
    private boolean lectorConectado = false;

    // Animación
    private Timer animTimer;
    private int animAngle = 0;

    public LoginFrame() {
        usuarioDAO = new UsuarioDAO();
        huellaDAO = new HuellaDAO();
        fingerprintManager = new ZKFingerprintManager();
        fingerprintManager.setListener(this);

        initComponents(); // Método generado por NetBeans
        initCustomComponents(); // Nuestro diseño personalizado
        intentarConectarLector();
    }

    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {
        lblTitulo = new javax.swing.JLabel();
        setDefaultCloseOperation(javax.swing.WindowConstants.EXIT_ON_CLOSE);
        setTitle("Sistema de Asistencia - Iniciar Sesion");

        lblTitulo.setFont(new java.awt.Font("Segoe UI", 1, 24)); // NOI18N
        lblTitulo.setText("Sistema de Asistencia");

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addGap(160, 160, 160)
                .addComponent(lblTitulo)
                .addContainerGap(160, Short.MAX_VALUE))
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addGap(250, 250, 250)
                .addComponent(lblTitulo)
                .addContainerGap(250, Short.MAX_VALUE))
        );

        pack();
        setLocationRelativeTo(null);
    }// </editor-fold>//GEN-END:initComponents

    private void initCustomComponents() {
        // Limpiamos el diseño auto-generado para poner el nuestro
        getContentPane().removeAll();
        setTitle("Sistema de Asistencia — Iniciar Sesión");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(520, 680);
        setLocationRelativeTo(null);
        setResizable(false);
        getContentPane().setBackground(COLOR_FONDO);
        setLayout(new BorderLayout());

        // === Panel Superior: Título ===
        JPanel panelTitulo = new JPanel();
        panelTitulo.setBackground(COLOR_FONDO);
        panelTitulo.setLayout(new BoxLayout(panelTitulo, BoxLayout.Y_AXIS));
        panelTitulo.setBorder(BorderFactory.createEmptyBorder(30, 0, 10, 0));

        JLabel lblTitulo = new JLabel("🏢 Sistema de Asistencia", SwingConstants.CENTER);
        lblTitulo.setFont(new Font("Segoe UI", Font.BOLD, 24));
        lblTitulo.setForeground(COLOR_TEXTO);
        lblTitulo.setAlignmentX(Component.CENTER_ALIGNMENT);

        JLabel lblSubtitulo = new JLabel("Registro de Empleados", SwingConstants.CENTER);
        lblSubtitulo.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        lblSubtitulo.setForeground(COLOR_TEXTO_SEC);
        lblSubtitulo.setAlignmentX(Component.CENTER_ALIGNMENT);

        panelTitulo.add(lblTitulo);
        panelTitulo.add(Box.createRigidArea(new Dimension(0, 5)));
        panelTitulo.add(lblSubtitulo);
        add(panelTitulo, BorderLayout.NORTH);

        // === Panel Central con CardLayout ===
        cardLayout = new CardLayout();
        panelCentral = new JPanel(cardLayout);
        panelCentral.setOpaque(false);

        panelHuella = crearPanelHuella();
        panelManual = crearPanelManual();

        panelCentral.add(panelHuella, "HUELLA");
        panelCentral.add(panelManual, "MANUAL");

        add(panelCentral, BorderLayout.CENTER);

        // === Panel Inferior: Toggle y estado ===
        JPanel panelInferior = new JPanel();
        panelInferior.setBackground(COLOR_FONDO);
        panelInferior.setLayout(new BoxLayout(panelInferior, BoxLayout.Y_AXIS));
        panelInferior.setBorder(BorderFactory.createEmptyBorder(0, 40, 25, 40));

        // Botón toggle entre modos
        JButton btnToggle = crearBotonLink("Cambiar a login manual / huella");
        btnToggle.addActionListener(e -> toggleModo());
        btnToggle.setAlignmentX(Component.CENTER_ALIGNMENT);

        // Estado de conexión
        lblMensaje = new JLabel(" ", SwingConstants.CENTER);
        lblMensaje.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        lblMensaje.setForeground(COLOR_TEXTO_SEC);
        lblMensaje.setAlignmentX(Component.CENTER_ALIGNMENT);

        panelInferior.add(btnToggle);
        panelInferior.add(Box.createRigidArea(new Dimension(0, 10)));
        panelInferior.add(lblMensaje);

        add(panelInferior, BorderLayout.SOUTH);

        // Cerrar lector al salir
        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                if (fingerprintManager.isActivo()) {
                    fingerprintManager.cerrar();
                }
                ConexionDB.getInstancia().cerrarConexion();
            }
        });

        // Animación de espera
        animTimer = new Timer(50, e -> {
            animAngle = (animAngle + 3) % 360;
            if (lblImagenHuella != null) {
                lblImagenHuella.repaint();
            }
        });
    }

    /**
     * Crea el panel de login por huella dactilar.
     */
    private JPanel crearPanelHuella() {
        JPanel panel = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                Graphics2D g2d = (Graphics2D) g.create();
                g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

                // Fondo del card con bordes redondeados
                int x = 40, y = 10, w = getWidth() - 80, h = getHeight() - 20;
                g2d.setColor(COLOR_PANEL);
                g2d.fill(new RoundRectangle2D.Float(x, y, w, h, 20, 20));

                // Borde sutil
                g2d.setColor(COLOR_PANEL_CLARO);
                g2d.setStroke(new BasicStroke(1));
                g2d.draw(new RoundRectangle2D.Float(x, y, w, h, 20, 20));

                g2d.dispose();
            }
        };
        panel.setOpaque(false);
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.setBorder(BorderFactory.createEmptyBorder(30, 70, 20, 70));

        // Icono de huella / imagen del sensor
        lblImagenHuella = new JLabel() {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                if (getIcon() == null && !lectorConectado) {
                    // Dibujar animación circular de espera
                    Graphics2D g2d = (Graphics2D) g.create();
                    g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

                    int cx = getWidth() / 2;
                    int cy = getHeight() / 2;
                    int radius = 50;

                    // Círculo base
                    g2d.setColor(COLOR_PANEL_CLARO);
                    g2d.setStroke(new BasicStroke(3));
                    g2d.drawOval(cx - radius, cy - radius, radius * 2, radius * 2);

                    // Arco animado
                    g2d.setColor(COLOR_ACENTO);
                    g2d.setStroke(new BasicStroke(3, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
                    g2d.drawArc(cx - radius, cy - radius, radius * 2, radius * 2, animAngle, 90);

                    // Icono de huella (texto)
                    g2d.setFont(new Font("Segoe UI Emoji", Font.PLAIN, 40));
                    FontMetrics fm = g2d.getFontMetrics();
                    String icon = "🔒";
                    g2d.setColor(COLOR_TEXTO_SEC);
                    g2d.drawString(icon, cx - fm.stringWidth(icon) / 2, cy + fm.getAscent() / 3);

                    g2d.dispose();
                }
            }
        };
        lblImagenHuella.setPreferredSize(new Dimension(200, 200));
        lblImagenHuella.setMaximumSize(new Dimension(200, 200));
        lblImagenHuella.setHorizontalAlignment(SwingConstants.CENTER);
        lblImagenHuella.setAlignmentX(Component.CENTER_ALIGNMENT);

        // Estado
        lblEstadoHuella = new JLabel("Conectando al lector...", SwingConstants.CENTER);
        lblEstadoHuella.setFont(new Font("Segoe UI", Font.BOLD, 14));
        lblEstadoHuella.setForeground(COLOR_TEXTO);
        lblEstadoHuella.setAlignmentX(Component.CENTER_ALIGNMENT);

        // Botón conectar/reconectar
        btnConectarLector = crearBoton("🔄 Reconectar Lector", COLOR_PANEL_CLARO);
        btnConectarLector.setAlignmentX(Component.CENTER_ALIGNMENT);
        btnConectarLector.addActionListener(e -> intentarConectarLector());
        btnConectarLector.setVisible(false);

        panel.add(Box.createRigidArea(new Dimension(0, 20)));
        panel.add(lblImagenHuella);
        panel.add(Box.createRigidArea(new Dimension(0, 20)));
        panel.add(lblEstadoHuella);
        panel.add(Box.createRigidArea(new Dimension(0, 15)));
        panel.add(btnConectarLector);

        return panel;
    }

    /**
     * Crea el panel de login manual (cédula + contraseña).
     */
    private JPanel crearPanelManual() {
        JPanel panel = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                Graphics2D g2d = (Graphics2D) g.create();
                g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

                int x = 40, y = 10, w = getWidth() - 80, h = getHeight() - 20;
                g2d.setColor(COLOR_PANEL);
                g2d.fill(new RoundRectangle2D.Float(x, y, w, h, 20, 20));
                g2d.setColor(COLOR_PANEL_CLARO);
                g2d.setStroke(new BasicStroke(1));
                g2d.draw(new RoundRectangle2D.Float(x, y, w, h, 20, 20));
                g2d.dispose();
            }
        };
        panel.setOpaque(false);
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.setBorder(BorderFactory.createEmptyBorder(40, 80, 20, 80));

        JLabel lblIcono = new JLabel("🔐", SwingConstants.CENTER);
        lblIcono.setFont(new Font("Segoe UI Emoji", Font.PLAIN, 48));
        lblIcono.setAlignmentX(Component.CENTER_ALIGNMENT);

        JLabel lblTitLogin = new JLabel("Iniciar Sesión Manual", SwingConstants.CENTER);
        lblTitLogin.setFont(new Font("Segoe UI", Font.BOLD, 18));
        lblTitLogin.setForeground(COLOR_TEXTO);
        lblTitLogin.setAlignmentX(Component.CENTER_ALIGNMENT);

        // Campo Cédula
        JLabel lblCedula = new JLabel("Cédula");
        lblCedula.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        lblCedula.setForeground(COLOR_TEXTO_SEC);
        lblCedula.setAlignmentX(Component.LEFT_ALIGNMENT);

        txtCedula = crearCampoTexto();

        // Campo Contraseña
        JLabel lblPass = new JLabel("Contraseña");
        lblPass.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        lblPass.setForeground(COLOR_TEXTO_SEC);
        lblPass.setAlignmentX(Component.LEFT_ALIGNMENT);

        txtContrasena = new JPasswordField();
        estilizarCampo(txtContrasena);

        // Botón login
        btnLoginManual = crearBoton("Ingresar", COLOR_ACENTO);
        btnLoginManual.setAlignmentX(Component.CENTER_ALIGNMENT);
        btnLoginManual.addActionListener(e -> loginManual());

        // Enter para login
        txtContrasena.addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                if (e.getKeyCode() == KeyEvent.VK_ENTER) {
                    loginManual();
                }
            }
        });

        panel.add(lblIcono);
        panel.add(Box.createRigidArea(new Dimension(0, 10)));
        panel.add(lblTitLogin);
        panel.add(Box.createRigidArea(new Dimension(0, 25)));
        panel.add(lblCedula);
        panel.add(Box.createRigidArea(new Dimension(0, 5)));
        panel.add(txtCedula);
        panel.add(Box.createRigidArea(new Dimension(0, 15)));
        panel.add(lblPass);
        panel.add(Box.createRigidArea(new Dimension(0, 5)));
        panel.add(txtContrasena);
        panel.add(Box.createRigidArea(new Dimension(0, 25)));
        panel.add(btnLoginManual);

        return panel;
    }

    // =============================================
    // Lógica de autenticación
    // =============================================

    /**
     * Intenta conectar al lector de huellas.
     */
    private void intentarConectarLector() {
        lblEstadoHuella.setText("Conectando al lector...");
        lblEstadoHuella.setForeground(COLOR_TEXTO);
        btnConectarLector.setVisible(false);
        animTimer.start();

        // Conectar en hilo separado para no bloquear UI
        new Thread(() -> {
            boolean ok = fingerprintManager.iniciar();
            if (ok) {
                // Cargar huellas de la BD al cache del SDK
                Map<String, String> huellas = huellaDAO.obtenerTodasHuellas();
                fingerprintManager.cargarHuellasEnCache(huellas);
            }
        }, "LectorInit").start();
    }

    /**
     * Login manual con cédula y contraseña.
     */
    private void loginManual() {
        String cedula = txtCedula.getText().trim();
        String contrasena = new String(txtContrasena.getPassword());

        if (cedula.isEmpty() || contrasena.isEmpty()) {
            mostrarMensaje("Complete todos los campos.", COLOR_ERROR);
            return;
        }

        Usuario usuario = usuarioDAO.autenticar(cedula, contrasena);
        if (usuario != null) {
            abrirPanelPrincipal(usuario);
        } else {
            mostrarMensaje("Cédula o contraseña incorrecta.", COLOR_ERROR);
            txtContrasena.setText("");
        }
    }

    /**
     * Abre el panel principal con el usuario autenticado.
     */
    private void abrirPanelPrincipal(Usuario usuario) {
        if (fingerprintManager.isActivo()) {
            fingerprintManager.cerrar();
        }
        animTimer.stop();

        PanelPrincipalFrame panel = new PanelPrincipalFrame(usuario);
        panel.setVisible(true);
        this.dispose();
    }

    private void toggleModo() {
        if (panelCentral.getComponent(0).isVisible()) {
            cardLayout.show(panelCentral, "MANUAL");
        } else {
            cardLayout.show(panelCentral, "HUELLA");
        }
    }

    private void mostrarMensaje(String msg, Color color) {
        lblMensaje.setText(msg);
        lblMensaje.setForeground(color);

        // Auto-limpiar después de 5 segundos
        Timer t = new Timer(5000, e -> lblMensaje.setText(" "));
        t.setRepeats(false);
        t.start();
    }

    // =============================================
    // HuellaListener - Callbacks del lector
    // =============================================

    @Override
    public void onCapturaExitosa(byte[] imagen, byte[] template, String base64) {
        // Mostrar imagen de la huella capturada
        try {
            fingerprintManager.guardarImagenBMP(imagen, "temp_fingerprint.bmp");
            BufferedImage img = ImageIO.read(new File("temp_fingerprint.bmp"));
            if (img != null) {
                Image scaled = img.getScaledInstance(180, 200, Image.SCALE_SMOOTH);
                lblImagenHuella.setIcon(new ImageIcon(scaled));
            }
        } catch (Exception e) {
            // No es crítico si falla la visualización
        }
    }

    @Override
    public void onCapturaError(String mensaje) {
        lblEstadoHuella.setText(mensaje);
        lblEstadoHuella.setForeground(COLOR_ERROR);
    }

    @Override
    public void onDispositivoListo(int ancho, int alto) {
        lectorConectado = true;
        animTimer.stop();
        lblEstadoHuella.setText("✅ Lector conectado — Coloque su dedo");
        lblEstadoHuella.setForeground(COLOR_EXITO);
        btnConectarLector.setVisible(false);
        mostrarMensaje("Lector ZK9500 listo. Esperando huella...", COLOR_EXITO);
    }

    @Override
    public void onDispositivoError(String mensaje) {
        lectorConectado = false;
        animTimer.stop();
        lblEstadoHuella.setText("❌ " + mensaje);
        lblEstadoHuella.setForeground(COLOR_ERROR);
        btnConectarLector.setVisible(true);
        mostrarMensaje("Sin lector. Use login manual.", COLOR_TEXTO_SEC);
        // Cambiar automáticamente al modo manual
        cardLayout.show(panelCentral, "MANUAL");
    }

    @Override
    public void onProgresoEnroll(int capturaActual, int totalCapturas) {
        // No usado en login, solo en registro
    }

    @Override
    public void onEnrollCompleto(String templateBase64) {
        // No usado en login
    }

    @Override
    public void onIdentificacionResultado(String cedula, int score) {
        if (cedula != null) {
            miproyectoequipo.utils.AudioPlayer.reproducirMP3("src/miproyectoequipo/audios/verificado.mp3");
            lblEstadoHuella.setText("✅ Huella reconocida — Bienvenido");
            lblEstadoHuella.setForeground(COLOR_EXITO);

            // Buscar usuario por cédula
            Usuario usuario = usuarioDAO.buscarPorCedula(cedula);
            if (usuario != null) {
                mostrarMensaje("Bienvenido, " + usuario.getNombre() + " (Score: " + score + ")", COLOR_EXITO);
                // Dar un momento para que el usuario vea el mensaje
                Timer t = new Timer(1000, e -> abrirPanelPrincipal(usuario));
                t.setRepeats(false);
                t.start();
            } else {
                lblEstadoHuella.setText("⚠ Huella reconocida pero sin usuario asociado.");
                lblEstadoHuella.setForeground(COLOR_ERROR);
            }
        } else {
            lblEstadoHuella.setText("❌ Huella no reconocida");
            lblEstadoHuella.setForeground(COLOR_ERROR);

            // Restaurar mensaje después de 2 segundos
            Timer t = new Timer(2000, e -> {
                if (lectorConectado) {
                    lblEstadoHuella.setText("Coloque su dedo en el lector");
                    lblEstadoHuella.setForeground(COLOR_TEXTO);
                }
            });
            t.setRepeats(false);
            t.start();
        }
    }

    // =============================================
    // Utilidades de UI
    // =============================================

    private JButton crearBoton(String texto, Color colorFondo) {
        JButton btn = new JButton(texto) {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2d = (Graphics2D) g.create();
                g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

                if (getModel().isPressed()) {
                    g2d.setColor(colorFondo.darker());
                } else if (getModel().isRollover()) {
                    g2d.setColor(colorFondo.brighter());
                } else {
                    g2d.setColor(colorFondo);
                }

                g2d.fill(new RoundRectangle2D.Float(0, 0, getWidth(), getHeight(), 12, 12));
                g2d.setColor(COLOR_TEXTO);
                g2d.setFont(getFont());
                FontMetrics fm = g2d.getFontMetrics();
                int x = (getWidth() - fm.stringWidth(getText())) / 2;
                int y = (getHeight() + fm.getAscent() - fm.getDescent()) / 2;
                g2d.drawString(getText(), x, y);
                g2d.dispose();
            }
        };
        btn.setFont(new Font("Segoe UI", Font.BOLD, 14));
        btn.setPreferredSize(new Dimension(280, 42));
        btn.setMaximumSize(new Dimension(280, 42));
        btn.setFocusPainted(false);
        btn.setBorderPainted(false);
        btn.setContentAreaFilled(false);
        btn.setCursor(new Cursor(Cursor.HAND_CURSOR));
        return btn;
    }

    private JButton crearBotonLink(String texto) {
        JButton btn = new JButton(texto);
        btn.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        btn.setForeground(COLOR_ACENTO);
        btn.setBorderPainted(false);
        btn.setContentAreaFilled(false);
        btn.setFocusPainted(false);
        btn.setCursor(new Cursor(Cursor.HAND_CURSOR));
        btn.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseEntered(MouseEvent e) {
                btn.setForeground(COLOR_ACENTO_HOVER);
            }
            @Override
            public void mouseExited(MouseEvent e) {
                btn.setForeground(COLOR_ACENTO);
            }
        });
        return btn;
    }

    private JTextField crearCampoTexto() {
        JTextField field = new JTextField();
        estilizarCampo(field);
        return field;
    }

    private void estilizarCampo(JTextField field) {
        field.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        field.setForeground(COLOR_TEXTO);
        field.setBackground(COLOR_INPUT_BG);
        field.setCaretColor(COLOR_TEXTO);
        field.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(COLOR_INPUT_BORDER, 1, true),
            BorderFactory.createEmptyBorder(8, 12, 8, 12)
        ));
        field.setMaximumSize(new Dimension(Integer.MAX_VALUE, 40));
        field.setAlignmentX(Component.LEFT_ALIGNMENT);
    }

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JLabel lblTitulo;
    // End of variables declaration//GEN-END:variables
}
