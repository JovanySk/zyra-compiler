package modulos.ir;

import java.util.List;

/**
 * FormateadorIR - Formatea código intermedio para visualización
 * 
 * Esta clase proporciona métodos para convertir cuádruplos y triplos
 * a representaciones legibles para humanos, optimizadas para mostrar
 * en la interfaz gráfica del IDE.
 * 
 * @author Zyra Compiler Team
 */
public class FormateadorIR {

    /**
     * Formatea la lista de cuádruplos en una tabla legible
     * 
     * @param cuadruplos Lista de cuádruplos a formatear
     * @return String formateado para visualización
     */
    public static String formatearCuadruplos(List<Cuadruplo> cuadruplos) {
        StringBuilder sb = new StringBuilder();

        // Encabezado
        sb.append("╔══════════════════════════════════════════════════════════════════════════╗\n");
        sb.append("║                        CUÁDRUPLOS (Quadruples)                           ║\n");
        sb.append("╚══════════════════════════════════════════════════════════════════════════╝\n\n");

        if (cuadruplos == null || cuadruplos.isEmpty()) {
            sb.append("  [No se generaron cuádruplos]\n");
            return sb.toString();
        }

        // Tabla de cuádruplos
        sb.append(String.format("%-5s │ %-55s │ %s\n",
                "Núm", "Representación Formal", "Código Legible"));
        sb.append("─".repeat(5)).append("─┼─");
        sb.append("─".repeat(55)).append("─┼─");
        sb.append("─".repeat(60)).append("\n");

        for (int i = 0; i < cuadruplos.size(); i++) {
            Cuadruplo c = cuadruplos.get(i);
            sb.append(String.format("%-5d │ %-55s │ %s\n",
                    i,
                    c.toString(),
                    c.toReadableString()));
        }

        sb.append("\n");
        sb.append("═".repeat(90)).append("\n");
        sb.append(String.format("Total de cuádruplos generados: %d\n", cuadruplos.size()));

        // Estadísticas
        int asignaciones = 0, operaciones = 0, saltos = 0, etiquetas = 0;
        for (Cuadruplo c : cuadruplos) {
            if (c.esAsignacion())
                asignaciones++;
            else if (c.esOperacionAritmetica() || c.esOperacionLogica() || c.esComparacion())
                operaciones++;
            else if (c.esSalto())
                saltos++;
            else if (c.esEtiqueta())
                etiquetas++;
        }

        sb.append(String.format("  - Asignaciones: %d\n", asignaciones));
        sb.append(String.format("  - Operaciones: %d\n", operaciones));
        sb.append(String.format("  - Saltos: %d\n", saltos));
        sb.append(String.format("  - Etiquetas: %d\n", etiquetas));

        return sb.toString();
    }

    /**
     * Formatea la lista de triplos en una tabla legible
     * 
     * @param triplos Lista de triplos a formatear
     * @return String formateado para visualización
     */
    public static String formatearTriplos(List<Triplo> triplos) {
        StringBuilder sb = new StringBuilder();

        // Encabezado
        sb.append("╔══════════════════════════════════════════════════════════════════════════╗\n");
        sb.append("║                          TRIPLOS (Triples)                               ║\n");
        sb.append("╚══════════════════════════════════════════════════════════════════════════╝\n\n");

        if (triplos == null || triplos.isEmpty()) {
            sb.append("  [No se generaron triplos]\n");
            return sb.toString();
        }

        // Tabla de triplos
        sb.append(String.format("%-10s │ %-50s │ %s\n",
                "Núm", "Representación Formal", "Código Legible"));
        sb.append("─".repeat(10)).append("─┼─");
        sb.append("─".repeat(50)).append("─┼─");
        sb.append("─".repeat(60)).append("\n");

        for (Triplo t : triplos) {
            sb.append(String.format("%-10s │ %-50s │ %s\n",
                    "(" + t.getNumero() + ")",
                    t.toString(),
                    t.toReadableString()));
        }

        sb.append("\n");
        sb.append("═".repeat(90)).append("\n");
        sb.append(String.format("Total de triplos generados: %d\n", triplos.size()));

        // Información adicional
        int referencias = 0;
        for (Triplo t : triplos) {
            if (t.arg1EsReferencia())
                referencias++;
            if (t.arg2EsReferencia())
                referencias++;
        }
        sb.append(String.format("  - Referencias indirectas: %d\n", referencias));

        return sb.toString();
    }

    /**
     * Formatea el código de tres direcciones (TAC) en formato compacto
     * 
     * @param cuadruplos Lista de cuádruplos
     * @return String formateado como código TAC
     */
    public static String formatearCodigoTresDirecciones(List<Cuadruplo> cuadruplos) {
        StringBuilder sb = new StringBuilder();

        // Encabezado
        sb.append("╔══════════════════════════════════════════════════════════════════════════╗\n");
        sb.append("║                  CÓDIGO DE TRES DIRECCIONES (TAC)                        ║\n");
        sb.append("╚══════════════════════════════════════════════════════════════════════════╝\n\n");

        if (cuadruplos == null || cuadruplos.isEmpty()) {
            sb.append("  [No se generó código intermedio]\n");
            return sb.toString();
        }

        // Código TAC con numeración
        for (int i = 0; i < cuadruplos.size(); i++) {
            String codigo = cuadruplos.get(i).toReadableString();

            // Formatear etiquetas y saltos con indentación
            if (cuadruplos.get(i).esEtiqueta()) {
                sb.append(String.format("%3d:  %s\n", i, codigo));
            } else if (cuadruplos.get(i).esSalto()) {
                sb.append(String.format("%3d:      %s\n", i, codigo));
            } else {
                sb.append(String.format("%3d:      %s\n", i, codigo));
            }
        }

        sb.append("\n");
        return sb.toString();
    }

    /**
     * Formatea todas las representaciones juntas
     * 
     * @param gci Generador de código intermedio
     * @return String con TAC, Cuádruplos y Triplos
     */
    public static String formatearTodo(GeneradorCodigoIntermedio gci) {
        StringBuilder sb = new StringBuilder();

        sb.append(formatearCodigoTresDirecciones(gci.getCuadruplos()));
        sb.append("\n\n");
        sb.append(formatearCuadruplos(gci.getCuadruplos()));
        sb.append("\n\n");
        sb.append(formatearTriplos(gci.getTriplos()));

        return sb.toString();
    }

    /**
     * Genera un reporte compacto para logging
     */
    public static String generarReporteCompacto(GeneradorCodigoIntermedio gci) {
        return String.format(
                "IR Generado: %d cuádruplos, %d triplos",
                gci.getCantidadCuadruplos(),
                gci.getCantidadTriplos());
    }
}
