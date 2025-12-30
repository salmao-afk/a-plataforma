package pt.ipleiria.estg.dei.dae.plataforma.exceptions;

public class MyEntityExistsException extends RuntimeException {
    public MyEntityExistsException(String message) {
        super(message);
    }
}
