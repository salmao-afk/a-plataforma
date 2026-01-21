package pt.ipleiria.estg.dei.dae.plataforma.ws;

import jakarta.ejb.EJB;
import jakarta.inject.Inject;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.SecurityContext;
import pt.ipleiria.estg.dei.dae.plataforma.dtos.AuthDTO;
import pt.ipleiria.estg.dei.dae.plataforma.dtos.EmailDTO;
import pt.ipleiria.estg.dei.dae.plataforma.dtos.NewPasswordDTO;
import pt.ipleiria.estg.dei.dae.plataforma.dtos.UserDTO;
import pt.ipleiria.estg.dei.dae.plataforma.ejbs.UserBean;
import pt.ipleiria.estg.dei.dae.plataforma.entities.User;
import pt.ipleiria.estg.dei.dae.plataforma.security.Authenticated;
import pt.ipleiria.estg.dei.dae.plataforma.security.TokenIssuer;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

@Path("auth")
@Produces({MediaType.APPLICATION_JSON})
@Consumes({MediaType.APPLICATION_JSON})
public class AuthService {

    @Context
    private SecurityContext securityContext;

    @Context
    private HttpServletRequest request;

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


    //logout
    //stateless
    @POST
    @Path("/logout")
    @Authenticated
    public Response logout() {
        if (request.getSession(false) != null) {
            request.getSession().invalidate();
        }
        return Response.ok("{\"mensagem\": \"Logout efetuado com sucesso\"}").build();
    }

    @GET
    @Authenticated
    @Path("/user")
    public Response getAuthenticatedUser() {
        var username = securityContext.getUserPrincipal().getName();
        var user = userBean.find(username);
        return Response.ok(UserDTO.fromUser(user)).build();
    }


    @POST
    @Path("/esqueci-password")
    public Response forgotPassword(EmailDTO dto) {
        userBean.sendPasswordResetEmail(dto.getEmail());

        return Response.ok("Se o email existir, as instruções foram enviadas.").build();
    }

    @POST
    @Path("/recuperar-password")
    public Response resetPassword(NewPasswordDTO dto) {
        try {
            userBean.resetPassword(dto.getToken(), dto.getNewPassword());
            return Response.ok("Password alterada com sucesso.").build();
        } catch (Exception e) {
            String errorMsg = e.getMessage();
            if (e.getCause() != null) {
                errorMsg = e.getCause().getMessage();
            }
            return Response.status(Response.Status.BAD_REQUEST).entity(errorMsg).build();
        }
    }

}


