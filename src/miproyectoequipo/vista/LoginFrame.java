
package miproyectoequipo.vista;

import java.awt.*;
import java.awt.event.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.util.Map;
import javax.imageio.ImageIO;
import javax.swing.*;
import miproyectoequipo.dao.ConexionDB;
import miproyectoequipo.dao.HuellaDAO;
import miproyectoequipo.dao.UsuarioDAO;
import miproyectoequipo.huella.HuellaListener;
import miproyectoequipo.huella.ZKFingerprintManager;
import miproyectoequipo.modelo.Usuario;

public class LoginFrame extends JFrame implements HuellaListener {

    private static final Color COLOR_UTA = new Color(122, 0, 30);
    private static final Color COLOR_UTA_OSCURO = new Color(90, 0, 22);
    private static final Color COLOR_BORDE = new Color(190, 190, 190);
    private static final Color COLOR_EXITO = new Color(0, 128, 0);
    private static final Color COLOR_ERROR = new Color(178, 0, 0);
    private static final Color COLOR_TEXTO = Color.BLACK;
    private static final Color COLOR_TEXTO_SEC = new Color(90, 90, 90);

    private JLabel lblEstadoHuella;
    private JLabel lblImagenHuella;
    private JLabel lblMensaje;
    private JTextField txtCedula;
    private JPasswordField txtContrasena;
    private JButton btnConectarLector;
    private JButton btnLoginManual;
    private JPanel panelHuella;
    private JPanel panelManual;
    private JPanel panelRostro;
    private CardLayout cardLayout;
    private JPanel panelCentral;
    private JLabel lblVideoRostro;

    private ZKFingerprintManager fingerprintManager;
    private miproyectoequipo.rostro.RostroManager rostroManager;
    private org.bytedeco.javacv.OpenCVFrameGrabber grabberRostro;
    private boolean camaraRostroCorriendo = false;
    private int modoActual = 0;

    private UsuarioDAO usuarioDAO;
    private miproyectoequipo.dao.EmpleadoDAO empleadoDAO;
    private HuellaDAO huellaDAO;
    private boolean lectorConectado = false;

    public LoginFrame() {
        usuarioDAO = new UsuarioDAO();
        empleadoDAO = new miproyectoequipo.dao.EmpleadoDAO();
        huellaDAO = new HuellaDAO();
        rostroManager = new miproyectoequipo.rostro.RostroManager();
        fingerprintManager = new ZKFingerprintManager();
        fingerprintManager.setListener(this);

        initComponents();
        initCustomComponents();
        intentarConectarLector();
    }

    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        lblTitulo = new javax.swing.JLabel();

        setDefaultCloseOperation(javax.swing.WindowConstants.EXIT_ON_CLOSE);
        setTitle("Sistema de Asistencia - Iniciar Sesion");

