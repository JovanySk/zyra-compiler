package modulos.ir;

import java.util.*;

/**
 * GeneradorEnsamblador COMPLETO - Con soporte print/read
 */
public class GeneradorEnsamblador {

    private GeneradorCodigoIntermedio gci;
    private Map<String, Integer> variableOffsets;
    private int offsetActual;
    private StringBuilder codigo;
    private Map<String, String> stringLabels;
    private int stringCounter;

    public GeneradorEnsamblador() {
        this.variableOffsets = new HashMap<>();
        this.offsetActual = 8;
        this.codigo = new StringBuilder();
        this.stringLabels = new HashMap<>();
        this.stringCounter = 0;
    }

    /** Registros de argumentos de la convención Windows x64. */
    private static final String[] ARG_REGS = { "rcx", "rdx", "r8", "r9" };

    /** Parámetros pendientes (cuádruplos "param") antes de un "call". */
    private final Deque<String> paramsPendientes = new ArrayDeque<>();

    /** Funciones definidas en el programa (nombre simple -> etiqueta NASM). */
    private final Map<String, String> funciones = new HashMap<>();

    /** Etiqueta de salida de la función que se está generando. */
    private String etiquetaRetorno;

    public String generar(GeneradorCodigoIntermedio gci) {
        this.gci = gci;
        codigo.setLength(0);
        stringLabels.clear();
        stringCounter = 0;
        funciones.clear();

        List<Cuadruplo> todos = gci.getCuadruplos();

        // 1. Separar el IR en funciones usando BEGIN_FUNCTION / END_FUNCTION
        List<String> nombres = new ArrayList<>();
        List<String> params = new ArrayList<>();
        List<List<Cuadruplo>> cuerpos = new ArrayList<>();
        List<Cuadruplo> actual = null;
        for (Cuadruplo c : todos) {
            if (c.getOperador().equals("BEGIN_FUNCTION")) {
                nombres.add(c.getArg1());
                params.add(c.getArg2());
                actual = new ArrayList<>();
                cuerpos.add(actual);
            } else if (c.getOperador().equals("END_FUNCTION")) {
                actual = null;
            } else if (actual != null) {
                actual.add(c);
            }
        }
        if (nombres.isEmpty()) {
            // IR sin marcadores: todo se trata como main
            nombres.add("main");
            params.add("");
            cuerpos.add(new ArrayList<>(todos));
        }
        for (String n : nombres) {
            funciones.put(n, n.equals("main") ? "main" : "fn_" + n.replaceAll("[^A-Za-z0-9_]", "_"));
        }

        // 2. Cabecera y sección de datos
        codigo.append("bits 64\n");
        codigo.append("default rel\n\n");
        codigo.append("extern printf\n");
        codigo.append("extern scanf\n");
        codigo.append("extern malloc\n\n");
        codigo.append("section .data\n");
        codigo.append("    fmt_int db '%d', 10, 0  ; Formato para imprimir int con newline\n");
        codigo.append("    fmt_str db '%s', 10, 0  ; Formato para imprimir string con newline\n");
        codigo.append("    fmt_scan db '%d', 0      ; Formato para scanf\n");
        for (Cuadruplo c : todos) {
            if (c.getOperador().equals("print_str")) {
                String texto = c.getArg1();
                if (!stringLabels.containsKey(texto)) {
                    String label = "str" + stringCounter++;
                    stringLabels.put(texto, label);
                    codigo.append(String.format("    %s db '%s', 0\n", label, escaparString(texto)));
                }
            }
        }
        codigo.append("\n");

        // 3. Código: main primero, después el resto de funciones
        codigo.append("section .text\n");
        codigo.append("global main\n");
        int iMain = nombres.indexOf("main");
        if (iMain >= 0) {
            generarFuncion(nombres.get(iMain), params.get(iMain), cuerpos.get(iMain));
        }
        for (int i = 0; i < nombres.size(); i++) {
            if (i != iMain) {
                generarFuncion(nombres.get(i), params.get(i), cuerpos.get(i));
            }
        }
        return codigo.toString();
    }

