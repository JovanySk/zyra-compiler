package excepciones.sintacticas;

import estructuras.Token;

@SuppressWarnings("serial")
public class ExcepcionHerenciaInvalida extends ExcepcionSintactica {
    public ExcepcionHerenciaInvalida(String claseHija, String clasePadre, Token token) {
        super(token.getLinea(), token.getColumna(), 
              "La clase '" + clasePadre + "' es una clase final del sistema y no puede ser extendida.",
              "extends " + clasePadre);
    }
}