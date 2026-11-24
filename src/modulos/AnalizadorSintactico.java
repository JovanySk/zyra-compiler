// Archivo: src/modulos/AnalizadorSintactico.java
// VERSIÓN COMPLETA Y CORREGIDA para reconocer literales decimales.
package modulos;

import estructuras.Helper;
import estructuras.Token;
import estructuras.TablaTokens;
import estructuras.ast.NodoAtributo;
import estructuras.ast.NodoClase;
import estructuras.ast.NodoMetodo;
import estructuras.ast.NodoPrograma;
import estructuras.ast.encadenados.*;
import estructuras.ast.expresiones.*;
import estructuras.ast.sentencias.NodoSentenciaCompuesta;
import estructuras.ast.sentencias.*;
import estructuras.ts.Clase;
import estructuras.ts.Constructor;
import estructuras.ts.Metodo;
import estructuras.ts.TablaSimbolos;
import estructuras.ts.tipos.*;
import estructuras.ts.variables.VarInstancia;
import estructuras.ts.variables.VarLocal;
import estructuras.ts.variables.VarParametro;
import excepciones.semanticas.ExcepcionSemantica;
import excepciones.sintacticas.*;
import java.util.LinkedList;
import java.util.List;

public class AnalizadorSintactico {
	
	private Token lookAhead;
	private Token lookBehind;
	private final TablaSimbolos TS;
	private final List<Token> tokens;
    private int tokenIndex;
    private final NodoPrograma nodoPrograma;
    private NodoClase nodoClaseActual;

    public AnalizadorSintactico(List<Token> allTokens) throws ExcepcionSemantica {
        this.tokens = allTokens;
        this.tokenIndex = 0;
        this.lookAhead = tokens.get(tokenIndex);
		this.lookBehind = null;
		this.TS = new TablaSimbolos();
        this.nodoPrograma = new NodoPrograma();
		cargarClasesSistema();
	}
    
    public TablaSimbolos getTablaSimbolos() {
        return TS;
    }

    public NodoPrograma getAST() {
        return nodoPrograma;
    }
    
	public void analizar() throws ExcepcionSintactica, ExcepcionSemantica {
		inicial();
        match("EOF");
	}

	private void inicial() throws ExcepcionSintactica, ExcepcionSemantica {
        if (!lookAhead.comparar("PR_Class")) {
            throw new ExcepcionSintacticaPersonalizada(
                lookAhead.getLinea(),
                lookAhead.getColumna(),
                "Todo programa Zyra debe comenzar con la declaración de una clase (usando la palabra clave 'class').",
                lookAhead.getLexema()
            );
        }

		while (lookAhead.comparar("PR_Class")) {
            clase();
        }

        if (!lookAhead.comparar("EOF")) {
            throw new ExcepcionSintacticaPersonalizada(
                lookAhead.getLinea(),
                lookAhead.getColumna(),
                "Se encontraron tokens inesperados después de la última declaración de clase. Se esperaba el fin del archivo.",
                lookAhead.getLexema()
            );
        }
	}
    
	private void clase() throws ExcepcionSintactica, ExcepcionSemantica {
        Token tokenClass = lookAhead; 
        match("PR_Class");

        if (!lookAhead.comparar("idClase")) {
            throw new ExcepcionSintacticaPersonalizada(
                tokenClass.getLinea(), 
                tokenClass.getColumna() + tokenClass.getLexema().length() + 1,
                "un identificador de clase (ej: class MiClase)",
                lookAhead.getLexema()
            );
        }
        
        match("idClase");
        Token idClase = lookBehind;
        
        nodoClaseActual = new NodoClase(idClase);
        nodoPrograma.agregarClase(nodoClaseActual);

        TS.claseActual = new Clase(idClase);
        TS.getClases().put(TS.claseActual.getNombreClase(), TS.claseActual);
        
        if (lookAhead.comparar("PR_Extends")) {
            herencia();
        } else {
            TS.claseActual.setPadre("Object");
        }
        
        match("P_Llave_A");
        listaMiembro();
        match("P_Llave_C");
	}
    
    private void listaMiembro() throws ExcepcionSintactica, ExcepcionSemantica {
		while (!lookAhead.comparar("P_Llave_C") && !lookAhead.getNombre().equals("EOF")) {
            miembro();
        }
	}
    
	private void miembro() throws ExcepcionSintactica, ExcepcionSemantica {
        if (lookAhead.getNombre().equals("PR_Public") || lookAhead.getNombre().equals("PR_Private") || Helper.esTipoValido(lookAhead.getNombre())) {
            Token preanalisisTipo = lookAhead;
            Token preanalisisID = tokens.get(tokenIndex + 1);

            if (preanalisisTipo.getNombre().equals("idClase") && preanalisisID.getNombre().equals("P_Parentesis_A")) {
                ctor();
            } else {
                atributo();
            }
        } else if (lookAhead.getNombre().equals("PR_Static") || lookAhead.getNombre().equals("PR_Dynamic")) {
             metodo();
        } else if (lookAhead.getNombre().equals("idClase")) {
             ctor();
        } else {
            throw new ExcepcionSintacticaPersonalizada(lookAhead.getLinea(), lookAhead.getColumna(), "un miembro de clase (atributo, método, constructor)", lookAhead.getLexema());
        }
	}