    /**
     * Genera una función x86-64 completa: prólogo, copia de parámetros desde
     * rcx/rdx/r8/r9 a la pila, cuerpo y epílogo con el valor de retorno en rax.
     */
    private void generarFuncion(String nombre, String listaParams, List<Cuadruplo> cuerpo) {
        variableOffsets.clear();
        offsetActual = 8;
        paramsPendientes.clear();
        String etiqueta = funciones.get(nombre);
        etiquetaRetorno = ".ret_" + etiqueta;
        boolean esMain = nombre.equals("main");

        String[] ps = (listaParams == null || listaParams.isEmpty()) ? new String[0] : listaParams.split(",");
        List<Cuadruplo> paraTamano = new ArrayList<>(cuerpo);
        for (String p : ps) {
            paraTamano.add(new Cuadruplo("=", p, "", p));
        }
        int tamano = calcularTamano(paraTamano);

        codigo.append("\n; ========== ").append(nombre).append("(").append(listaParams == null ? "" : listaParams)
                .append(") ==========\n");
        codigo.append(etiqueta).append(":\n");
        gen("push rbp");
        gen("mov rbp, rsp");
        if (tamano > 0) {
            gen("sub rsp, " + tamano);
        }
        for (int i = 0; i < ps.length; i++) {
            if (i < ARG_REGS.length) {
                gen("mov " + loc(ps[i]) + ", " + ARG_REGS[i] + "    ; parámetro " + ps[i]);
            } else {
                comentario("AVISO: solo se soportan 4 parámetros por registro; " + ps[i] + " se ignora");
            }
        }
        codigo.append("\n");

        for (Cuadruplo c : cuerpo) {
            String op = c.getOperador();
            if (op.equals("param")) {
                comentario(c.toReadableString());
                paramsPendientes.addLast(c.getArg1());
                continue;
            }
            if (op.equals("call")) {
                generarLlamada(c);
                continue;
            }
            if (op.equals("return")) {
                comentario(c.toReadableString());
                if (c.getArg1() != null && !c.getArg1().isEmpty()) {
                    cargar("rax", c.getArg1());
                } else {
                    gen("xor rax, rax");
                }
                gen("jmp " + etiquetaRetorno);
                continue;
            }
            if (op.equals("print")) {
                generarPrint(c);
                continue;
            }
            if (op.equals("print_str")) {
                generarPrintString(c);
                continue;
            }
            if (op.equals("read")) {
                generarRead(c);
                continue;
            }
            if (op.equals("new_array")) {
                genNewArray(c);
                continue;
            }
            traducir(c);
        }

        if (esMain) {
            gen("xor rax, rax");
        }
        codigo.append(etiquetaRetorno).append(":\n");
        gen("mov rsp, rbp");
        gen("pop rbp");
        gen("ret");
    }

    /**
     * call f, n: toma los últimos n parámetros pendientes, los carga en
     * rcx/rdx/r8/r9, reserva el shadow space y guarda rax en el temporal destino.
     */
    private void generarLlamada(Cuadruplo c) {
        comentario(c.toReadableString());
        String objetivo = c.getArg1();
        String simple = objetivo.contains(".") ? objetivo.substring(objetivo.lastIndexOf('.') + 1) : objetivo;
        int n = esNum(c.getArg2()) ? Integer.parseInt(c.getArg2()) : 0;

        List<String> args = new ArrayList<>();
        for (int i = 0; i < n && !paramsPendientes.isEmpty(); i++) {
            args.add(0, paramsPendientes.removeLast());
        }

        String etiqueta = funciones.get(simple);
        if (etiqueta == null) {
            comentario("AVISO: '" + objetivo + "' no tiene código nativo (métodos de instancia/constructores)");
            if (c.getResultado() != null && !c.getResultado().isEmpty()) {
                gen("xor rax, rax");
                gen("mov " + loc(c.getResultado()) + ", rax");
            }
            return;
        }
        for (int i = 0; i < args.size() && i < ARG_REGS.length; i++) {
            cargar(ARG_REGS[i], args.get(i));
        }
        gen("sub rsp, 32");
        gen("call " + etiqueta);
        gen("add rsp, 32");
        if (c.getResultado() != null && !c.getResultado().isEmpty()) {
            gen("mov " + loc(c.getResultado()) + ", rax");
        }
    }

    private void generarPrint(Cuadruplo c) {
        comentario(c.toReadableString());

        // Primer parámetro (rcx): dirección del formato
        gen("lea rcx, [fmt_int]");

        // Segundo parámetro (rd x): valor a imprimir
        if (esNum(c.getArg1())) {
            gen("mov rdx, " + c.getArg1());
        } else {
            gen("mov rdx, " + loc(c.getArg1()));
        }

        // Shadow space y llamada
        gen("sub rsp, 32");
        gen("call printf");
        gen("add rsp, 32");
    }

