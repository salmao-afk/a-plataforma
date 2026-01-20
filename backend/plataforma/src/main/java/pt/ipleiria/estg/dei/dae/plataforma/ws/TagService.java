package pt.ipleiria.estg.dei.dae.plataforma.ws;

import jakarta.annotation.security.RolesAllowed;
import jakarta.ejb.EJB;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.SecurityContext;
import pt.ipleiria.estg.dei.dae.plataforma.dtos.TagDTO;
import pt.ipleiria.estg.dei.dae.plataforma.ejbs.TagBean;
import pt.ipleiria.estg.dei.dae.plataforma.ejbs.UserBean;
import pt.ipleiria.estg.dei.dae.plataforma.entities.Tag;
import pt.ipleiria.estg.dei.dae.plataforma.entities.User;
import pt.ipleiria.estg.dei.dae.plataforma.exceptions.MyEntityNotFoundException;
import pt.ipleiria.estg.dei.dae.plataforma.security.Authenticated;

import java.util.List;

@Path("tags")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@Authenticated
public class TagService {

    @EJB
    private UserBean userBean;

    @EJB
    private TagBean tagBean;

    @GET
    public List<TagDTO> getAllTags(){
        return TagDTO.from(tagBean.findAll());
    }

    @GET
    @Path("/{id}")
    public TagDTO getTag(@PathParam("id") long id) throws MyEntityNotFoundException{
        return TagDTO.from(tagBean.findOrFail(id));
    }

    @POST
    @RolesAllowed({"ADMINISTRATOR", "RESPONSIBLE"})
    public Response createTag(TagDTO tagDTO){
        tagBean.create(tagDTO.getName(), tagDTO.getDescription());
        return Response.status(Response.Status.CREATED).entity(tagDTO).build();
    }

    @PUT
    @Path("/{id}")
    @RolesAllowed({"ADMINISTRATOR", "RESPONSIBLE"})
    public Response updateTag(@PathParam("id") long id, TagDTO dto) throws MyEntityNotFoundException{
        tagBean.update(id, dto.getName(), dto.getDescription());
        return Response.ok().build();
    }

    @DELETE
    @Path("/{id}")
    @RolesAllowed({"ADMINISTRATOR", "RESPONSIBLE"})
    public Response deleteTag(@PathParam("id") long id) throws MyEntityNotFoundException {
        tagBean.delete(id);
        return Response.ok("Tag apagada.").build();
    }

    @POST
    @Path("/{id}/subscrever")
    public Response subscribeTag(@PathParam("id") long tagId, @Context SecurityContext securityContext)
            throws MyEntityNotFoundException {

        // 1. Receber o contexto AQUI nos parâmetros para não dar erro 500
        String username = securityContext.getUserPrincipal().getName();

        // 2. Chamar o Bean (que trata da lógica e da gravação)
        try {
            userBean.subscribeTag(username, tagId);
        } catch (IllegalArgumentException e) {
            return Response.status(Response.Status.CONFLICT).entity(e.getMessage()).build();
        }

        return Response.ok("Tag subscrita com sucesso.").build();
    }

    @DELETE
    @Path("/{id}/dessubscrever")
    public Response unsubscribeTag(@PathParam("id") long tagId, @Context SecurityContext securityContext)
            throws MyEntityNotFoundException {

        String username = securityContext.getUserPrincipal().getName();

        userBean.unsubscribeTag(username, tagId);

        return Response.ok("Subscrição removida.").build();
    }
}
