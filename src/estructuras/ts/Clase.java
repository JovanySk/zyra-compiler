// Contenido completo, corregido para soportar sobrecarga de constructores
package estructuras.ts;

import estructuras.Token;
import estructuras.ast.sentencias.NodoBloque;
import estructuras.ts.variables.VarInstancia;
import excepciones.semanticas.ExcepcionAtributoDuplicado;
import excepciones.semanticas.ExcepcionClaseNoDeclarada;
import excepciones.semanticas.ExcepcionConstructorDuplicado;
import excepciones.semanticas.ExcepcionConstructorMalNombrado;
import excepciones.semanticas.ExcepcionHerenciaPropia;
import excepciones.semanticas.ExcepcionMetodoDuplicado;
import excepciones.semanticas.ExcepcionMetodoMalHeredado;
import excepciones.semanticas.ExcepcionSemantica;
import excepciones.sintacticas.ExcepcionHerenciaInvalida;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import modulos.GenCod;
import estructuras.ast.expresiones.NodoExpresion;
import estructuras.ts.variables.VarParametro;

public class Clase {

	private Token identificador;
	private String herencia;
	// --- CAMBIO: De un solo Constructor a una lista de Constructores ---
	private LinkedList<Constructor> constructores;
	private HashMap<String, LinkedList<Metodo>> metodos;
	private HashMap<String, VarInstancia> atributos;
	private boolean estaActualizada;
	private int cantAtrib;
	private int cantMet;
	private int id;

	public Clase(Token iden, String padre) {
		identificador = iden;
		herencia = padre;
		// --- CAMBIO: Inicializar la lista ---
		constructores = new LinkedList<>();
		metodos = new HashMap<>();
		atributos = new HashMap<>();
		estaActualizada = false;
		cantAtrib = 0;
		cantMet = 0;
		id = GenCod.generarNumClase();
	}

	public Clase(Token iden) {
		this(iden, "Object");
	}

	public void agregarAtributo(VarInstancia v) throws ExcepcionSemantica {
		String nombreAtrib = v.getToken().getLexema();
		if (atributos.containsKey(nombreAtrib)) {
			throw new ExcepcionAtributoDuplicado(nombreAtrib, v.getToken());
		} else
			atributos.put(nombreAtrib, v);
	}

	public void agregarMetodo(Metodo m) throws ExcepcionSemantica {
		String nombreMet = m.getToken().getLexema();

		LinkedList<Metodo> sobrecargas = metodos.get(nombreMet);

		if (sobrecargas == null) {
			sobrecargas = new LinkedList<Metodo>();
			sobrecargas.add(m);
			metodos.put(nombreMet, sobrecargas);
		} else {
			for (Metodo metodoExistente : sobrecargas) {
				if (metodoExistente.comparar(m)) {
					throw new ExcepcionMetodoDuplicado(nombreMet, m.getToken());
				}
			}
			sobrecargas.add(m);
		}
	}

	// --- CAMBIO: Devuelve el primer constructor, útil para el chequeo de "main" o
	// por defecto ---
	public Constructor getConstructor() {
		if (constructores.isEmpty())
			return null;
		return constructores.getFirst();
	}

	// --- CAMBIO: Devuelve la lista completa de constructores ---
	public LinkedList<Constructor> getConstructores() {
		return constructores;
	}

	// --- CAMBIO: Lógica para manejar sobrecarga de constructores ---
	public void setConstructor(Constructor nuevoConstructor) throws ExcepcionSemantica {
		if (nuevoConstructor == null || nuevoConstructor.getToken() == null) {
			return;
		}
		if (!nuevoConstructor.getToken().getLexema().equals(identificador.getLexema())) {
			throw new ExcepcionConstructorMalNombrado(nuevoConstructor.getToken().getLexema(),
					nuevoConstructor.getToken());
		}

		for (Constructor existente : constructores) {
			if (existente.comparar(nuevoConstructor)) {
				throw new ExcepcionConstructorDuplicado(nuevoConstructor.getToken().getLexema(),
						nuevoConstructor.getToken());
			}
		}
		constructores.add(nuevoConstructor);
	}

