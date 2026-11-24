// Stub con IR
package estructuras.ast.sentencias;

import estructuras.ts.TablaSimbolos;
import estructuras.ts.tipos.Tipo;
import estructuras.ts.variables.VarLocal;
import excepciones.semanticas.ExcepcionSemantica;
import excepciones.semanticas.ExcepcionSemanticaPersonalizada;
import java.util.LinkedList;

public class NodoDeclaracion extends NodoSentencia {

	private final Tipo tipo;
	private final LinkedList<VarLocal> variables;

	public NodoDeclaracion(Tipo tipo, LinkedList<VarLocal> listaVars) {
		this.tipo = tipo;
		variables = listaVars;
	}

	public Tipo getTipo() {
		return tipo;
	}

	public LinkedList<VarLocal> getVariables() {
		return variables;
	}

	@Override
	public void chequear() throws ExcepcionSemantica {
		if (!tipo.esValido()) {
			throw new ExcepcionSemanticaPersonalizada(variables.getFirst().getToken().getLinea(),
					"El tipo " + tipo.getNombreClase() + " no corresponde a un tipo valido.",
					variables.getFirst().getToken());
		} else {
			for (VarLocal var : variables) {
				TablaSimbolos.metodoActual.agregarVariable(var);

				if (TablaSimbolos.bloqueActual != null) {
					TablaSimbolos.bloqueActual.agregarVariable(var);
				}
			}
		}
	}

	/**
	 * Genera IR para declaración (usualmente vacío, se maneja en scope)
	 */
	public void generar(modulos.ir.GeneradorCodigoIntermedio gci) {
		// En IR, las declaraciones no generan código
		// El espacio para variables se maneja automáticamente
	}
}
