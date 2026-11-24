// Contenido completo y CORREGIDO
package estructuras.ast.expresiones;

import estructuras.Token;
import estructuras.ast.encadenados.NodoEncadenado;
import estructuras.ts.Metodo;
import estructuras.ts.TablaSimbolos;
import estructuras.ts.tipos.Tipo;
import estructuras.ts.tipos.TipoClase;
import estructuras.ts.tipos.TipoMetodo;
import excepciones.semanticas.ExcepcionSemantica;
import excepciones.semanticas.ExcepcionSemanticaPersonalizada;
import modulos.GenCod;

public class NodoThis extends NodoPrimario {

	private Token identificador;

	public NodoThis(Token t) {
		super();
		identificador = t;
	}

	public NodoThis(Token t, NodoEncadenado cad) {
		super(cad);
		identificador = t;
	}

	@Override
	public Token getToken() {
		return identificador;
	}

	@Override
	public TipoMetodo chequear() throws ExcepcionSemantica {
		if (!TablaSimbolos.metodoActual.esConstructor()) {
			Metodo met = (Metodo) TablaSimbolos.metodoActual;
			if (met.getEsEstatico())
				throw new ExcepcionSemanticaPersonalizada(identificador.getLinea(),
						"No se puede hacer uso de 'this' dentro de un metodo estatico.", identificador);
		}

		Token tk = new Token("idClase", TablaSimbolos.claseActual.getNombreClase(), identificador.getLinea(),
				identificador.getColumna());
		Tipo receptor = new TipoClase(tk);

		if (this.getCadena() != null) {
			this.getCadena().setEsLadoIzq(this.getEsLadoIzq());
			return this.getCadena().chequear(receptor);
		} else
			return receptor;
	}

	/**
	 * Genera código intermedio para 'this'
	 * 
	 * @param gci Generador de código intermedio
	 * @return Referencia a 'this'
	 */
	public String generar(modulos.ir.GeneradorCodigoIntermedio gci) {
		// 'this' es una referencia especial
		// En IR, puede representarse como un nombre especial
		return "this";
	}

	@Override
	public boolean terminaEnVar() {
		if (this.getCadena() == null) {
			return false;
		} else {
			return this.getCadena().terminaEnVar();
		}
	}

	@Override
	public boolean terminaEnLlamada() {
		if (this.getCadena() == null) {
			return false;
		} else {
			return this.getCadena().terminaEnLlamada();
		}
	}
}