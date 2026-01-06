package pt.ipleiria.estg.dei.dae.plataforma.ws;

import jakarta.annotation.security.RolesAllowed;
import jakarta.ejb.EJB;
import jakarta.validation.Valid;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.SecurityContext;
import pt.ipleiria.estg.dei.dae.plataforma.dtos.CommentDTO;
import pt.ipleiria.estg.dei.dae.plataforma.dtos.CommentVisibilityDTO;
import pt.ipleiria.estg.dei.dae.plataforma.ejbs.CommentBean;
import pt.ipleiria.estg.dei.dae.plataforma.entities.Comment;
import pt.ipleiria.estg.dei.dae.plataforma.security.Authenticated;
import pt.ipleiria.estg.dei.dae.plataforma.exceptions.MyEntityNotFoundException;

import java.util.HashMap;
import java.util.Map;

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
    public Response createComment(@PathParam("id") long publicationId, @Valid CommentDTO request) throws MyEntityNotFoundException{
        // Obter o utilizador autenticado pelo Security Context
        String username = securityContext.getUserPrincipal().getName();

        Comment newComment = commentBean.create(publicationId,username,request.getContent());
        return Response.status(Response.Status.CREATED)
                .entity(CommentDTO.from(newComment))
                .build();
    }


    @Path("/{commentId}/visibility")
    @PUT
    @RolesAllowed({"ADMINISTRATOR", "RESPONSIBLE"})
    public Response toggleVisibility( @PathParam("commentId") long commentId, @Valid CommentVisibilityDTO request) throws MyEntityNotFoundException{
        String username = securityContext.getUserPrincipal().getName();

        commentBean.toggleVisibility(commentId, username, request.isHidden(), request.getHideReason());
        Map<String, String> response = new HashMap<>();
        response.put("mensagem", "Comentário " + (request.isHidden() ? "ocultado" : "mostrado") + " com sucesso");

        return Response.ok(response).build();
    }

}