	private void metodo() throws ExcepcionSintactica, ExcepcionSemantica {
		boolean esEstatico = formaMetodo();
        boolean esPublico = visibilidad(); 
		TipoMetodo tipo = tipoMetodo();
        
        if (lookAhead.comparar("idClase") && lookAhead.getLexema().equals("Main")) {
             throw new ExcepcionSintacticaPersonalizada(
                lookAhead.getLinea(),
                lookAhead.getColumna(),
                "El método principal debe llamarse 'main' (en minúsculas).",
                lookAhead.getLexema()
            );
        }

		match("idMetVar");
        Token idMetodo = lookBehind;
        
		Metodo met = new Metodo(idMetodo, esEstatico, tipo, TS.claseActual, esPublico);
		TS.claseActual.agregarMetodo(met);
		TS.metodoActual = met;
        
		argsFormales();
		NodoBloque bloq = bloque();
		TS.metodoActual.setCodigo(bloq);
        
        LinkedList<VarParametro> parametros = TS.metodoActual.getListaParametros();
        NodoMetodo nodoMet = new NodoMetodo(idMetodo, tipo, bloq, parametros, esPublico, esEstatico);
        nodoClaseActual.agregarMetodo(nodoMet);
	}
    
    private void match(String tipoToken) throws ExcepcionSintactica {
		if (lookAhead.comparar(tipoToken)) {
			consumirToken();
		} else { 
			String esperado;
            TablaTokens tt = new TablaTokens();
            if (tipoToken.equals(tt.ID_MET_VAR)) {
                esperado = "un identificador para método o variable";
            } else if (tipoToken.equals(tt.ID_CLASE)) {
                esperado = "un identificador de Clase";
            } else {
                esperado = "el token '" + TablaTokens.traducirToken(tipoToken) + "'";
            }
			throw new ExcepcionSintactica(lookAhead.getLinea(), lookAhead.getColumna(), esperado, lookAhead.getLexema());
		}
	}

	private void consumirToken() {
        if (tokenIndex < tokens.size() - 1) {
            lookBehind = lookAhead;
            tokenIndex++;
            lookAhead = tokens.get(tokenIndex);
        }
    }

    private void cargarClasesSistema() throws ExcepcionSemantica {
		Token tok = new Token("idClase","Object",0,0);
		Clase cla = new Clase(tok,null);
		TS.agregarClase("Object",cla);
		
		tok = new Token("idClase","System",0,0);
		cla = new Clase(tok,"Object");
		TS.agregarClase("System",cla);
		TS.claseActual = cla;
		
		Metodo met = new Metodo(new Token("idMetVar","read",0,0),true,new TipoInt(),TS.claseActual, true);
		met.setCodigo(new NodoBloqueSystem("read"));
		TS.claseActual.agregarMetodo(met);
		
		LinkedList<VarParametro> listaPar = new LinkedList<>();
		VarParametro varP = new VarParametro(new Token("idMetVar","b",0,0),new TipoBoolean());
		listaPar.add(varP);
		met = new Metodo(new Token("idMetVar","printB",0,0),listaPar,true,new TipoVoid(),TS.claseActual, true);	
		met.setCodigo(new NodoBloqueSystem("printB"));
		TS.claseActual.agregarMetodo(met);
		
		listaPar = new LinkedList<>();
		varP = new VarParametro(new Token("idMetVar","c",0,0),new TipoChar());
		listaPar.add(varP);
		met = new Metodo(new Token("idMetVar","printC",0,0),listaPar,true,new TipoVoid(),TS.claseActual, true);	
		met.setCodigo(new NodoBloqueSystem("printC"));
		TS.claseActual.agregarMetodo(met);
		
		listaPar = new LinkedList<>();
		varP = new VarParametro(new Token("idMetVar","i",0,0),new TipoInt());
		listaPar.add(varP);
		met = new Metodo(new Token("idMetVar","printI",0,0),listaPar,true,new TipoVoid(),TS.claseActual, true);	
		met.setCodigo(new NodoBloqueSystem("printI"));
		TS.claseActual.agregarMetodo(met);
		
		listaPar = new LinkedList<>();
		varP = new VarParametro(new Token("idMetVar","s",0,0),new TipoString());
		listaPar.add(varP);
		met = new Metodo(new Token("idMetVar","printS",0,0),listaPar,true,new TipoVoid(),TS.claseActual, true);	
		met.setCodigo(new NodoBloqueSystem("printS"));
		TS.claseActual.agregarMetodo(met);
		
		met = new Metodo(new Token("idMetVar","println",0,0),true,new TipoVoid(),TS.claseActual, true);	
		met.setCodigo(new NodoBloqueSystem("println"));
		TS.claseActual.agregarMetodo(met);
		
		listaPar = new LinkedList<>();
		varP = new VarParametro(new Token("idMetVar","b",0,0),new TipoBoolean());
		listaPar.add(varP);
		met = new Metodo(new Token("idMetVar","printBln",0,0),listaPar,true,new TipoVoid(),TS.claseActual, true);	
		met.setCodigo(new NodoBloqueSystem("printBln"));
		TS.claseActual.agregarMetodo(met);
		
		listaPar = new LinkedList<>();
		varP = new VarParametro(new Token("idMetVar","c",0,0),new TipoChar());
		listaPar.add(varP);
		met = new Metodo(new Token("idMetVar","printCln",0,0),listaPar,true,new TipoVoid(),TS.claseActual, true);	
		met.setCodigo(new NodoBloqueSystem("printCln"));
		TS.claseActual.agregarMetodo(met);
		
		listaPar = new LinkedList<>();
		varP = new VarParametro(new Token("idMetVar","i",0,0),new TipoInt());
		listaPar.add(varP);
		met = new Metodo(new Token("idMetVar","printIln",0,0),listaPar,true,new TipoVoid(),TS.claseActual, true);	
		met.setCodigo(new NodoBloqueSystem("printIln"));
		TS.claseActual.agregarMetodo(met);
		
		listaPar = new LinkedList<>();
		varP = new VarParametro(new Token("idMetVar","s",0,0),new TipoString());
		listaPar.add(varP);
		met = new Metodo(new Token("idMetVar","printSln",0,0),listaPar,true,new TipoVoid(),TS.claseActual, true);	
		met.setCodigo(new NodoBloqueSystem("printSln"));
		TS.claseActual.agregarMetodo(met);	
	}

