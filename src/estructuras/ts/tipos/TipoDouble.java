package estructuras.ts.tipos;

public class TipoDouble extends TipoPrimitivo {

	public String getNombreClase() {
		return "double";
	}

    @Override
    public boolean conforma(TipoMetodo t) {
        // Un double solo conforma con otro double. No permitimos asignación implícita a int.
        return t.getNombreClase().equals(this.getNombreClase());
    }
}
