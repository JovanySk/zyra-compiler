// IMPLEMENTACIÓN COMPLETA con IR - Nodo crítico
package estructuras.ast.sentencias;

import estructuras.Token;
import estructuras.ast.expresiones.NodoExpresion;
import estructuras.ast.expresiones.NodoLiteral;
import estructuras.ast.expresiones.NodoVariable;
import estructuras.ast.expresiones.NodoAccesoArreglo;
import estructuras.ts.TablaSimbolos;
import estructuras.ts.tipos.TipoMetodo;
import estructuras.ts.variables.Variable;
import excepciones.semanticas.ExcepcionSemantica;
import excepciones.semanticas.ExcepcionSemanticaPersonalizada;
import java.util.LinkedList;
import modulos.GenCod;

public class NodoFor extends NodoSentencia {

    private final Token tokenFor;
    private final NodoSentencia inicializacion;
    private final NodoExpresion condicion;
    private final NodoSentencia actualizacion;
    private final NodoSentencia cuerpo;

    public NodoFor(Token token, NodoSentencia inicializacion, NodoExpresion condicion, NodoSentencia actualizacion,
            NodoSentencia cuerpo) {
        this.tokenFor = token;
        this.inicializacion = inicializacion;
        this.condicion = condicion;
        this.actualizacion = actualizacion;
        this.cuerpo = cuerpo;
    }

    public Token getTokenFor() {
        return tokenFor;
    }

    public NodoSentencia getInicializacion() {
        return inicializacion;
    }

    public NodoExpresion getCondicion() {
        return condicion;
    }

    public NodoSentencia getActualizacion() {
        return actualizacion;
    }

    public NodoSentencia getCuerpo() {
        return cuerpo;
    }

    @Override
    public void chequear() throws ExcepcionSemantica {
        // Desazucarado semántico: convertir for en bloque con while
        LinkedList<NodoSentencia> sentenciasWhile = new LinkedList<>();
        if (cuerpo != null)
            sentenciasWhile.add(cuerpo);
        if (actualizacion != null)
            sentenciasWhile.add(actualizacion);
        NodoBloque cuerpoWhile = new NodoBloque(sentenciasWhile);

        NodoExpresion cond = (condicion != null) ? condicion
                : new NodoLiteral(new Token("PR_True", "true", tokenFor.getLinea(), tokenFor.getColumna()));
        NodoWhile whileSintetico = new NodoWhile(tokenFor, cond, cuerpoWhile);

        LinkedList<NodoSentencia> sentenciasFor = new LinkedList<>();
        if (inicializacion != null)
            sentenciasFor.add(inicializacion);
        sentenciasFor.add(whileSintetico);

        NodoBloque bloqueFor = new NodoBloque(sentenciasFor);
        bloqueFor.chequear();
    }

    /**
     * Genera código intermedio para for
     * 
     * @param gci Generador de código intermedio
     */
    public void generar(modulos.ir.GeneradorCodigoIntermedio gci) {
        // Crear contexto de for con etiquetas
        modulos.ir.GeneradorCodigoIntermedio.ContextoFor ctx = gci.iniciarFor();

        // Inicialización
        if (inicializacion != null) {
            inicializacion.generar(gci);
        }

        // Etiqueta de condición
        gci.generarEtiqueta(ctx.etiquetaCondicion);

        // Evaluar condición (o true si no hay)
        if (condicion != null) {
            String cond = condicion.generar(gci);
            gci.generarIfFalse(cond, ctx.etiquetaFin);
        }

        // Etiqueta de cuerpo
        gci.generarEtiqueta(ctx.etiquetaCuerpo);

        // Cuerpo del for
        if (cuerpo != null) {
            cuerpo.generar(gci);
        }

        // Etiqueta de incremento (para continue)
        gci.generarEtiqueta(ctx.etiquetaIncremento);

        // Actualización
        if (actualizacion != null) {
            actualizacion.generar(gci);
        }

        // Saltar de vuelta a la condición
        gci.generarGoto(ctx.etiquetaCondicion);

        // Etiqueta de fin (para break)
        gci.generarEtiqueta(ctx.etiquetaFin);

        // Finalizar contexto
        gci.finalizarFor();
    }
}
