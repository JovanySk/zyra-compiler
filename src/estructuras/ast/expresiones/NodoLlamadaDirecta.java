// Stub simple para NodoLlamadaDirecta
package estructuras.ast.expresiones;

import java.util.HashMap;
import java.util.LinkedList;
import estructuras.Token;
import estructuras.ast.encadenados.NodoEncadenado;
import estructuras.ts.Metodo;
import estructuras.ts.TablaSimbolos;
import estructuras.ts.tipos.TipoMetodo;
import estructuras.ts.variables.VarParametro;
import excepciones.semanticas.ExcepcionMetodoInexistente;
import excepciones.semanticas.ExcepcionParametroNoConforma;
import excepciones.semanticas.ExcepcionParametrosNoCoinciden;
import excepciones.semanticas.ExcepcionSemantica;
import excepciones.semanticas.ExcepcionSemanticaPersonalizada;
import modulos.GenCod;

public class NodoLlamadaDirecta extends NodoPrimario {

	private Token identificador;
	private LinkedList<NodoExpresion> actualArgs;
	private Metodo metLlamado;

	public NodoLlamadaDirecta(Token id, LinkedList<NodoExpresion> listaParams, NodoEncadenado cad) {
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
		String nombreMetodo = identificador.getLexema();

		// FUNCIONES BUILT-IN: print y read
		if (nombreMetodo.equals("print") || nombreMetodo.equals("read")) {
			return chequearFuncionBuiltIn(nombreMetodo);
		}

		HashMap<String, LinkedList<Metodo>> tablaMetodos = TablaSimbolos.claseActual.getMetodos();
		if (!tablaMetodos.containsKey(nombreMetodo))
			throw new ExcepcionMetodoInexistente(nombreMetodo, TablaSimbolos.claseActual.getNombreClase(),
					identificador);

		LinkedList<TipoMetodo> tiposArgsActuales = new LinkedList<>();
		for (NodoExpresion expr : actualArgs) {
			tiposArgsActuales.add(expr.chequear());
		}

		metLlamado = null;
		LinkedList<Metodo> sobrecargas = tablaMetodos.get(nombreMetodo);

		for (Metodo candidato : sobrecargas) {
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
					if (metLlamado != null) {
						throw new ExcepcionSemanticaPersonalizada(identificador.getLinea(),
								"Llamada ambigua al método sobrecargado '" + nombreMetodo + "'.", identificador);
					}
					metLlamado = candidato;
				}
			}
		}

		if (metLlamado == null) {
			throw new ExcepcionMetodoInexistente(identificador.getLexema(), TablaSimbolos.claseActual.getNombreClase(),
					identificador);
		}

		if (!TablaSimbolos.metodoActual.esConstructor()) {
			Metodo metActual = (Metodo) TablaSimbolos.metodoActual;
			if (metActual.getEsEstatico() && !metLlamado.getEsEstatico())
				throw new ExcepcionSemanticaPersonalizada(identificador.getLinea(),
						"El metodo '" + identificador.getLexema()
								+ "' no es estatico y esta siendo invocado desde un metodo estatico.",
						identificador);
		}

		TipoMetodo receptor = metLlamado.getTipoRetorno();

		if (this.getCadena() != null) {
			this.getCadena().setEsLadoIzq(this.getEsLadoIzq());
			return this.getCadena().chequear(receptor);
		} else
			return receptor;
	}

	/**
	 * Chequea funciones built-in: print y read
	 */
	private TipoMetodo chequearFuncionBuiltIn(String nombre) throws ExcepcionSemantica {
		if (nombre.equals("print")) {
			// print(expr) - requiere 1 parámetro int
			if (actualArgs.size() != 1) {
				throw new ExcepcionSemanticaPersonalizada(identificador.getLinea(),
						"print() requiere exactamente 1 parámetro", identificador);
			}
			TipoMetodo tipoArg = actualArgs.get(0).chequear();
			// Aceptar cualquier tipo que sea convertible a int
			return new estructuras.ts.tipos.TipoVoid();
		} else if (nombre.equals("read")) {
			// read() - no requiere parámetros, retorna int
			if (actualArgs.size() != 0) {
				throw new ExcepcionSemanticaPersonalizada(identificador.getLinea(),
						"read() no requiere parámetros", identificador);
			}
			return new estructuras.ts.tipos.TipoInt();
		}
		return new estructuras.ts.tipos.TipoVoid();
	}

	/**
	 * STUB: Genera código IR para llamada directa (versión simplificada)
	 */
	public String generar(modulos.ir.GeneradorCodigoIntermedio gci) {
		String nombreMetodo = identificador.getLexema();

		// MANEJAR FUNCIONES BUILT-IN
		if (nombreMetodo.equals("print")) {
			// print(expr) - generar código para imprimir
			NodoExpresion argExpr = actualArgs.get(0);

			// Detectar si es string literal
			boolean esString = false;
			String textoString = "";
			if (argExpr instanceof NodoLiteral) {
				NodoLiteral lit = (NodoLiteral) argExpr;
				if (lit.getToken().getNombre().equals("L_String")) {
					esString = true;
					// Extraer el texto del lexema del token
					String lexema = lit.getToken().getLexema();
					// El lexema viene CON comillas, quitarlas solo si están presentes
					if (lexema.startsWith("\"") && lexema.endsWith("\"") && lexema.length() >= 2) {
						textoString = lexema.substring(1, lexema.length() - 1);
					} else {
						// Si no tiene comillas, usar tal cual
						textoString = lexema;
					}
				}
			}

			if (esString) {
				// String: emitir print_str con el texto real
				gci.emitir("print_str", textoString, "", "");
			} else {
				// Número: generar valor y emitir print
				String valor = argExpr.generar(gci);
				gci.emitir("print", valor, "", "");
			}
			return "";
		} else if (nombreMetodo.equals("read")) {
			// read() - generar código para leer
			String temp = gci.nuevoTemporal();
			gci.emitir("read", "", "", temp);
			return temp;
		}

		// Funciones normales
		// TODO: Implementar generación completa con IR
		// Generar parámetros
		for (NodoExpresion expr : actualArgs) {
			String param = expr.generar(gci);
			gci.generarParam(param);
		}

		// Generar llamada
		TipoMetodo receptor = metLlamado.getTipoRetorno();
		if (!receptor.esVoid()) {
			return gci.generarLlamada(identificador.getLexema(), actualArgs.size());
		} else {
			gci.generarLlamadaVoid(identificador.getLexema(), actualArgs.size());
			return "";
		}
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
