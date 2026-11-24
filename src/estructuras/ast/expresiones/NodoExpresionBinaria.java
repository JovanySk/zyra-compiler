// Contenido completo y CORREGIDO
package estructuras.ast.expresiones;

import estructuras.Token;
import estructuras.ts.TablaSimbolos;
import estructuras.ts.tipos.Tipo;
import estructuras.ts.tipos.TipoBoolean;
import estructuras.ts.tipos.TipoClase;
import estructuras.ts.tipos.TipoInt;
import estructuras.ts.tipos.TipoMetodo;
import estructuras.ts.tipos.TipoString;
import excepciones.semanticas.ExcepcionExpBinMalFormada;
import excepciones.semanticas.ExcepcionSemantica;
import excepciones.semanticas.ExcepcionSemanticaPersonalizada;
import modulos.GenCod;

public class NodoExpresionBinaria extends NodoExpresion {

	private final NodoExpresion ladoIzq;
	private final NodoExpresion ladoDer;
	private final Token simbolo;
	private TipoMetodo tipoResultante; // Variable de instancia para guardar el tipo

	public NodoExpresionBinaria(Token temp, NodoExpresion lizq, NodoExpresion lder) {
		simbolo = temp;
		ladoIzq = lizq;
		ladoDer = lder;
		tipoResultante = null; // Inicializar a null
	}

	public NodoExpresion getLadoIzq() {
		return ladoIzq;
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
		TipoMetodo ti = ladoIzq.chequear();
		TipoMetodo td = null;

		if (!simbolo.getLexema().equals("instanceof")) {
			td = ladoDer.chequear();
		}

		switch (simbolo.getLexema()) {
			case "+":
				// <-- CORRECCIÓN: Lógica de tipos para el operador '+' -->
				if (ti.getNombreClase().equals("String") && td.getNombreClase().equals("String")) {
					this.tipoResultante = new TipoString();
					return tipoResultante;
				}
				if (ti.getNombreClase().equals("int") && td.getNombreClase().equals("int")) {
					this.tipoResultante = new TipoInt();
					return tipoResultante;
				} else
					throw new ExcepcionSemanticaPersonalizada(simbolo.getLinea(),
							"El operador '+' solo es aplicable entre dos 'int' o dos 'String'. Se encontraron '"
									+ ti.getNombreClase() + "' y '" + td.getNombreClase() + "'.",
							simbolo);

			case "-":
			case "*":
			case "/":
			case "%":
				// <-- CORRECCIÓN: Lógica de tipos para operadores aritméticos -->
				if (ti.getNombreClase().equals("int") && td.getNombreClase().equals("int")) {
					this.tipoResultante = new TipoInt();
					return tipoResultante;
				} else
					throw new ExcepcionSemanticaPersonalizada(simbolo.getLinea(),
							"El operador '" + simbolo.getLexema()
									+ "' solo es aplicable a tipos 'int'. Se encontraron '" + ti.getNombreClase()
									+ "' y '" + td.getNombreClase() + "'.",
							simbolo);

			case "&&":
			case "||":
				// <-- CORRECCIÓN: Lógica de tipos para operadores lógicos -->
				if (ti.getNombreClase().equals("boolean") && td.getNombreClase().equals("boolean")) {
					this.tipoResultante = new TipoBoolean();
					return tipoResultante;
				} else
					throw new ExcepcionSemanticaPersonalizada(simbolo.getLinea(),
							"El operador '" + simbolo.getLexema()
									+ "' solo es aplicable a tipos 'boolean'. Se encontraron '" + ti.getNombreClase()
									+ "' y '" + td.getNombreClase() + "'.",
							simbolo);

			case "!=":
			case "==":
				// <-- CORRECCIÓN: Lógica de tipos para operadores de igualdad -->
				if (ti.conforma(td) || td.conforma(ti)) {
					this.tipoResultante = new TipoBoolean();
					return tipoResultante;
				} else
					throw new ExcepcionSemanticaPersonalizada(simbolo.getLinea(),
							"No se pueden comparar con '" + simbolo.getLexema() + "' tipos incompatibles: '"
									+ ti.getNombreClase() + "' y '" + td.getNombreClase() + "'.",
							simbolo);

			case ">":
			case "<":
			case ">=":
			case "<=":
				// <-- CORRECCIÓN: Lógica de tipos para operadores de comparación -->
				if (ti.getNombreClase().equals("int") && td.getNombreClase().equals("int")) {
					this.tipoResultante = new TipoBoolean();
					return tipoResultante;
				} else
					throw new ExcepcionSemanticaPersonalizada(simbolo.getLinea(),
							"El operador '" + simbolo.getLexema()
									+ "' solo es aplicable a tipos 'int'. Se encontraron '" + ti.getNombreClase()
									+ "' y '" + td.getNombreClase() + "'.",
							simbolo);

			case "instanceof":
				td = new TipoClase(((NodoLiteral) ladoDer).getToken());
				if (!td.esValido()) {
					throw new ExcepcionSemanticaPersonalizada(simbolo.getLinea(),
							"El tipo '" + td.getNombreClase() + "' a la derecha de instanceof no existe.", simbolo);
				}

				if (!(ti instanceof TipoClase)) {
					throw new ExcepcionSemanticaPersonalizada(simbolo.getLinea(),
							"El operador instanceof no puede aplicarse a un tipo primitivo: " + ti.getNombreClase(),
							simbolo);
				}

				if (!ti.conforma(td) && !td.conforma(ti)) {
					throw new ExcepcionSemanticaPersonalizada(simbolo.getLinea(),
							"Tipos incompatibles para el operador instanceof: " + ti.getNombreClase() + " y "
									+ td.getNombreClase(),
							simbolo);
				} else {
					this.tipoResultante = new TipoBoolean();
					return tipoResultante;
				}
		}
		return null; // No se debería alcanzar
	}

	/**
	 * Genera código intermedio para esta expresión binaria
	 * 
	 * @param gci Generador de código intermedio
	 * @return Temporal que contiene el resultado de la operación
	 */
	public String generar(modulos.ir.GeneradorCodigoIntermedio gci) {
		// Generar código para los operandos
		String t1 = ladoIzq.generar(gci);

		String operador = simbolo.getLexema();

		// instanceof es un caso especial
		if (operador.equals("instanceof")) {
			// TODO: Implementar instanceof en IR
			// Por ahora, generamos una comparación simple
			String nombreClase = ((NodoLiteral) ladoDer).getToken().getLexema();
			String resultado = gci.nuevoTemporal();
			gci.emitir("instanceof", t1, nombreClase, resultado);
			return resultado;
		}

		String t2 = ladoDer.generar(gci);

		// Generar la operación binaria
		String resultado = gci.generarOperacionBinaria(operador, t1, t2);

		return resultado;
	}
}
