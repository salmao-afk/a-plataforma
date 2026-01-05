package pt.ipleiria.estg.dei.dae.plataforma.ws;

import jakarta.annotation.security.RolesAllowed;
import jakarta.ejb.EJB;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.SecurityContext;
import pt.ipleiria.estg.dei.dae.plataforma.dtos.CommentDTO;
import pt.ipleiria.estg.dei.dae.plataforma.ejbs.CommentBean;
import pt.ipleiria.estg.dei.dae.plataforma.entities.Comment;
import pt.ipleiria.estg.dei.dae.plataforma.security.Authenticated;
import pt.ipleiria.estg.dei.dae.plataforma.exceptions.MyEntityNotFoundException;

@Path("/publicacoes/{id}/comentarios")
@Authenticated
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class CommentService {
    @EJB
    private CommentBean commentBean;

    @Context
    private SecurityContext securityContext;

    @POST
    @RolesAllowed("COLLABORATOR")
    public Response createComment(@PathParam("id") long publicationId, CommentDTO request) throws MyEntityNotFoundException{
        // Obter o utilizador autenticado pelo Security Context
        String username = securityContext.getUserPrincipal().getName();

        Comment newComment = commentBean.create(publicationId,username,request.getContent());
        return Response.status(Response.Status.CREATED)
                .entity(CommentDTO.from(newComment))
                .build();
    }

}
