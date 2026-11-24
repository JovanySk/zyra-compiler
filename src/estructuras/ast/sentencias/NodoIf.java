// IMPLEMENTACIÓN COMPLETA con IR - Nodo crítico
package estructuras.ast.sentencias;

import estructuras.Token;
import estructuras.ast.expresiones.NodoExpresion;
import estructuras.ts.tipos.TipoMetodo;
import excepciones.semanticas.ExcepcionSemantica;
import excepciones.semanticas.ExcepcionSemanticaPersonalizada;
import modulos.GenCod;

public class NodoIf extends NodoSentencia {

	private Token identificador;
	private NodoExpresion condicion;
	private NodoSentencia sentencia;
	private NodoSentencia sentenciaElse;
	private String etiqueta;

	public NodoIf(Token id, NodoExpresion expr, NodoSentencia sent, NodoSentencia sentElse) {
		identificador = id;
		condicion = expr;
		sentencia = sent;
		sentenciaElse = sentElse;
	}

	public Token getToken() {
		return identificador;
	}

	public NodoExpresion getCondicion() {
		return condicion;
	}

	public NodoSentencia getSentencia() {
		return sentencia;
	}

	public NodoSentencia getSentenciaElse() {
		return sentenciaElse;
	}

	@Override
	public void chequear() throws ExcepcionSemantica {
		TipoMetodo te = condicion.chequear();
		if (!te.getNombreClase().equals("boolean"))
			throw new ExcepcionSemanticaPersonalizada(identificador.getLinea(),
					"La condicion de un 'if' debe ser una expresion boolean a. Se encontró un tipo '"
							+ te.getNombreClase() + "'.",
					identificador);

		sentencia.chequear();
		if (sentenciaElse != null) {
			sentenciaElse.chequear();
		}
	}

	/**
	 * Genera código intermedio para if-then-else
	 * 
	 * @param gci Generador de código intermedio
	 */
	public void generar(modulos.ir.GeneradorCodigoIntermedio gci) {
		// Crear contexto de if con etiquetas
		modulos.ir.GeneradorCodigoIntermedio.ContextoIf ctx = gci.iniciarIf();

		// Evaluar condición
		String cond = condicion.generar(gci);

		if (sentenciaElse != null) {
			// if-else: if_false cond goto L1 (else)
			gci.generarIfFalse(cond, ctx.etiquetaElse);

			// Cuerpo del then
			sentencia.generar(gci);

			// Saltar al final
			gci.generarGoto(ctx.etiquetaFin);

			// Etiqueta del else
			gci.generarEtiqueta(ctx.etiquetaElse);
			sentenciaElse.generar(gci);

			// Etiqueta de fin
			gci.generarEtiqueta(ctx.etiquetaFin);
		} else {
			// if sin else: if_false cond goto L_fin
			gci.generarIfFalse(cond, ctx.etiquetaFin);

			// Cuerpo del if
			sentencia.generar(gci);

			// Etiqueta de fin
			gci.generarEtiqueta(ctx.etiquetaFin);
		}
	}
}
