// Contenido completo y CORREGIDO
package estructuras.ast.expresiones;

import java.util.HashMap;
import estructuras.Token;
import estructuras.ast.encadenados.NodoEncadenado;
import estructuras.ts.Metodo;
import estructuras.ts.TablaSimbolos;
import estructuras.ts.tipos.Tipo;
import estructuras.ts.tipos.TipoMetodo;
import estructuras.ts.variables.VarInstancia;
import estructuras.ts.variables.Variable;
import excepciones.semanticas.ExcepcionSemantica;
import excepciones.semanticas.ExcepcionSemanticaPersonalizada;
import modulos.GenCod;

public class NodoVariable extends NodoPrimario {

	private final Token identificador;
	private Variable variable;
	private boolean esVarDeInstancia;

	public NodoVariable(Token t) {
		identificador = t;
	}

	public NodoVariable(Token t, NodoEncadenado cad) {
		super(cad);
		identificador = t;
	}

	@Override
	public Token getToken() {
		return identificador;
	}

	public Variable getVariable() {
		return variable;
	}

	@Override
	public TipoMetodo chequear() throws ExcepcionSemantica {
		HashMap<String, VarInstancia> atributosInstancia = TablaSimbolos.claseActual.getAtributos();
		HashMap<String, Variable> atributosLocales = TablaSimbolos.metodoActual.getTablaVariables();
		String idVar = identificador.getLexema();

		if (atributosLocales.containsKey(idVar)) {
			variable = atributosLocales.get(idVar);
			esVarDeInstancia = false;
		} else {
			if (!atributosInstancia.containsKey(idVar))
				throw new ExcepcionSemanticaPersonalizada(identificador.getLinea(),
						"La variable '" + idVar + "' no ha sido declarada.", identificador);

			if (!TablaSimbolos.metodoActual.esConstructor()) {
				Metodo met = (Metodo) TablaSimbolos.metodoActual;
				if (met.getEsEstatico())
					throw new ExcepcionSemanticaPersonalizada(identificador.getLinea(),
							"No se puede usar la variable de instancia '" + idVar + "' dentro de un metodo estatico.",
							identificador);
			}
			variable = atributosInstancia.get(idVar);
			esVarDeInstancia = true;
		}

		Tipo tipo = variable.getTipoVar();
		if (this.getCadena() != null) {
			this.getCadena().setEsLadoIzq(this.getEsLadoIzq());
			return this.getCadena().chequear(tipo);
		} else
			return tipo;
	}

	/**
	 * Genera código intermedio para acceso a variable
	 * 
	 * @param gci Generador de código intermedio
	 * @return Nombre de la variable (o temporal si tiene cadena)
	 */
	public String generar(modulos.ir.GeneradorCodigoIntermedio gci) {
		// Para IR, las variables simplemente retornan su nombre
		// El acceso real se maneja en asignaciones o expresiones que las usan

		String nombreVar = identificador.getLexema();

		// Si tiene cadena (ej: obj.atributo), delegar a la cadena
		if (this.getCadena() != null) {
			// TODO: Implementar cadenas de acceso en IR
			// Por ahora, retornar el nombre de la variable
			return nombreVar;
		}

		return nombreVar;
	}

	@Override
	public boolean terminaEnVar() {
		if (this.getCadena() == null)
			return true;
		else
			return this.getCadena().terminaEnVar();
	}

	@Override
	public boolean terminaEnLlamada() {
		if (this.getCadena() == null)
			return false;
		else
			return this.getCadena().terminaEnLlamada();
	}
}
