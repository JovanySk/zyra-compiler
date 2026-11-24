// IMPLEMENTACIÓN COMPLETA con IR
package estructuras.ast.sentencias;

import estructuras.Token;
import estructuras.ast.expresiones.NodoExpresion;
import estructuras.ts.Metodo;
import estructuras.ts.TablaSimbolos;
import estructuras.ts.tipos.TipoMetodo;
import excepciones.semanticas.ExcepcionSemantica;
import excepciones.semanticas.ExcepcionSemanticaPersonalizada;
import modulos.GenCod;

public class NodoReturn extends NodoSentencia {

	private Token identificador;
	private NodoExpresion expresion;

	public NodoReturn(Token id, NodoExpresion expr) {
		identificador = id;
		expresion = expr;
	}

	public Token getToken() {
		return identificador;
	}

	public NodoExpresion getExpresion() {
		return expresion;
	}

	@Override
	public void chequear() throws ExcepcionSemantica {
		if (TablaSimbolos.metodoActual.esConstructor())
			throw new ExcepcionSemanticaPersonalizada(identificador.getLinea(),
					"El constructor no puede contener un return.", identificador);

		Metodo m = (Metodo) TablaSimbolos.metodoActual;

		if (expresion == null) {
			if (!m.getTipoRetorno().esVoid())
				throw new ExcepcionSemanticaPersonalizada(identificador.getLinea(),
						"El metodo '" + m.getToken().getLexema() + "' debe devolver un valor de tipo '"
								+ m.getTipoRetorno().getNombreClase() + "', pero se encontró un return vacío.",
						identificador);
		} else {
			TipoMetodo tipoExpresion = expresion.chequear();
			TipoMetodo tipoRetornoMetodo = m.getTipoRetorno();

			if (tipoRetornoMetodo.esVoid()) {
				throw new ExcepcionSemanticaPersonalizada(identificador.getLinea(),
						"El metodo void '" + m.getToken().getLexema() + "' no puede devolver un valor.", identificador);
			}

			if (!tipoExpresion.conforma(tipoRetornoMetodo)) {
				if (tipoRetornoMetodo.getNombreClase().equals("int")
						&& tipoExpresion.getNombreClase().equals("double")) {
					throw new ExcepcionSemanticaPersonalizada(identificador.getLinea(),
							"Incompatibilidad de tipos. No se puede devolver un 'double' desde un método que retorna 'int'. "
									+
									"Sugerencia: Zyra no realiza conversiones implícitas de double a int, considere un casteo explícito si la pérdida de precisión es intencional.",
							identificador);
				} else {
					throw new ExcepcionSemanticaPersonalizada(identificador.getLinea(),
							"Incompatibilidad de tipos. No se puede devolver un valor de tipo '"
									+ tipoExpresion.getNombreClase() + "' desde un método que retorna '"
									+ tipoRetornoMetodo.getNombreClase() + "'.",
							identificador);
				}
			}
		}
	}

	/**
	 * Genera código intermedio para return
	 * 
	 * @param gci Generador de código intermedio
	 */
	public void generar(modulos.ir.GeneradorCodigoIntermedio gci) {
		if (expresion != null) {
			// Return con valor
			String valor = expresion.generar(gci);
			gci.generarReturn(valor);
		} else {
			// Return void
			gci.generarReturn(null);
		}
	}
}
