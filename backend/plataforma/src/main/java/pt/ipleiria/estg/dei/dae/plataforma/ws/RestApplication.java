package pt.ipleiria.estg.dei.dae.plataforma.ws;

import jakarta.ws.rs.ApplicationPath;
import jakarta.ws.rs.core.Application;

@ApplicationPath("/api") // <--- ISTO É O QUE FALTA
public class RestApplication extends Application {
    // Fica vazio, serve apenas para configuração
}
