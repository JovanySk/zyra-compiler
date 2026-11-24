// Contenido completo y CORREGIDO
package estructuras.ts;

import estructuras.Token;
import estructuras.ts.variables.VarParametro;
import estructuras.ts.variables.Variable;
import excepciones.semanticas.ExcepcionClaseNoDeclarada;
import excepciones.semanticas.ExcepcionSemantica;
import java.util.Collections;
import java.util.LinkedList;
import modulos.GenCod;

public class Constructor extends Unidad {

	public Constructor(Token id) {
		super(id);
	}

	@Override
	public void chequearDeclaraciones() throws ExcepcionSemantica {
		for (Variable var : this.getTablaVariables().values()) {
			if (!var.getTipoVar().esValido()) {
				// Excepción CORREGIDA: Pasa el token de la variable, no solo la línea.
				throw new ExcepcionClaseNoDeclarada(var.getTipoVar().getNombreClase(), var.getToken());
			}
		}
	}

	@Override
	public boolean esConstructor() {
		return true;
	}

	@Override
	public void chequearSentencias() throws ExcepcionSemantica {
		// <-- CORRECCIÓN FUNDAMENTAL: Establecer el bloque actual -->
		// Al igual que en Metodo.java, esto es esencial para el manejo de ámbito.
		TablaSimbolos.bloqueActual = this.getCodigo();
		this.getCodigo().chequear();
	}

	@Override
	public void generarSentencias(modulos.ir.GeneradorCodigoIntermedio gci) {
		LinkedList<VarParametro> listaParam = new LinkedList<>(this.getListaParametros());
		Collections.sort(listaParam);
		int cantParametros = this.getListaParametros().size();
		for (int i = 0; i < listaParam.size(); i++) {
			listaParam.get(i).setOffset(cantParametros + 3 - i);
		}

		GenCod.gen(getLabel(), "LOADFP", "Guardar enlace dinamico");
		GenCod.gen("LOADSP", "Inicializar el FP");
		GenCod.gen("STOREFP", "");

		int cantVarsLocales = this.getTablaVariables().size() - this.getListaParametros().size();
		if (cantVarsLocales > 0)
			GenCod.gen("RMEM " + cantVarsLocales, "Reservar espacio para variables locales");

		// === MARCAR INICIO DE FUNCIÓN EN IR ===
		gci.marcarInicioFuncion(this.getToken().getLexema());

		// Generar IR del código del constructor
		this.getCodigo().generar(gci);

		// === MARCAR FIN DE FUNCIÓN EN IR ===
		gci.marcarFinFuncion(this.getToken().getLexema());

		if (cantVarsLocales > 0)
			GenCod.gen("FMEM " + cantVarsLocales, "Liberar espacio para variables locales");

		GenCod.gen("STOREFP", "Reestablece el contexto");
	}
}
