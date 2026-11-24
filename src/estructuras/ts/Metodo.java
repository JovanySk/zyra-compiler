// Contenido completo y CORREGIDO con visibilidad y chequeo de return mejorado
package estructuras.ts;

import estructuras.Token;
import estructuras.ast.sentencias.NodoBloque;
import estructuras.ast.sentencias.NodoBloqueSystem;
import estructuras.ast.sentencias.NodoIf;
import estructuras.ast.sentencias.NodoReturn;
import estructuras.ast.sentencias.NodoSentencia;
import estructuras.ts.tipos.TipoMetodo;
import estructuras.ts.variables.VarParametro;
import estructuras.ts.variables.Variable;
import excepciones.semanticas.ExcepcionClaseNoDeclarada;
import excepciones.semanticas.ExcepcionSemantica;
import excepciones.semanticas.ExcepcionSemanticaPersonalizada;
import java.util.Collections;
import java.util.LinkedList;
import modulos.GenCod;
import estructuras.ast.expresiones.NodoExpresion;

public class Metodo extends Unidad implements Comparable<Metodo> {

    private boolean esEstatico;
    private TipoMetodo tipoRetorno;
    private Clase claseImplementadora;
    private boolean esPublico;

    public Metodo(Token id, boolean estatico, TipoMetodo retorno, Clase cla, boolean esPublico) {
        super(id);
        this.esEstatico = estatico;
        this.tipoRetorno = retorno;
        this.setClaseImplementadora(cla);
        this.esPublico = esPublico;
    }

    public Metodo(Token id, boolean estatico, TipoMetodo retorno, Clase cla) {
        this(id, estatico, retorno, cla, true);
    }

    public Metodo(Token id, LinkedList<VarParametro> listaPar, boolean estatico, TipoMetodo retorno, Clase cla,
            boolean esPublico) {
        super(id, listaPar);
        this.esEstatico = estatico;
        this.tipoRetorno = retorno;
        this.setClaseImplementadora(cla);
        this.esPublico = esPublico;
    }

    public Metodo(Token id, LinkedList<VarParametro> listaPar, boolean estatico, TipoMetodo retorno, Clase cla) {
        this(id, listaPar, estatico, retorno, cla, true);
    }

    public boolean getEsEstatico() {
        return esEstatico;
    }

    public TipoMetodo getTipoRetorno() {
        return tipoRetorno;
    }

    public boolean getEsPublico() {
        return esPublico;
    }

    public Clase getClaseImplementadora() {
        return claseImplementadora;
    }

    public void setClaseImplementadora(Clase claseImplementadora) {
        this.claseImplementadora = claseImplementadora;
    }

    public boolean esConstructor() {
        return false;
    }

    @Override
    public void chequearDeclaraciones() throws ExcepcionSemantica {
        if (!tipoRetorno.esVoid() && !tipoRetorno.esValido()) {
            throw new ExcepcionClaseNoDeclarada(tipoRetorno.getNombreClase(), this.getToken());
        }
        for (Variable var : this.getTablaVariables().values()) {
            if (!var.getTipoVar().esValido()) {
                throw new ExcepcionClaseNoDeclarada(var.getTipoVar().getNombreClase(), var.getToken());
            }
        }
    }

    private boolean tieneReturnGarantizado(NodoSentencia sentencia) {
        if (sentencia == null) {
            return false;
        }
        if (sentencia instanceof NodoReturn) {
            return true;
        }

        if (sentencia instanceof NodoBloque) {
            NodoBloque bloque = (NodoBloque) sentencia;
            for (NodoSentencia s : bloque.getSentencias()) {
                if (tieneReturnGarantizado(s)) {
                    return true;
                }
            }
            return false;
        }

        if (sentencia instanceof NodoIf) {
            NodoIf nodoIf = (NodoIf) sentencia;
            if (nodoIf.getSentenciaElse() == null) {
                return false;
            }
            return tieneReturnGarantizado(nodoIf.getSentencia()) && tieneReturnGarantizado(nodoIf.getSentenciaElse());
        }
        return false;
    }

