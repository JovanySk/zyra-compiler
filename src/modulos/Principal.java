// Contenido completo, FINAL y CORREGIDO
package modulos;

import Vistas.Main;
import estructuras.Token;
import excepciones.lexicas.ExcepcionLexica;
import excepciones.semanticas.ExcepcionSemantica;
import excepciones.sintacticas.ExcepcionSintactica;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.List;

public class Principal {

    private Main m;

    public Principal(String direccion, Main m) {
        this.m = m;
        leerArchivo(direccion);
    }

    public void leerArchivo(String direccion) {
        try {
            File archEntrada = new File(direccion);
            if (!archEntrada.exists()) {
                System.out.println("[Error] No existe el archivo de entrada especificado.");
                return;
            }

            String salida = direccion.endsWith(".txt")
                    ? direccion.substring(0, direccion.length() - 4) + ".asm"
                    : direccion + ".asm";

            System.out.println("[Advertencia] Se utilizara la siguiente salida: \"" + salida + "\".");

            File archSalida = new File(salida);
            if (!archSalida.exists()) {
                archSalida.createNewFile();
            }

            BufferedWriter bw = new BufferedWriter(new FileWriter(archSalida));
            GenCod.setBuffer(bw);
            BufferedReader br = new BufferedReader(new FileReader(archEntrada));

            // --- Fase Léxica ---
            AnalizadorLexico lexico = new AnalizadorLexico(br);
            List<Token> tokens = lexico.getAllTokens();

            // --- Fase Sintáctica ---
            AnalizadorSintactico asintactico = new AnalizadorSintactico(tokens);
            asintactico.analizar();

            System.out.println("Analisis Sintactico... ¡Correcto!");

            // --- Fases Semánticas ---
            asintactico.getTablaSimbolos().chequearDeclaraciones();
            asintactico.getAST().chequear();
            System.out.println("Analisis Semantico... ¡Correcto!");

            // --- Fase de Generación de Código Intermedio ---
            modulos.ir.GeneradorCodigoIntermedio gci = new modulos.ir.GeneradorCodigoIntermedio();
            asintactico.getAST().generar(gci);

            // TODO: Futuro - aplicar optimizaciones aquí
            // OptimizadorIR optimizador = new PropagacionConstantes();
            // optimizador.optimizar(gci);

            // TODO: Fase 6 - Generar ensamblador desde IR
            // Por ahora, mantenemos la generación directa para compatibilidad
            System.out.println("Generacion de Codigo Intermedio... ¡Correcto!");
            System.out.println("  - Cuadruplos generados: " + gci.getCantidadCuadruplos());

            GenCod.close();
            System.out.println("Generacion de Codigo... ¡Correcto!");

        } catch (ExcepcionSintactica e) {
            System.out.println("No se pudo completar el analisis sintactico: ¡FALLÓ!");
            System.out.println(e.getMessage());
        } catch (IOException e1) {
            System.out.println("Error de archivos: " + e1.getMessage());
        } catch (ExcepcionLexica e2) {
            System.out.println("No se pudo completar el analisis lexico: " + e2.getMessage());
        } catch (ExcepcionSemantica e4) {
            System.out.println("No se pudo completar el analisis semantico: " + e4.getMessage());
        } catch (Exception e5) {
            System.out.println("Se produjo un error inesperado: " + e5.getMessage());
            e5.printStackTrace();
        }
    }
}
