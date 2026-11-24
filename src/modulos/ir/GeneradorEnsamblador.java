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

    public String generar(GeneradorCodigoIntermedio gci) {
        this.gci = gci;
        codigo.setLength(0);
        variableOffsets.clear();
        offsetActual = 8;
        stringLabels.clear();
        stringCounter = 0;

        // Encontrar el código de main (hasta primer return)
        List<Cuadruplo> codigoMain = new ArrayList<>();
        for (Cuadruplo c : gci.getCuadruplos()) {
            codigoMain.add(c);
            if (c.getOperador().equals("return")) {
                break;
            }
        }

        int tamano = calcularTamano(codigoMain);

        // Header
        codigo.append("bits 64\n");
        codigo.append("default rel\n\n");

        // Extern para I/O
        codigo.append("extern printf\n");
        codigo.append("extern scanf\n");
        codigo.append("extern malloc\n\n");

        // Data section con formatos y strings
        codigo.append("section .data\n");
        codigo.append("    fmt_int db '%d', 10, 0  ; Formato para imprimir int con newline\n");
        codigo.append("    fmt_str db '%s', 10, 0  ; Formato para imprimir string con newline\n");
        codigo.append("    fmt_scan db '%d', 0      ; Formato para scanf\n");

        // Agregar strings literales recolectados
        for (Cuadruplo c : codigoMain) {
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

        // Code section
        codigo.append("section .text\n");
        codigo.append("global main\n\n");

        // Main
        codigo.append("; ========== main con I/O ==========\n");
        codigo.append("main:\n");
        codigo.append("    push rbp\n");
        codigo.append("    mov rbp, rsp\n");
        if (tamano > 0) {
            codigo.append(String.format("    sub rsp, %d\n", tamano));
        }
        codigo.append("\n");

        // Generar código
        for (Cuadruplo c : codigoMain) {
            String op = c.getOperador();

            if (op.equals("param")) {
                comentario("SKIP: " + c.toReadableString());
                continue;
            }

            if (op.equals("call")) {
                comentario("INLINE: " + c.toReadableString());
                if (c.getResultado() != null && !c.getResultado().isEmpty()) {
                    gen("xor rax, rax");
                    gen("mov " + loc(c.getResultado()) + ", rax");
                }
                continue;
            }

            if (op.equals("return")) {
                continue;
            }

            // PRINT: llamada a printf
            if (op.equals("print")) {
                generarPrint(c);
                continue;
            }

            // PRINT_STR: imprimir string
            if (op.equals("print_str")) {
                generarPrintString(c);
                continue;
            }

            // READ: llamada a scanf
            if (op.equals("read")) {
                generarRead(c);
                continue;
            }

            // NEW_ARRAY: asignación de memoria para array
            if (op.equals("new_array")) {
                genNewArray(c);
                continue;
            }

            traducir(c);
        }

        // Epílogo
        codigo.append("\n    mov rsp, rbp\n");
        codigo.append("    pop rbp\n");
        codigo.append("    xor rax, rax\n");
        codigo.append("    ret\n");

        return codigo.toString();
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
