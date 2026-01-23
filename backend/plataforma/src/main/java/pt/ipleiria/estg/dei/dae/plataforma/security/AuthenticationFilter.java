package pt.ipleiria.estg.dei.dae.plataforma.security;

import io.jsonwebtoken.Jwts;
import jakarta.annotation.Priority;
import jakarta.ejb.EJB;
import jakarta.ws.rs.NotAuthorizedException;
import jakarta.ws.rs.NotFoundException;
import jakarta.ws.rs.Priorities;
import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.container.ContainerRequestFilter;
import jakarta.ws.rs.core.*;
import jakarta.ws.rs.ext.Provider;
import pt.ipleiria.estg.dei.dae.plataforma.ejbs.UserBean;
import pt.ipleiria.estg.dei.dae.plataforma.entities.User;

import javax.crypto.spec.SecretKeySpec;
import java.security.Principal;
@Provider
@Authenticated
@Priority(Priorities.AUTHENTICATION)
public class AuthenticationFilter implements ContainerRequestFilter {
    @EJB
    private UserBean userBean;
    @Context
    private UriInfo uriInfo;
    @Override
    public void filter(ContainerRequestContext requestContext) {
        var header = requestContext.getHeaderString(HttpHeaders.AUTHORIZATION);
        if (header == null || !header.startsWith("Bearer ")) {
            throw new
                    NotAuthorizedException("Authorization header must be provided");
        }
        String token = header.substring("Bearer".length()).trim();
        var user = userBean.find(getUsername(token));
        try {
            if (user == null) {
                requestContext.abortWith(Response.status(Response.Status.UNAUTHORIZED).build());
                return;
            }

            if (!user.isActive()) {
                requestContext.abortWith(Response.status(Response.Status.FORBIDDEN)
                    .entity("{\"erro\": \"Conta suspensa. Contacte o administrador.\"}")
                    .type(MediaType.APPLICATION_JSON)
                    .build());
                return;
            }

        } catch (Exception e) {
            requestContext.abortWith(Response.status(Response.Status.INTERNAL_SERVER_ERROR).build());
        }

        requestContext.setSecurityContext(new SecurityContext() {
            @Override
            public Principal getUserPrincipal() {
                return user::getUsername;
            }
            @Override
            public boolean isUserInRole(String s) {
                 return user.getRole().name().equals(s);
            }
            @Override
            public boolean isSecure() {
                return uriInfo.getAbsolutePath().toString().startsWith("https");
            }
            @Override
            public String getAuthenticationScheme() { return "Bearer"; }
        });
    }
    private String getUsername(String token) {
        var key = new SecretKeySpec(
                TokenIssuer.SECRET_KEY, TokenIssuer.ALGORITHM);
        try {
            return Jwts.parser()
                    .verifyWith(key)
                    .build()
                    .parseSignedClaims(token)
                    .getPayload()
                    .getSubject();
        } catch (Exception e) {
            throw new NotAuthorizedException("Invalid JWT");
        }
    }
}
