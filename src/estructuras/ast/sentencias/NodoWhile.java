// IMPLEMENTACIÓN COMPLETA con IR - Nodo crítico
package estructuras.ast.sentencias;

import estructuras.Token;
import estructuras.ast.expresiones.NodoExpresion;
import estructuras.ts.tipos.TipoMetodo;
import excepciones.semanticas.ExcepcionSemantica;
import excepciones.semanticas.ExcepcionSemanticaPersonalizada;
import modulos.GenCod;

public class NodoWhile extends NodoSentencia {

	private Token identificador;
	private NodoExpresion condicion;
	private NodoSentencia sentencia;
	private String etiqueta;

	public NodoWhile(Token id, NodoExpresion expr, NodoSentencia sent) {
		identificador = id;
		condicion = expr;
		sentencia = sent;
	}

	public NodoExpresion getCondicion() {
		return condicion;
	}

	public NodoSentencia getSentencia() {
		return sentencia;
	}

	@Override
	public void chequear() throws ExcepcionSemantica {
		TipoMetodo tc = condicion.chequear();
		if (!tc.getNombreClase().equals("boolean"))
			throw new ExcepcionSemanticaPersonalizada(identificador.getLinea(),
					"La condicion de un 'while' debe ser una expresion booleana. Se encontró un tipo '"
							+ tc.getNombreClase() + "'.",
					identificador);

		sentencia.chequear();
	}

	/**
	 * Genera código intermedio para while
	 * 
	 * @param gci Generador de código intermedio
	 */
	public void generar(modulos.ir.GeneradorCodigoIntermedio gci) {
		// Crear contexto de while con etiquetas
		modulos.ir.GeneradorCodigoIntermedio.ContextoWhile ctx = gci.iniciarWhile();

		// Etiqueta de inicio (para continue y para volver a evaluar condición)
		gci.generarEtiqueta(ctx.etiquetaInicio);

		// Evaluar condición
		String cond = condicion.generar(gci);

		// Si es falso, saltar al fin
		gci.generarIfFalse(cond, ctx.etiquetaFin);

		// Cuerpo del while
		sentencia.generar(gci);

		// Saltar de vuelta al inicio
		gci.generarGoto(ctx.etiquetaInicio);

		// Etiqueta de fin (para break)
		gci.generarEtiqueta(ctx.etiquetaFin);

		// Finalizar contexto (limpia pilas de etiquetas)
		gci.finalizarWhile();
	}
}