	private void atributo() throws ExcepcionSintactica, ExcepcionSemantica {
		boolean esPublico = visibilidad();
		Tipo tipoAtrib = tipo();
		LinkedList<Token> listaVars = listaDecVars();
		for (Token var : listaVars) {
			VarInstancia varInst = new VarInstancia(var, tipoAtrib, esPublico);
			TS.claseActual.agregarAtributo(varInst);
            nodoClaseActual.agregarAtributo(new NodoAtributo(var, tipoAtrib, esPublico));
		}
		match("P_Puntocoma");
	}
    
	private void ctor() throws ExcepcionSintactica, ExcepcionSemantica {
		match("idClase");
        Token idCtor = lookBehind;

		Constructor ctor = new Constructor(idCtor);
		TS.claseActual.setConstructor(ctor);
		TS.metodoActual = ctor;
		
        argsFormales();
		NodoBloque bloq = bloque();
		TS.metodoActual.setCodigo(bloq);
        
        nodoClaseActual.agregarConstructor(bloq);
	}
    
    private void herencia() throws ExcepcionSintactica, ExcepcionSemantica {
		match("PR_Extends");
		match("idClase");
		TS.claseActual.setPadre(lookBehind.getLexema());
	}
    
    private void argsFormales() throws ExcepcionSintactica, ExcepcionSemantica {
		match("P_Parentesis_A");
		listaArgsFormalesOpc();
	}
	
	private void listaArgsFormalesOpc() throws ExcepcionSintactica, ExcepcionSemantica {
		if (lookAhead.comparar("P_Parentesis_C")) {
			match("P_Parentesis_C");
		} else if (Helper.esTipoValido(lookAhead.getNombre())) {
            listaArgsFormales();
            match("P_Parentesis_C");
        } else {
            throw new ExcepcionArgFormMalFormado(lookAhead.getLinea(), lookAhead.getColumna(), lookAhead.getLexema());
        }
	}
	
	private void listaArgsFormales() throws ExcepcionSintactica, ExcepcionSemantica {
		argFormal();
		restoListaArgsFormales();
	}
	
	private void argFormal() throws ExcepcionSintactica, ExcepcionSemantica {
		 if (Helper.esTipoValido(lookAhead.getNombre())) {
            Tipo tipo = tipo();
            match("idMetVar");
            VarParametro var = new VarParametro(lookBehind,tipo);
            TS.metodoActual.agregarParametro(var);
        } else {
            throw new ExcepcionSintacticaPersonalizada(lookAhead.getLinea(), lookAhead.getColumna(), "un tipo de dato válido", lookAhead.getLexema());
        }
	}
	
	private void restoListaArgsFormales() throws ExcepcionSintactica, ExcepcionSemantica {
		if (lookAhead.comparar("P_Coma")) {
			match("P_Coma");
			listaArgsFormales();
		} else if (!lookAhead.comparar("P_Parentesis_C"))
			throw new ExcepcionSintacticaPersonalizada(lookBehind.getLinea(), lookBehind.getColumna() ,"una coma ',' o un paréntesis de cierre ')'", lookBehind.getLexema()); 
	}
	
    private boolean formaMetodo() throws ExcepcionSintactica {
		boolean esEstatico = true;
		if (lookAhead.comparar("PR_Static")) 
			match("PR_Static");
		else if (lookAhead.comparar("PR_Dynamic")) {
				match("PR_Dynamic");
				esEstatico = false;
		} else throw new ExcepcionSintacticaPersonalizada(lookAhead.getLinea(), lookAhead.getColumna(), "'static' o 'dynamic'", lookAhead.getLexema());
		return esEstatico;
	}
	
	private TipoMetodo tipoMetodo() throws ExcepcionSintactica {
		if (lookAhead.comparar("PR_Void")) {
			match("PR_Void");
			return new TipoVoid();
		}
		else return tipo();
	}

