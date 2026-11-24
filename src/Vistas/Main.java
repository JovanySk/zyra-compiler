// Archivo: src/Vistas/Main.java
// VERSIÓN COMPLETA, FINAL Y CORREGIDA
package Vistas;

// import com.formdev.flatlaf.FlatDarkLaf;  // Comentado temporalmente para testing
import estructuras.Token;
import estructuras.ast.NodoPrograma;
import estructuras.ts.TablaSimbolos;
import excepciones.lexicas.ExcepcionLexica;
import excepciones.semanticas.ExcepcionSemantica;
import excepciones.sintacticas.ExcepcionSintactica;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Font;
import java.awt.event.ActionEvent;
import java.awt.event.InputEvent;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintStream;
import java.io.StringReader;
import java.nio.file.Files;
import java.util.LinkedList;
import java.util.List;
import javax.swing.*;
import javax.swing.filechooser.FileNameExtensionFilter;
import javax.swing.table.DefaultTableModel;
import javax.swing.text.BadLocationException;
import javax.swing.text.DefaultHighlighter;
import javax.swing.text.Highlighter;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.undo.UndoManager;
import modulos.AnalizadorLexico;
import modulos.AnalizadorSintactico;
import modulos.CustomOutputStream;
import modulos.GenCod;
import modulos.SyntaxHighlighter;
import modulos.TextLineNumber;
import javax.swing.text.Element;

// === NUEVO: Imports para visualización de IR ===
import modulos.ir.FormateadorIR;

// Importaciones de Nodos y otras estructuras
import estructuras.ast.NodoAtributo;
import estructuras.ast.NodoClase;
import estructuras.ast.NodoMetodo;
import estructuras.ast.expresiones.*;
import estructuras.ast.sentencias.*;
import estructuras.ts.Clase;
import estructuras.ts.Constructor;
import estructuras.ts.Metodo;
import estructuras.ts.variables.VarInstancia;
import estructuras.ts.variables.VarLocal;
import estructuras.ts.variables.VarParametro;

public class Main extends javax.swing.JFrame {

    private JMenuBar menuBar;
    private JMenu menuArchivo, menuEditar, menuCompilar, menuAyuda;
    private JMenuItem itemNuevo, itemAbrir, itemGuardar, itemGuardarComo, itemSalir;
    private JMenuItem itemDeshacer, itemRehacer, itemCopiar, itemCortar, itemPegar;
    private JMenuItem itemEjecutar, itemManual, itemAcercaDe;
    private JTabbedPane jTabbedPaneResultados;
    private JTable jTableTokens;
    private DefaultTableModel tokenTableModel;
    private JTree jTreeSintactico;
    private JLabel jLabelStatus;
    private JTextArea jTextAreaErrores;
    private JTable jTableSimbolos;
    private DefaultTableModel symbolTableModel;

    // === NUEVO: Componentes para visualización de IR ===
    private JTextArea jTextAreaTAC;
    private JTextArea jTextAreaCuadruplos;
    private JTextArea jTextAreaTriplos;
    private JTextPane jTextPaneCodigo;
    private SyntaxHighlighter syntaxHighlighter;
    private File archivoActual = null;
    private final UndoManager undoManager = new UndoManager();
    private JScrollPane jScrollPaneEditor;

    public Main() {
        initComponents();
        configurarIDE();
    }

