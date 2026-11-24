// Contenido completo y CORREGIDO con validaciones semánticas robustas
package estructuras.ts;

import estructuras.Token;
import estructuras.ast.NodoClase;
import estructuras.ast.NodoPrograma;
import estructuras.ast.sentencias.NodoBloque;
import estructuras.ts.variables.VarInstancia;
import excepciones.semanticas.ExcepcionAtributoYaNombrado;
import excepciones.semanticas.ExcepcionClaseDeclarada;
import excepciones.semanticas.ExcepcionClaseNoDeclarada;
import excepciones.semanticas.ExcepcionFaltaMain;
import excepciones.semanticas.ExcepcionHerenciaCircular;
import excepciones.semanticas.ExcepcionSemantica;
import java.util.HashMap;
import java.util.LinkedList;
import modulos.GenCod;

public class TablaSimbolos {
	
	public static Clase claseActual;
	public static Unidad metodoActual;
	public static NodoBloque bloqueActual;
	public static HashMap<String,Clase> clases;
	public static Metodo main;
	
	public TablaSimbolos(){
		claseActual = null;
		metodoActual = null;
		bloqueActual = null;
		clases = new HashMap<String,Clase>();
	}
	
	public void agregarClase(String nom, Clase c) {
		if (nom!=null) {
			clases.put(nom, c);
		}
	}

    public void chequearClasesDuplicadas(NodoPrograma ast) throws ExcepcionClaseDeclarada {
        HashMap<String, Token> nombresVistos = new HashMap<>();
        for (NodoClase nodoClase : ast.getClases()) {
            Token tokenClase = nodoClase.getToken();
            String nombreClase = tokenClase.getLexema();
            if (nombresVistos.containsKey(nombreClase)) {
                throw new ExcepcionClaseDeclarada(nombreClase, tokenClase);
            } else {
                nombresVistos.put(nombreClase, tokenClase);
            }
        }
    }
	
	public boolean estaClase(String nombreClase){
		if (nombreClase != null)
			return clases.containsKey(nombreClase);
		else return false;
	}
	
    public HashMap<String, Clase> getClases() {
        return clases;
    }

	public void chequearDeclaraciones() throws ExcepcionSemantica {
		actualizarMetodos();
		main = chequearMain();
		chequearHerenciaCircular();
		chequearAtributos();
		
		for (Clase cla : clases.values()) {
			cla.chequearDeclaraciones();
		}
	}
	
	private void actualizarMetodos() throws ExcepcionSemantica {
		for (Clase cla: clases.values()) {
			cla.actualizar();
		}
	}
	
	public void chequearSentencias() throws ExcepcionSemantica {
		for (Clase cla : clases.values()) {
			cla.chequearSentencias();
		}
	}
	
	private Metodo chequearMain() throws ExcepcionSemantica {
		int cantMains = 0;
		String nombreClase = null;
		Metodo mainLocal = null;
		for (Clase cla : clases.values()) {
			HashMap<String,LinkedList<Metodo>> listaMets = cla.getMetodos();
			if (listaMets.containsKey("main")) {
				for (Metodo met : listaMets.get("main")) {
					if (met.getEsEstatico() && met.getTipoRetorno().getNombreClase().equals("void") && met.getListaParametros().isEmpty()) {
						cantMains++;
						if (mainLocal==null) {
							mainLocal = met;
							nombreClase = cla.getNombreClase();
						}
					}
				}
			}
		}
		if (cantMains==0) {
			throw new ExcepcionFaltaMain();
		} else if (cantMains>1) {
			System.out.println("[Advertencia] Se encontraron varias clases con un método main válido.");
			System.out.println("[Advertencia] Se utilizará el main de la clase '"+nombreClase+"' como punto de entrada.");
		}
		return mainLocal;
	}
	
	private void chequearHerenciaCircular() throws ExcepcionSemantica {
		for (String nomClase : clases.keySet()) {
            if (!nomClase.equals("Object") && !nomClase.equals("System")) {
			    chequearHC(nomClase, clases.get(nomClase).getNombreClasePadre(), new HashMap<String, Boolean>());
            }
		}
	}
	
	private void chequearHC(String claseBase, String ancestro, HashMap<String,Boolean> visitados) throws ExcepcionSemantica {
		if (ancestro != null){
			if (visitados.containsKey(ancestro)) {
                throw new ExcepcionHerenciaCircular(claseBase, clases.get(claseBase).getToken());
            }

			if (!ancestro.equals("Object")) {
				if (!clases.containsKey(ancestro))
					throw new ExcepcionClaseNoDeclarada(ancestro, clases.get(claseBase).getToken());
				else {
                    visitados.put(ancestro, true);
					chequearHC(claseBase, clases.get(ancestro).getNombreClasePadre(), visitados);
                }
			}
		}
	}
	
	private void chequearAtributos() throws ExcepcionSemantica {
		for (Clase cla : clases.values()) {
			for (VarInstancia atrib : cla.getAtributos().values()) {
                if (atrib.getRol() == null || !atrib.getRol().equals("Atributo de Instancia")) continue;
				String nombre = atrib.getToken().getLexema();
				if (cla.getMetodos().containsKey(nombre) || 
                   (cla.getConstructor() != null && cla.getConstructor().getToken().getLexema().equals(nombre)) || 
                    cla.getNombreClase().equals(nombre)) {
					throw new ExcepcionAtributoYaNombrado(nombre, atrib.getToken());
				}
			}
		}
	}

    // --- REFACTORIZACIÓN DEL CÓDIGO DE ARRANQUE ---
	public static void generarArranqueMain() {	
		GenCod.gen("PUSH simple_heap_init");
		GenCod.gen("CALL");
		GenCod.gen("PUSH "+main.getLabel());
		GenCod.gen("CALL");
		GenCod.gen("HALT");	
	}

    public static void generarRutinasSistema() {
        GenCod.gen("simple_heap_init","RET 0","Retorna inmediatamente");
		GenCod.gen("simple_malloc","LOADFP","Inicialización unidad");
		GenCod.gen("LOADSP");
		GenCod.gen("STOREFP","Finaliza inicialización del RA");
		GenCod.gen("LOADHL","hl");
		GenCod.gen("DUP","hl");
		GenCod.gen("PUSH 1","1");
		GenCod.gen("ADD","hl+1");
		GenCod.gen("STORE 4","Guarda el resultado (un puntero a la primer celda de la región de memoria)");
		GenCod.gen("LOAD 3","Carga la cantidad de celdas a alojar (parámetro que debe ser positivo)");
		GenCod.gen("ADD");
		GenCod.gen("STOREHL","Mueve el heap limit (hl). Expande el heap");
		GenCod.gen("STOREFP");
		GenCod.gen("RET 1","Retorna eliminando el parámetro");
		
		GenCod.gen("error_cast","PUSH mensaje_error_cast","Apilo msj de error");
		GenCod.gen("SPRINT");
		GenCod.gen("PRNLN");
		GenCod.gen("HALT");
    }

    public static void generarDatosSistema() {
        GenCod.gen("mensaje_error_cast","DW \"[Excepcion en tiempo de ejecución] Se produjo un error al intentar realizar un casteo.\",0","Mensaje para casteos erroneos");
    }
}