	private NodoSentencia sentencia() throws ExcepcionSintactica, ExcepcionSemantica {
		if (Helper.esTipoValido(lookAhead.getNombre())) {
			return declaracionCompleta();
		}

		switch (lookAhead.getNombre()) {
			case "P_Puntocoma": match("P_Puntocoma"); return new NodoSentenciaVacia();
			case "PR_If": return sentenciaIf();
			case "PR_While": return sentenciaWhile();
			case "PR_For": return sentenciaFor();
            case "PR_Switch": return sentenciaSwitch();									
			case "PR_Return": return sentenciaReturn();
            case "PR_Break": match("PR_Break"); match("P_Puntocoma"); return new NodoBreak();
			case "P_Llave_A": return bloque();

			case "P_Parentesis_A":	
			case "PR_This":			
			case "idMetVar":
			case "PR_New": {
                NodoPrimario pri = primario();
                NodoSentencia sent = asigOSentencia(pri);
                
                try {
                    match("P_Puntocoma");
                } catch (ExcepcionSintactica e) {
                    throw new ExcepcionSintactica(
                        lookBehind.getLinea(), 
                        lookBehind.getColumna() + lookBehind.getLexema().length(),
                        "el token ';'",
                        lookAhead.getLexema()
                    );
                }
                return sent;
            }

            case "PR_Else":
                throw new ExcepcionSintacticaPersonalizada(lookAhead.getLinea(), lookAhead.getColumna(), "Se encontró un 'else' sin una sentencia 'if' que lo preceda.");
			default: 
                throw new ExcepcionSintacticaPersonalizada(lookAhead.getLinea(), lookAhead.getColumna(), "una sentencia válida", lookAhead.getLexema()); 
		}
	}
    
    private NodoSentencia declaracionCompleta() throws ExcepcionSintactica, ExcepcionSemantica {
        Tipo tipo = tipo();
        
        LinkedList<VarLocal> variablesDeclaradas = new LinkedList<>();
        LinkedList<NodoAsignacion> asignaciones = new LinkedList<>();

        do {
            match("idMetVar");
            Token idVar = lookBehind;
            
            VarLocal nuevaVar = new VarLocal(idVar, tipo);
            variablesDeclaradas.add(nuevaVar);

            if (lookAhead.comparar("O_Asignacion")) {
                Token opAsig = lookAhead;
                match("O_Asignacion");
                NodoExpresion expr = expresion();
                
                if (expr instanceof NodoLiteral) {
                    nuevaVar.setValor(((NodoLiteral) expr).getToken().getLexema());
                }
                
                NodoVariable varNodo = new NodoVariable(idVar);
                asignaciones.add(new NodoAsignacion(varNodo, expr, opAsig));
            }

            if (!lookAhead.comparar("P_Coma")) {
                break;
            }
            match("P_Coma");

        } while (true);

        match("P_Puntocoma");

        NodoDeclaracion declaracionGeneral = new NodoDeclaracion(tipo, variablesDeclaradas);

        if (asignaciones.isEmpty()) {
            return declaracionGeneral;
        }

        LinkedList<NodoSentencia> sentenciasBloque = new LinkedList<>();
        sentenciasBloque.add(declaracionGeneral);
        sentenciasBloque.addAll(asignaciones);
        
        return new NodoSentenciaCompuesta(sentenciasBloque);
    }

	private NodoBloque bloque() throws ExcepcionSintactica, ExcepcionSemantica {
        match("P_Llave_A");
        if (lookAhead.comparar("P_Llave_A")) {
            throw new ExcepcionSintacticaPersonalizada(lookAhead.getLinea(), lookAhead.getColumna(), "Se encontró una llave '{' inesperada. ¿Quizás escribiste '{{' por error al iniciar el bloque?", lookAhead.getLexema());
        }
        LinkedList<NodoSentencia> listaSent = listaSentencias();
        match("P_Llave_C");
        return new NodoBloque(listaSent);
	}

	private LinkedList<NodoSentencia> listaSentencias() throws ExcepcionSintactica, ExcepcionSemantica {
		LinkedList<NodoSentencia> listaSent = new LinkedList<>();
		while (!lookAhead.comparar("P_Llave_C") && !lookAhead.getNombre().equals("EOF")) {
			listaSent.add(sentencia());
		}
		return listaSent;
	}
    
	private NodoSentencia sentenciaReturn() throws ExcepcionSintactica {
		Token temp = lookAhead;
		match("PR_Return");
		NodoExpresion expr = expresionOpcional();
		match("P_Puntocoma");
		return new NodoReturn(temp,expr);
	}

    private NodoSentencia sentenciaIf() throws ExcepcionSintactica, ExcepcionSemantica {
		Token ifToken = lookAhead;
		match("PR_If");
		match("P_Parentesis_A");
		NodoExpresion expr = expresion();
		match("P_Parentesis_C");
		
        NodoSentencia sent = sentencia();
		
		NodoSentencia sentElse = null;
		if (lookAhead.comparar("PR_Else")) {
			sentElse = elseOpcional();
		}
		return new NodoIf(ifToken,expr,sent,sentElse);
	}
    
	private NodoSentencia sentenciaWhile() throws ExcepcionSintactica, ExcepcionSemantica {
		Token temp = lookAhead;
		match("PR_While");
		match("P_Parentesis_A");
		NodoExpresion expr = expresion();
		match("P_Parentesis_C");
		NodoSentencia sent = sentencia();
		return new NodoWhile(temp,expr,sent);
	}