	public HashMap<String, LinkedList<Metodo>> getMetodos() {
		return metodos;
	}

	public Metodo getMetodo(String nombre, LinkedList<VarParametro> params) throws ExcepcionSemantica {
		LinkedList<Metodo> sobrecargas = metodos.get(nombre);
		if (sobrecargas != null) {
			for (Metodo m : sobrecargas) {
				if (m.getListaParametros().size() == params.size()) {
					boolean coinciden = true;
					for (int i = 0; i < params.size(); i++) {
						if (!params.get(i).getTipoVar().getNombreClase()
								.equals(m.getListaParametros().get(i).getTipoVar().getNombreClase())) {
							coinciden = false;
							break;
						}
					}
					if (coinciden)
						return m;
				}
			}
		}
		return null;
	}

	public HashMap<String, VarInstancia> getAtributos() {
		if (!this.getNombreClase().equals("Object") && !this.getNombreClase().equals("System")) {
			Clase padre = TablaSimbolos.clases.get(herencia);
			if (padre != null) {
				HashMap<String, VarInstancia> atributosHeredados = new HashMap<>(padre.getAtributos());
				atributosHeredados.putAll(atributos);
				return atributosHeredados;
			}
		}
		return atributos;
	}

	public boolean estaActualizada() {
		return estaActualizada;
	}

	public void setEstaActualizada(boolean estaActualizada) {
		this.estaActualizada = estaActualizada;
	}

	public void setPadre(String padre) throws ExcepcionSemantica, ExcepcionHerenciaInvalida {
		if (identificador.getLexema().equals(padre))
			throw new ExcepcionHerenciaPropia(padre, identificador);
		if (padre.equals("System") || padre.equals("SystemIO"))
			throw new ExcepcionHerenciaInvalida(this.getNombreClase(), padre, identificador);
		else
			herencia = padre;
	}

	public String getNombreClase() {
		return identificador.getLexema();
	}

	public String getNombreClasePadre() {
		return herencia;
	}

	public Token getToken() {
		return identificador;
	}

	public void chequearDeclaraciones() throws ExcepcionSemantica {
		chequearConstructorSiNoExiste();

		if (!this.getNombreClase().equals("Object")) {
			if (!TablaSimbolos.clases.containsKey(herencia)) {
				throw new ExcepcionClaseNoDeclarada(herencia, identificador);
			}
		}
		for (VarInstancia atrib : atributos.values()) {
			if (!atrib.getTipoVar().esValido()) {
				throw new ExcepcionClaseNoDeclarada(atrib.getTipoVar().getNombreClase(), atrib.getToken());
			}
		}
		for (LinkedList<Metodo> listaMets : metodos.values()) {
			for (Metodo met : listaMets) {
				met.chequearDeclaraciones();
			}
		}

		// --- CAMBIO: Chequear todos los constructores ---
		for (Constructor ctor : constructores) {
			ctor.chequearDeclaraciones();
		}
	}

	public void chequearSentencias() throws ExcepcionSemantica {
		TablaSimbolos.claseActual = this;

		// --- CAMBIO: Chequear todos los constructores ---
		for (Constructor ctor : constructores) {
			TablaSimbolos.metodoActual = ctor;
			TablaSimbolos.bloqueActual = ctor.getCodigo();
			ctor.chequearSentencias();
		}

		for (LinkedList<Metodo> listaMets : metodos.values()) {
			for (Metodo met : listaMets) {
				TablaSimbolos.metodoActual = met;
				TablaSimbolos.bloqueActual = met.getCodigo();
				if (met.getClaseImplementadora().getNombreClase().equals(TablaSimbolos.claseActual.getNombreClase()))
					met.chequearSentencias();
			}
		}
	}

