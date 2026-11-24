// Stub simple para NodoConstructor - TO DO: Refinar después
package estructuras.ast.expresiones;

import java.util.LinkedList;
import estructuras.Token;
import estructuras.ast.encadenados.NodoEncadenado;
import estructuras.ts.Clase;
import estructuras.ts.Constructor;
import estructuras.ts.TablaSimbolos;
import estructuras.ts.tipos.Tipo;
import estructuras.ts.tipos.TipoClase;
import estructuras.ts.tipos.TipoMetodo;
import estructuras.ts.variables.VarParametro;
import excepciones.semanticas.ExcepcionClaseNoDeclarada;
import excepciones.semanticas.ExcepcionParametroNoConforma;
import excepciones.semanticas.ExcepcionParametrosNoCoinciden;
import excepciones.semanticas.ExcepcionSemantica;
import excepciones.semanticas.ExcepcionSemanticaPersonalizada;
import modulos.GenCod;

public class NodoConstructor extends NodoPrimario {

	private Token identificador;
	private LinkedList<NodoExpresion> actualArgs;
	private Constructor ctorLlamado;

	public NodoConstructor(Token id, LinkedList<NodoExpresion> listaParams, NodoEncadenado cad) {
		super(cad);
		identificador = id;
		actualArgs = listaParams;
	}

	@Override
	public Token getToken() {
		return identificador;
	}

	public LinkedList<NodoExpresion> getActualArgs() {
		return actualArgs;
	}

	@Override
	public TipoMetodo chequear() throws ExcepcionSemantica {
		if (!TablaSimbolos.clases.containsKey(identificador.getLexema()))
			throw new ExcepcionClaseNoDeclarada(identificador.getLexema(), identificador);

		Clase clase = TablaSimbolos.clases.get(identificador.getLexema());

		LinkedList<TipoMetodo> tiposArgsActuales = new LinkedList<>();
		for (NodoExpresion expr : actualArgs) {
			tiposArgsActuales.add(expr.chequear());
		}

		ctorLlamado = null;
		for (Constructor candidato : clase.getConstructores()) {
			LinkedList<VarParametro> argsFormales = candidato.getListaParametros();
			if (argsFormales.size() == tiposArgsActuales.size()) {
				boolean conformanTodos = true;
				for (int i = 0; i < tiposArgsActuales.size(); i++) {
					if (!tiposArgsActuales.get(i).conforma(argsFormales.get(i).getTipoVar())) {
						conformanTodos = false;
						break;
					}
				}
				if (conformanTodos) {
					if (ctorLlamado != null) {
						throw new ExcepcionSemanticaPersonalizada(identificador.getLinea(),
								"Llamada ambigua al constructor de '" + identificador.getLexema() + "'.",
								identificador);
					}
					ctorLlamado = candidato;
				}
			}
		}

		if (ctorLlamado == null) {
			throw new ExcepcionSemanticaPersonalizada(identificador.getLinea(),
					"No se encontró un constructor para la clase '" + identificador.getLexema()
							+ "' que coincida con los argumentos dados.",
					identificador);
		}

		Tipo receptor = new TipoClase(identificador);

		if (this.getCadena() != null) {
			this.getCadena().setEsLadoIzq(this.getEsLadoIzq());
			return this.getCadena().chequear(receptor);
		} else
			return receptor;
	}

	/**
	 * STUB: Genera código IR para constructor (versión simplificada)
	 */
	public String generar(modulos.ir.GeneradorCodigoIntermedio gci) {
		// TODO: Implementar generación completa de constructor con IR
		// Por ahora, stub simple
		String temp = gci.nuevoTemporal();

		// Generar parámetros
		for (NodoExpresion expr : actualArgs) {
			String param = expr.generar(gci);
			gci.generarParam(param);
		}

		// Llamada al constructor
		gci.emitir("new", identificador.getLexema(), String.valueOf(actualArgs.size()), temp);

		return temp;
	}

	@Override
	public boolean terminaEnVar() {
		if (this.getCadena() == null)
			return false;
		else
			return this.getCadena().terminaEnVar();
	}

	@Override
	public boolean terminaEnLlamada() {
		if (this.getCadena() == null)
			return true;
		else
			return this.getCadena().terminaEnLlamada();
	}
}