	private NodoFor sentenciaFor() throws ExcepcionSintactica, ExcepcionSemantica {
		Token forToken = lookAhead;
		match("PR_For");
		match("P_Parentesis_A");
		NodoSentencia inicializacion = forInicial();
		match("P_Puntocoma");
		NodoExpresion condicion = null;
		if (!lookAhead.getNombre().equals("P_Puntocoma")) {
			condicion = expresion();
		}
		match("P_Puntocoma");
		NodoSentencia actualizacion = forActualizacion();
		match("P_Parentesis_C");
		NodoSentencia cuerpo = sentencia();
		return new NodoFor(forToken, inicializacion, condicion, actualizacion, cuerpo);
	}

	private NodoSentencia forInicial() throws ExcepcionSintactica, ExcepcionSemantica {
		if (lookAhead.getNombre().equals("P_Puntocoma")) {
			return new NodoSentenciaVacia();
		}
		
		if (Helper.esTipoValido(lookAhead.getNombre())) {
			return parseDeclaracionEnFor(); 
		} else {
			return asigOSentencia(primario());
		}
	}

    private NodoSentencia parseDeclaracionEnFor() throws ExcepcionSintactica, ExcepcionSemantica {
        Tipo tipo = tipo();
        match("idMetVar");
        Token idVar = lookBehind;

        LinkedList<VarLocal> listaVars = new LinkedList<>();
        listaVars.add(new VarLocal(idVar, tipo));
        NodoDeclaracion decl = new NodoDeclaracion(tipo, listaVars);

        if (lookAhead.comparar("O_Asignacion")) {
            Token tokenAsig = lookAhead;
            match("O_Asignacion");
            NodoExpresion expr = expresion();
            
            NodoVariable var = new NodoVariable(idVar);
            NodoAsignacion asig = new NodoAsignacion(var, expr, tokenAsig);
            
            LinkedList<NodoSentencia> sentenciasBloque = new LinkedList<>();
            sentenciasBloque.add(decl);
            sentenciasBloque.add(asig);
            return new NodoSentenciaCompuesta(sentenciasBloque);
        } else {
            return decl;
        }
    }

	private NodoSentencia forActualizacion() throws ExcepcionSintactica {
		if (lookAhead.getNombre().equals("P_Parentesis_C")) {
			return new NodoSentenciaVacia();
		}
		return asigOSentencia(primario());
	}
	
	private NodoSwitch sentenciaSwitch() throws ExcepcionSintactica, ExcepcionSemantica {
        match("PR_Switch");
        match("P_Parentesis_A");
        NodoExpresion expr = expresion();
        match("P_Parentesis_C");
        match("P_Llave_A");
        LinkedList<NodoCase> casos = bloqueSwitch();
        match("P_Llave_C");
        return new NodoSwitch(expr, casos);
    }

    private LinkedList<NodoCase> bloqueSwitch() throws ExcepcionSintactica, ExcepcionSemantica {
        LinkedList<NodoCase> listaCasos = new LinkedList<>();
        boolean haEncontradoDefault = false;
        while(lookAhead.comparar("PR_Case") || lookAhead.comparar("PR_Default")){
            if (lookAhead.comparar("PR_Case")) {
                if (haEncontradoDefault) {
                    throw new ExcepcionSintacticaPersonalizada(lookAhead.getLinea(), lookAhead.getColumna(), "Una etiqueta 'case' no puede aparecer después de la etiqueta 'default'.", lookAhead.getLexema());
                }
                listaCasos.add(clausulaCase());
            } else { 
                if (haEncontradoDefault) {
                    throw new ExcepcionSintacticaPersonalizada(lookAhead.getLinea(), lookAhead.getColumna(), "Solo se permite una etiqueta 'default' en una sentencia switch.", lookAhead.getLexema());
                }
                haEncontradoDefault = true;
                listaCasos.add(clausulaDefault());
            }
        }
        return listaCasos;
    }

    private NodoCase clausulaCase() throws ExcepcionSintactica, ExcepcionSemantica {
        match("PR_Case");
        NodoLiteral literal = literal();
        match("P_DosPuntos");
        LinkedList<NodoSentencia> sentencias = listaSentenciasSwitch();
        return new NodoCase(literal, sentencias);
    }

    private NodoCase clausulaDefault() throws ExcepcionSintactica, ExcepcionSemantica {
        match("PR_Default");
        match("P_DosPuntos");
        LinkedList<NodoSentencia> sentencias = listaSentenciasSwitch();
        return new NodoCase(null, sentencias);
    }

    private LinkedList<NodoSentencia> listaSentenciasSwitch() throws ExcepcionSintactica, ExcepcionSemantica {
        LinkedList<NodoSentencia> lista = new LinkedList<>();
        while(!lookAhead.comparar("PR_Case") && !lookAhead.comparar("PR_Default") && !lookAhead.comparar("P_Llave_C")) {
            lista.add(sentencia());
        }
        return lista;
    }

	private NodoSentencia elseOpcional() throws ExcepcionSintactica, ExcepcionSemantica {
		match("PR_Else");
		return sentencia();
	}

	private NodoExpresion expresionOpcional() throws ExcepcionSintactica {
		if (!lookAhead.comparar("P_Puntocoma")) {
			return expresion();
		}
		return null;
	}
    
