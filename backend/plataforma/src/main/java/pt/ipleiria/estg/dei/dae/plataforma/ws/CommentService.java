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
import pt.ipleiria.estg.dei.dae.plataforma.ejbs.CommentBean;
import pt.ipleiria.estg.dei.dae.plataforma.entities.Comment;
import pt.ipleiria.estg.dei.dae.plataforma.security.Authenticated;
import pt.ipleiria.estg.dei.dae.plataforma.exceptions.MyEntityNotFoundException;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Path("/publicacoes/{pubId}/comentarios")
@Authenticated
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class CommentService {
    @EJB
    private CommentBean commentBean;

    @Context
    private SecurityContext securityContext;

    @POST
    @Authenticated
    public Response createComment(@PathParam("pubId") long publicationId, @Valid CommentDTO dto) throws MyEntityNotFoundException{
        // Obter o utilizador autenticado pelo Security Context
        String username = securityContext.getUserPrincipal().getName();

        Comment newComment = commentBean.create(publicationId,username,dto.getContent());
        CommentDTO createdCommentDTO = CommentDTO.from(newComment);

        // Estrutura de resposta com o comentário e a mensagem
        Map<String, Object> response = new HashMap<>();
        response.put("comentario", createdCommentDTO);
        response.put("mensagem", "Comentário adicionado com sucesso. Notificações enviadas aos subscritores.");

        return Response.status(Response.Status.CREATED)
                .entity(response)
                .build();
    }


    @Path("/{commentId}/visibilidade")
    @PUT
    @RolesAllowed({"ADMINISTRATOR", "RESPONSIBLE"})
    public Response toggleVisibility( @PathParam("commentId") long commentId, CommentDTO dto) throws MyEntityNotFoundException{
        if(dto.isHidden() == null){
            throw new IllegalArgumentException("Necessita de alterar algum campo(hidden)!");
        }
        String username = securityContext.getUserPrincipal().getName();

        commentBean.toggleVisibility(commentId, username, dto.isHidden(), dto.getHideReason());
        Map<String, String> response = new HashMap<>();
        response.put("mensagem", "Comentário " + (dto.isHidden() ? "ocultado" : "mostrado") + " com sucesso");

        return Response.ok(response).build();
    }

    @Path("/{commentId}")
    @DELETE
    public Response deleteComment(@PathParam("commentId") long commentId) throws MyEntityNotFoundException{
        String username = securityContext.getUserPrincipal().getName();
        commentBean.deleteComment(commentId, username);
        Map<String, String> response = new HashMap<>();
        response.put("mensagem", "Comentário apagado com sucesso");

        return Response.ok(response).build();
    }

    @GET
    public Response getComments(@PathParam("pubId") long pubId) throws MyEntityNotFoundException{
        List<Comment> commentsList = commentBean.getComments(pubId);

        //Converte para DTOs
        List<CommentDTO> commentsDTO = CommentDTO.from(commentsList);

        return Response.ok(commentsDTO).build();
    }


}