	public void generarVTable() {
		List<Metodo> listaDinamicos = new LinkedList<>();
		for (LinkedList<Metodo> listaMets : metodos.values()) {
			for (Metodo met : listaMets) {
				if (!met.getEsEstatico())
					listaDinamicos.add(met);
			}
		}

		Collections.sort(listaDinamicos);

		if (listaDinamicos.isEmpty()) {
			GenCod.gen("VT_" + getLabel(), "NOP", "VT de la clase " + getNombreClase());
		} else {
			GenCod.label("VT_" + getLabel());
			for (Metodo met : listaDinamicos) {
				GenCod.gen("DW " + met.getLabel(),
						"Metodo " + met.getToken().getLexema() + " cuyo offset es " + met.getOffset());
			}
		}
	}

	public void generar(modulos.ir.GeneradorCodigoIntermedio gci) {
		// --- CAMBIO: Generar código para todos los constructores ---
		for (Constructor ctor : constructores) {
			ctor.generarSentencias(gci);
		}

		for (LinkedList<Metodo> listaMets : metodos.values()) {
			for (Metodo met : listaMets) {
				if (met.getClaseImplementadora().getNombreClase().equals(this.getNombreClase()))
					met.generarSentencias(gci);
			}
		}
	}

	public void chequearConstructorSiNoExiste() {
		// --- CAMBIO: Solo añade un constructor por defecto si la lista está vacía ---
		if (constructores.isEmpty()) {
			Constructor ctor = new Constructor(new Token("idClase", this.getNombreClase(), 0, 0));
			ctor.setCodigo(new NodoBloque());
			constructores.add(ctor);
		}
	}

	public void actualizar() throws ExcepcionSemantica {
		if (!estaActualizada) {
			if (!identificador.getLexema().equals("Object")) {
				Clase padre = TablaSimbolos.clases.get(herencia);
				padre.actualizar();

				cantAtrib = padre.getCantAtrib();
				for (VarInstancia vi : atributos.values()) {
					if (vi.getRol() != null && vi.getRol().equals("Atributo de Instancia")) {
						vi.setOffset(cantAtrib + 2);
						cantAtrib++;
					}
				}

				for (VarInstancia atrPadre : padre.getAtributos().values()) {
					String nombreAtrib = atrPadre.getToken().getLexema();
					if (atributos.containsKey(nombreAtrib)) {
						// No se agrega, el hijo redefine.
					} else {
						atributos.put(nombreAtrib, atrPadre);
					}
				}

				for (String nombreMet : padre.getMetodos().keySet()) {
					LinkedList<Metodo> metodosPadre = padre.getMetodos().get(nombreMet);
					LinkedList<Metodo> metodosHijo = metodos.get(nombreMet);

					for (Metodo metPadre : metodosPadre) {
						if (!metPadre.getEsEstatico()) {
							boolean redefinido = false;
							if (metodosHijo != null) {
								for (Metodo metHijo : metodosHijo) {
									if (metHijo.comparar(metPadre)) {
										metHijo.setOffset(metPadre.getOffset());
										redefinido = true;
										break;
									}
								}
							}
							if (!redefinido) {
								this.agregarMetodo(metPadre);
							}
						}
					}
				}

				cantMet = padre.getCantMet();
				for (LinkedList<Metodo> listaMets : metodos.values()) {
					for (Metodo met : listaMets) {
						if (!met.getEsEstatico() && !met.estaChequeado()) {
							met.setOffset(cantMet++);
						}
					}
				}
				estaActualizada = true;
			}
		}
	}

	public int getCantAtrib() {
		return cantAtrib;
	}

	public void setCantAtrib(int cantAtrib) {
		this.cantAtrib = cantAtrib;
	}

	public int getCantMet() {
		return cantMet;
	}

	public void setCantMet(int cantMet) {
		this.cantMet = cantMet;
	}

	public String getLabel() {
		return identificador.getLexema() + "_" + id;
	}

	public int getId() {
		return id;
	}
}
