// Contenido completo y CORREGIDO
package excepciones.semanticas;

import estructuras.Token;

@SuppressWarnings("serial")
public class ExcepcionExpBinMalFormada extends ExcepcionSemantica {

	public ExcepcionExpBinMalFormada (String op, String permitidos, Token token) {
		super("El operador binario '"+op+"' solo es aplicable a tipos '"+permitidos+"'.", token);
	}
}