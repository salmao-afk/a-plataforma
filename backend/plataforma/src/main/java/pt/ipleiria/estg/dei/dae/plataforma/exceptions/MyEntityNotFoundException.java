package pt.ipleiria.estg.dei.dae.plataforma.exceptions;

public class MyEntityNotFoundException extends Exception {


    public MyEntityNotFoundException() {
        super();
    }

    //Para conseguir mandar com mensagens
    public MyEntityNotFoundException(String message) {
        super(message);
    }
}