    private void generarPrintString(Cuadruplo c) {
        comentario(c.toReadableString());

        String texto = c.getArg1();
        String label = stringLabels.get(texto);

        // Primer parámetro (rcx): formato para string
        gen("lea rcx, [fmt_str]");

        // Segundo parámetro (rdx): dirección del string
        gen("lea rdx, [" + label + "]");

        // Shadow space y llamada
        gen("sub rsp, 32");
        gen("call printf");
        gen("add rsp, 32");
    }

    private void generarRead(Cuadruplo c) {
        comentario(c.toReadableString());

        // Primer parámetro: formato
        gen("lea rcx, [fmt_scan]");

        // Segundo parámetro: dirección donde guardar
        gen("lea rdx, " + loc(c.getResultado()));

        // Shadow space
        gen("sub rsp, 32");
        gen("call scanf");
        gen("add rsp, 32");
    }

    private void genLecturaArray(Cuadruplo c) {
        // temp = array[indice]
        // c.getArg1() = array, c.getArg2() = indice, c.getResultado() = temp
        comentario(c.toReadableString());

        String array = c.getArg1();
        String indice = c.getArg2();
        String resultado = c.getResultado();

        // Cargar índice en rcx
        cargar("rcx", indice);

        // Multiplicar índice por 8 (tamaño de int en x64)
        gen("imul rcx, 8");

        // Cargar PUNTERO del array (el array está en heap)
        // NO usar lea, usar mov para obtener el valor del puntero
        gen("mov rax, " + loc(array));

        // Sumar offset: rax = base + (indice * 8)
        gen("add rax, rcx");

        // Leer valor en rax y guardarlo en resultado
        gen("mov rax, [rax]");
        gen("mov " + loc(resultado) + ", rax");
    }

    private void genEscrituraArray(Cuadruplo c) {
        // array[indice] = valor
        // c.getArg1() = array, c.getArg2() = indice, c.getResultado() = valor
        comentario(c.toReadableString());

        String array = c.getArg1();
        String indice = c.getArg2();
        String valor = c.getResultado();

        // Cargar valor en rdx
        cargar("rdx", valor);

        // Cargar índice en rcx
        cargar("rcx", indice);

        // Multiplicar índice por 8
        gen("imul rcx, 8");

        // Cargar PUNTERO del array (el array está en heap)
        // NO usar lea, usar mov para obtener el valor del puntero
        gen("mov rax, " + loc(array));

        // Sumar offset: rax = base + (indice * 8)
        gen("add rax, rcx");

        // Escribir valor en la posición
        gen("mov [rax], rdx");
    }

    private void genNewArray(Cuadruplo c) {
        // new_array: resultado = new tipo[tamaño]
        // c.getArg1() = tipo, c.getArg2() = tamaño, c.getResultado() = variable destino
        comentario(c.toReadableString());

        String tamano = c.getArg2();
        String resultado = c.getResultado();

        // Cargar tamaño en rcx
        cargar("rcx", tamano);

        // Multiplicar por 8 (tamaño de cada elemento)
        gen("imul rcx, 8");

        // Llamar malloc(rcx)
        gen("sub rsp, 32"); // Shadow space
        gen("call malloc");
        gen("add rsp, 32");

        // malloc retorna puntero en rax, guardarlo
        gen("mov " + loc(resultado) + ", rax");
    }

    private int calcularTamano(List<Cuadruplo> cuads) {
        Set<String> vars = new HashSet<>();
        for (Cuadruplo c : cuads) {
            agregar(vars, c.getArg1());
            agregar(vars, c.getArg2());
            agregar(vars, c.getResultado());
        }
        int t = vars.size() * 8;
        return ((t + 15) / 16) * 16;
    }

    private void agregar(Set<String> vars, String val) {
        if (val != null && !val.isEmpty() && !esNum(val) && !esLabel(val)) {
            vars.add(val);
        }
    }

