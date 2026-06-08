
package miproyectoequipo.vista;

import java.awt.*;
import java.awt.event.*;
import java.awt.print.PrinterException;
import java.text.MessageFormat;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import javax.swing.*;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.JTableHeader;
import miproyectoequipo.dao.*;
import miproyectoequipo.huella.ZKFingerprintManager;
import miproyectoequipo.modelo.*;

public class PanelPrincipalFrame extends JFrame {

    private static final Color COLOR_UTA = new Color(122, 0, 30);
    private static final Color COLOR_UTA_OSCURO = new Color(90, 0, 22);
    private static final Color COLOR_EXITO = new Color(0, 128, 0);
    private static final Color COLOR_ERROR = new Color(178, 0, 0);
    private static final Color COLOR_TEXTO = Color.BLACK;
    private static final Color COLOR_GRIS = new Color(90, 90, 90);

    private final Usuario usuarioActual;
    private final ZKFingerprintManager fingerprintManager;
    private JPanel panelContenido;
    private CardLayout cardLayout;
    private JLabel lblReloj;
    private Timer relojTimer;

    private JTable tablaEmpleados;
    private JTextField txtCrudCedula;
    private JTextField txtCrudNombre;
    private JTextField txtCrudApellido;
    private JTextField txtCrudCargo;
    private JComboBox<String> cmbCrudTipoContrato;
    private JTextField txtCrudBuscar;
    private JLabel lblCrudResultado;

    public PanelPrincipalFrame(Usuario usuario) {
        this.usuarioActual = usuario;
        this.fingerprintManager = new ZKFingerprintManager();
        initComponents();
        iniciarReloj();
    }