    private void jButtonAnalizarActionPerformed(ActionEvent evt) {
        limpiarResultados();
        jTabbedPaneResultados.setSelectedIndex(3);
        jTextAreaErrores.append("Iniciando compilación completa...\n");
        String codigoFuente = jTextPaneCodigo.getText();

        if (codigoFuente.trim().isEmpty()) {
            jTextAreaErrores.append("AVISO: No hay código para analizar.\n");
            return;
        }

        if (archivoActual == null) {
            jTextAreaErrores.append("ERROR: Por favor, guarde el archivo antes de compilar.\n");
            guardarComoArchivo();
            if (archivoActual == null) {
                jTextAreaErrores.append("Compilación cancelada.\n");
                return;
            }
        } else {
            guardarArchivo();
        }

        String rutaSalida;
        String rutaEntrada = archivoActual.getAbsolutePath();
        if (rutaEntrada.toLowerCase().endsWith(".txt")) {
            rutaSalida = rutaEntrada.substring(0, rutaEntrada.length() - 4) + ".asm";
        } else {
            rutaSalida = rutaEntrada + ".asm";
        }

        BufferedWriter bw = null;
        try {
            bw = new BufferedWriter(new FileWriter(rutaSalida));
            GenCod.setBuffer(bw);

            // FASE 1: ANÁLISIS LÉXICO
            jTextAreaErrores.append("1. Ejecutando Análisis Léxico... ");
            AnalizadorLexico lexico = new AnalizadorLexico(new BufferedReader(new StringReader(codigoFuente)));
            List<Token> tokens = lexico.getAllTokens();
            poblarTablaTokens(tokens);
            actualizarStatus(tokens);
            jTextAreaErrores.append("¡Correcto!\n");

            // FASE 2: ANÁLISIS SINTÁCTICO
            jTextAreaErrores.append("2. Ejecutando Análisis Sintáctico... ");
            AnalizadorSintactico sintactico = new AnalizadorSintactico(tokens);
            sintactico.analizar();
            jTextAreaErrores.append("¡Correcto!\n");
            poblarArbolSintactico(sintactico.getAST());

            jTextAreaErrores.append("3. Ejecutando Análisis Semántico...\n");
            TablaSimbolos tablaSimbolos = sintactico.getTablaSimbolos();
            NodoPrograma ast = sintactico.getAST();

            // ORQUESTACIÓN SEMÁNTICA
            jTextAreaErrores.append("  - Chequeando y consolidando declaraciones (herencia, duplicados, main)... ");
            tablaSimbolos.chequearClasesDuplicadas(ast);
            tablaSimbolos.chequearDeclaraciones();
            jTextAreaErrores.append("¡Correcto!\n");

            jTextAreaErrores.append("  - Poblando la tabla de símbolos para visualización... ");
            poblarTablaSimbolosDesdeAST(ast, tablaSimbolos);
            jTextAreaErrores.append("¡Correcto!\n");

            jTextAreaErrores.append("  - Chequeando sentencias y tipos... ");
            tablaSimbolos.chequearSentencias();
            jTextAreaErrores.append("¡Correcto!\n");

            // FASE 4: GENERACIÓN DE CÓDIGO INTERMEDIO
            jTextAreaErrores.append("4. Generando Código Intermedio... ");

            // === NUEVO SISTEMA IR ===
            modulos.ir.GeneradorCodigoIntermedio gci = new modulos.ir.GeneradorCodigoIntermedio();
            ast.generar(gci);

            jTextAreaErrores.append("¡Correcto!\n");
            jTextAreaErrores.append("   - Cuádruplos generados: " + gci.getCantidadCuadruplos() + "\n");

            // === NUEVO: Formatear y mostrar IR en pestañas ===
            jTextAreaTAC.setText(FormateadorIR.formatearCodigoTresDirecciones(gci.getCuadruplos()));
            jTextAreaCuadruplos.setText(FormateadorIR.formatearCuadruplos(gci.getCuadruplos()));
            jTextAreaTriplos.setText(FormateadorIR.formatearTriplos(gci.getTriplos()));
            jTextAreaErrores.append("   - IR visualizada en pestañas TAC/Cuádruplos/Triplos\n");

            // TODO Futuro: Aplicar optimizaciones
            // modulos.ir.optimizacion.PropagacionConstantes opt = new
            // modulos.ir.optimizacion.PropagacionConstantes();
            // if (opt.optimizar(gci)) {
            // jTextAreaErrores.append(" - Optimizaciones aplicadas\n");
            // }

            // FASE 5: GENERACIÓN DE ENSAMBLADOR DESDE IR
            jTextAreaErrores.append("5. Generando Ensamblador desde IR... ");

            // Crear archivo ASM
            File archivoASM = new File(archivoActual.getParent(),
                    archivoActual.getName().replace(".txt", ".asm"));

            // Generar ensamblador desde IR en lugar de AST directo
            modulos.ir.GeneradorEnsamblador genAsm = new modulos.ir.GeneradorEnsamblador();
            String codigoAsm = genAsm.generar(gci);

            // Escribir a archivo - UNA SOLA VEZ
            GenCod.setBuffer(new BufferedWriter(new FileWriter(archivoASM)));
            GenCod.write(codigoAsm); // Escribir código completo (.data + .code)
            GenCod.close();

            jTextAreaErrores.append("¡Correcto!\n");
            jTextAreaErrores.append("   - Ensamblador generado desde IR\n");
            jTextAreaErrores.append("   - Archivo: " + archivoASM.getName() + "\n");

            // FASE 6: ENSAMBLAR CON NASM
            jTextAreaErrores.append("6. Ensamblando con NASM... ");
            File archivoOBJ = new File(archivoActual.getParent(),
                    archivoActual.getName().replace(".txt", ".obj"));

            try {
                ProcessBuilder pbNasm = new ProcessBuilder(
                        "nasm", "-f", "win64",
                        archivoASM.getAbsolutePath(),
                        "-o", archivoOBJ.getAbsolutePath());
                pbNasm.redirectErrorStream(true);
                Process pNasm = pbNasm.start();
                int exitNasm = pNasm.waitFor();

                if (exitNasm != 0) {
                    java.io.BufferedReader reader = new java.io.BufferedReader(
                            new java.io.InputStreamReader(pNasm.getInputStream()));
                    String line;
                    StringBuilder errorNasm = new StringBuilder();
                    while ((line = reader.readLine()) != null) {
                        errorNasm.append(line).append("\n");
                    }
                    jTextAreaErrores.append("¡FALLÓ!\n\n");
                    jTextAreaErrores.append("ERROR NASM:\n" + errorNasm.toString());
                } else {
                    jTextAreaErrores.append("¡Correcto!\n");
                    jTextAreaErrores.append("   - Archivo objeto: " + archivoOBJ.getName() + "\n");

                    // FASE 7: ENLAZAR
                    jTextAreaErrores.append("7. Enlazando ejecutable... ");
                    File archivoEXE = new File(archivoActual.getParent(),
                            archivoActual.getName().replace(".txt", ".exe"));

                    ProcessBuilder pbLink = new ProcessBuilder(
                            "gcc",
                            archivoOBJ.getAbsolutePath(),
                            "-o", archivoEXE.getAbsolutePath(),
                            "-nostartfiles",
                            "-e", "main");
                    pbLink.redirectErrorStream(true);
                    Process pLink = pbLink.start();
                    int exitLink = pLink.waitFor();

                    if (exitLink != 0) {
                        java.io.BufferedReader readerLink = new java.io.BufferedReader(
                                new java.io.InputStreamReader(pLink.getInputStream()));
                        String lineLink;
                        StringBuilder errorLink = new StringBuilder();
                        while ((lineLink = readerLink.readLine()) != null) {
                            errorLink.append(lineLink).append("\n");
                        }
                        jTextAreaErrores.append("¡FALLÓ!\n");
                        jTextAreaErrores.append("ERROR GCC:\n" + errorLink.toString());
                    } else {
                        jTextAreaErrores.append("¡Correcto!\n");
                        jTextAreaErrores.append("   - Ejecutable: " + archivoEXE.getName() + "\n");

                        // FASE 8: EJECUTAR
                        jTextAreaErrores.append("8. Ejecutando... ");

                        // Detectar si el programa necesita entrada (tiene read)
                        boolean necesitaInput = false;
                        for (modulos.ir.Cuadruplo c : gci.getCuadruplos()) {
                            if (c.getOperador().equals("read")) {
                                necesitaInput = true;
                                break;
                            }
                        }

                        if (necesitaInput) {
                            // Lanzar en consola externa para permitir entrada interactiva
                            jTextAreaErrores.append("\n");
                            jTextAreaErrores.append("   ⚠️  El programa requiere entrada del usuario\n");
                            jTextAreaErrores.append("   📋 Abriendo consola interactiva...\n\n");

                            String help = "Write-Host '═══════════════════════════════════════' -ForegroundColor Cyan; "
                                    +
                                    "Write-Host '  PROGRAMA ZYRA - ENTRADA INTERACTIVA' -ForegroundColor Cyan; " +
                                    "Write-Host '═══════════════════════════════════════' -ForegroundColor Cyan; " +
                                    "Write-Host ''; " +
                                    "Write-Host '📖 GUÍA DE CÓDIGOS:' -ForegroundColor Yellow; " +
                                    "Write-Host '  1111 → Ingrese PRIMER número (a)' -ForegroundColor Green; " +
                                    "Write-Host '  2222 → Ingrese SEGUNDO número (b)' -ForegroundColor Green; " +
                                    "Write-Host '  9999 → Resultado de la operación' -ForegroundColor Magenta; " +
                                    "Write-Host ''; " +
                                    "Write-Host '▶️  INICIANDO PROGRAMA...' -ForegroundColor White; " +
                                    "Write-Host '═══════════════════════════════════════' -ForegroundColor Cyan; " +
                                    "Write-Host ''; ";

                            ProcessBuilder pbConsole = new ProcessBuilder(
                                    "cmd.exe", "/c", "start", "powershell.exe",
                                    "-NoExit", "-Command",
                                    help + archivoEXE.getAbsolutePath() + "; " +
                                            "Write-Host ''; Write-Host '═══════════════════════════════════════' -ForegroundColor Cyan; "
                                            +
                                            "Write-Host 'Programa finalizado. Presione cualquier tecla.' -ForegroundColor Gray; "
                                            +
                                            "$null = $Host.UI.RawUI.ReadKey('NoEcho,IncludeKeyDown')");
                            Process pConsole = pbConsole.start();
                            jTextAreaErrores.append("   ✅ Consola lanzada (ejecutándose en ventana externa)\n");
                        } else {
                            // Programa sin entrada - ejecutar y capturar salida
                            ProcessBuilder pbRun = new ProcessBuilder(archivoEXE.getAbsolutePath());
                            pbRun.redirectErrorStream(true);
                            Process pRun = pbRun.start();

                            // Capturar salida del programa
                            java.io.BufferedReader readerRun = new java.io.BufferedReader(
                                    new java.io.InputStreamReader(pRun.getInputStream()));
                            StringBuilder outputPrograma = new StringBuilder();
                            String lineRun;
                            while ((lineRun = readerRun.readLine()) != null) {
                                outputPrograma.append(lineRun).append("\n");
                            }

                            int exitCode = pRun.waitFor();
                            jTextAreaErrores.append("Código salida: " + exitCode + "\n");

                            // Mostrar salida del programa si hay
                            if (outputPrograma.length() > 0) {
                                jTextAreaErrores.append("\n--- SALIDA DEL PROGRAMA ---\n");
                                jTextAreaErrores.append(outputPrograma.toString());
                                jTextAreaErrores.append("---------------------------\n");
                            }
                        }
                    }
                }
            } catch (Exception eProc) {
                jTextAreaErrores.append("\n   - Error ejecutando: " + eProc.getMessage() + "\n");
            }

            jTextAreaErrores.append("\n--------------------------------------------------\n");
            jTextAreaErrores.append("COMPILACIÓN FINALIZADA CON ÉXITO.\n");
            jTextAreaErrores.append("Archivo ASM: " + archivoASM.getAbsolutePath() + "\n");
            jTextAreaErrores.append("--------------------------------------------------\n");
            jTabbedPaneResultados.setSelectedIndex(2);

        } catch (ExcepcionLexica ex) {
            jTextAreaErrores.append("¡FALLÓ!\n\n");
            jTextAreaErrores.append("ERROR LÉXICO: " + ex.getMessage() + "\n");
            resaltarLineaError(ex.getLinea());
        } catch (ExcepcionSintactica ex) {
            jTextAreaErrores.append("¡FALLÓ!\n\n");
            String sugerencia = generarSugerencia(ex);
            String mensaje = String.format("ERROR SINTÁCTICO: %s\n> Sugerencia: %s\n", ex.getMessage(), sugerencia);
            jTextAreaErrores.append(mensaje);
            resaltarLineaError(ex.getLineaError());
        } catch (ExcepcionSemantica ex) {
            jTextAreaErrores.append("¡FALLÓ!\n\n");
            Token tokenError = ex.getToken();
            String infoToken = (tokenError != null)
                    ? String.format("[Línea: %d, Col: %d, Lexema: '%s']", tokenError.getLinea(),
                            tokenError.getColumna(), tokenError.getLexema())
                    : "[Error global]";
            jTextAreaErrores.append("ERROR SEMÁNTICO: " + ex.getMessage() + "\n" + infoToken + "\n");
            if (tokenError != null)
                resaltarLineaError(tokenError.getLinea());
        } catch (IOException ex) {
            jTextAreaErrores.append("¡FALLÓ!\n\n");
            jTextAreaErrores.append("ERROR DE ARCHIVO: " + ex.getMessage() + "\n");
        } catch (Exception ex) {
            jTextAreaErrores.append("¡FALLÓ CON UN ERROR INESPERADO!\n\n");
            jTextAreaErrores.append("Error: " + ex.getClass().getSimpleName() + " - " + ex.getMessage() + "\n");
            ex.printStackTrace(new PrintStream(new CustomOutputStream(jTextAreaErrores)));
        } finally {
            try {
                if (bw != null) {
                    bw.close();
                }
            } catch (IOException e) {
                jTextAreaErrores.append("ERROR: No se pudo cerrar el archivo de salida.\n");
            }
        }
    }