	private NodoSentencia asigOSentencia(NodoPrimario pri) throws ExcepcionSintactica {
		if (lookAhead.comparar("O_Asignacion")) {
			Token temp = lookAhead;
			match("O_Asignacion");
			NodoExpresion expr = expresion();
			return new NodoAsignacion(pri,expr,temp);
		} else {
			return new NodoSentenciaLlamada(pri.getToken(), pri);
		}
	}

	private NodoPrimario primario() throws ExcepcionSintactica {
		NodoPrimario prim;
		
		switch (lookAhead.getNombre()) {
			case "PR_This":
				match("PR_This");
				prim = new NodoThis(lookBehind);
				break;					
			case "idMetVar":
				match("idMetVar");
				prim = primarioDesdeId(lookBehind);
				break;						
			case "PR_New":
                prim = expresionNew();
                break;
			case "P_Parentesis_A":
				prim = primarioParentizado();
				break;
			default:
				throw new ExcepcionSintacticaPersonalizada(lookAhead.getLinea(), lookAhead.getColumna(), "el inicio de una expresión primaria ('this', un id, 'new' o '(')", lookAhead.getLexema());  
		}
        
        return sufijos(prim);
	}

    private NodoPrimario sufijos(NodoPrimario primActual) throws ExcepcionSintactica {
        while(lookAhead.comparar("P_Punto") || lookAhead.comparar("P_Corchetes_A")) {
            NodoEncadenado enc;
            if (lookAhead.comparar("P_Punto")) {
                match("P_Punto");
                match("idMetVar");
                Token id = lookBehind;
                if(lookAhead.comparar("P_Parentesis_A")) {
                    enc = new NodoLlamadaEncadenada(id, argsActuales());
                } else {
                    if (id.getLexema().equals("length")) {
                        enc = new NodoAccesoLength(id);
                    } else {
                        enc = new NodoIdEncadenado(id);
                    }
                }
                primActual.setCadena(enc);
            } else { 
                match("P_Corchetes_A");
                NodoExpresion indice = expresion();
                match("P_Corchetes_C");
                primActual = new NodoAccesoArreglo(primActual, indice);
            }
        }
        return primActual;
    }

	private NodoPrimario expresionNew() throws ExcepcionSintactica {
        Token newToken = lookAhead;
        match("PR_New");
        Token tipoId = lookAhead;
        Tipo tipoBase = tipoSinCorchetes();

        if (lookAhead.comparar("P_Corchetes_A")) {
            match("P_Corchetes_A");
            NodoExpresion dim = expresion();
            match("P_Corchetes_C");
            return new NodoCreacionArreglo(newToken, tipoBase, dim);

        } else if (lookAhead.comparar("P_Parentesis_A")) { 
            if (!(tipoBase instanceof TipoClase)) {
                throw new ExcepcionSintacticaPersonalizada(tipoId.getLinea(), tipoId.getColumna(), "un nombre de clase antes de los argumentos del constructor", tipoId.getLexema());
            }
            LinkedList<NodoExpresion> listaParams = argsActuales();
            return new NodoConstructor(tipoId, listaParams, null);
        } else {
            throw new ExcepcionSintacticaPersonalizada(lookAhead.getLinea(), lookAhead.getColumna(), "argumentos de constructor '(...)' o dimensión de arreglo '[...]' al crear un objeto", lookAhead.getLexema());
        }
    }
	
	private NodoPrimario primarioDesdeId(Token tok) throws ExcepcionSintactica {
		if (lookAhead.comparar("P_Parentesis_A")) {
			return new NodoLlamadaDirecta(tok,argsActuales(),null);
		} else {
            return new NodoVariable(tok); 
        }
	}

	private NodoPrimario primarioParentizado() throws ExcepcionSintactica {
        Token parenA = lookAhead;
        match("P_Parentesis_A");
        NodoExpresion expr;

        if ("idClase".equals(lookAhead.getNombre())) {
            Token idc = lookAhead;
            consumirToken();
            if (lookAhead.comparar("P_Punto")) {
                match("P_Punto");
                match("idMetVar");
                Token temp = lookBehind;
                LinkedList<NodoExpresion> listaParam = argsActuales();
                match("P_Parentesis_C");
                return new NodoLlamadaEstatica(temp, idc, listaParam, null);
            } else {
                tokenIndex--;
                lookAhead = tokens.get(tokenIndex);
                if (tokenIndex > 0) lookBehind = tokens.get(tokenIndex - 1); else lookBehind = null;
            }
        }
        
        expr = expresion();
        match("P_Parentesis_C");
        return new NodoExpParentizada(parenA, expr, null);
	}
	
	private LinkedList<NodoExpresion> argsActuales() throws ExcepcionSintactica {
		match("P_Parentesis_A");
		LinkedList<NodoExpresion> args = listaExpsOpcional();
		match("P_Parentesis_C");
		return args;
	}
	
	private LinkedList<NodoExpresion> listaExpsOpcional() throws ExcepcionSintactica {
		if (!lookAhead.comparar("P_Parentesis_C")) 
			return listaExps();
		return new LinkedList<>();
	}
	