    private void initComponents() {
        setTitle("Sistema de Asistencia UTA - " + usuarioActual.getNombre());
        setSize(1024, 700);
        setLocationRelativeTo(null);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLayout(new BorderLayout());

        add(crearBanner(), BorderLayout.NORTH);

        add(crearMenuLateral(), BorderLayout.WEST);

        cardLayout = new CardLayout();
        panelContenido = new JPanel(cardLayout);

        panelContenido.add(crearPanelBienvenida(), "BIENVENIDA");
        if (usuarioActual.getPerfil() == Usuario.Perfil.ADMINISTRADOR) {
            panelContenido.add(crearPanelGestionEmpleados(), "GESTION_EMPLEADOS");
        }
        panelContenido.add(crearPanelReportes(), "REPORTES");

        add(panelContenido, BorderLayout.CENTER);

        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                if (fingerprintManager.isActivo()) {
                    fingerprintManager.cerrar();
                }
                if (relojTimer != null) relojTimer.stop();
                ConexionDB.getInstancia().cerrarConexion();
            }
        });
    }

    private JPanel crearBanner() {
        JPanel banner = new JPanel(new BorderLayout());
        banner.setBackground(COLOR_UTA);
        banner.setBorder(BorderFactory.createMatteBorder(0, 0, 3, 0, COLOR_UTA_OSCURO));

        JLabel lblTitulo = new JLabel("  Universidad Técnica de Ambato - Sistema de Asistencia");
        lblTitulo.setFont(new Font("Segoe UI", Font.BOLD, 20));
        lblTitulo.setForeground(Color.WHITE);
        lblTitulo.setBorder(BorderFactory.createEmptyBorder(12, 10, 12, 10));
        banner.add(lblTitulo, BorderLayout.WEST);

        JPanel derecha = new JPanel();
        derecha.setOpaque(false);
        derecha.setLayout(new BoxLayout(derecha, BoxLayout.Y_AXIS));
        derecha.setBorder(BorderFactory.createEmptyBorder(8, 10, 8, 15));

        String rol = usuarioActual.getPerfil() == Usuario.Perfil.ADMINISTRADOR ? "Administrador" : "Empleado";
        JLabel lblUser = new JLabel(usuarioActual.getNombre() + "  (" + rol + ")");
        lblUser.setFont(new Font("Segoe UI", Font.BOLD, 13));
        lblUser.setForeground(Color.WHITE);
        lblUser.setAlignmentX(Component.RIGHT_ALIGNMENT);

        lblReloj = new JLabel();
        lblReloj.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        lblReloj.setForeground(new Color(255, 220, 220));
        lblReloj.setAlignmentX(Component.RIGHT_ALIGNMENT);

        derecha.add(lblUser);
        derecha.add(Box.createRigidArea(new Dimension(0, 3)));
        derecha.add(lblReloj);
        banner.add(derecha, BorderLayout.EAST);

        return banner;
    }

    private JScrollPane crearMenuLateral() {
        JPanel menu = new JPanel();
        menu.setLayout(new BoxLayout(menu, BoxLayout.Y_AXIS));
        menu.setBackground(new Color(235, 235, 235));
        menu.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createMatteBorder(0, 0, 0, 1, Color.GRAY),
            BorderFactory.createEmptyBorder(12, 8, 12, 8)
        ));

        menu.add(crearEtiquetaSeccion("NAVEGACIÓN"));
        menu.add(crearBotonMenu("Inicio", e -> cardLayout.show(panelContenido, "BIENVENIDA")));

        if (usuarioActual.getPerfil() == Usuario.Perfil.ADMINISTRADOR) {
            menu.add(Box.createRigidArea(new Dimension(0, 10)));
            menu.add(crearEtiquetaSeccion("ADMINISTRACIÓN"));
            menu.add(crearBotonMenu("Registrar Huella", e -> abrirRegistroHuella()));
            menu.add(crearBotonMenu("Registrar Rostro", e -> abrirRegistroRostro()));
            menu.add(crearBotonMenu("Gestión de Empleados", e -> {
                actualizarTablaEmpleados();
                cardLayout.show(panelContenido, "GESTION_EMPLEADOS");
            }));
        }

        menu.add(Box.createRigidArea(new Dimension(0, 10)));
        menu.add(crearEtiquetaSeccion("OPERACIONES"));
        menu.add(crearBotonMenu("Marcar Asistencia", e -> registrarAsistencia()));
        menu.add(crearBotonMenu("Consultas y Reportes", e -> cardLayout.show(panelContenido, "REPORTES")));

        menu.add(Box.createVerticalGlue());
        menu.add(crearBotonMenu("Cerrar Sesión", e -> cerrarSesion()));

        JScrollPane sp = new JScrollPane(menu,
            JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED, JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        sp.setPreferredSize(new Dimension(220, 0));
        sp.setBorder(null);
        return sp;
    }

    private JPanel crearPanelBienvenida() {
        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.setBorder(BorderFactory.createEmptyBorder(25, 30, 25, 30));
        panel.setBackground(Color.WHITE);

        JLabel lblBienvenida = new JLabel("Bienvenido, " + usuarioActual.getNombre());
        lblBienvenida.setFont(new Font("Segoe UI", Font.BOLD, 26));
        lblBienvenida.setForeground(COLOR_UTA);
        lblBienvenida.setAlignmentX(Component.LEFT_ALIGNMENT);

        JLabel lblFecha = new JLabel(LocalDate.now().format(
            DateTimeFormatter.ofPattern("EEEE, dd 'de' MMMM 'de' yyyy")));
        lblFecha.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        lblFecha.setForeground(COLOR_GRIS);
        lblFecha.setAlignmentX(Component.LEFT_ALIGNMENT);

        panel.add(lblBienvenida);
        panel.add(Box.createRigidArea(new Dimension(0, 5)));
        panel.add(lblFecha);
        panel.add(Box.createRigidArea(new Dimension(0, 25)));

        if (usuarioActual.getPerfil() == Usuario.Perfil.ADMINISTRADOR) {
            int totalEmpleados = new EmpleadoDAO().listarTodos().size();
            int totalHuellas = new HuellaDAO().obtenerTodasHuellas().size();

            JPanel cards = new JPanel(new GridLayout(1, 2, 20, 0));
            cards.setOpaque(false);
            cards.setMaximumSize(new Dimension(Integer.MAX_VALUE, 110));
            cards.setAlignmentX(Component.LEFT_ALIGNMENT);
            cards.add(crearTarjetaResumen("Empleados Activos", String.valueOf(totalEmpleados)));
            cards.add(crearTarjetaResumen("Huellas Registradas", String.valueOf(totalHuellas)));

            panel.add(cards);
            panel.add(Box.createRigidArea(new Dimension(0, 25)));
        }

        JLabel lblInstrucciones = new JLabel("<html><body style='width: 600px'>"
            + "<h3 style='color:#7a001e; font-family:Segoe UI;'>Instrucciones de Uso del Sistema</h3>"
            + "<p style='font-size:12px; line-height:1.6;'>"
            + "Este sistema le permite registrar y consultar de forma exacta los horarios de entrada y "
            + "salida, así como visualizar reportes detallados con cálculo automático de sueldos.<br><br>"
            + "<b>Control de Asistencias Oficial:</b><br>"
            + "&bull; Entrada Mañana: 08:00 a 13:00<br>"
            + "&bull; Almuerzo (libre): 13:00 a 14:00<br>"
            + "&bull; Entrada Tarde: 14:00 a 17:00<br>"
            + "&bull; Total Horas Diarias: 8 horas.<br><br>"
            + "<b>Compensación y Descuentos:</b><br>"
            + "&bull; Tiempo Completo: sueldo mensual fijo de $1500. Se descuentan $0.20 por cada minuto "
            + "de atraso en los ingresos.<br>"
            + "&bull; Tiempo Parcial: $5.00 por hora laborada, sin superar 8 horas diarias."
            + "</p></body></html>");
        lblInstrucciones.setAlignmentX(Component.LEFT_ALIGNMENT);

        panel.add(lblInstrucciones);
        panel.add(Box.createVerticalGlue());

        return panel;
    }

    private JPanel crearPanelGestionEmpleados() {
        JPanel panel = new JPanel(new BorderLayout(0, 10));
        panel.setBorder(BorderFactory.createEmptyBorder(15, 20, 15, 20));
        panel.setBackground(Color.WHITE);

        panel.add(crearTituloSeccion("Gestión Integral de Empleados"), BorderLayout.NORTH);

        JPanel panelIzquierda = new JPanel(new BorderLayout(0, 8));
        panelIzquierda.setOpaque(false);

        JPanel panelBuscador = new JPanel(new BorderLayout(8, 0));
        panelBuscador.setOpaque(false);
        JLabel lblBuscar = new JLabel("Buscar:");
        lblBuscar.setFont(new Font("Segoe UI", Font.BOLD, 13));
        txtCrudBuscar = new JTextField();
        txtCrudBuscar.addKeyListener(new KeyAdapter() {
            @Override
            public void keyReleased(KeyEvent e) {
                filtrarTablaEmpleados(txtCrudBuscar.getText().trim());
            }
        });
        panelBuscador.add(lblBuscar, BorderLayout.WEST);
        panelBuscador.add(txtCrudBuscar, BorderLayout.CENTER);
        panelIzquierda.add(panelBuscador, BorderLayout.NORTH);

        String[] columnas = {"Cédula", "Nombre", "Apellido", "Cargo", "Tipo Contrato"};
        DefaultTableModel model = new DefaultTableModel(columnas, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
        tablaEmpleados = new JTable(model);
        estilizarTabla(tablaEmpleados);
        tablaEmpleados.getSelectionModel().addListSelectionListener(e -> {
            int fila = tablaEmpleados.getSelectedRow();
            if (fila >= 0) {
                txtCrudCedula.setText((String) tablaEmpleados.getValueAt(fila, 0));
                txtCrudCedula.setEditable(false);
                txtCrudNombre.setText((String) tablaEmpleados.getValueAt(fila, 1));
                txtCrudApellido.setText((String) tablaEmpleados.getValueAt(fila, 2));
                txtCrudCargo.setText((String) tablaEmpleados.getValueAt(fila, 3));
                cmbCrudTipoContrato.setSelectedItem((String) tablaEmpleados.getValueAt(fila, 4));
            }
        });
        panelIzquierda.add(new JScrollPane(tablaEmpleados), BorderLayout.CENTER);

        JPanel panelDerecha = new JPanel();
        panelDerecha.setLayout(new BoxLayout(panelDerecha, BoxLayout.Y_AXIS));
        panelDerecha.setPreferredSize(new Dimension(300, 0));
        panelDerecha.setBorder(BorderFactory.createTitledBorder(
            BorderFactory.createLineBorder(COLOR_UTA), "Formulario de Datos"));
        panelDerecha.setBackground(Color.WHITE);

        txtCrudCedula = new JTextField();
        txtCrudNombre = new JTextField();
        txtCrudApellido = new JTextField();
        txtCrudCargo = new JTextField();
        cmbCrudTipoContrato = new JComboBox<>(new String[]{"TIEMPO_COMPLETO", "TIEMPO_PARCIAL"});
        limitarAltura(txtCrudCedula, txtCrudNombre, txtCrudApellido, txtCrudCargo);
        limitarAltura(cmbCrudTipoContrato);

        lblCrudResultado = new JLabel(" ");
        lblCrudResultado.setFont(new Font("Segoe UI", Font.BOLD, 11));
        lblCrudResultado.setAlignmentX(Component.LEFT_ALIGNMENT);

        JButton btnGuardar = new JButton("Registrar Nuevo");
        btnGuardar.addActionListener(e -> crudRegistrar());
        JButton btnModificar = new JButton("Modificar Seleccionado");
        btnModificar.addActionListener(e -> crudModificar());
        JButton btnEliminar = new JButton("Eliminar / Dar de Baja");
        btnEliminar.addActionListener(e -> crudEliminar());
        JButton btnLimpiar = new JButton("Limpiar Campos");
        btnLimpiar.addActionListener(e -> limpiarCamposCrud());
        for (JButton b : new JButton[]{btnGuardar, btnModificar, btnEliminar, btnLimpiar}) {
            b.setAlignmentX(Component.LEFT_ALIGNMENT);
            b.setMaximumSize(new Dimension(Integer.MAX_VALUE, 32));
        }

        panelDerecha.add(crearLabel("Cédula *"));
        panelDerecha.add(txtCrudCedula);
        panelDerecha.add(Box.createRigidArea(new Dimension(0, 6)));
        panelDerecha.add(crearLabel("Nombre *"));
        panelDerecha.add(txtCrudNombre);
        panelDerecha.add(Box.createRigidArea(new Dimension(0, 6)));
        panelDerecha.add(crearLabel("Apellido *"));
        panelDerecha.add(txtCrudApellido);
        panelDerecha.add(Box.createRigidArea(new Dimension(0, 6)));
        panelDerecha.add(crearLabel("Cargo"));
        panelDerecha.add(txtCrudCargo);
        panelDerecha.add(Box.createRigidArea(new Dimension(0, 6)));
        panelDerecha.add(crearLabel("Tipo Contrato"));
        panelDerecha.add(cmbCrudTipoContrato);
        panelDerecha.add(Box.createRigidArea(new Dimension(0, 12)));
        panelDerecha.add(btnGuardar);
        panelDerecha.add(Box.createRigidArea(new Dimension(0, 5)));
        panelDerecha.add(btnModificar);
        panelDerecha.add(Box.createRigidArea(new Dimension(0, 5)));
        panelDerecha.add(btnEliminar);
        panelDerecha.add(Box.createRigidArea(new Dimension(0, 5)));
        panelDerecha.add(btnLimpiar);
        panelDerecha.add(Box.createRigidArea(new Dimension(0, 10)));
        panelDerecha.add(lblCrudResultado);
        panelDerecha.add(Box.createVerticalGlue());

        JSplitPane split = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, panelIzquierda, panelDerecha);
        split.setDividerLocation(560);
        split.setResizeWeight(1.0);
        panel.add(split, BorderLayout.CENTER);

        actualizarTablaEmpleados();
        return panel;
    }

    private JPanel crearPanelReportes() {
        JPanel panel = new JPanel(new BorderLayout(0, 10));
        panel.setBorder(BorderFactory.createEmptyBorder(15, 20, 15, 20));
        panel.setBackground(Color.WHITE);

        panel.add(crearTituloSeccion("Módulo de Consultas e Informes Financieros"), BorderLayout.NORTH);

        JTabbedPane tabs = new JTabbedPane();
        tabs.setFont(new Font("Segoe UI", Font.BOLD, 13));
        tabs.addTab("Reporte Mensual", crearSubPanelReporteMensual());
        tabs.addTab("Tiempo Completo", crearSubPanelReporteTC());
        tabs.addTab("Tiempo Parcial", crearSubPanelReporteTP());
        tabs.addTab("Asistencias por Rango", crearSubPanelAsistenciasFechas());

        panel.add(tabs, BorderLayout.CENTER);
        return panel;
    }

    private JPanel crearSubPanelReporteMensual() {
        JPanel panel = new JPanel(new BorderLayout(0, 12));
        panel.setBorder(BorderFactory.createEmptyBorder(12, 12, 12, 12));
        panel.setBackground(Color.WHITE);

        JPanel panelSuperior = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 5));
        panelSuperior.setOpaque(false);

        JComboBox<String> cmbCedula = new JComboBox<>();
        cmbCedula.setPreferredSize(new Dimension(240, 28));
        prepararComboCedulasTodos(cmbCedula);

        JComboBox<String> cmbMes = new JComboBox<>(MESES);
        JComboBox<String> cmbAnio = new JComboBox<>(ANIOS);
        JButton btnGenerar = new JButton("Generar Reporte");
        JButton btnImprimir = new JButton("Imprimir");
        btnImprimir.setEnabled(false);

        panelSuperior.add(new JLabel("Empleado:"));
        panelSuperior.add(cmbCedula);
        panelSuperior.add(new JLabel("Mes:"));
        panelSuperior.add(cmbMes);
        panelSuperior.add(new JLabel("Año:"));
        panelSuperior.add(cmbAnio);
        panelSuperior.add(btnGenerar);
        panelSuperior.add(btnImprimir);
        panel.add(panelSuperior, BorderLayout.NORTH);

        String[] columnas = {"Empleado", "Cédula", "Fecha", "Ent. Mañana", "Sal. Mañana", "Ent. Tarde", "Sal. Tarde", "Atraso (min)", "Horas"};
        DefaultTableModel model = new DefaultTableModel(columnas, 0);
        JTable tabla = new JTable(model);
        estilizarTabla(tabla);
        panel.add(new JScrollPane(tabla), BorderLayout.CENTER);

        JPanel panelMetricas = new JPanel(new GridLayout(1, 3, 12, 0));
        panelMetricas.setOpaque(false);
        panelMetricas.setPreferredSize(new Dimension(0, 70));
        JLabel valDias = new JLabel("—", SwingConstants.CENTER);
        JLabel valAtraso = new JLabel("—", SwingConstants.CENTER);
        JLabel valHoras = new JLabel("—", SwingConstants.CENTER);
        panelMetricas.add(crearTarjetaMetrica("Días con Registro", valDias, COLOR_UTA));
        panelMetricas.add(crearTarjetaMetrica("Total Atrasos", valAtraso, new Color(180, 120, 0)));
        panelMetricas.add(crearTarjetaMetrica("Total Horas", valHoras, COLOR_EXITO));
        panel.add(panelMetricas, BorderLayout.SOUTH);

        cmbCedula.addActionListener(e -> {
            model.setRowCount(0);
            valDias.setText("—");
            valAtraso.setText("—");
            valHoras.setText("—");
            btnImprimir.setEnabled(false);
        });

        btnGenerar.addActionListener(e -> {
            String sel = (String) cmbCedula.getSelectedItem();
            boolean todos = "TODOS LOS EMPLEADOS".equals(sel);
            String cedula = todos ? null : extraerCedula(sel);
            if (!todos && cedula == null) {
                JOptionPane.showMessageDialog(this, "Debe seleccionar un empleado.", "Aviso", JOptionPane.WARNING_MESSAGE);
                return;
            }

            int mesNum = cmbMes.getSelectedIndex() + 1;
            int anioNum = Integer.parseInt((String) cmbAnio.getSelectedItem());
            LocalDate inicioMes = LocalDate.of(anioNum, mesNum, 1);
            LocalDate finMes = inicioMes.withDayOfMonth(inicioMes.lengthOfMonth());

            AsistenciaDAO asistDAO = new AsistenciaDAO();
            EmpleadoDAO empDAO = new EmpleadoDAO();
            List<RegistroAsistencia> registros = todos
                ? asistDAO.listarTodosPorRango(inicioMes, finMes)
                : asistDAO.listarPorCedulaYRango(cedula, inicioMes, finMes);

            model.setRowCount(0);
            int totalAtraso = 0;
            double totalHoras = 0;
            for (RegistroAsistencia r : registros) {
                Empleado emp = empDAO.buscarPorCedula(r.getCedulaEmpleado());
                totalAtraso += r.getMinutosAtraso();
                totalHoras += r.calcularHorasTrabajadas();
                model.addRow(new Object[]{
                    emp != null ? emp.getNombreCompleto() : "Desconocido",
                    r.getCedulaEmpleado(),
                    r.getFecha().toString(),
                    fmt(r.getHoraEntradaManana()), fmt(r.getHoraSalidaManana()),
                    fmt(r.getHoraEntradaTarde()), fmt(r.getHoraSalidaTarde()),
                    r.getMinutosAtraso() + " min",
                    String.format("%.2f hrs", r.calcularHorasTrabajadas())
                });
            }

            valDias.setText(String.valueOf(registros.size()));
            valAtraso.setText(totalAtraso + " min");
            valHoras.setText(String.format("%.2f hrs", totalHoras));
            btnImprimir.setEnabled(model.getRowCount() > 0);
        });

        btnImprimir.addActionListener(e -> imprimirTabla(tabla,
            "Reporte Mensual - " + cmbCedula.getSelectedItem()));

        return panel;
    }

    private JPanel crearSubPanelReporteTC() {
        JPanel panel = new JPanel(new BorderLayout(0, 12));
        panel.setBorder(BorderFactory.createEmptyBorder(12, 12, 12, 12));
        panel.setBackground(Color.WHITE);

        JPanel panelSuperior = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 5));
        panelSuperior.setOpaque(false);

        JComboBox<String> cmbCedula = new JComboBox<>();
        cmbCedula.setPreferredSize(new Dimension(220, 28));
        prepararComboCedulas(cmbCedula, Empleado.TipoContrato.TIEMPO_COMPLETO);

        JComboBox<String> cmbMes = new JComboBox<>(MESES);
        JComboBox<String> cmbAnio = new JComboBox<>(ANIOS);
        JButton btnGenerar = new JButton("Generar Reporte");
        JButton btnImprimir = new JButton("Imprimir");
        btnImprimir.setEnabled(false);

        panelSuperior.add(new JLabel("Empleado:"));
        panelSuperior.add(cmbCedula);
        panelSuperior.add(new JLabel("Mes:"));
        panelSuperior.add(cmbMes);
        panelSuperior.add(new JLabel("Año:"));
        panelSuperior.add(cmbAnio);
        panelSuperior.add(btnGenerar);
        panelSuperior.add(btnImprimir);
        panel.add(panelSuperior, BorderLayout.NORTH);

        String[] columnas = {"Fecha", "Ent. Mañana", "Sal. Mañana", "Ent. Tarde", "Sal. Tarde", "Atraso (min)", "Descuento ($)"};
        DefaultTableModel model = new DefaultTableModel(columnas, 0);
        JTable tabla = new JTable(model);
        estilizarTabla(tabla);
        panel.add(new JScrollPane(tabla), BorderLayout.CENTER);

        JPanel panelMetricas = new JPanel(new GridLayout(1, 4, 12, 0));
        panelMetricas.setOpaque(false);
        panelMetricas.setPreferredSize(new Dimension(0, 70));
        JLabel valBase = new JLabel("—", SwingConstants.CENTER);
        JLabel valAtraso = new JLabel("—", SwingConstants.CENTER);
        JLabel valDescuento = new JLabel("—", SwingConstants.CENTER);
        JLabel valNeto = new JLabel("—", SwingConstants.CENTER);
        panelMetricas.add(crearTarjetaMetrica("Sueldo Base", valBase, COLOR_UTA));
        panelMetricas.add(crearTarjetaMetrica("Total Atrasos", valAtraso, new Color(180, 120, 0)));
        panelMetricas.add(crearTarjetaMetrica("Descuentos", valDescuento, COLOR_ERROR));
        panelMetricas.add(crearTarjetaMetrica("Sueldo Neto", valNeto, COLOR_EXITO));
        panel.add(panelMetricas, BorderLayout.SOUTH);

        cmbCedula.addActionListener(e -> {
            model.setRowCount(0);
            valBase.setText("—");
            valAtraso.setText("—");
            valDescuento.setText("—");
            valNeto.setText("—");
            btnImprimir.setEnabled(false);
        });

        btnGenerar.addActionListener(e -> {
            String cedula = extraerCedula(cmbCedula.getSelectedItem());
            if (cedula == null) {
                JOptionPane.showMessageDialog(this, "Debe seleccionar un empleado.", "Aviso", JOptionPane.WARNING_MESSAGE);
                return;
            }
            Empleado emp = new EmpleadoDAO().buscarPorCedula(cedula);
            if (emp == null || emp.getTipoContrato() != Empleado.TipoContrato.TIEMPO_COMPLETO) {
                JOptionPane.showMessageDialog(this, "Empleado no encontrado o no es de Tiempo Completo.", "Error", JOptionPane.ERROR_MESSAGE);
                return;
            }

            int mesNum = cmbMes.getSelectedIndex() + 1;
            int anioNum = Integer.parseInt((String) cmbAnio.getSelectedItem());
            LocalDate inicioMes = LocalDate.of(anioNum, mesNum, 1);
            LocalDate finMes = inicioMes.withDayOfMonth(inicioMes.lengthOfMonth());

            List<RegistroAsistencia> registros = new AsistenciaDAO().listarPorCedulaYRango(cedula, inicioMes, finMes);
            model.setRowCount(0);
            int totalAtraso = 0;
            for (RegistroAsistencia r : registros) {
                int atrasoDia = r.getMinutosAtraso();
                totalAtraso += atrasoDia;
                model.addRow(new Object[]{
                    r.getFecha().toString(),
                    fmt(r.getHoraEntradaManana()), fmt(r.getHoraSalidaManana()),
                    fmt(r.getHoraEntradaTarde()), fmt(r.getHoraSalidaTarde()),
                    atrasoDia + " min",
                    String.format("$%.2f", atrasoDia * 0.20)
                });
            }
            double descTotal = totalAtraso * 0.20;
            double sueldoNeto = Math.max(1500.00 - descTotal, 0.00);
            valBase.setText("$1500.00");
            valAtraso.setText(totalAtraso + " min");
            valDescuento.setText(String.format("$%.2f", descTotal));
            valNeto.setText(String.format("$%.2f", sueldoNeto));
            btnImprimir.setEnabled(true);
        });

        btnImprimir.addActionListener(e -> imprimirTabla(tabla,
            "Reporte Tiempo Completo - " + cmbCedula.getSelectedItem()));

        return panel;
    }

    private JPanel crearSubPanelReporteTP() {
        JPanel panel = new JPanel(new BorderLayout(0, 12));
        panel.setBorder(BorderFactory.createEmptyBorder(12, 12, 12, 12));
        panel.setBackground(Color.WHITE);

        JPanel panelSuperior = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 5));
        panelSuperior.setOpaque(false);

        JComboBox<String> cmbCedula = new JComboBox<>();
        cmbCedula.setPreferredSize(new Dimension(220, 28));
        prepararComboCedulas(cmbCedula, Empleado.TipoContrato.TIEMPO_PARCIAL);

        JComboBox<String> cmbMes = new JComboBox<>(MESES);
        JComboBox<String> cmbAnio = new JComboBox<>(ANIOS);
        JButton btnGenerar = new JButton("Generar Reporte");
        JButton btnImprimir = new JButton("Imprimir");
        btnImprimir.setEnabled(false);

        panelSuperior.add(new JLabel("Empleado:"));
        panelSuperior.add(cmbCedula);
        panelSuperior.add(new JLabel("Mes:"));
        panelSuperior.add(cmbMes);
        panelSuperior.add(new JLabel("Año:"));
        panelSuperior.add(cmbAnio);
        panelSuperior.add(btnGenerar);
        panelSuperior.add(btnImprimir);
        panel.add(panelSuperior, BorderLayout.NORTH);

        String[] columnas = {"Fecha", "Ent. Mañana", "Sal. Mañana", "Ent. Tarde", "Sal. Tarde", "Horas Laboradas"};
        DefaultTableModel model = new DefaultTableModel(columnas, 0);
        JTable tabla = new JTable(model);
        estilizarTabla(tabla);
        panel.add(new JScrollPane(tabla), BorderLayout.CENTER);

        JPanel panelMetricas = new JPanel(new GridLayout(1, 3, 12, 0));
        panelMetricas.setOpaque(false);
        panelMetricas.setPreferredSize(new Dimension(0, 70));
        JLabel valHoras = new JLabel("—", SwingConstants.CENTER);
        JLabel valTarifa = new JLabel("—", SwingConstants.CENTER);
        JLabel valNeto = new JLabel("—", SwingConstants.CENTER);
        panelMetricas.add(crearTarjetaMetrica("Total Horas", valHoras, COLOR_UTA));
        panelMetricas.add(crearTarjetaMetrica("Tarifa por Hora", valTarifa, new Color(180, 120, 0)));
        panelMetricas.add(crearTarjetaMetrica("Sueldo a Recibir", valNeto, COLOR_EXITO));
        panel.add(panelMetricas, BorderLayout.SOUTH);

        cmbCedula.addActionListener(e -> {
            model.setRowCount(0);
            valHoras.setText("—");
            valTarifa.setText("—");
            valNeto.setText("—");
            btnImprimir.setEnabled(false);
        });

        btnGenerar.addActionListener(e -> {
            String cedula = extraerCedula(cmbCedula.getSelectedItem());
            if (cedula == null) {
                JOptionPane.showMessageDialog(this, "Debe seleccionar un empleado.", "Aviso", JOptionPane.WARNING_MESSAGE);
                return;
            }
            Empleado emp = new EmpleadoDAO().buscarPorCedula(cedula);
            if (emp == null || emp.getTipoContrato() != Empleado.TipoContrato.TIEMPO_PARCIAL) {
                JOptionPane.showMessageDialog(this, "Empleado no encontrado o no es de Tiempo Parcial.", "Error", JOptionPane.ERROR_MESSAGE);
                return;
            }

            int mesNum = cmbMes.getSelectedIndex() + 1;
            int anioNum = Integer.parseInt((String) cmbAnio.getSelectedItem());
            LocalDate inicioMes = LocalDate.of(anioNum, mesNum, 1);
            LocalDate finMes = inicioMes.withDayOfMonth(inicioMes.lengthOfMonth());

            List<RegistroAsistencia> registros = new AsistenciaDAO().listarPorCedulaYRango(cedula, inicioMes, finMes);
            model.setRowCount(0);
            double totalHoras = 0;
            for (RegistroAsistencia r : registros) {
                double horasDia = r.calcularHorasTrabajadas();
                totalHoras += horasDia;
                model.addRow(new Object[]{
                    r.getFecha().toString(),
                    fmt(r.getHoraEntradaManana()), fmt(r.getHoraSalidaManana()),
                    fmt(r.getHoraEntradaTarde()), fmt(r.getHoraSalidaTarde()),
                    String.format("%.2f hrs", horasDia)
                });
            }
            valHoras.setText(String.format("%.2f hrs", totalHoras));
            valTarifa.setText("$5.00");
            valNeto.setText(String.format("$%.2f", totalHoras * 5.00));
            btnImprimir.setEnabled(true);
        });

        btnImprimir.addActionListener(e -> imprimirTabla(tabla,
            "Reporte Tiempo Parcial - " + cmbCedula.getSelectedItem()));

        return panel;
    }

    private JPanel crearSubPanelAsistenciasFechas() {
        JPanel panel = new JPanel(new BorderLayout(0, 12));
        panel.setBorder(BorderFactory.createEmptyBorder(12, 12, 12, 12));
        panel.setBackground(Color.WHITE);

        JPanel panelSuperior = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 5));
        panelSuperior.setOpaque(false);

        JTextField txtFechaInicio = new JTextField(LocalDate.now().minusDays(7).toString(), 10);
        JTextField txtFechaFin = new JTextField(LocalDate.now().toString(), 10);
        JButton btnConsultar = new JButton("Consultar Rango");
        JButton btnImprimir = new JButton("Imprimir");
        btnImprimir.setEnabled(false);

        panelSuperior.add(new JLabel("Fecha Inicio (AAAA-MM-DD):"));
        panelSuperior.add(txtFechaInicio);
        panelSuperior.add(new JLabel("Fecha Fin (AAAA-MM-DD):"));
        panelSuperior.add(txtFechaFin);
        panelSuperior.add(btnConsultar);
        panelSuperior.add(btnImprimir);
        panel.add(panelSuperior, BorderLayout.NORTH);

        String[] columnas = {"Empleado", "Cédula", "Fecha", "Ent. Mañana", "Sal. Mañana", "Ent. Tarde", "Sal. Tarde"};
        DefaultTableModel model = new DefaultTableModel(columnas, 0);
        JTable tabla = new JTable(model);
        estilizarTabla(tabla);
        panel.add(new JScrollPane(tabla), BorderLayout.CENTER);

        btnConsultar.addActionListener(e -> {
            try {
                LocalDate desde = LocalDate.parse(txtFechaInicio.getText().trim());
                LocalDate hasta = LocalDate.parse(txtFechaFin.getText().trim());

                AsistenciaDAO asistDAO = new AsistenciaDAO();
                EmpleadoDAO empDAO = new EmpleadoDAO();

                List<RegistroAsistencia> registros;
                if (usuarioActual.getPerfil() == Usuario.Perfil.ADMINISTRADOR) {
                    registros = asistDAO.listarTodosPorRango(desde, hasta);
                } else {
                    registros = asistDAO.listarPorCedulaYRango(usuarioActual.getCedula(), desde, hasta);
                }

                model.setRowCount(0);
                for (RegistroAsistencia r : registros) {
                    Empleado emp = empDAO.buscarPorCedula(r.getCedulaEmpleado());
                    model.addRow(new Object[]{
                        emp != null ? emp.getNombreCompleto() : "Desconocido",
                        r.getCedulaEmpleado(),
                        r.getFecha().toString(),
                        fmt(r.getHoraEntradaManana()), fmt(r.getHoraSalidaManana()),
                        fmt(r.getHoraEntradaTarde()), fmt(r.getHoraSalidaTarde())
                    });
                }
                btnImprimir.setEnabled(model.getRowCount() > 0);
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(this, "Formato de fecha inválido. Utilice AAAA-MM-DD.", "Error de Formato", JOptionPane.ERROR_MESSAGE);
            }
        });

        btnImprimir.addActionListener(e -> imprimirTabla(tabla, "Reporte de Asistencias por Rango"));

        return panel;
    }

    private void actualizarTablaEmpleados() {
        if (tablaEmpleados == null) return;
        DefaultTableModel model = (DefaultTableModel) tablaEmpleados.getModel();
        model.setRowCount(0);
        for (Empleado e : new EmpleadoDAO().listarTodos()) {
            model.addRow(new Object[]{
                e.getCedula(), e.getNombre(), e.getApellido(), e.getCargo(), e.getTipoContrato().name()
            });
        }
    }

    private void filtrarTablaEmpleados(String texto) {
        DefaultTableModel model = (DefaultTableModel) tablaEmpleados.getModel();
        model.setRowCount(0);
        String t = texto.toLowerCase();
        for (Empleado e : new EmpleadoDAO().listarTodos()) {
            if (e.getCedula().contains(texto)
                || e.getNombre().toLowerCase().contains(t)
                || e.getApellido().toLowerCase().contains(t)) {
                model.addRow(new Object[]{
                    e.getCedula(), e.getNombre(), e.getApellido(), e.getCargo(), e.getTipoContrato().name()
                });
            }
        }
    }

    private void crudRegistrar() {
        String cedula = txtCrudCedula.getText().trim();
        String nombre = txtCrudNombre.getText().trim();
        String apellido = txtCrudApellido.getText().trim();
        String cargo = txtCrudCargo.getText().trim();

        if (cedula.isEmpty() || nombre.isEmpty() || apellido.isEmpty()) {
            mostrarResultadoCrud("Complete cédula, nombre y apellido.", false);
            return;
        }

        Empleado emp = construirEmpleado(cedula, nombre, apellido, cargo);
        EmpleadoDAO dao = new EmpleadoDAO();
        if (dao.insertar(emp)) {

            Usuario user = new Usuario(cedula, nombre + " " + apellido, "clave123", Usuario.Perfil.EMPLEADO);
            new UsuarioDAO().insertar(user);
            mostrarResultadoCrud("Registrado (Usuario: " + cedula + ", Clave: clave123)", true);
            limpiarCamposCrud();
            actualizarTablaEmpleados();
        } else {
            mostrarResultadoCrud("Error. ¿Cédula duplicada?", false);
        }
    }

    private void crudModificar() {
        String cedula = txtCrudCedula.getText().trim();
        String nombre = txtCrudNombre.getText().trim();
        String apellido = txtCrudApellido.getText().trim();
        String cargo = txtCrudCargo.getText().trim();

        if (cedula.isEmpty() || nombre.isEmpty() || apellido.isEmpty()) {
            mostrarResultadoCrud("Seleccione un empleado y complete los campos.", false);
            return;
        }

        Empleado emp = construirEmpleado(cedula, nombre, apellido, cargo);
        if (new EmpleadoDAO().modificar(emp)) {
            mostrarResultadoCrud("Modificado correctamente.", true);
            limpiarCamposCrud();
            actualizarTablaEmpleados();
        } else {
            mostrarResultadoCrud("Error al modificar.", false);
        }
    }

    private void crudEliminar() {
        String cedula = txtCrudCedula.getText().trim();
        if (cedula.isEmpty()) {
            mostrarResultadoCrud("Seleccione el empleado a eliminar.", false);
            return;
        }
        int op = JOptionPane.showConfirmDialog(this, "¿Está seguro de eliminar a este empleado?", "Eliminar", JOptionPane.YES_NO_OPTION);
        if (op == JOptionPane.YES_OPTION) {
            if (new EmpleadoDAO().eliminar(cedula)) {
                mostrarResultadoCrud("Empleado dado de baja.", true);
                limpiarCamposCrud();
                actualizarTablaEmpleados();
            } else {
                mostrarResultadoCrud("Error al eliminar.", false);
            }
        }
    }

    private Empleado construirEmpleado(String cedula, String nombre, String apellido, String cargo) {
        Empleado.TipoContrato tipo = Empleado.TipoContrato.valueOf((String) cmbCrudTipoContrato.getSelectedItem());
        if (tipo == Empleado.TipoContrato.TIEMPO_COMPLETO) {
            return new EmpleadoTiempoCompleto(cedula, nombre, apellido, cargo);
        }
        return new EmpleadoTiempoParcial(cedula, nombre, apellido, cargo);
    }

    private void mostrarResultadoCrud(String texto, boolean exito) {
        lblCrudResultado.setText("<html>" + texto + "</html>");
        lblCrudResultado.setForeground(exito ? COLOR_EXITO : COLOR_ERROR);
    }

    private void limpiarCamposCrud() {
        txtCrudCedula.setText("");
        txtCrudCedula.setEditable(true);
        txtCrudNombre.setText("");
        txtCrudApellido.setText("");
        txtCrudCargo.setText("");
        cmbCrudTipoContrato.setSelectedIndex(0);
    }

    private void abrirRegistroHuella() {
        if (!fingerprintManager.isActivo()) {
            new Thread(() -> {
                boolean ok = fingerprintManager.iniciar();
                if (ok) {
                    Map<String, String> huellas = new HuellaDAO().obtenerTodasHuellas();
                    fingerprintManager.cargarHuellasEnCache(huellas);
                }
                SwingUtilities.invokeLater(() -> {
                    RegistroHuellaFrame frame = new RegistroHuellaFrame(fingerprintManager);
                    frame.setOnCerrar(() -> fingerprintManager.setListener(null));
                    frame.setVisible(true);
                });
            }, "LectorInit-Enroll").start();
        } else {
            RegistroHuellaFrame frame = new RegistroHuellaFrame(fingerprintManager);
            frame.setOnCerrar(() -> fingerprintManager.setListener(null));
            frame.setVisible(true);
        }
    }

    private void abrirRegistroRostro() {
        String cedula = JOptionPane.showInputDialog(this, "Ingrese la cédula del empleado para registrar rostro:", "Registrar Rostro", JOptionPane.QUESTION_MESSAGE);
        if (cedula != null && !cedula.trim().isEmpty()) {
            Empleado emp = new EmpleadoDAO().buscarPorCedula(cedula.trim());
            if (emp != null) {
                new RegistroRostroFrame(emp).setVisible(true);
            } else {
                JOptionPane.showMessageDialog(this, "Empleado no encontrado.", "Error", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    private void registrarAsistencia() {

        String cedula = usuarioActual.getCedula();
        if (usuarioActual.getPerfil() == Usuario.Perfil.ADMINISTRADOR) {
            String inputCedula = JOptionPane.showInputDialog(this, "Ingrese la cédula del empleado para registrar asistencia:", cedula);
            if (inputCedula == null || inputCedula.trim().isEmpty()) return;
            cedula = inputCedula.trim();
        }

        Empleado emp = new EmpleadoDAO().buscarPorCedula(cedula);
        if (emp == null) {
            JOptionPane.showMessageDialog(this, "Empleado con cédula " + cedula + " no registrado o inactivo.", "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }

        AsistenciaDAO asistDAO = new AsistenciaDAO();
        LocalDate hoy = LocalDate.now();
        RegistroAsistencia reg = asistDAO.buscarPorCedulaYFecha(cedula, hoy);
        if (reg == null) {
            reg = new RegistroAsistencia(cedula);
        }

        List<String> disponibles = new ArrayList<>();
        if (reg.getHoraEntradaManana() == null) disponibles.add("Entrada Mañana");
        if (reg.getHoraEntradaManana() != null && reg.getHoraSalidaManana() == null) disponibles.add("Salida Mañana");
        if (reg.getHoraEntradaTarde() == null) disponibles.add("Entrada Tarde");
        if (reg.getHoraEntradaTarde() != null && reg.getHoraSalidaTarde() == null) disponibles.add("Salida Tarde");

        if (disponibles.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Este empleado ya registró todas las marcaciones de hoy.", "Aviso", JOptionPane.WARNING_MESSAGE);
            return;
        }

        String sugerida = disponibles.get(0);
        String marca = (String) JOptionPane.showInputDialog(this,
            "Empleado: " + emp.getNombreCompleto() + "\n"
            + "Hora actual: " + LocalTime.now().format(DateTimeFormatter.ofPattern("HH:mm:ss")) + "\n\n"
            + "Seleccione el tipo de marcación a registrar:",
            "Paso 1 de 2 - Tipo de Marcación",
            JOptionPane.QUESTION_MESSAGE, null,
            disponibles.toArray(), sugerida);

        if (marca == null) return;

        Object[] metodos = {"Huella Dactilar", "Reconocimiento Facial"};
        int eleccion = JOptionPane.showOptionDialog(this,
            "Marcación seleccionada: " + marca + "\n\n"
            + "Ahora valide su identidad. ¿Con qué método biométrico desea continuar?",
            "Paso 2 de 2 - Validación Biométrica",
            JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE,
            null, metodos, metodos[0]);

        if (eleccion == JOptionPane.CLOSED_OPTION) return;

        boolean validado = false;
        if (eleccion == 0) {

            if (!new HuellaDAO().tieneHuella(cedula)) {
                JOptionPane.showMessageDialog(this,
                    "Este empleado no tiene una huella registrada.\n"
                    + "Use el reconocimiento facial o registre su huella primero.",
                    "Sin huella", JOptionPane.WARNING_MESSAGE);
                return;
            }
            ValidacionHuellaDialog dialog = new ValidacionHuellaDialog(this, fingerprintManager, cedula, emp.getNombreCompleto());
            dialog.setVisible(true);
            validado = dialog.isValidado();
        } else if (eleccion == 1) {
            ValidacionRostroDialog dialog = new ValidacionRostroDialog(this, usuarioActual);
            dialog.setVisible(true);
            validado = dialog.isValidado();
        }

        if (!validado) {
            JOptionPane.showMessageDialog(this, "Validación biométrica fallida o cancelada.", "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }

        LocalTime ahora = LocalTime.now();
        if (marca.equals("Entrada Mañana")) {
            reg.setHoraEntradaManana(ahora);
        } else if (marca.equals("Salida Mañana")) {
            reg.setHoraSalidaManana(ahora);
        } else if (marca.equals("Entrada Tarde")) {
            reg.setHoraEntradaTarde(ahora);
        } else if (marca.equals("Salida Tarde")) {
            reg.setHoraSalidaTarde(ahora);
        }
        reg.calcularMinutosAtraso();

        if (asistDAO.registrarAsistencia(reg)) {
            JOptionPane.showMessageDialog(this,
                "Asistencia registrada correctamente.\n"
                + marca + ": " + ahora.format(DateTimeFormatter.ofPattern("HH:mm:ss"))
                + (reg.getMinutosAtraso() > 0 ? "\nAtraso acumulado: " + reg.getMinutosAtraso() + " minutos" : ""),
                "Asistencia", JOptionPane.INFORMATION_MESSAGE);
        } else {
            JOptionPane.showMessageDialog(this, "Error al registrar la asistencia.", "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void cerrarSesion() {
        int opcion = JOptionPane.showConfirmDialog(this, "¿Desea cerrar sesión?", "Confirmar", JOptionPane.YES_NO_OPTION);
        if (opcion == JOptionPane.YES_OPTION) {
            if (fingerprintManager.isActivo()) {
                fingerprintManager.cerrar();
            }
            if (relojTimer != null) relojTimer.stop();
            new LoginFrame().setVisible(true);
            this.dispose();
        }
    }

    private void iniciarReloj() {
        relojTimer = new Timer(1000, e -> lblReloj.setText(
            LocalTime.now().format(DateTimeFormatter.ofPattern("HH:mm:ss"))
            + "  -  " + LocalDate.now().format(DateTimeFormatter.ofPattern("dd/MM/yyyy"))));
        relojTimer.start();
    }

    private void imprimirTabla(JTable tabla, String titulo) {
        if (tabla.getRowCount() == 0) {
            JOptionPane.showMessageDialog(this, "No hay datos para imprimir. Genere primero el reporte.", "Aviso", JOptionPane.WARNING_MESSAGE);
            return;
        }
        try {
            MessageFormat encabezado = new MessageFormat(titulo);
            MessageFormat pie = new MessageFormat("Página {0}");
            boolean impreso = tabla.print(JTable.PrintMode.FIT_WIDTH, encabezado, pie);
            if (!impreso) {
                JOptionPane.showMessageDialog(this, "Impresión cancelada por el usuario.", "Información", JOptionPane.INFORMATION_MESSAGE);
            }
        } catch (PrinterException ex) {
            JOptionPane.showMessageDialog(this, "Error al imprimir: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private static final String[] MESES = {
        "Enero", "Febrero", "Marzo", "Abril", "Mayo", "Junio",
        "Julio", "Agosto", "Septiembre", "Octubre", "Noviembre", "Diciembre"
    };
    private static final String[] ANIOS = {"2025", "2026", "2027", "2028"};

    private static String fmt(LocalTime t) {
        return t != null ? t.toString() : "—";
    }

    private void prepararComboCedulas(JComboBox<String> combo, Empleado.TipoContrato filtro) {
        recargarComboCedulas(combo, filtro);
        if (usuarioActual.getPerfil() == Usuario.Perfil.EMPLEADO) {
            combo.setEnabled(false);
            return;
        }
        combo.addPopupMenuListener(new javax.swing.event.PopupMenuListener() {
            @Override public void popupMenuWillBecomeVisible(javax.swing.event.PopupMenuEvent e) {
                recargarComboCedulas(combo, filtro);
            }
            @Override public void popupMenuWillBecomeInvisible(javax.swing.event.PopupMenuEvent e) {}
            @Override public void popupMenuCanceled(javax.swing.event.PopupMenuEvent e) {}
        });
    }

    private void prepararComboCedulasTodos(JComboBox<String> combo) {
        recargarComboCedulasTodos(combo);
        if (usuarioActual.getPerfil() == Usuario.Perfil.EMPLEADO) {
            combo.setEnabled(false);
            return;
        }
        combo.addPopupMenuListener(new javax.swing.event.PopupMenuListener() {
            @Override public void popupMenuWillBecomeVisible(javax.swing.event.PopupMenuEvent e) {
                recargarComboCedulasTodos(combo);
            }
            @Override public void popupMenuWillBecomeInvisible(javax.swing.event.PopupMenuEvent e) {}
            @Override public void popupMenuCanceled(javax.swing.event.PopupMenuEvent e) {}
        });
    }

    private void recargarComboCedulasTodos(JComboBox<String> combo) {
        Object sel = combo.getSelectedItem();
        combo.removeAllItems();
        if (usuarioActual.getPerfil() == Usuario.Perfil.EMPLEADO) {
            combo.addItem(usuarioActual.getCedula());
            return;
        }
        combo.addItem("TODOS LOS EMPLEADOS");
        for (Empleado e : new EmpleadoDAO().listarTodos()) {
            combo.addItem(e.getCedula() + " - " + e.getNombreCompleto());
        }
        if (sel != null) combo.setSelectedItem(sel);
    }

    private void recargarComboCedulas(JComboBox<String> combo, Empleado.TipoContrato filtro) {
        Object sel = combo.getSelectedItem();
        combo.removeAllItems();
        if (usuarioActual.getPerfil() == Usuario.Perfil.EMPLEADO) {
            combo.addItem(usuarioActual.getCedula());
            return;
        }
        combo.addItem("-- Seleccione --");
        for (Empleado e : new EmpleadoDAO().listarTodos()) {
            if (e.getTipoContrato() == filtro) {
                combo.addItem(e.getCedula() + " - " + e.getNombreCompleto());
            }
        }
        if (sel != null) combo.setSelectedItem(sel);
    }

    private String extraerCedula(Object item) {
        if (item == null) return null;
        String s = item.toString().trim();
        if (s.isEmpty() || s.startsWith("--")) return null;
        int idx = s.indexOf(" - ");
        return idx >= 0 ? s.substring(0, idx).trim() : s;
    }

    private JPanel crearTarjetaResumen(String titulo, String valor) {
        JPanel card = new JPanel(new BorderLayout());
        card.setBackground(new Color(248, 240, 242));
        card.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(COLOR_UTA, 2),
            BorderFactory.createEmptyBorder(10, 14, 10, 14)));
        JLabel lblTit = new JLabel(titulo);
        lblTit.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        lblTit.setForeground(COLOR_GRIS);
        JLabel lblVal = new JLabel(valor);
        lblVal.setFont(new Font("Segoe UI", Font.BOLD, 28));
        lblVal.setForeground(COLOR_UTA);
        card.add(lblTit, BorderLayout.NORTH);
        card.add(lblVal, BorderLayout.CENTER);
        return card;
    }

    private JPanel crearTarjetaMetrica(String titulo, JLabel lblValor, Color colorValor) {
        JPanel card = new JPanel(new BorderLayout());
        card.setBackground(Color.WHITE);
        card.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(COLOR_UTA),
            BorderFactory.createEmptyBorder(6, 6, 6, 6)));
        JLabel lblTit = new JLabel(titulo, SwingConstants.CENTER);
        lblTit.setFont(new Font("Segoe UI", Font.BOLD, 12));
        lblTit.setForeground(COLOR_GRIS);
        lblValor.setFont(new Font("Segoe UI", Font.BOLD, 18));
        lblValor.setForeground(colorValor);
        card.add(lblTit, BorderLayout.NORTH);
        card.add(lblValor, BorderLayout.CENTER);
        return card;
    }

    private JLabel crearTituloSeccion(String texto) {
        JLabel lbl = new JLabel(texto);
        lbl.setFont(new Font("Segoe UI", Font.BOLD, 20));
        lbl.setForeground(COLOR_UTA);
        lbl.setBorder(BorderFactory.createEmptyBorder(0, 0, 8, 0));
        return lbl;
    }

    private JLabel crearEtiquetaSeccion(String texto) {
        JLabel lbl = new JLabel(texto);
        lbl.setFont(new Font("Segoe UI", Font.BOLD, 11));
        lbl.setForeground(COLOR_UTA);
        lbl.setAlignmentX(Component.LEFT_ALIGNMENT);
        lbl.setBorder(BorderFactory.createEmptyBorder(4, 4, 4, 4));
        return lbl;
    }

    private JButton crearBotonMenu(String texto, ActionListener action) {
        JButton btn = new JButton(texto);
        btn.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        btn.setHorizontalAlignment(SwingConstants.LEFT);
        btn.setMaximumSize(new Dimension(Integer.MAX_VALUE, 34));
        btn.setAlignmentX(Component.LEFT_ALIGNMENT);
        btn.setFocusPainted(false);
        btn.setCursor(new Cursor(Cursor.HAND_CURSOR));
        btn.addActionListener(action);
        return btn;
    }

    private JLabel crearLabel(String texto) {
        JLabel lbl = new JLabel(texto);
        lbl.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        lbl.setForeground(COLOR_TEXTO);
        lbl.setAlignmentX(Component.LEFT_ALIGNMENT);
        return lbl;
    }

    private void limitarAltura(JComponent... comps) {
        for (JComponent c : comps) {
            c.setMaximumSize(new Dimension(Integer.MAX_VALUE, 30));
            c.setAlignmentX(Component.LEFT_ALIGNMENT);
        }
    }

    private void estilizarTabla(JTable tabla) {
        tabla.setRowHeight(24);
        tabla.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        tabla.setShowGrid(true);
        tabla.setGridColor(new Color(210, 210, 210));
        tabla.getTableHeader().setReorderingAllowed(false);

        JTableHeader header = tabla.getTableHeader();
        header.setPreferredSize(new Dimension(0, 28));
        header.setDefaultRenderer(new DefaultTableCellRenderer() {
            {
                setHorizontalAlignment(SwingConstants.CENTER);
                setOpaque(true);
            }
            @Override
            public Component getTableCellRendererComponent(JTable t, Object value,
                    boolean isSelected, boolean hasFocus, int row, int column) {
                super.getTableCellRendererComponent(t, value, isSelected, hasFocus, row, column);
                setBackground(COLOR_UTA);
                setForeground(Color.WHITE);
                setFont(new Font("Segoe UI", Font.BOLD, 13));
                setBorder(BorderFactory.createMatteBorder(0, 0, 1, 1, COLOR_UTA_OSCURO));
                return this;
            }
        });
    }
}