    private void configurarIDE() {
        this.setLocationRelativeTo(null);
        this.setTitle("Zyra IDE");
        jTextPaneCodigo.getDocument().addUndoableEditListener(undoManager);
        Action undoAction = new AbstractAction("Deshacer") {
            @Override
            public void actionPerformed(ActionEvent e) {
                if (undoManager.canUndo())
                    undoManager.undo();
            }
        };
        Action redoAction = new AbstractAction("Rehacer") {
            @Override
            public void actionPerformed(ActionEvent e) {
                if (undoManager.canRedo())
                    undoManager.redo();
            }
        };
        itemDeshacer.setAction(undoAction);
        itemRehacer.setAction(redoAction);
        jTextPaneCodigo.getInputMap(JComponent.WHEN_FOCUSED)
                .put(KeyStroke.getKeyStroke(KeyEvent.VK_Z, InputEvent.CTRL_DOWN_MASK), "Undo");
        jTextPaneCodigo.getActionMap().put("Undo", undoAction);
        jTextPaneCodigo.getInputMap(JComponent.WHEN_FOCUSED)
                .put(KeyStroke.getKeyStroke(KeyEvent.VK_Y, InputEvent.CTRL_DOWN_MASK), "Redo");
        jTextPaneCodigo.getActionMap().put("Redo", redoAction);
        itemDeshacer.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_Z, InputEvent.CTRL_DOWN_MASK));
        itemRehacer.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_Y, InputEvent.CTRL_DOWN_MASK));
        jTextPaneCodigo.addKeyListener(new KeyAdapter() {
            @Override
            public void keyReleased(KeyEvent e) {
                if (!e.isActionKey() && e.getKeyCode() != KeyEvent.VK_SHIFT && e.getKeyCode() != KeyEvent.VK_CONTROL
                        && e.getKeyCode() != KeyEvent.VK_ALT && e.getKeyCode() != KeyEvent.VK_CAPS_LOCK) {
                    syntaxHighlighter.colorear();
                }
            }
        });
        syntaxHighlighter = new SyntaxHighlighter(jTextPaneCodigo);
        syntaxHighlighter.colorear();
    }

    @SuppressWarnings({ "serial", "unchecked" })
    private void initComponents() {
        setDefaultCloseOperation(javax.swing.WindowConstants.EXIT_ON_CLOSE);
        menuBar = new JMenuBar();
        menuArchivo = new JMenu("Archivo");
        itemNuevo = new JMenuItem("Nuevo");
        itemNuevo.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_N, InputEvent.CTRL_DOWN_MASK));
        itemNuevo.addActionListener(e -> nuevoArchivo());
        menuArchivo.add(itemNuevo);
        itemAbrir = new JMenuItem("Abrir");
        itemAbrir.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_O, InputEvent.CTRL_DOWN_MASK));
        itemAbrir.addActionListener(e -> abrirArchivo());
        menuArchivo.add(itemAbrir);
        itemGuardar = new JMenuItem("Guardar");
        itemGuardar.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_S, InputEvent.CTRL_DOWN_MASK));
        itemGuardar.addActionListener(e -> guardarArchivo());
        menuArchivo.add(itemGuardar);
        itemGuardarComo = new JMenuItem("Guardar Como...");
        itemGuardarComo.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_G, InputEvent.CTRL_DOWN_MASK));
        itemGuardarComo.addActionListener(e -> guardarComoArchivo());
        menuArchivo.add(itemGuardarComo);
        menuArchivo.addSeparator();
        itemSalir = new JMenuItem("Salir");
        itemSalir.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_Q, InputEvent.CTRL_DOWN_MASK));
        itemSalir.addActionListener(e -> salirAplicacion());
        menuArchivo.add(itemSalir);
        menuBar.add(menuArchivo);
        menuEditar = new JMenu("Editar");
        itemDeshacer = new JMenuItem("Deshacer");
        itemRehacer = new JMenuItem("Rehacer");
        menuEditar.add(itemDeshacer);
        menuEditar.add(itemRehacer);
        menuEditar.addSeparator();
        itemCortar = new JMenuItem("Cortar");
        itemCortar.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_X, InputEvent.CTRL_DOWN_MASK));
        itemCortar.addActionListener(e -> jTextPaneCodigo.cut());
        menuEditar.add(itemCortar);
        itemCopiar = new JMenuItem("Copiar");
        itemCopiar.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_C, InputEvent.CTRL_DOWN_MASK));
        itemCopiar.addActionListener(e -> jTextPaneCodigo.copy());
        menuEditar.add(itemCopiar);
        itemPegar = new JMenuItem("Pegar");
        itemPegar.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_V, InputEvent.CTRL_DOWN_MASK));
        itemPegar.addActionListener(e -> jTextPaneCodigo.paste());
        menuEditar.add(itemPegar);
        menuBar.add(menuEditar);
        menuCompilar = new JMenu("Compilar");
        itemEjecutar = new JMenuItem("Ejecutar (F6)");
        itemEjecutar.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_F6, 0));
        itemEjecutar.addActionListener(this::jButtonAnalizarActionPerformed);
        menuCompilar.add(itemEjecutar);
        menuBar.add(menuCompilar);
        menuAyuda = new JMenu("Ayuda");
        itemManual = new JMenuItem("Manual de Usuario (F1)");
        itemManual.addActionListener(e -> mostrarManual());
        menuAyuda.add(itemManual);
        itemAcercaDe = new JMenuItem("Acerca de...");
        itemAcercaDe.addActionListener(e -> mostrarAcercaDe());
        menuAyuda.add(itemAcercaDe);
        menuBar.add(menuAyuda);
        this.setJMenuBar(menuBar);
        getContentPane().setLayout(new BorderLayout(5, 5));
        jLabelStatus = new JLabel(" Palabras reservadas: 0 | Identificadores: 0 | Líneas: 0");
        jLabelStatus.setFont(new Font("Consolas", Font.PLAIN, 12));
        getContentPane().add(jLabelStatus, BorderLayout.NORTH);
        jTextPaneCodigo = new JTextPane();
        jTextPaneCodigo.setFont(new Font("Consolas", 0, 14));
        jTextPaneCodigo.setBackground(new Color(43, 43, 43));
        jTextPaneCodigo.setForeground(Color.WHITE);
        jTextPaneCodigo.setCaretColor(Color.ORANGE);
        jScrollPaneEditor = new JScrollPane(jTextPaneCodigo);
        TextLineNumber tln = new TextLineNumber(jTextPaneCodigo);
        jScrollPaneEditor.setRowHeaderView(tln);
        getContentPane().add(jScrollPaneEditor, BorderLayout.CENTER);
        jTabbedPaneResultados = new JTabbedPane();
        jTabbedPaneResultados.setFont(new Font("Consolas", Font.PLAIN, 12));
        tokenTableModel = new DefaultTableModel(new String[] { "Token", "Lexema", "Línea", "Columna" }, 0) {
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
        jTableTokens = new JTable(tokenTableModel);
        jTabbedPaneResultados.addTab("Análisis Léxico", new JScrollPane(jTableTokens));
        jTreeSintactico = new JTree(new DefaultMutableTreeNode("Programa"));
        jTreeSintactico.setCellRenderer(new AstTreeCellRenderer());
        jTabbedPaneResultados.addTab("Análisis Sintáctico", new JScrollPane(jTreeSintactico));
        symbolTableModel = new DefaultTableModel(new String[] { "Nombre", "Rol", "Tipo", "Alcance", "Valor" }, 0) {
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
        jTableSimbolos = new JTable(symbolTableModel);
        jTabbedPaneResultados.addTab("Tabla de Símbolos", new JScrollPane(jTableSimbolos));
        jTextAreaErrores = new JTextArea();
        jTextAreaErrores.setEditable(false);
        jTextAreaErrores.setFont(new Font("Consolas", Font.BOLD, 13));
        jTextAreaErrores.setForeground(Color.WHITE);
        jTextAreaErrores.setBackground(new Color(43, 43, 43));
        PrintStream printStream = new PrintStream(new CustomOutputStream(jTextAreaErrores));
        System.setOut(printStream);
        System.setErr(printStream);
        jTabbedPaneResultados.addTab("Consola", new JScrollPane(jTextAreaErrores));

        // === NUEVO: Pestañas de IR ===
        jTextAreaTAC = new JTextArea();
        jTextAreaTAC.setEditable(false);
        jTextAreaTAC.setFont(new Font("Consolas", Font.PLAIN, 11));
        jTabbedPaneResultados.addTab("TAC", new JScrollPane(jTextAreaTAC));

        jTextAreaCuadruplos = new JTextArea();
        jTextAreaCuadruplos.setEditable(false);
        jTextAreaCuadruplos.setFont(new Font("Consolas", Font.PLAIN, 11));
        jTabbedPaneResultados.addTab("Cuádruplos", new JScrollPane(jTextAreaCuadruplos));

        jTextAreaTriplos = new JTextArea();
        jTextAreaTriplos.setEditable(false);
        jTextAreaTriplos.setFont(new Font("Consolas", Font.PLAIN, 11));
        jTabbedPaneResultados.addTab("Triplos", new JScrollPane(jTextAreaTriplos));

        JPanel southPanel = new JPanel(new BorderLayout());
        southPanel.setPreferredSize(new java.awt.Dimension(0, 250));
        southPanel.add(jTabbedPaneResultados, BorderLayout.CENTER);
        getContentPane().add(southPanel, BorderLayout.SOUTH);
        pack();
        setSize(1024, 768);
    }

    private void limpiarResultados() {
        if (tokenTableModel != null)
            tokenTableModel.setRowCount(0);
        if (jTreeSintactico != null)
            jTreeSintactico.setModel(new DefaultTreeModel(new DefaultMutableTreeNode("Programa")));
        if (jTextAreaErrores != null)
            jTextAreaErrores.setText("");
        if (jTextPaneCodigo != null)
            jTextPaneCodigo.getHighlighter().removeAllHighlights();
        if (jLabelStatus != null)
            jLabelStatus.setText(" Palabras reservadas: 0 | Identificadores: 0 | Líneas: 0");
        if (symbolTableModel != null)
            symbolTableModel.setRowCount(0);
    }

    private void poblarTablaSimbolosDesdeAST(NodoPrograma ast, TablaSimbolos ts) {
        symbolTableModel.setRowCount(0);
        if (ast == null || ts == null)
            return;

        for (NodoClase nodoClase : ast.getClases()) {
            Clase claseTS = ts.clases.get(nodoClase.getToken().getLexema());
            if (claseTS == null)
                continue;

            for (VarInstancia var : claseTS.getAtributos().values()) {
                if (var.getToken() == null || var.getRol() == null || !var.getRol().equals("Atributo de Instancia"))
                    continue;
                symbolTableModel.addRow(new Object[] {
                        var.getToken().getLexema(), "Atributo", var.getTipoVar().getNombreClase(),
                        claseTS.getNombreClase(), (var.getValor() != null ? var.getValor() : "-")
                });
            }

            Constructor ctor = claseTS.getConstructor();
            if (ctor != null && ctor.getToken() != null) {
                String alcance = ctor.getToken().getLexema() + "(...)";
                symbolTableModel.addRow(new Object[] { ctor.getToken().getLexema(), "Constructor", "void",
                        claseTS.getNombreClase(), "-" });
                for (VarParametro param : ctor.getListaParametros()) {
                    symbolTableModel.addRow(new Object[] { param.getToken().getLexema(), "Parámetro",
                            param.getTipoVar().getNombreClase(), alcance,
                            (param.getValor() != null ? param.getValor() : "-") });
                }
                buscarYAgregarLocales(ctor.getCodigo(), alcance);
            }

            for (LinkedList<Metodo> listaSobrecargas : claseTS.getMetodos().values()) {
                for (Metodo met : listaSobrecargas) {
                    if (!met.getClaseImplementadora().getNombreClase().equals(claseTS.getNombreClase()))
                        continue;
                    String alcance = met.getToken().getLexema() + "(...)";
                    symbolTableModel.addRow(new Object[] { met.getToken().getLexema(), "Método",
                            met.getTipoRetorno().getNombreClase(), claseTS.getNombreClase(), "-" });
                    for (VarParametro param : met.getListaParametros()) {
                        symbolTableModel.addRow(new Object[] { param.getToken().getLexema(), "Parámetro",
                                param.getTipoVar().getNombreClase(), alcance,
                                (param.getValor() != null ? param.getValor() : "-") });
                    }
                    buscarYAgregarLocales(met.getCodigo(), alcance);
                }
            }
        }
    }

    private void buscarYAgregarLocales(NodoSentencia sentencia, String alcance) {
        if (sentencia == null)
            return;
        if (sentencia instanceof NodoDeclaracion) {
            for (VarLocal varLocal : ((NodoDeclaracion) sentencia).getVariables()) {
                symbolTableModel.addRow(new Object[] {
                        varLocal.getToken().getLexema(), "Variable Local", varLocal.getTipoVar().getNombreClase(),
                        alcance, (varLocal.getValor() != null ? varLocal.getValor() : "-")
                });
            }
        } else if (sentencia instanceof NodoBloque) {
            for (NodoSentencia s : ((NodoBloque) sentencia).getSentencias()) {
                buscarYAgregarLocales(s, alcance);
            }
        } else if (sentencia instanceof NodoIf) {
            buscarYAgregarLocales(((NodoIf) sentencia).getSentencia(), alcance);
            if (((NodoIf) sentencia).getSentenciaElse() != null) {
                buscarYAgregarLocales(((NodoIf) sentencia).getSentenciaElse(), alcance);
            }
        } else if (sentencia instanceof NodoWhile) {
            buscarYAgregarLocales(((NodoWhile) sentencia).getSentencia(), alcance);
        } else if (sentencia instanceof NodoFor) {
            buscarYAgregarLocales(((NodoFor) sentencia).getInicializacion(), alcance);
            buscarYAgregarLocales(((NodoFor) sentencia).getCuerpo(), alcance);
        } else if (sentencia instanceof NodoSentenciaCompuesta) {
            for (NodoSentencia s : ((NodoSentenciaCompuesta) sentencia).getSentencias()) {
                buscarYAgregarLocales(s, alcance);
            }
        }
    }

    private String generarSugerencia(ExcepcionSintactica ex) {
        String esperado = ex.getEsperado();
        String encontrado = ex.getEncontrado();
        if (esperado.contains(";") && !encontrado.equals(";"))
            return "¿Olvidaste un punto y coma al final de la sentencia anterior?";
        return "Revisa la estructura general cerca de '" + ex.getEncontrado() + "'.";
    }

    private void poblarTablaTokens(List<Token> tokens) {
        tokenTableModel.setRowCount(0);
        if (tokens == null)
            return;
        for (Token token : tokens) {
            tokenTableModel.addRow(new Object[] {
                    token.getNombre(), token.getLexema(), token.getLinea(), token.getColumna()
            });
        }
    }

    private void poblarArbolSintactico(NodoPrograma ast) {
        if (ast == null) {
            jTreeSintactico.setModel(new DefaultTreeModel(new DefaultMutableTreeNode("Programa (AST no generado)")));
            return;
        }
        DefaultMutableTreeNode root = crearNodosGUI(ast);
        jTreeSintactico.setModel(new DefaultTreeModel(root));
        for (int i = 0; i < jTreeSintactico.getRowCount(); i++) {
            jTreeSintactico.expandRow(i);
        }
    }

    private DefaultMutableTreeNode crearNodosGUI(Object nodoAst) {
        if (nodoAst == null)
            return null;
        DefaultMutableTreeNode guiNode = new DefaultMutableTreeNode(nodoAst);
        if (nodoAst instanceof NodoPrograma) {
            for (NodoClase c : ((NodoPrograma) nodoAst).getClases())
                guiNode.add(crearNodosGUI(c));
        } else if (nodoAst instanceof NodoClase) {
            NodoClase n = (NodoClase) nodoAst;
            if (!n.getAtributos().isEmpty()) {
                DefaultMutableTreeNode attrsNode = new DefaultMutableTreeNode("Atributos");
                for (NodoAtributo a : n.getAtributos())
                    attrsNode.add(crearNodosGUI(a));
                guiNode.add(attrsNode);
            }
            if (!n.getConstructores().isEmpty()) {
                DefaultMutableTreeNode ctorsNode = new DefaultMutableTreeNode("Constructores");
                for (NodoSentencia c : n.getConstructores())
                    ctorsNode.add(crearNodosGUI(c));
                guiNode.add(ctorsNode);
            }
            if (!n.getMetodos().isEmpty()) {
                DefaultMutableTreeNode methodsNode = new DefaultMutableTreeNode("Métodos");
                for (NodoMetodo m : n.getMetodos())
                    methodsNode.add(crearNodosGUI(m));
                guiNode.add(methodsNode);
            }
        } else if (nodoAst instanceof NodoMetodo) {
            NodoMetodo n = (NodoMetodo) nodoAst;
            if (!n.getParametros().isEmpty()) {
                DefaultMutableTreeNode paramsNode = new DefaultMutableTreeNode("Parámetros");
                for (VarParametro p : n.getParametros())
                    paramsNode.add(crearNodosGUI(p));
                guiNode.add(paramsNode);
            }
            guiNode.add(crearNodosGUI(n.getBloque()));
        } else if (nodoAst instanceof NodoBloque) {
            for (NodoSentencia s : ((NodoBloque) nodoAst).getSentencias())
                guiNode.add(crearNodosGUI(s));
        } else if (nodoAst instanceof NodoSentenciaCompuesta) {
            for (NodoSentencia s : ((NodoSentenciaCompuesta) nodoAst).getSentencias())
                guiNode.add(crearNodosGUI(s));
        } else if (nodoAst instanceof NodoDeclaracion) {
            for (VarLocal v : ((NodoDeclaracion) nodoAst).getVariables())
                guiNode.add(crearNodosGUI(v));
        } else if (nodoAst instanceof NodoAsignacion) {
            guiNode.add(crearNodosGUI(((NodoAsignacion) nodoAst).getLadoIzq()));
            guiNode.add(crearNodosGUI(((NodoAsignacion) nodoAst).getLadoDer()));
        } else if (nodoAst instanceof NodoIf) {
            guiNode.add(crearNodosGUI(((NodoIf) nodoAst).getCondicion()));
            guiNode.add(crearNodosGUI(((NodoIf) nodoAst).getSentencia()));
            if (((NodoIf) nodoAst).getSentenciaElse() != null)
                guiNode.add(crearNodosGUI(((NodoIf) nodoAst).getSentenciaElse()));
        } else if (nodoAst instanceof NodoWhile) {
            guiNode.add(crearNodosGUI(((NodoWhile) nodoAst).getCondicion()));
            guiNode.add(crearNodosGUI(((NodoWhile) nodoAst).getSentencia()));
        } else if (nodoAst instanceof NodoReturn) {
            if (((NodoReturn) nodoAst).getExpresion() != null)
                guiNode.add(crearNodosGUI(((NodoReturn) nodoAst).getExpresion()));
        } else if (nodoAst instanceof NodoExpresionBinaria) {
            guiNode.add(crearNodosGUI(((NodoExpresionBinaria) nodoAst).getLadoIzq()));
            guiNode.add(crearNodosGUI(((NodoExpresionBinaria) nodoAst).getLadoDer()));
        } else if (nodoAst instanceof NodoExpresionUnaria) {
            guiNode.add(crearNodosGUI(((NodoExpresionUnaria) nodoAst).getLadoDer()));
        } else if (nodoAst instanceof NodoSentenciaLlamada) {
            guiNode.add(crearNodosGUI(((NodoSentenciaLlamada) nodoAst).getLlamada()));
        } else if (nodoAst instanceof NodoExpParentizada) {
            guiNode.add(crearNodosGUI(((NodoExpParentizada) nodoAst).getExpresion()));
        }
        return guiNode;
    }

    private void actualizarStatus(List<Token> tokens) {
        if (tokens == null)
            return;
        int palabrasReservadas = 0;
        int identificadores = 0;
        int lineas = 0;
        for (Token token : tokens) {
            if (token.getNombre().startsWith("PR_"))
                palabrasReservadas++;
            else if (token.getNombre().equals("idClase") || token.getNombre().equals("idMetVar"))
                identificadores++;
            if (token.getLinea() > lineas)
                lineas = token.getLinea();
        }
        jLabelStatus.setText(String.format(" Palabras reservadas: %d | Identificadores: %d | Líneas: %d",
                palabrasReservadas, identificadores, lineas));
    }

    private void resaltarLineaError(int linea) {
        try {
            if (linea <= 0)
                return;
            Element root = jTextPaneCodigo.getDocument().getDefaultRootElement();
            Element lineElement = root.getElement(linea - 1);
            if (lineElement != null) {
                int startIndex = lineElement.getStartOffset();
                int endIndex = lineElement.getEndOffset();
                Highlighter.HighlightPainter painter = new DefaultHighlighter.DefaultHighlightPainter(
                        new Color(255, 99, 71, 100));
                jTextPaneCodigo.getHighlighter().addHighlight(startIndex, endIndex, painter);
            }
        } catch (BadLocationException e) {
            // Ignorar si la línea no se puede resaltar
        }
    }

    private void nuevoArchivo() {
        limpiarResultados();
        jTextPaneCodigo.setText("");
        archivoActual = null;
        this.setTitle("Zyra IDE - Sin Título");
        undoManager.discardAllEdits();
        syntaxHighlighter.colorear();
    }

    private void abrirArchivo() {
        JFileChooser chooser = new JFileChooser();
        try {
            File testDir = new File(new File(".").getCanonicalPath() + "/test");
            if (testDir.exists())
                chooser.setCurrentDirectory(testDir);
        } catch (IOException e) {
        }
        chooser.setFileSelectionMode(JFileChooser.FILES_ONLY);
        chooser.setFileFilter(new FileNameExtensionFilter("Archivos de texto Zyra", "txt"));
        if (chooser.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
            archivoActual = chooser.getSelectedFile();
            try {
                String contenido = new String(Files.readAllBytes(archivoActual.toPath()));
                limpiarResultados();
                jTextPaneCodigo.setText(contenido);
                this.setTitle("Zyra IDE - " + archivoActual.getName());
                undoManager.discardAllEdits();
                syntaxHighlighter.colorear();
            } catch (IOException ex) {
                JOptionPane.showMessageDialog(this, "Error al leer el archivo: " + ex.getMessage(), "Error de Archivo",
                        JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    private void guardarArchivo() {
        if (archivoActual == null) {
            guardarComoArchivo();
        } else {
            try (FileWriter fw = new FileWriter(archivoActual)) {
                fw.write(jTextPaneCodigo.getText());
                jTextAreaErrores.append("\nArchivo guardado exitosamente: " + archivoActual.getName());
            } catch (IOException ex) {
                JOptionPane.showMessageDialog(this, "Error al guardar el archivo: " + ex.getMessage(),
                        "Error de Archivo", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    private void guardarComoArchivo() {
        JFileChooser chooser = new JFileChooser();
        chooser.setFileFilter(new FileNameExtensionFilter("Archivos de texto Zyra", "txt"));
        if (chooser.showSaveDialog(this) == JFileChooser.APPROVE_OPTION) {
            archivoActual = chooser.getSelectedFile();
            if (!archivoActual.getName().toLowerCase().endsWith(".txt")) {
                archivoActual = new File(archivoActual.getParentFile(), archivoActual.getName() + ".txt");
            }
            this.setTitle("Zyra IDE - " + archivoActual.getName());
            guardarArchivo();
        }
    }

    private void salirAplicacion() {
        System.exit(0);
    }

    private void mostrarManual() {
        JOptionPane.showMessageDialog(this, "Manual no disponible.", "Manual", JOptionPane.INFORMATION_MESSAGE);
    }

    private void mostrarAcercaDe() {
        JOptionPane.showMessageDialog(this, "Zyra IDE - Compilador de MiniJava", "Acerca de",
                JOptionPane.PLAIN_MESSAGE);
    }

    public static void main(String args[]) {
        try {
            // UIManager.setLookAndFeel(new FlatDarkLaf()); // Comentado - usando Look and
            // Feel por defecto
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception ex) {
            System.err.println("Failed to initialize LaF");
        }

        java.awt.EventQueue.invokeLater(() -> {
            new Main().setVisible(true);
        });
    }
}
