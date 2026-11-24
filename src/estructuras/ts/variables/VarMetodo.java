// Contenido completo y CORREGIDO
package estructuras.ts.variables;

import estructuras.Token;
import estructuras.ts.tipos.Tipo;

public abstract class VarMetodo extends Variable {

	public VarMetodo(Token id, Tipo tipo, String rol) {
		super(id, tipo, rol); // <-- CAMBIO AQUÍ
	}
}
