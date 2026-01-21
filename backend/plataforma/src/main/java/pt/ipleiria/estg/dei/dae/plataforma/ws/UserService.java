package pt.ipleiria.estg.dei.dae.plataforma.ws;

import jakarta.annotation.security.RolesAllowed;
import jakarta.ejb.EJB;
import jakarta.validation.Valid;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.SecurityContext;
import pt.ipleiria.estg.dei.dae.plataforma.dtos.*;
import pt.ipleiria.estg.dei.dae.plataforma.ejbs.*;
import pt.ipleiria.estg.dei.dae.plataforma.entities.Comment;
import pt.ipleiria.estg.dei.dae.plataforma.entities.Publication;
import pt.ipleiria.estg.dei.dae.plataforma.entities.Rating;
import pt.ipleiria.estg.dei.dae.plataforma.entities.User;
import pt.ipleiria.estg.dei.dae.plataforma.exceptions.MyEntityExistsException;
import pt.ipleiria.estg.dei.dae.plataforma.exceptions.MyEntityNotFoundException;
import pt.ipleiria.estg.dei.dae.plataforma.security.Authenticated;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

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

    @EJB
    private PublicationBean publicationBean;

    @EJB
    private RatingBean ratingBean;

    @EJB
    private EmailBean emailBean;

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
    @RolesAllowed({"ADMINISTRATOR"})
    public List<UserDTO> getAllUsers() {
        return UserDTO.fromUsers(userBean.findAll());
    }

    //Obter um utilizador
    @GET
    @Path("/{username}")
    @Produces(MediaType.APPLICATION_JSON)
    @RolesAllowed({"ADMINISTRATOR"})
    public Response getUser(@PathParam("username") String username) throws MyEntityNotFoundException {
        User user = userBean.findOrFail(username);
        return Response.ok(UserDTO.fromUser(user)).build();
    }

    //atualizar users
    @PUT
    @Path("/{username}")
    @RolesAllowed({"ADMINISTRATOR"})
    public Response updateUser(@PathParam("username") String username, UserProfileDTO dto) throws MyEntityNotFoundException {
        if(dto.getEmail() == null && dto.getName() == null){
            throw new IllegalArgumentException("Necessita de alterar algum campo(email ou nome)!");
        }
        userBean.update(username, dto.getEmail(), dto.getName());
        User user = userBean.findOrFail(username);
        var publicacoes = publicationBean.findUserPublications(username);
        var comentarios = commentBean.getUserComments(username);
        var ratings = ratingBean.getUserRatings(username);
        UserProfileDTO responseDTO = new UserProfileDTO(
                user,
                publicacoes,
                comentarios,
                ratings,
                user.getSubscribedTags()
        );

        return Response.ok(responseDTO).build();
    }

    //apagar users
    @DELETE
    @Path("/{username}")
    @RolesAllowed({"ADMINISTRATOR"})
    public Response deleteUser(@PathParam("username") String username) throws MyEntityNotFoundException {
        userBean.delete(username);
        Map<String, String> response = new HashMap<>();
        response.put("mensagem", "Utilizador removido com sucesso.");

        return Response.ok(response).build();
    }


    //suspender user
    @PATCH
    @Path("/{username}/estado")
    @RolesAllowed({"ADMINISTRATOR"})
    public Response suspendUser(@PathParam("username") String username,@Valid UserStatusDTO dto) throws MyEntityNotFoundException {
        if(dto.isActive()){
            userBean.activate(username);
        }
        else {
            userBean.suspend(username, dto.getStatusReason());
        }
        Map<String, String> response = new HashMap<>();
        response.put("mensagem", "User " + (dto.isActive() ? "ativado" : "suspenso") + " com sucesso");
        return Response.ok(response).build();
    }


    //mudar role de user
    @PATCH
    @Path("/{username}/role")
    @RolesAllowed({"ADMINISTRATOR"})
    public Response changeUserRole(@PathParam("username") String username,@Valid UserRoleDTO dto) throws MyEntityNotFoundException {
        userBean.changeRole(username, dto.getRole());
        Map<String, String> response = new HashMap<>();
        response.put("mensagem", "Role do utilizador alterado com sucesso");
        return Response.ok(response).build();
    }


    @GET
    @Path("/{username}/tags-subscritas")
    @RolesAllowed({"ADMINISTRATOR"})
    public Response getUserTags(@PathParam("username") String username) throws MyEntityNotFoundException {
        User user = userBean.findOrFail(username);
        List<TagDTO> tagList = TagDTO.from(user.getSubscribedTags());
        return Response.ok(tagList).build();
    }

    @GET
    @Path("/{username}/atividade")
    @RolesAllowed({"ADMINISTRATOR"})
    @Produces(MediaType.APPLICATION_JSON)
    public Response getUserActivity(@PathParam("username") String username) throws MyEntityNotFoundException {
        UserActivityDTO activityDTO = userBean.getUserActivity(username);
        return Response.ok(activityDTO).build();
    }

    //-----------------------------Dados Pessoais-----------------------------
    @GET
    @Path("/me")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getMyProfile() throws MyEntityNotFoundException {
        String username = securityContext.getUserPrincipal().getName();

        // ERRO ESTAVA AQUI: userBean.getUserProfile(username);
        // CORREÇÃO: Vamos construir o perfil aqui no Serviço
        UserProfileDTO dto = buildCompleteUserProfile(username);

        // Retorna o objeto (se o frontend espera lista, usa List.of, mas o ideal é objeto direto)
        return Response.ok(List.of(dto)).build();
    }

    // ... outros métodos ...

    // --- ADICIONE ESTE MÉTODO NO FINAL DA CLASSE UserService ---
    // Este método junta as peças todas (User + Publicações + Comentários + Ratings)
    private UserProfileDTO buildCompleteUserProfile(String username) throws MyEntityNotFoundException {
        // 1. Buscar o User básico
        User user = userBean.findOrFail(username);

        // 2. Buscar as Publicações desse user (usando o PublicationBean que JÁ ESTÁ injetado aqui)
        var publicacoes = publicationBean.findUserPublications(username);

        // 3. Buscar comentários e ratings
        var comentarios = commentBean.getUserComments(username);
        var ratings = ratingBean.getUserRatings(username);

        // 4. Juntar tudo no DTO
        return new UserProfileDTO(
                user,
                publicacoes,
                comentarios,
                ratings,
                user.getSubscribedTags()
        );
    }


    @PUT
    @Path("/me")
    @Produces(MediaType.APPLICATION_JSON)
    public Response updateMyProfile(UserProfileDTO dto) throws MyEntityNotFoundException {
        String username = securityContext.getUserPrincipal().getName();
        userBean.update(username, dto.getEmail(), dto.getName());
        User user = userBean.findOrFail(username);
        var publicacoes = publicationBean.findUserPublications(username);
        var comentarios = commentBean.getUserComments(username);
        var ratings = ratingBean.getUserRatings(username);
        UserProfileDTO responseDTO = new UserProfileDTO(
                user,
                publicacoes,
                comentarios,
                ratings,
                user.getSubscribedTags()
        );

        return Response.ok(responseDTO).build();
    }

    @GET
    @Path("/me/ratings")
    public Response getMyRatings() throws MyEntityNotFoundException{
        String username = securityContext.getUserPrincipal().getName();
        List<RatingDTO> ratings = ratingBean.getUserRatings(username).stream()
                .map(RatingDTO::from)
                .collect(Collectors.toList());
        return Response.ok(ratings).build();
    }

    //obter comentarios do user que esta a fazer o pedido
    @Path("/me/comentarios")
    @GET
    public Response getMyComments() throws MyEntityNotFoundException{
        //vai buscar o username do utilizador que faz o pedido
        String username = securityContext.getUserPrincipal().getName();
        List<Comment> commentList = commentBean.getUserComments(username);
        List<CommentUserDTO> commentsDtos = CommentUserDTO.from(commentList);
        return Response.ok(commentsDtos).build();
    }

    @Path("/me/tags-subscritas")
    @GET
    public Response getMyTags() throws MyEntityNotFoundException{
        String username = securityContext.getUserPrincipal().getName();
        List<TagDTO> tags = TagDTO.from(userBean.findOrFail(username).getSubscribedTags());
        return Response.ok(tags).build();
    }

    @GET
    @Path("/me/atividade")
    @RolesAllowed({"ADMINISTRATOR"})
    @Produces(MediaType.APPLICATION_JSON)
    public Response getMyActivity() throws MyEntityNotFoundException {
        String username = securityContext.getUserPrincipal().getName();

        UserActivityDTO activityDTO = userBean.getUserActivity(username);
        return Response.ok(activityDTO).build();
    }



    //-----------------------------Publicações-----------------------------
    //obter publi de um user especifico
    @GET
    @Path("/{username}/publicacoes")
    @RolesAllowed({"ADMINISTRATOR"})
    @Produces(MediaType.APPLICATION_JSON)
    public Response getUserPublications(@PathParam("username") String username) throws MyEntityNotFoundException{
        List<Publication> publications = publicationBean.findUserPublications(username);
        return Response.ok(PublicationDTO.from(publications)).build();
    }


    //-----------------------------Comentarios-----------------------------
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


    //-----------------------------Ratings-----------------------------
    @GET
    @Path("/{username}/ratings")
    @RolesAllowed({"ADMINISTRATOR", "RESPONSIBLE"})
    public Response getUserRatings(@PathParam("username") String username, @Context SecurityContext securityContext) {
        // Buscar dados e converter para DTO
        var ratings = ratingBean.getUserRatings(username);

        List<RatingDTO> dtos = ratings.stream()
                .map(RatingDTO::from)
                .collect(Collectors.toList());

        return Response.ok(dtos).build();
    }



    @POST
    @Path("/{username}/email")
    public Response sendEmail(@PathParam("username") String username, EmailDTO email)
            throws MyEntityNotFoundException {
        User user = userBean.findOrFail(username);

        emailBean.send(user.getEmail(), email.getSubject(), email.getBody());

        return Response.ok("Email enviado.").build();
    }


}