// Contenido completo y CORREGIDO
package estructuras.ast.expresiones;

import estructuras.Token;
import estructuras.ts.TablaSimbolos;
import estructuras.ts.tipos.Tipo;
import estructuras.ts.tipos.TipoBoolean;
import estructuras.ts.tipos.TipoClase;
import estructuras.ts.tipos.TipoInt;
import estructuras.ts.tipos.TipoMetodo;
import excepciones.semanticas.ExcepcionSemantica;
import excepciones.semanticas.ExcepcionSemanticaPersonalizada;
import modulos.GenCod;

public class NodoExpresionUnaria extends NodoExpresion {

	private final NodoExpresion ladoDer;
	private final Token simbolo;

	public NodoExpresionUnaria(Token temp, NodoExpresion lder) {
		simbolo = temp;
		ladoDer = lder;
	}

	public NodoExpresion getLadoDer() {
		return ladoDer;
	}

	public Token getSimbolo() {
		return simbolo;
	}

	@Override
	public Token getToken() {
		return simbolo;
	}

	@Override
	public TipoMetodo chequear() throws ExcepcionSemantica {
		TipoMetodo td = ladoDer.chequear();
		switch (simbolo.getLexema()) {
			case "+":
			case "-":
				if (td.getNombreClase().equals("int"))
					return new TipoInt();
				else
					throw new ExcepcionSemanticaPersonalizada(simbolo.getLinea(),
							"El operador unario '" + simbolo.getLexema() + "' solo es aplicable a tipos enteros.",
							simbolo);
			case "!":
				if (td.getNombreClase().equals("boolean"))
					return new TipoBoolean();
				else
					throw new ExcepcionSemanticaPersonalizada(simbolo.getLinea(),
							"El operador unario '!' solo es aplicable a tipos booleanos.", simbolo);
			default: // es un casteo, tiene la forma [idClase] <expr>
				if (td.esVoid())
					throw new ExcepcionSemanticaPersonalizada(simbolo.getLinea(),
							"La expresion del casteo no puede ser de tipo void.", simbolo);

				Tipo tip = new TipoClase(simbolo);
				if (!tip.conforma(td))
					throw new ExcepcionSemanticaPersonalizada(simbolo.getLinea(), "Los tipos del casteo no conforman.",
							simbolo);

				return tip;
		}
	}

	/**
	 * Genera código intermedio para esta expresión unaria
	 * 
	 * @param gci Generador de código intermedio
	 * @return Temporal que contiene el resultado
	 */
	public String generar(modulos.ir.GeneradorCodigoIntermedio gci) {
		String operando = ladoDer.generar(gci);

		String operador = simbolo.getLexema();

		// Generar la operación unaria
		String resultado = gci.generarOperacionUnaria(operador, operando);

		return resultado;
	}
}
