// Stub actualizado con IR
package estructuras.ast.sentencias;

import excepciones.semanticas.ExcepcionSemantica;
import modulos.GenCod;

public class NodoBloqueSystem extends NodoBloque {

	private String tipoBloque;

	public NodoBloqueSystem(String tipo) {
		tipoBloque = tipo;
	}

	@Override
	public void chequear() throws ExcepcionSemantica {
		// Chequeo delegado a super si es necesario
	}

	/**
	 * Genera IR para operaciones del sistema (print, read, etc.)
	 */
	@Override
	public void generar(modulos.ir.GeneradorCodigoIntermedio gci) {
		// Generar cuádruplos para operaciones del sistema
		switch (tipoBloque) {
			case "read":
				gci.generarRead();
				break;
			case "printB":
			case "printBln":
				gci.generarPrint("boolean");
				break;
			case "printI":
			case "printIln":
				gci.generarPrint("int");
				break;
			case "printC":
			case "printCln":
				gci.generarPrint("char");
				break;
			case "printS":
			case "printSln":
				gci.generarPrint("string");
				break;
			case "println":
				gci.generarPrint("newline");
				break;
		}
	}
}
