package pt.ipleiria.estg.dei.dae.plataforma.ws;

import jakarta.annotation.security.RolesAllowed;
import jakarta.ejb.EJB;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.SecurityContext;
import pt.ipleiria.estg.dei.dae.plataforma.dtos.CommentDTO;
import pt.ipleiria.estg.dei.dae.plataforma.dtos.CommentUserDTO;
import pt.ipleiria.estg.dei.dae.plataforma.dtos.UserDTO;
import pt.ipleiria.estg.dei.dae.plataforma.ejbs.CommentBean;
import pt.ipleiria.estg.dei.dae.plataforma.ejbs.UserBean;
import pt.ipleiria.estg.dei.dae.plataforma.entities.Comment;
import pt.ipleiria.estg.dei.dae.plataforma.entities.User;
import pt.ipleiria.estg.dei.dae.plataforma.exceptions.MyEntityExistsException;
import pt.ipleiria.estg.dei.dae.plataforma.exceptions.MyEntityNotFoundException;
import pt.ipleiria.estg.dei.dae.plataforma.security.Authenticated;

import java.util.List;

@Path("utilizadores") // ou "users"
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@Authenticated
public class UserService {

    @EJB
    private UserBean userBean;

    @Context
    private SecurityContext securityContext;

    @EJB
    private CommentBean commentBean;

    //Criar utilizador
    @POST
    @RolesAllowed({"ADMINISTRATOR"}) // Só Admins podem chamar isto
    public Response createNewUser(UserDTO dto) throws MyEntityExistsException {
        if (dto.getPassword() == null || dto.getPassword().isEmpty()) {
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity("Password is mandatory").build();
        }
        userBean.create(
                dto.getUsername(),
                dto.getPassword(),
                dto.getName(),
                dto.getEmail(),
                dto.getRole()
        );

        return Response.status(Response.Status.CREATED).entity(dto).build();
    }

    //Listar todos os utilizadores
    @GET
    public List<UserDTO> getAllUsers() {
        return UserDTO.fromUsers(userBean.findAll());
    }

    //Obter um utilizador
    @GET
    @Path("/{username}")
    @Produces(MediaType.APPLICATION_JSON)
    @RolesAllowed({"ADMINISTRATOR", "RESPONSIBLE", "COLLABORATOR"})
    public Response getUser(@PathParam("username") String username) throws MyEntityNotFoundException {

        User user = userBean.find(username);
        String principal = securityContext.getUserPrincipal().getName();

        if (securityContext.isUserInRole("ADMINISTRATOR") ||
                securityContext.isUserInRole("RESPONSIBLE") ||
                principal.equals(username)) {

            return Response.ok(UserDTO.fromUser(user)).build();
        }
        return Response.status(Response.Status.FORBIDDEN).build();
    }

    //obter comentarios de um determinado user
    @GET
    @Path("/{username}/comentarios")
    @RolesAllowed({"ADMINISTRATOR"})
    @Produces(MediaType.APPLICATION_JSON)
    public Response getUserComments(@PathParam("username") String username) throws MyEntityNotFoundException{

        List<Comment> commentList = commentBean.getUserComments(username);
        List<CommentUserDTO> commentsDtos = CommentUserDTO.from(commentList);

        return Response.ok(commentsDtos).build();
    }

    //obter comentarios do user que esta a fazer o pedido
    @Path("/me/comentarios")
    @GET
    @RolesAllowed({"COLLABORATOR"})
    public Response getMyComments() throws MyEntityNotFoundException{
        //vai buscar o username do utilizador que faz o pedido
        String username = securityContext.getUserPrincipal().getName();
        List<Comment> commentList = commentBean.getUserComments(username);
        List<CommentUserDTO> commentsDtos = CommentUserDTO.from(commentList);
        return Response.ok(commentsDtos).build();
    }
}