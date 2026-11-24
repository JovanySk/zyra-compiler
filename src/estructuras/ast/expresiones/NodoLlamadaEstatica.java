// Stub for NodoLlamadaEstatica
package estructuras.ast.expresiones;

import java.util.LinkedList;
import java.util.HashMap;
import estructuras.Token;
import estructuras.ast.encadenados.NodoEncadenado;
import estructuras.ts.Clase;
import estructuras.ts.Metodo;
import estructuras.ts.TablaSimbolos;
import estructuras.ts.tipos.TipoMetodo;
import estructuras.ts.variables.VarParametro;
import excepciones.semanticas.ExcepcionClaseNoDeclarada;
import excepciones.semanticas.ExcepcionMetodoInexistente;
import excepciones.semanticas.ExcepcionParametroNoConforma;
import excepciones.semanticas.ExcepcionParametrosNoCoinciden;
import excepciones.semanticas.ExcepcionSemantica;
import excepciones.semanticas.ExcepcionSemanticaPersonalizada;
import modulos.GenCod;

public class NodoLlamadaEstatica extends NodoPrimario {

	private Token idClase;
	private Token identificador;
	private LinkedList<NodoExpresion> actualArgs;
	private Metodo metLlamado;

	public NodoLlamadaEstatica(Token id, Token idclase, LinkedList<NodoExpresion> listaParams, NodoEncadenado cad) {
		super(cad);
		identificador = id;
		actualArgs = listaParams;
		this.idClase = idclase;
	}

	@Override
	public Token getToken() {
		return identificador;
	}

	public Token getIdClase() {
		return idClase;
	}

	@Override
	public TipoMetodo chequear() throws ExcepcionSemantica {
		if (!TablaSimbolos.clases.containsKey(idClase.getLexema()))
			throw new ExcepcionClaseNoDeclarada(idClase.getLexema(), idClase);

		Clase cla = TablaSimbolos.clases.get(idClase.getLexema());
		String nombreMetodo = identificador.getLexema();
		if (!cla.getMetodos().containsKey(nombreMetodo))
			throw new ExcepcionMetodoInexistente(nombreMetodo, idClase.getLexema(), identificador);

		LinkedList<TipoMetodo> tiposArgsActuales = new LinkedList<>();
		for (NodoExpresion expr : actualArgs) {
			tiposArgsActuales.add(expr.chequear());
		}

		metLlamado = null;
		LinkedList<Metodo> sobrecargas = cla.getMetodos().get(nombreMetodo);

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
			throw new ExcepcionMetodoInexistente(nombreMetodo, idClase.getLexema(), identificador);
		}

		if (!metLlamado.getEsEstatico())
			throw new ExcepcionSemanticaPersonalizada(identificador.getLinea(),
					"El metodo '" + identificador.getLexema() + "' debe ser un metodo estatico.", identificador);

		TipoMetodo receptor = metLlamado.getTipoRetorno();
		if (this.getCadena() != null) {
			this.getCadena().setEsLadoIzq(this.getEsLadoIzq());
			return this.getCadena().chequear(receptor);
		} else
			return receptor;
	}

	/**
	 * STUB: IR generation for static call
	 */
	public String generar(modulos.ir.GeneradorCodigoIntermedio gci) {
		// Generate parameters
		for (NodoExpresion expr : actualArgs) {
			String param = expr.generar(gci);
			gci.generarParam(param);
		}

		String nombreCompleto = idClase.getLexema() + "." + identificador.getLexema();

		if (!metLlamado.getTipoRetorno().esVoid()) {
			return gci.generarLlamada(nombreCompleto, actualArgs.size());
		} else {
			gci.generarLlamadaVoid(nombreCompleto, actualArgs.size());
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