    private void traducir(Cuadruplo c) {
        switch (c.getOperador()) {
            case "=":
                genAsig(c);
                break;
            case "+":
            case "-":
            case "*":
            case "/":
                genArit(c);
                break;
            case "<":
            case ">":
            case "<=":
            case ">=":
            case "==":
            case "!=":
                genCmp(c);
                break;
            case "label":
                codigo.append(c.getResultado()).append(":\n");
                break;
            case "goto":
                comentario(c.toReadableString());
                gen("jmp " + c.getResultado());
                break;
            case "if_false":
                genIfF(c);
                break;
            case "if_true":
                genIfT(c);
                break;
            case "=[]":
                // Lectura de array: temp = array[índice]
                genLecturaArray(c);
                break;
            case "[]=":
                // Escritura en array: array[índice] = valor
                genEscrituraArray(c);
                break;
            default:
                comentario("SKIP: " + c.toString());
                break;
        }
    }

    private void genAsig(Cuadruplo c) {
        comentario(c.toReadableString());
        cargar("rax", c.getArg1());
        gen("mov " + loc(c.getResultado()) + ", rax");
    }

    private void genArit(Cuadruplo c) {
        comentario(c.toReadableString());
        cargar("rax", c.getArg1());

        switch (c.getOperador()) {
            case "+":
                if (esNum(c.getArg2()))
                    gen("add rax, " + c.getArg2());
                else
                    gen("add rax, " + loc(c.getArg2()));
                break;
            case "-":
                if (esNum(c.getArg2()))
                    gen("sub rax, " + c.getArg2());
                else
                    gen("sub rax, " + loc(c.getArg2()));
                break;
            case "*":
                cargar("rbx", c.getArg2());
                gen("imul rbx");
                break;
            case "/":
                gen("cqo");
                cargar("rbx", c.getArg2());
                gen("idiv rbx");
                break;
        }
        gen("mov " + loc(c.getResultado()) + ", rax");
    }

    private void genCmp(Cuadruplo c) {
        comentario(c.toReadableString());
        cargar("rax", c.getArg1());
        if (esNum(c.getArg2()))
            gen("cmp rax, " + c.getArg2());
        else
            gen("cmp rax, " + loc(c.getArg2()));

        String t = ".L" + System.nanoTime() + "t";
        String e = ".L" + System.nanoTime() + "e";

        switch (c.getOperador()) {
            case "<":
                gen("jl " + t);
                break;
            case ">":
                gen("jg " + t);
                break;
            case "<=":
                gen("jle " + t);
                break;
            case ">=":
                gen("jge " + t);
                break;
            case "==":
                gen("je " + t);
                break;
            case "!=":
                gen("jne " + t);
                break;
        }

        gen("mov rax, 0");
        gen("jmp " + e);
        codigo.append(t).append(":\n");
        gen("mov rax, 1");
        codigo.append(e).append(":\n");
        gen("mov " + loc(c.getResultado()) + ", rax");
    }

    private void genIfF(Cuadruplo c) {
        comentario(c.toReadableString());
        cargar("rax", c.getArg1());
        gen("cmp rax, 0");
        gen("je " + c.getResultado());
    }

    private void genIfT(Cuadruplo c) {
        comentario(c.toReadableString());
        cargar("rax", c.getArg1());
        gen("cmp rax, 0");
        gen("jne " + c.getResultado());
    }

    private void cargar(String reg, String val) {
        if (esNum(val))
            gen("mov " + reg + ", " + val);
        else
            gen("mov " + reg + ", " + loc(val));
    }

    private String loc(String nombre) {
        if (!variableOffsets.containsKey(nombre)) {
            variableOffsets.put(nombre, offsetActual);
            offsetActual += 8;
        }
        return "[rbp-" + variableOffsets.get(nombre) + "]";
    }

    private boolean esNum(String s) {
        if (s == null || s.isEmpty())
            return false;
        try {
            Integer.parseInt(s);
            return true;
        } catch (NumberFormatException e) {
            return false;
        }
    }

    private boolean esLabel(String s) {
        return s != null && (s.startsWith("L") || s.startsWith(".L"));
    }

    private void gen(String i) {
        codigo.append("    ").append(i).append("\n");
    }

    private void comentario(String t) {
        codigo.append(String.format("    ; %s\n", t));
    }

    private String escaparString(String s) {
        // Escapar caracteres especiales para sintaxis NASM
        // Solo escapar lo mínimo necesario
        return s.replace("'", "''"); // Duplicar apóstrofes para NASM
    }
}
