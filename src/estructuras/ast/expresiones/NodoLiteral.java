// Contenido completo y CORREGIDO
package estructuras.ast.expresiones;

import estructuras.Token;
import estructuras.ts.tipos.*;
import excepciones.semanticas.ExcepcionSemantica;
import modulos.GenCod;

public class NodoLiteral extends NodoExpPrimario {

	private Token literal;
	private String labelString;

	public NodoLiteral(Token token) {
		literal = token;
		setEsLadoIzq(false);
	}

	@Override
	public Token getToken() {
		return literal;
	}

	@Override
	public TipoMetodo chequear() throws ExcepcionSemantica {
		switch (literal.getNombre()) {
			case "L_Entero":
				return new TipoInt();
			case "L_Decimal":
				return new TipoDouble(); // <-- CORRECCIÓN: Se añade el nuevo tipo double.
			case "L_Caracter":
				return new TipoChar();
			case "PR_True":
				return new TipoBoolean();
			case "PR_False":
				return new TipoBoolean();
			case "L_String":
				labelString = GenCod.agregarStringLiteral(literal);
				return new TipoString();
			case "PR_Null":
				return new TipoNull(literal);
		}
		return null;
	}

	/**
	 * Genera código intermedio para este literal
	 * 
	 * @param gci Generador de código intermedio
	 * @return El valor del literal como string (para usar en cuádruplos)
	 */
	public String generar(modulos.ir.GeneradorCodigoIntermedio gci) {
		// Los literales simplemente retornan su valor
		// No generan cuádruplos directamente, solo proveen valores

		switch (literal.getNombre()) {
			case "L_Entero":
				return literal.getLexema();

			case "L_Decimal":
				// Retornar el valor decimal como string
				return literal.getLexema();

			case "L_Caracter":
				// Convertir carácter a su valor ASCII
				char letra = '0';
				if (literal.getLexema().length() == 1)
					letra = literal.getLexema().charAt(0);
				else {
					switch (literal.getLexema().charAt(1)) {
						case 't':
							letra = '\t';
							break;
						case 'n':
							letra = '\n';
							break;
						case '\\':
							letra = '\\';
							break;
						case '\'':
							letra = '\'';
							break;
					}
				}
				return String.valueOf((int) letra);

			case "PR_True":
				return "true";

			case "PR_False":
				return "false";

			case "L_String":
				// Para strings, necesitamos una etiqueta
				// Por ahora retornamos el lexema directamente
				// TODO: Gestión de strings literals en IR
				labelString = GenCod.agregarStringLiteral(literal);
				return labelString;

			case "PR_Null":
				return "null";
		}
		return "0";
	}

}
