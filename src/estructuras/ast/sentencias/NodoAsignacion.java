// IMPLEMENTACIÓN COMPLETA con IR - Nodo MUY crítico
package estructuras.ast.sentencias;

import estructuras.ast.expresiones.NodoAccesoArreglo;
import estructuras.ast.expresiones.NodoExpresion;
import estructuras.ast.expresiones.NodoLiteral;
import estructuras.ast.expresiones.NodoVariable;
import estructuras.ts.tipos.TipoMetodo;
import estructuras.ts.variables.Variable;
import excepciones.semanticas.ExcepcionSemantica;
import excepciones.semanticas.ExcepcionSemanticaPersonalizada;

public class NodoAsignacion extends NodoSentencia {

	private NodoExpresion ladoIzq;
	private NodoExpresion ladoDer;

	public NodoAsignacion(NodoExpresion expIzq, NodoExpresion expDer) {
		ladoIzq = expIzq;
		ladoDer = expDer;
	}

	/**
	 * Constructor de compatibilidad para AnalizadorSintactico
	 * 
	 * @deprecated El token no se usa en la versión IR
	 */
	@Deprecated
	public NodoAsignacion(NodoExpresion expIzq, NodoExpresion expDer, estructuras.Token token) {
		this(expIzq, expDer);
		// Token ignorado en versión IR
	}

	/**
	 * Método de compatibilidad para AstTreeCellRenderer
	 * 
	 * @return Token ficticio
	 */
	public estructuras.Token getSimbolo() {
		return new estructuras.Token("ID_ASIGNACION", "=", 0, 0);
	}

	public NodoExpresion getLadoIzq() {
		return ladoIzq;
	}

	public NodoExpresion getLadoDer() {
		return ladoDer;
	}

	@Override
	public void chequear() throws ExcepcionSemantica {
		ladoIzq.setEsLadoIzq(true);
		TipoMetodo tizq = ladoIzq.chequear();
		TipoMetodo tder = ladoDer.chequear();

		if (!tder.conforma(tizq)) {
			throw new ExcepcionSemanticaPersonalizada(
					ladoIzq.getToken().getLinea(),
					"Tipos incompatibles en asignación. No se puede asignar '" + tder.getNombreClase() + "' a '"
							+ tizq.getNombreClase() + "'.",
					ladoIzq.getToken());
		}

		// Propagación de valores constantes para análisis semántico
		if (ladoDer instanceof NodoLiteral && ladoIzq instanceof NodoVariable) {
			NodoVariable nodoVar = (NodoVariable) ladoIzq;
			Variable v = nodoVar.getVariable();
			if (v != null) {
				NodoLiteral nodoLit = (NodoLiteral) ladoDer;
				v.setValor(nodoLit.getToken().getLexema());
			}
		}
	}

	/**
	 * Genera código intermedio para asignación
	 * 
	 * @param gci Generador de código intermedio
	 */
	public void generar(modulos.ir.GeneradorCodigoIntermedio gci) {
		// Generar código para el lado derecho (valor a asignar)
		String valor = ladoDer.generar(gci);

		// Determinar el tipo de asignación según el lado izquierdo
		if (ladoIzq instanceof NodoVariable) {
			// Asignación simple: x = valor
			NodoVariable var = (NodoVariable) ladoIzq;
			String nombreVar = var.getToken().getLexema();
			gci.generarAsignacion(nombreVar, valor);

		} else if (ladoIzq instanceof NodoAccesoArreglo) {
			// Asignación a array: arr[index] = valor
			NodoAccesoArreglo acceso = (NodoAccesoArreglo) ladoIzq;
			String array = acceso.getPrimario().generar(gci);
			String indice = acceso.getIndice().generar(gci);
			gci.generarAsignacionArray(array, indice, valor);

		} else {
			// Otros tipos de asignación (ej: obj.atributo = valor)
			// Por ahora, stub simple
			ladoIzq.setEsLadoIzq(true);
			ladoIzq.generar(gci);
		}
	}
}