	private LinkedList<NodoExpresion> listaExps() throws ExcepcionSintactica {
		LinkedList<NodoExpresion> listaExpr;
		NodoExpresion expr = expresion();
		listaExpr = restoListaExps();
		listaExpr.addFirst(expr);
		return listaExpr;
	}
	
	private LinkedList<NodoExpresion> restoListaExps() throws ExcepcionSintactica {
		LinkedList<NodoExpresion> listaExpr = new LinkedList<>();
		if (lookAhead.comparar("P_Coma")) {
			match("P_Coma");
			listaExpr = listaExps();
		} else if (!lookAhead.comparar("P_Parentesis_C"))
			throw new ExcepcionSintacticaPersonalizada(lookBehind.getLinea(), lookBehind.getColumna() ,"una coma ',' o un paréntesis de cierre ')'", lookBehind.getLexema()); 
		return listaExpr;
	}
	
	private NodoExpresion expresion() throws ExcepcionSintactica {
		return expOr();
	}
	
	private NodoExpresion expOr() throws ExcepcionSintactica {
		NodoExpresion expr = expAnd();
		return expOrPrimo(expr);
	}

	private NodoExpresion expOrPrimo(NodoExpresion expr) throws ExcepcionSintactica {
		if (lookAhead.comparar("O_Or")){
			Token temp = lookAhead;
			match("O_Or");
			NodoExpresion ladoDer = expAnd();
			NodoExpresionBinaria neb = new NodoExpresionBinaria(temp,expr,ladoDer);
			return expOrPrimo(neb);
		}
        return expr;
	}
	
	private NodoExpresion expAnd() throws ExcepcionSintactica {
		NodoExpresion expr = expIgual();
		return expAndPrimo(expr);
	}
	
	private NodoExpresion expAndPrimo(NodoExpresion expr) throws ExcepcionSintactica {
		if (lookAhead.comparar("O_And")){
			Token temp = lookAhead;
			match("O_And");
			NodoExpresion ladoDer = expIgual();
			NodoExpresionBinaria neb = new NodoExpresionBinaria(temp,expr,ladoDer);
			return expAndPrimo(neb);
		}
        return expr;
	}
	
	private NodoExpresion expIgual() throws ExcepcionSintactica {
		NodoExpresion expr = expComp();
		return expIgualPrimo(expr);
	}
	
	private NodoExpresion expIgualPrimo(NodoExpresion expr) throws ExcepcionSintactica {
		if (lookAhead.comparar("O_Comparacion") || lookAhead.comparar("O_Distinto")) {
            Token temp = lookAhead;
            opIgual();
            NodoExpresion ladoDer = expComp();
            NodoExpresionBinaria neb = new NodoExpresionBinaria(temp,expr,ladoDer);
            return expIgualPrimo(neb);
        } else {
            return expr;
        }
	}
	
	private NodoExpresion expComp() throws ExcepcionSintactica {
		NodoExpresion expr = expAd();
		return restoExpComp(expr);
	}
	
	private NodoExpresion restoExpComp(NodoExpresion expr) throws ExcepcionSintactica {
		if (lookAhead.getNombre().startsWith("O_Mayor") || lookAhead.getNombre().startsWith("O_Menor")){
            Token temp = lookAhead;
            opComp();
            NodoExpresion ladoDer = expAd();
            return new NodoExpresionBinaria(temp,expr,ladoDer);
        } else if (lookAhead.comparar("PR_Instanceof")) {
			Token aux = lookAhead;
			match("PR_Instanceof");
			match("idClase");
			return new NodoExpresionBinaria(aux,expr,new NodoLiteral(lookBehind));
        } else {
            return expr;
        }
	}
	
	private NodoExpresion expAd() throws ExcepcionSintactica {
		NodoExpresion expr = expMul();
		return expAdPrimo(expr);
	}

	private NodoExpresion expAdPrimo(NodoExpresion expr)  throws ExcepcionSintactica {
		if (lookAhead.comparar("O_Suma") || lookAhead.comparar("O_Resta")) {
            Token temp = lookAhead;
            opAd();
            NodoExpresion ladoDer = expMul();
            NodoExpresionBinaria neb = new NodoExpresionBinaria(temp,expr,ladoDer);
            return expAdPrimo(neb);
        }
        return expr;
	}
	
	private NodoExpresion expMul() throws ExcepcionSintactica {
		NodoExpresion expr = expUnaria();
		return expMulPrimo(expr);
	}

	private NodoExpresion expMulPrimo(NodoExpresion expr)  throws ExcepcionSintactica {
		if (lookAhead.comparar("O_Mult") || lookAhead.comparar("O_Div") || lookAhead.comparar("O_Mod")) {
            Token temp = lookAhead;
            opMul();
            NodoExpresion ladoDer = expUnaria();
            NodoExpresionBinaria neb = new NodoExpresionBinaria(temp,expr,ladoDer);
            return expMulPrimo(neb);
        } else {
            return expr;
        }
	}
	