    @Override
    public void chequearSentencias() throws ExcepcionSemantica {
        TablaSimbolos.bloqueActual = this.getCodigo();
        this.getCodigo().chequear();

        if (this.getCodigo() instanceof NodoBloqueSystem) {
            return;
        }

        if (!this.getTipoRetorno().esVoid()) {
            if (!tieneReturnGarantizado(this.getCodigo())) {
                throw new ExcepcionSemanticaPersonalizada(this.getToken().getLinea(), "El método no-void '"
                        + this.getToken().getLexema()
                        + "' debe garantizar una sentencia 'return' en todos sus posibles caminos de ejecución.",
                        this.getToken());
            }
        }
    }

    // Metodo modificado para usar GCI global y agregar marcadores de función
    @Override
    public void generarSentencias(modulos.ir.GeneradorCodigoIntermedio gci) {
        LinkedList<VarParametro> listaParam = new LinkedList<>(this.getListaParametros());
        Collections.sort(listaParam);
        int cantParametros = this.getListaParametros().size();
        for (int i = 0; i < listaParam.size(); i++) {
            if (esEstatico) {
                listaParam.get(i).setOffset(cantParametros + 2 - i);
            } else {
                listaParam.get(i).setOffset(cantParametros + 3 - i);
            }
        }

        GenCod.gen(getLabel(), "LOADFP", "Guardar enlace dinamico");
        GenCod.gen("LOADSP", "Inicializar el FP");
        GenCod.gen("STOREFP", "");

        int cantVarsLocales = this.getTablaVariables().size() - this.getListaParametros().size();

        // === MARCAR INICIO DE FUNCIÓN EN IR ===
        gci.marcarInicioFuncion(this.getToken().getLexema());

        // Generar IR del código del método
        this.getCodigo().generar(gci);

        // === MARCAR FIN DE FUNCIÓN EN IR ===
        gci.marcarFinFuncion(this.getToken().getLexema());

        if (this.getTipoRetorno().esVoid()) {
            if (cantVarsLocales > 0)
                GenCod.gen("FMEM " + cantVarsLocales, "Liberar espacio para variables locales");
            GenCod.gen("STOREFP", "Reestablece el contexto");
            int cantParamsParaRet = esEstatico ? cantParametros : cantParametros + 1;
            GenCod.gen("RET " + cantParamsParaRet, "Retorna de la llamada, liberando parametros");
        }
    }

    public boolean comparar(Metodo met) {
        if (!this.getToken().getLexema().equals(met.getToken().getLexema()))
            return false;
        if (esEstatico != met.getEsEstatico())
            return false;
        if (!tipoRetorno.getNombreClase().equals(met.getTipoRetorno().getNombreClase()))
            return false;
        if (this.getListaParametros().size() != met.getListaParametros().size())
            return false;

        LinkedList<VarParametro> listaArgsMet = met.getListaParametros();
        LinkedList<VarParametro> listaArgsThis = this.getListaParametros();
        for (int i = 0; i < listaArgsMet.size(); i++) {
            if (!listaArgsThis.get(i).getTipoVar().getNombreClase()
                    .equals(listaArgsMet.get(i).getTipoVar().getNombreClase()))
                return false;
        }
        return true;
    }

    public boolean compararArgs(LinkedList<NodoExpresion> args) throws ExcepcionSemantica {
        if (this.getListaParametros().size() != args.size())
            return false;

        LinkedList<VarParametro> listaArgsThis = this.getListaParametros();
        for (int i = 0; i < args.size(); i++) {
            TipoMetodo tipoParamActual = args.get(i).chequear();
            TipoMetodo tipoParamFormal = listaArgsThis.get(i).getTipoVar();
            if (!tipoParamActual.conforma(tipoParamFormal))
                return false;
        }
        return true;
    }

    @Override
    public int compareTo(Metodo met) {
        return new Integer(this.getOffset()).compareTo(new Integer(met.getOffset()));
    }
}
