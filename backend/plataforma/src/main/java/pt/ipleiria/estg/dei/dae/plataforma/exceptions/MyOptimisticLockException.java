package pt.ipleiria.estg.dei.dae.plataforma.exceptions;

public class MyOptimisticLockException extends Exception {

    public MyOptimisticLockException(String message) {
        super(message);
    }
}