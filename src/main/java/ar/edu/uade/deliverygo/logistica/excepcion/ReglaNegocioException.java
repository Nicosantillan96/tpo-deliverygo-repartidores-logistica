package ar.edu.uade.deliverygo.logistica.excepcion;

public class ReglaNegocioException extends RuntimeException {

    private final CodigoError codigo;

    public ReglaNegocioException(CodigoError codigo, String mensaje) {
        super(mensaje);
        this.codigo = codigo;
    }

    public CodigoError getCodigo() {
        return codigo;
    }
}
