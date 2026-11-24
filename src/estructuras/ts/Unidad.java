// Contenido completo y CORREGIDO
package estructuras.ts;

import estructuras.Token;
import estructuras.ast.sentencias.NodoBloque;
import estructuras.ts.variables.VarLocal;
import estructuras.ts.variables.VarParametro;
import estructuras.ts.variables.Variable;
import excepciones.semanticas.ExcepcionParametroDuplicado;
import excepciones.semanticas.ExcepcionSemantica;
import excepciones.semanticas.ExcepcionVarLocalDuplicada;
import java.util.HashMap;
import java.util.LinkedList;
import modulos.GenCod;

public abstract class Unidad {

	private final Token identificador;
	private NodoBloque codigo;
	private final LinkedList<VarParametro> listaParametros;
	private final HashMap<String, Variable> tablaVariables;
	private int offset;
	private final String label;
	private boolean chequeado;

	public Unidad(Token id) {
		identificador = id;
		listaParametros = new LinkedList<>();
		tablaVariables = new HashMap<>();
		codigo = new NodoBloque();
		offset = -1;
		label = "met_" + id.getLexema() + "_" + GenCod.generarEtiqueta();
		chequeado = false;
	}

	public Unidad(Token id, LinkedList<VarParametro> listaPar) {
		this(id);
		setListaParametros(listaPar);
	}

	public String getLabel() {
		return label;
	}

	public void agregarParametro(VarParametro var) throws ExcepcionSemantica {
		if (tablaVariables.containsKey(var.getToken().getLexema()))
			throw new ExcepcionParametroDuplicado(var.getToken().getLexema(), var.getToken());
		else {
			tablaVariables.put(var.getToken().getLexema(), var);
			listaParametros.addLast(var);
		}
	}

	public void agregarVariable(VarLocal var) throws ExcepcionVarLocalDuplicada {
		String varName = var.getToken().getLexema();
		if (tablaVariables.containsKey(varName)) {
			throw new ExcepcionVarLocalDuplicada(varName, var.getToken());
		} else {
			tablaVariables.put(varName, var);
			int cantVarsLocales = tablaVariables.size() - listaParametros.size();
			var.setOffset(-cantVarsLocales);
		}
	}

	public abstract void chequearDeclaraciones() throws ExcepcionSemantica;

	public abstract void chequearSentencias() throws ExcepcionSemantica;

	public abstract void generarSentencias(modulos.ir.GeneradorCodigoIntermedio gci);

	public Token getToken() {
		return identificador;
	}

	public void setIdentificador(Token identificador) {
		/* NO-OP, es final */ }

	public NodoBloque getCodigo() {
		return codigo;
	}

	public void setCodigo(NodoBloque codigo) {
		this.codigo = codigo;
	}

	public LinkedList<VarParametro> getListaParametros() {
		return listaParametros;
	}

	public void setListaParametros(LinkedList<VarParametro> listaParametros) {
		this.listaParametros.addAll(listaParametros);
	}

	public HashMap<String, Variable> getTablaVariables() {
		return tablaVariables;
	}

	public void setTablaVariables(HashMap<String, Variable> tablaVariables) {
		this.tablaVariables.putAll(tablaVariables);
	}

	public abstract boolean esConstructor();

	public int getOffset() {
		return offset;
	}

	public void setOffset(int offset) {
		this.offset = offset;
		chequeado = true;
	}

	public boolean estaChequeado() {
		return chequeado;
	}

	// --- MÉTODO NUEVO PARA COMPARAR SIGNATURAS ---
	public boolean comparar(Unidad otraUnidad) {
		// En la sobrecarga, el nombre debe ser igual.
		if (!this.getToken().getLexema().equals(otraUnidad.getToken().getLexema()))
			return false;

		// El número de parámetros debe ser el mismo.
		if (this.getListaParametros().size() != otraUnidad.getListaParametros().size())
			return false;

		// Los tipos de los parámetros, en orden, deben ser idénticos.
		LinkedList<VarParametro> misArgs = this.getListaParametros();
		LinkedList<VarParametro> otrosArgs = otraUnidad.getListaParametros();
		for (int i = 0; i < misArgs.size(); i++) {
			if (!misArgs.get(i).getTipoVar().getNombreClase().equals(otrosArgs.get(i).getTipoVar().getNombreClase()))
				return false;
		}
		// Si todo coincide, es la misma firma.
		return true;
	}
}