	private NodoExpresion expUnaria() throws ExcepcionSintactica {
		if (lookAhead.getNombre().startsWith("O_Suma") || lookAhead.getNombre().startsWith("O_Resta") || lookAhead.getNombre().startsWith("O_Not")) {
            Token temp = lookAhead;
            opUnario();
            NodoExpresion ladoDer = expUnaria();
            return new NodoExpresionUnaria(temp,ladoDer);
        } else if (lookAhead.comparar("P_Corchetes_A")) { // Casteo
			match("P_Corchetes_A");
			Token temp = lookAhead;
			match("idClase");
			match("P_Corchetes_C");
			NodoExpresion exprUn = expUnaria();
			return new NodoExpresionUnaria(temp, exprUn);
        } else {
            return operando();	
        }
	}
	
	private NodoExpPrimario operando() throws ExcepcionSintactica {
		switch (lookAhead.getNombre()) {
			case "PR_Null": case "PR_False": case "PR_True":
			case "L_Entero": case "L_Caracter": case "L_String": case "L_Decimal":
                return literal();
			case "P_Parentesis_A": case "PR_This": case "idMetVar": case "PR_New":
                return primario();
			default: 
                if (lookAhead.comparar("P_Puntocoma")) {
                    throw new ExcepcionSintacticaPersonalizada(lookAhead.getLinea(), lookBehind.getColumna(), "Se esperaba una expresión o un valor después de '" + lookBehind.getLexema() + "'.", "un punto y coma");
                }
                throw new ExcepcionLiteralFaltante(lookAhead.getLinea(), lookAhead.getColumna() ,lookAhead.getLexema()); 	
		}
	}	
	
	private NodoLiteral literal() throws ExcepcionSintactica {
		if (lookAhead.getNombre().startsWith("L_") || lookAhead.getNombre().equals("PR_Null") || lookAhead.getNombre().equals("PR_True") || lookAhead.getNombre().equals("PR_False")) {
            match(lookAhead.getNombre());
            return new NodoLiteral(lookBehind);
        } else {
            throw new ExcepcionLiteralFaltante(lookAhead.getLinea(), lookBehind.getColumna(), lookBehind.getLexema());
        }
	}	
		
	private boolean visibilidad() throws ExcepcionSintactica {
		boolean esPublico = true;
		if (lookAhead.comparar("PR_Public")) 
			match("PR_Public");
		else if (lookAhead.comparar("PR_Private")) {
			match("PR_Private"); 
			esPublico = false;
		}
		return esPublico;
	}
	
	private Tipo tipo() throws ExcepcionSintactica {
		Tipo tipoBase = tipoSinCorchetes();
		while(lookAhead.comparar("P_Corchetes_A")) {
			match("P_Corchetes_A");
			match("P_Corchetes_C");
			tipoBase = new TipoArreglo(tipoBase);
		}
		return tipoBase;
	}

    private Tipo tipoSinCorchetes() throws ExcepcionSintactica {
        if (lookAhead.comparar("idClase")) {
			match("idClase");
			return new TipoClase(lookBehind);
		} else {
			return tipoPrimitivo();
		}
    }
	
	private TipoPrimitivo tipoPrimitivo() throws ExcepcionSintactica {
		switch (lookAhead.getNombre()) {
			case "PR_Boolean": 	match("PR_Boolean"); return new TipoBoolean();
			case "PR_Int":		match("PR_Int"); return new TipoInt();
			case "PR_Char":		match("PR_Char"); return new TipoChar();
			case "PR_String":	match("PR_String"); return new TipoString();
			default: throw new ExcepcionSintacticaPersonalizada(lookAhead.getLinea(), lookAhead.getColumna() ,"un tipo de dato válido", lookAhead.getLexema()); 
		}
	}
	
	private LinkedList<Token> listaDecVars() throws ExcepcionSintactica {
		LinkedList<Token> listaVars = new LinkedList<>();
		match("idMetVar");
		listaVars.add(lookBehind);
		restoListaDecVars(listaVars);
		return listaVars;
	}
	
	private void restoListaDecVars(LinkedList<Token> listaVars) throws ExcepcionSintactica {
		if (lookAhead.comparar("P_Coma")) {
			match("P_Coma");
			match("idMetVar");
            listaVars.add(lookBehind);
			restoListaDecVars(listaVars);
		}
	}
	
	private void opIgual() throws ExcepcionSintactica {
		if (lookAhead.comparar("O_Comparacion") || lookAhead.comparar("O_Distinto")) {
            match(lookAhead.getNombre());
        }
	}
	
	private void opComp() throws ExcepcionSintactica {
        if (lookAhead.comparar("O_Menor") || lookAhead.comparar("O_Mayor") || lookAhead.comparar("O_Menorigual") || lookAhead.comparar("O_Mayorigual")) {
            match(lookAhead.getNombre());
        }
	}
	
	private void opAd() throws ExcepcionSintactica {
		if (lookAhead.comparar("O_Suma") || lookAhead.comparar("O_Resta")) {
            match(lookAhead.getNombre());
        }
	}
	
	private void opUnario() throws ExcepcionSintactica {
		if (lookAhead.getNombre().startsWith("O_Suma") || lookAhead.getNombre().startsWith("O_Resta") || lookAhead.getNombre().startsWith("O_Not")) {
            match(lookAhead.getNombre());
        }
	}
	
	private void opMul() throws ExcepcionSintactica {
		if (lookAhead.comparar("O_Mult") || lookAhead.comparar("O_Div") || lookAhead.comparar("O_Mod")) {
            match(lookAhead.getNombre());
        }
	}
}

