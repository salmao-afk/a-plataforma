package pt.ipleiria.estg.dei.dae.plataforma.ws; // Ou .filters, confirma o teu package

import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.container.ContainerRequestFilter;
import jakarta.ws.rs.container.ContainerResponseContext;
import jakarta.ws.rs.container.ContainerResponseFilter;
import jakarta.ws.rs.container.PreMatching;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.Provider;
import java.io.IOException;

@Provider
@PreMatching // <--- ISTO É FUNDAMENTAL: Corre antes de tudo!
public class CorsFilter implements ContainerRequestFilter, ContainerResponseFilter {

    /**
     * Parte 1: Request Filter (Entrada)
     * Resolve o erro "It does not have HTTP ok status"
     */
    @Override
    public void filter(ContainerRequestContext requestContext) throws IOException {
        // Se o browser mandar um OPTIONS (Preflight), respondemos já com 200 OK
        // e impedimos que o servidor continue a procurar endpoints que não existem.
        if (requestContext.getMethod().equalsIgnoreCase("OPTIONS")) {
            requestContext.abortWith(Response.ok().build());
        }
    }

    /**
     * Parte 2: Response Filter (Saída)
     * Adiciona os cabeçalhos que permitem o acesso
     */
    @Override
    public void filter(ContainerRequestContext requestContext, ContainerResponseContext responseContext) throws IOException {
        //permitir TUDO (mais fácil para desenvolvimento):
        responseContext.getHeaders().add("Access-Control-Allow-Origin", "*");

        //forçar apenas o frontend:
        // responseContext.getHeaders().add("Access-Control-Allow-Origin", "http://localhost:3000");

        responseContext.getHeaders().add("Access-Control-Allow-Credentials", "true");
        responseContext.getHeaders().add("Access-Control-Allow-Headers", "origin, content-type, accept, authorization");
        responseContext.getHeaders().add("Access-Control-Allow-Methods", "GET, POST, PUT, DELETE, OPTIONS, HEAD, PATCH");
    }
}