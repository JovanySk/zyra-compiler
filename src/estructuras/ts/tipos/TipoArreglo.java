package estructuras.ts.tipos;

public class TipoArreglo extends Tipo {

    private Tipo tipoDeElementos;

    public TipoArreglo(Tipo tipo) {
        this.tipoDeElementos = tipo;
    }

    public Tipo getTipoDeElementos() {
        return tipoDeElementos;
    }

    @Override
    public String getNombreClase() {
        return tipoDeElementos.getNombreClase() + "[]";
    }

    @Override
    public boolean esTipoPrimitivo() {
        return false;
    }

    @Override
    public boolean esTipoClase() {
        return true; 
    }

    @Override
    public boolean conforma(TipoMetodo tipoAAsignar) {
        if (tipoAAsignar.getNombreClase().equals("Object") || tipoAAsignar.getNombreClase().equals("null")) {
            return true;
        }
        
        if (tipoAAsignar instanceof TipoArreglo) {
            TipoArreglo tipoArregloAAsignar = (TipoArreglo) tipoAAsignar;
            return this.tipoDeElementos.conforma(tipoArregloAAsignar.getTipoDeElementos());
        }
        
        return false;
    }

    @Override
    public boolean esValido() {
        return tipoDeElementos.esValido();
    }
}
