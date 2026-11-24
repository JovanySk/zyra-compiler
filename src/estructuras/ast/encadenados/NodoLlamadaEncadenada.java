// Stub final con IR
package estructuras.ast.encadenados;

import java.util.LinkedList;
import estructuras.Token;
import estructuras.ast.expresiones.NodoExpresion;
import estructuras.ts.Clase;
import estructuras.ts.Metodo;
import estructuras.ts.TablaSimbolos;
import estructuras.ts.tipos.Tipo;
import estructuras.ts.tipos.TipoMetodo;
import estructuras.ts.variables.VarParametro;
import excepciones.semanticas.ExcepcionMetodoInexistente;
import excepciones.semanticas.ExcepcionParametroNoConforma;
import excepciones.semanticas.ExcepcionParametrosNoCoinciden;
import excepciones.semanticas.ExcepcionReceptorPrimitivo;
import excepciones.semanticas.ExcepcionReceptorVoid;
import excepciones.semanticas.ExcepcionSemantica;
import excepciones.semanticas.ExcepcionSemanticaPersonalizada;
import modulos.GenCod;

public class NodoLlamadaEncadenada extends NodoEncadenado {

	private LinkedList<NodoExpresion> paramActuales;
	private Metodo metLlamado;

	public NodoLlamadaEncadenada(Token id, NodoEncadenado cad) {
		super(id, cad);
		paramActuales = new LinkedList<>();
	}

	public NodoLlamadaEncadenada(Token id, NodoEncadenado cad, LinkedList<NodoExpresion> listaParam) {
		this(id, cad);
		paramActuales = listaParam;
	}

	public NodoLlamadaEncadenada(Token id, LinkedList<NodoExpresion> listaParam) {
		super(id, null);
		paramActuales = listaParam;
	}

	public boolean terminaEnVar() {
		if (this.getCadena() == null)
			return false;
		else
			return this.getCadena().terminaEnVar();
	}

	public boolean terminaEnLlamada() {
		if (this.getCadena() == null)
			return true;
		else
			return this.getCadena().terminaEnLlamada();
	}

	@Override
	public TipoMetodo chequear(TipoMetodo receptor) throws ExcepcionSemantica {
		Token tokenLlamada = this.getToken();

		if (receptor.esTipoPrimitivo())
			throw new ExcepcionReceptorPrimitivo(tokenLlamada);
		if (receptor.esVoid())
			throw new ExcepcionReceptorVoid(tokenLlamada);

		String claseReceptora = receptor.getNombreClase();
		String metodoActual = tokenLlamada.getLexema();
		Clase clase = TablaSimbolos.clases.get(claseReceptora);

		if (clase == null) {
			throw new ExcepcionSemanticaPersonalizada(tokenLlamada.getLinea(),
					"El tipo '" + claseReceptora + "' no puede ser dereferenciado.", tokenLlamada);
		}
		LinkedList<TipoMetodo> tiposArgsActuales = new LinkedList<>();
		for (NodoExpresion expr : paramActuales) {
			tiposArgsActuales.add(expr.chequear());
		}

		metLlamado = null;
		LinkedList<Metodo> sobrecargas = clase.getMetodos().get(metodoActual);
		if (sobrecargas == null)
			throw new ExcepcionMetodoInexistente(metodoActual, claseReceptora, tokenLlamada);

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
						throw new ExcepcionSemanticaPersonalizada(tokenLlamada.getLinea(),
								"Llamada ambigua al método sobrecargado '" + metodoActual + "'.", tokenLlamada);
					}
					metLlamado = candidato;
				}
			}
		}

		if (metLlamado == null)
			throw new ExcepcionMetodoInexistente(metodoActual, claseReceptora, tokenLlamada);

		TipoMetodo tipoRetorno = metLlamado.getTipoRetorno();
		if (this.getCadena() != null) {
			this.getCadena().setEsLadoIzq(this.getEsLadoIzq());
			return this.getCadena().chequear(tipoRetorno);
		} else
			return tipoRetorno;
	}

	/**
	 * STUB: Genera IR para llamada encadenada
	 */
	@Override
	public String generar(modulos.ir.GeneradorCodigoIntermedio gci) {
		// Generar parámetros
		for (NodoExpresion expr : paramActuales) {
			String param = expr.generar(gci);
			gci.generarParam(param);
		}

		// Llamada encadenada
		String resultado = gci.generarLlamada(metLlamado.getToken().getLexema(), paramActuales.size());

		// Si hay más cadena, procesar
		if (this.getCadena() != null) {
			return this.getCadena().generar(gci);
		}

		return resultado;
	}
}