        lblTitulo.setFont(new java.awt.Font("Segoe UI", 1, 24));
        lblTitulo.setText("Sistema de Asistencia");

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addGap(160, 160, 160)
                .addComponent(lblTitulo)
                .addContainerGap(172, Short.MAX_VALUE))
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

        getContentPane().removeAll();
        setTitle("Universidad Técnica de Ambato - Iniciar Sesión");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(520, 640);
        setLocationRelativeTo(null);
        setResizable(false);
        setLayout(new BorderLayout());

        JPanel panelTitulo = new JPanel();
        panelTitulo.setBackground(COLOR_UTA);
        panelTitulo.setLayout(new BoxLayout(panelTitulo, BoxLayout.Y_AXIS));
        panelTitulo.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createMatteBorder(0, 0, 3, 0, COLOR_UTA_OSCURO),
            BorderFactory.createEmptyBorder(18, 0, 18, 0)));

        JLabel lblTit = new JLabel("Sistema de Asistencia", SwingConstants.CENTER);
        lblTit.setFont(new Font("Segoe UI", Font.BOLD, 24));
        lblTit.setForeground(Color.WHITE);
        lblTit.setAlignmentX(Component.CENTER_ALIGNMENT);

        JLabel lblSubtitulo = new JLabel("Universidad Técnica de Ambato", SwingConstants.CENTER);
        lblSubtitulo.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        lblSubtitulo.setForeground(new Color(255, 220, 220));
        lblSubtitulo.setAlignmentX(Component.CENTER_ALIGNMENT);

        panelTitulo.add(lblTit);
        panelTitulo.add(Box.createRigidArea(new Dimension(0, 4)));
        panelTitulo.add(lblSubtitulo);
        add(panelTitulo, BorderLayout.NORTH);

        cardLayout = new CardLayout();
        panelCentral = new JPanel(cardLayout);

        panelHuella = crearPanelHuella();
        panelManual = crearPanelManual();
        panelRostro = crearPanelRostro();

        panelCentral.add(panelHuella, "HUELLA");
        panelCentral.add(panelManual, "MANUAL");
        panelCentral.add(panelRostro, "ROSTRO");

        add(panelCentral, BorderLayout.CENTER);

        JPanel panelInferior = new JPanel();
        panelInferior.setLayout(new BoxLayout(panelInferior, BoxLayout.Y_AXIS));
        panelInferior.setBorder(BorderFactory.createEmptyBorder(8, 40, 18, 40));

        JPanel panelBotonesModo = new JPanel(new FlowLayout(FlowLayout.CENTER, 10, 0));

        JButton btnHuella = new JButton("Huella");
        btnHuella.addActionListener(e -> cambiarModo(0));
        JButton btnManual = new JButton("Manual");
        btnManual.addActionListener(e -> cambiarModo(1));
        JButton btnRostro = new JButton("Rostro");
        btnRostro.addActionListener(e -> cambiarModo(2));

        panelBotonesModo.add(btnHuella);
        panelBotonesModo.add(btnManual);
        panelBotonesModo.add(btnRostro);

        lblMensaje = new JLabel(" ", SwingConstants.CENTER);
        lblMensaje.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        lblMensaje.setForeground(COLOR_TEXTO_SEC);
        lblMensaje.setAlignmentX(Component.CENTER_ALIGNMENT);

        panelInferior.add(panelBotonesModo);
        panelInferior.add(Box.createRigidArea(new Dimension(0, 8)));
        panelInferior.add(lblMensaje);

        add(panelInferior, BorderLayout.SOUTH);

        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                if (fingerprintManager.isActivo()) {
                    fingerprintManager.cerrar();
                }
                detenerCamaraLogin();
                ConexionDB.getInstancia().cerrarConexion();
            }
        });
    }

    private JPanel crearPanelHuella() {
        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createEmptyBorder(20, 60, 20, 60),
            BorderFactory.createTitledBorder(
                BorderFactory.createLineBorder(COLOR_UTA), "Ingreso por Huella Dactilar")));

        lblImagenHuella = new JLabel("(Sensor de huella)", SwingConstants.CENTER);
        lblImagenHuella.setPreferredSize(new Dimension(200, 200));
        lblImagenHuella.setMaximumSize(new Dimension(200, 200));
        lblImagenHuella.setHorizontalAlignment(SwingConstants.CENTER);
        lblImagenHuella.setForeground(COLOR_TEXTO_SEC);
        lblImagenHuella.setBorder(BorderFactory.createLineBorder(COLOR_BORDE));
        lblImagenHuella.setAlignmentX(Component.CENTER_ALIGNMENT);

        lblEstadoHuella = new JLabel("Conectando al lector...", SwingConstants.CENTER);
        lblEstadoHuella.setFont(new Font("Segoe UI", Font.BOLD, 14));
        lblEstadoHuella.setForeground(COLOR_TEXTO);
        lblEstadoHuella.setAlignmentX(Component.CENTER_ALIGNMENT);

        btnConectarLector = new JButton("Reconectar Lector");
        btnConectarLector.setAlignmentX(Component.CENTER_ALIGNMENT);
        btnConectarLector.addActionListener(e -> intentarConectarLector());
        btnConectarLector.setVisible(false);

        panel.add(Box.createRigidArea(new Dimension(0, 20)));
        panel.add(lblImagenHuella);
        panel.add(Box.createRigidArea(new Dimension(0, 20)));
        panel.add(lblEstadoHuella);
        panel.add(Box.createRigidArea(new Dimension(0, 15)));
        panel.add(btnConectarLector);
        panel.add(Box.createVerticalGlue());

        return panel;
    }

    private JPanel crearPanelManual() {
        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createEmptyBorder(20, 70, 20, 70),
            BorderFactory.createTitledBorder(
                BorderFactory.createLineBorder(COLOR_UTA), "Inicio de Sesión Manual")));

        JLabel lblCedula = new JLabel("Cédula, correo o usuario");
        lblCedula.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        lblCedula.setForeground(COLOR_TEXTO_SEC);
        lblCedula.setAlignmentX(Component.LEFT_ALIGNMENT);

        txtCedula = crearCampoTexto();

        JLabel lblPass = new JLabel("Contraseña");
        lblPass.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        lblPass.setForeground(COLOR_TEXTO_SEC);
        lblPass.setAlignmentX(Component.LEFT_ALIGNMENT);

        txtContrasena = new JPasswordField();
        estilizarCampo(txtContrasena);

        btnLoginManual = new JButton("Ingresar");
        btnLoginManual.setAlignmentX(Component.LEFT_ALIGNMENT);
        btnLoginManual.setMaximumSize(new Dimension(Integer.MAX_VALUE, 36));
        btnLoginManual.addActionListener(e -> loginManual());

        JButton btnRegistrar = new JButton("Registrar nuevo usuario");
        btnRegistrar.setAlignmentX(Component.LEFT_ALIGNMENT);
        btnRegistrar.setMaximumSize(new Dimension(Integer.MAX_VALUE, 32));
        btnRegistrar.addActionListener(e -> mostrarDialogoRegistro());

        txtContrasena.addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                if (e.getKeyCode() == KeyEvent.VK_ENTER) {
                    loginManual();
                }
            }
        });

        panel.add(Box.createRigidArea(new Dimension(0, 15)));
        panel.add(lblCedula);
        panel.add(Box.createRigidArea(new Dimension(0, 5)));
        panel.add(txtCedula);
        panel.add(Box.createRigidArea(new Dimension(0, 15)));
        panel.add(lblPass);
        panel.add(Box.createRigidArea(new Dimension(0, 5)));
        panel.add(txtContrasena);
        panel.add(Box.createRigidArea(new Dimension(0, 20)));
        panel.add(btnLoginManual);
        panel.add(Box.createRigidArea(new Dimension(0, 8)));
        panel.add(new JSeparator());
        panel.add(Box.createRigidArea(new Dimension(0, 8)));
        panel.add(btnRegistrar);
        panel.add(Box.createVerticalGlue());

        return panel;
    }

    private JPanel crearPanelRostro() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createEmptyBorder(20, 50, 20, 50),
            BorderFactory.createTitledBorder(
                BorderFactory.createLineBorder(COLOR_UTA), "Ingreso por Reconocimiento Facial")));

        lblVideoRostro = new JLabel("Esperando cámara...", SwingConstants.CENTER);
        lblVideoRostro.setForeground(COLOR_TEXTO_SEC);
        lblVideoRostro.setBorder(BorderFactory.createLineBorder(COLOR_BORDE));
        panel.add(lblVideoRostro, BorderLayout.CENTER);

        return panel;
    }

    private void iniciarCamaraLogin() {
        if (camaraRostroCorriendo) return;
        camaraRostroCorriendo = true;
        lblVideoRostro.setText("Iniciando cámara...");

        new Thread(() -> {
            try {
                grabberRostro = new org.bytedeco.javacv.OpenCVFrameGrabber(0);
                grabberRostro.start();

                org.bytedeco.javacv.OpenCVFrameConverter.ToMat converterMat = new org.bytedeco.javacv.OpenCVFrameConverter.ToMat();
                org.bytedeco.javacv.Java2DFrameConverter converterBimg = new org.bytedeco.javacv.Java2DFrameConverter();

                int confirmaciones = 0;
                int idReconocidoAnterior = -1;

                while (camaraRostroCorriendo && grabberRostro != null) {
                    org.bytedeco.javacv.Frame frame = grabberRostro.grab();
                    if (frame == null) continue;

                    org.bytedeco.opencv.opencv_core.Mat matImage = converterMat.convert(frame);
                    if (matImage == null) continue;

                    org.bytedeco.opencv.opencv_core.RectVector faces = rostroManager.detectarRostros(matImage);

                    if (faces.size() > 0) {
                        org.bytedeco.opencv.opencv_core.Rect rect = faces.get(0);
                        org.bytedeco.opencv.opencv_core.Mat faceMat = new org.bytedeco.opencv.opencv_core.Mat(matImage, rect);

                        int idReconocido = rostroManager.reconocerRostro(faceMat);

                        if (idReconocido != -1) {
                            if (idReconocido == idReconocidoAnterior) {
                                confirmaciones++;
                            } else {
                                confirmaciones = 1;
                                idReconocidoAnterior = idReconocido;
                            }

                            org.bytedeco.opencv.global.opencv_imgproc.rectangle(matImage, rect, new org.bytedeco.opencv.opencv_core.Scalar(0, 255, 0, 0), 2, 8, 0);

                            if (confirmaciones >= 5) {
                                camaraRostroCorriendo = false;
                                miproyectoequipo.modelo.Empleado emp = empleadoDAO.buscarPorId(idReconocido);
                                if (emp != null) {
                                    Usuario usuario = usuarioDAO.buscarPorCedula(emp.getCedula());
                                    if (usuario != null) {
                                        SwingUtilities.invokeLater(() -> {
                                            miproyectoequipo.utils.AudioPlayer.reproducirMP3("src/miproyectoequipo/audios/verificado.mp3");
                                            mostrarMensaje("¡Rostro reconocido! Bienvenido " + usuario.getNombre(), COLOR_EXITO);
                                            abrirPanelPrincipal(usuario);
                                        });
                                        break;
                                    }
                                }
                            }
                        } else {
                            confirmaciones = 0;
                            idReconocidoAnterior = -1;
                            org.bytedeco.opencv.global.opencv_imgproc.rectangle(matImage, rect, new org.bytedeco.opencv.opencv_core.Scalar(0, 0, 255, 0), 2, 8, 0);
                        }
                    }

                    BufferedImage bimg = converterBimg.convert(converterMat.convert(matImage));
                    if (bimg != null && camaraRostroCorriendo) {
                        SwingUtilities.invokeLater(() -> lblVideoRostro.setIcon(new ImageIcon(bimg.getScaledInstance(250, 250, Image.SCALE_SMOOTH))));
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            } finally {
                detenerCamaraLogin();
            }
        }).start();
    }

    private void detenerCamaraLogin() {
        camaraRostroCorriendo = false;
        if (grabberRostro != null) {
            try {
                grabberRostro.stop();
                grabberRostro.release();
            } catch (Exception e) {
                e.printStackTrace();
            }
            grabberRostro = null;
        }
    }

    private void intentarConectarLector() {
        lblEstadoHuella.setText("Conectando al lector...");
        lblEstadoHuella.setForeground(COLOR_TEXTO);
        btnConectarLector.setVisible(false);

        new Thread(() -> {
            boolean ok = fingerprintManager.iniciar();
            if (ok) {

                Map<String, String> huellas = huellaDAO.obtenerTodasHuellas();
                fingerprintManager.cargarHuellasEnCache(huellas);
            }
        }, "LectorInit").start();
    }

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

    private void mostrarDialogoRegistro() {
        JTextField txtCed = new JTextField();
        JTextField txtNom = new JTextField();
        JTextField txtApe = new JTextField();
        JTextField txtUsuario = new JTextField();
        JTextField txtEmail = new JTextField();
        JTextField txtCargo = new JTextField();
        JPasswordField txtPass = new JPasswordField();
        JComboBox<String> cmbTipo = new JComboBox<>(new String[]{"TIEMPO_COMPLETO", "TIEMPO_PARCIAL"});

        JPanel form = new JPanel(new GridLayout(0, 1, 4, 3));
        form.add(new JLabel("Cédula *"));
        form.add(txtCed);
        form.add(new JLabel("Nombre *"));
        form.add(txtNom);
        form.add(new JLabel("Apellido"));
        form.add(txtApe);
        form.add(new JLabel("Nombre de usuario (para ingresar)"));
        form.add(txtUsuario);
        form.add(new JLabel("Correo electrónico (para ingresar)"));
        form.add(txtEmail);
        form.add(new JLabel("Cargo"));
        form.add(txtCargo);
        form.add(new JLabel("Contraseña *"));
        form.add(txtPass);
        form.add(new JLabel("Tipo de Contrato"));
        form.add(cmbTipo);

        int op = JOptionPane.showConfirmDialog(this, form, "Registro de Usuario",
            JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);
        if (op != JOptionPane.OK_OPTION) return;

        String ced = txtCed.getText().trim();
        String nom = txtNom.getText().trim();
        String ape = txtApe.getText().trim();
        String nombreUsuario = txtUsuario.getText().trim();
        String email = txtEmail.getText().trim();
        String cargo = txtCargo.getText().trim();
        String pass = new String(txtPass.getPassword());

        if (ced.isEmpty() || nom.isEmpty() || pass.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Complete cédula, nombre y contraseña.", "Datos incompletos", JOptionPane.WARNING_MESSAGE);
            return;
        }
        if (usuarioDAO.buscarPorCedula(ced) != null) {
            JOptionPane.showMessageDialog(this, "Ya existe un usuario con esa cédula.", "Cédula duplicada", JOptionPane.ERROR_MESSAGE);
            return;
        }

        Usuario.Perfil perfil = Usuario.Perfil.EMPLEADO;
        String nombreCompleto = (nom + " " + ape).trim();
        Usuario nuevo = new Usuario(ced, nombreCompleto,
            nombreUsuario.isEmpty() ? null : nombreUsuario,
            email.isEmpty() ? null : email,
            pass, perfil);

        if (!usuarioDAO.insertar(nuevo)) {
            JOptionPane.showMessageDialog(this, "No se pudo registrar el usuario.", "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }

        miproyectoequipo.modelo.Empleado.TipoContrato tipo =
            miproyectoequipo.modelo.Empleado.TipoContrato.valueOf((String) cmbTipo.getSelectedItem());
        miproyectoequipo.modelo.Empleado emp;
        if (tipo == miproyectoequipo.modelo.Empleado.TipoContrato.TIEMPO_COMPLETO) {
            emp = new miproyectoequipo.modelo.EmpleadoTiempoCompleto(ced, nom, ape, cargo);
        } else {
            emp = new miproyectoequipo.modelo.EmpleadoTiempoParcial(ced, nom, ape, cargo);
        }
        empleadoDAO.insertar(emp);

        JOptionPane.showMessageDialog(this,
            "Empleado registrado correctamente.\n\nCédula: " + ced
            + "\n\nYa puede iniciar sesión.",
            "Registro exitoso", JOptionPane.INFORMATION_MESSAGE);

        txtCedula.setText(ced);
        txtContrasena.requestFocusInWindow();
    }

    private void abrirPanelPrincipal(Usuario usuario) {
        if (fingerprintManager.isActivo()) {
            fingerprintManager.cerrar();
        }
        detenerCamaraLogin();

        PanelPrincipalFrame panel = new PanelPrincipalFrame(usuario);
        panel.setVisible(true);
        this.dispose();
    }

    private void cambiarModo(int modoSeleccionado) {
        modoActual = modoSeleccionado;

        if (modoActual != 2) {
            detenerCamaraLogin();
        }

        if (modoActual == 0) {
            cardLayout.show(panelCentral, "HUELLA");
        } else if (modoActual == 1) {
            cardLayout.show(panelCentral, "MANUAL");
        } else {
            cardLayout.show(panelCentral, "ROSTRO");
            iniciarCamaraLogin();
        }
    }

    private void mostrarMensaje(String msg, Color color) {
        lblMensaje.setText(msg);
        lblMensaje.setForeground(color);

        Timer t = new Timer(5000, e -> lblMensaje.setText(" "));
        t.setRepeats(false);
        t.start();
    }

    @Override
    public void onCapturaExitosa(byte[] imagen, byte[] template, String base64) {

        try {
            fingerprintManager.guardarImagenBMP(imagen, "temp_fingerprint.bmp");
            BufferedImage img = ImageIO.read(new File("temp_fingerprint.bmp"));
            if (img != null) {
                Image scaled = img.getScaledInstance(180, 200, Image.SCALE_SMOOTH);
                lblImagenHuella.setText(null);
                lblImagenHuella.setIcon(new ImageIcon(scaled));
            }
        } catch (Exception e) {

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
        lblEstadoHuella.setText("Lector conectado - Coloque su dedo");
        lblEstadoHuella.setForeground(COLOR_EXITO);
        btnConectarLector.setVisible(false);
        mostrarMensaje("Lector ZK9500 listo. Esperando huella...", COLOR_EXITO);
    }

    @Override
    public void onDispositivoError(String mensaje) {
        lectorConectado = false;
        lblEstadoHuella.setText(mensaje);
        lblEstadoHuella.setForeground(COLOR_ERROR);
        btnConectarLector.setVisible(true);
        mostrarMensaje("Sin lector. Use el inicio de sesión manual.", COLOR_TEXTO_SEC);

        cardLayout.show(panelCentral, "MANUAL");
    }

    @Override
    public void onProgresoEnroll(int capturaActual, int totalCapturas) {

    }

    @Override
    public void onEnrollCompleto(String templateBase64) {

    }

    @Override
    public void onIdentificacionResultado(String cedula, int score) {
        if (cedula != null) {
            miproyectoequipo.utils.AudioPlayer.reproducirMP3("src/miproyectoequipo/audios/verificado.mp3");
            lblEstadoHuella.setText("Huella reconocida - Bienvenido");
            lblEstadoHuella.setForeground(COLOR_EXITO);

            Usuario usuario = usuarioDAO.buscarPorCedula(cedula);
            if (usuario != null) {
                mostrarMensaje("Bienvenido, " + usuario.getNombre() + " (Score: " + score + ")", COLOR_EXITO);

                Timer t = new Timer(1000, e -> abrirPanelPrincipal(usuario));
                t.setRepeats(false);
                t.start();
            } else {
                lblEstadoHuella.setText("Huella reconocida pero sin usuario asociado.");
                lblEstadoHuella.setForeground(COLOR_ERROR);
            }
        } else {
            lblEstadoHuella.setText("Huella no reconocida");
            lblEstadoHuella.setForeground(COLOR_ERROR);

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

    private JTextField crearCampoTexto() {
        JTextField field = new JTextField();
        estilizarCampo(field);
        return field;
    }

    private void estilizarCampo(JTextField field) {
        field.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        field.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(COLOR_BORDE, 1),
            BorderFactory.createEmptyBorder(6, 8, 6, 8)
        ));
        field.setMaximumSize(new Dimension(Integer.MAX_VALUE, 36));
        field.setAlignmentX(Component.LEFT_ALIGNMENT);
    }

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JLabel lblTitulo;
    // End of variables declaration//GEN-END:variables
}
