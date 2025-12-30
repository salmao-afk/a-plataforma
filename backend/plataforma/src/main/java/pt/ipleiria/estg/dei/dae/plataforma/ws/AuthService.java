package pt.ipleiria.estg.dei.dae.plataforma.ws;

import jakarta.ejb.EJB;
import jakarta.inject.Inject;
import jakarta.validation.Valid;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.SecurityContext;
import pt.ipleiria.estg.dei.dae.plataforma.dtos.AuthDTO;
import pt.ipleiria.estg.dei.dae.plataforma.dtos.UserDTO;
import pt.ipleiria.estg.dei.dae.plataforma.ejbs.UserBean;
import pt.ipleiria.estg.dei.dae.plataforma.entities.User;
import pt.ipleiria.estg.dei.dae.plataforma.security.Authenticated;
import pt.ipleiria.estg.dei.dae.plataforma.security.TokenIssuer;

import java.util.Collections;

@Path("auth")
@Produces({MediaType.APPLICATION_JSON})
@Consumes({MediaType.APPLICATION_JSON})
public class AuthService {

    @Context
    private SecurityContext securityContext;

    @EJB
    private UserBean userBean;
    @POST
    @Path("/login")
    public Response authenticate(@Valid AuthDTO auth) {
        if (userBean.canLogin(auth.getUsername(), auth.getPassword())) {
            String token = TokenIssuer.issue(auth.getUsername());
            return Response.ok(Collections.singletonMap("token", token)).build();
        }
        return Response.status(Response.Status.UNAUTHORIZED).build();
    }

    @GET
    @Authenticated
    @Path("/user")
    public Response getAuthenticatedUser() {
        var username = securityContext.getUserPrincipal().getName();
        var user = userBean.find(username);
        return Response.ok(UserDTO.fromUser(user)).build();
    }
}
