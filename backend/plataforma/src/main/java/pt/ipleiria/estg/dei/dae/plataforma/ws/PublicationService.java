package pt.ipleiria.estg.dei.dae.plataforma.ws;

import jakarta.ejb.EJB;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.SecurityContext;
import org.jboss.resteasy.plugins.providers.multipart.InputPart;
import org.jboss.resteasy.plugins.providers.multipart.MultipartFormDataInput;
import pt.ipleiria.estg.dei.dae.plataforma.dtos.PublicationDTO;
import pt.ipleiria.estg.dei.dae.plataforma.dtos.PublicationHistoryDTO;
import pt.ipleiria.estg.dei.dae.plataforma.ejbs.PublicationBean;
import pt.ipleiria.estg.dei.dae.plataforma.entities.Publication;
import pt.ipleiria.estg.dei.dae.plataforma.exceptions.MyEntityNotFoundException;
import pt.ipleiria.estg.dei.dae.plataforma.security.Authenticated;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Path("/publicacoes")
@Authenticated
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.MULTIPART_FORM_DATA)
public class PublicationService {
    @EJB
    private PublicationBean publicationBean;

    @Context
    private SecurityContext securityContext; //to get username from token

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public List<PublicationDTO> getAllPublications() {
        return PublicationDTO.from(publicationBean.findAll());
    }


    @GET
    @Path("/me")
    @Authenticated
    @Produces(MediaType.APPLICATION_JSON)
    public List<PublicationDTO> getMyPublications() {
        return PublicationDTO.from(publicationBean.findUserPublications(securityContext.getUserPrincipal().getName()));
    }

    @GET
    @Path("/{id}")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getPublication(@PathParam("id") long id) throws MyEntityNotFoundException {
        Publication publication = publicationBean.findOrFail(id);
        PublicationDTO dto = PublicationDTO.from(publication);
        return Response.ok(dto).build();
    }

    @POST
    @Consumes(MediaType.MULTIPART_FORM_DATA)
    public Response createPublication(MultipartFormDataInput input) throws IOException {

        //get username from token
        String uploaderUsername = securityContext.getUserPrincipal().getName();

        //read data from multipart form
        Map<String, List<InputPart>> uploadForm = input.getFormDataMap();

        String title = getTextValue(uploadForm, "title");
        String type = getTextValue(uploadForm, "type");
        String scientificArea = getTextValue(uploadForm, "scientificArea");
        String description = getTextValue(uploadForm, "description");

        // Autores e Tags vêm como listas ou strings separadas.
        // Simplificação: vamos assumir que vêm separadas por vírgulas ou várias partes.
        // Aqui assumo que o frontend envia várias vezes a mesma key ou separado por vírgula
        List<String> tags = getListValue(uploadForm, "tags");
        List<String> authors = getListValue(uploadForm, "authors");

        List<InputPart> fileParts = uploadForm.get("file");
        InputStream fileContent = null;
        String fileName = "unknown.pdf";

        if (fileParts != null && !fileParts.isEmpty()) {
            InputPart filePart = fileParts.get(0); // Assume 1 ficheiro
            // Truque para sacar o nome do ficheiro (Ficha 9)
            fileName = getFileName(filePart.getHeaders());
            fileContent = filePart.getBody(InputStream.class, null);
        }
        publicationBean.create(title, scientificArea, description, authors, type, uploaderUsername, tags, fileName, fileContent);

        return Response.status(Response.Status.CREATED).build();
    }

    //Função Auxiliar
    private String getTextValue(Map<String, List<InputPart>> uploadForm, String key) throws IOException {
        List<InputPart> parts = uploadForm.get(key);
        if (parts != null && !parts.isEmpty()) {
            return parts.get(0).getBodyAsString();
        }
        return null;
    }

    // Extrai listas (ex: tags enviadas várias vezes ou separadas por virgula)
    private List<String> getListValue(Map<String, List<InputPart>> uploadForm, String key) throws IOException {
        List<String> result = new ArrayList<>();
        List<InputPart> parts = uploadForm.get(key);
        if (parts != null) {
            for (InputPart part : parts) {
                // Se vier "tag1, tag2", faz split. Se vierem partes separadas, adiciona.
                String val = part.getBodyAsString();
                if(val.contains(",")) {
                    for(String s : val.split(",")) result.add(s.trim());
                } else {
                    result.add(val);
                }
            }
        }
        return result;
    }

    //Função Auxiliar
    private String getFileName(jakarta.ws.rs.core.MultivaluedMap<String, String> headers) {
        String[] contentDisposition = headers.getFirst("Content-Disposition").split(";");
        for (String filename : contentDisposition) {
            if ((filename.trim().startsWith("filename"))) {
                String[] name = filename.split("=");
                String finalName = name[1].trim().replaceAll("\"", "");
                return finalName;
            }
        }
        return "unknown";
    }

    @GET
    @Path("/{id}/download")
    @Produces(MediaType.APPLICATION_OCTET_STREAM) // Indica que é um ficheiro binário
    public Response downloadFile(@PathParam("id") long id) throws MyEntityNotFoundException {

        // 1. Buscar a publicação para saber o nome original (ex: "relatorio.pdf")
        Publication publication = publicationBean.findOrFail(id);

        // 2. Buscar o ficheiro físico
        File file = publicationBean.getFile(id);

        // 3. Construir a resposta com o Header "Content-Disposition"
        // Isto obriga o browser a abrir a janela de "Guardar como..." com o nome correto
        return Response.ok(file)
                .header("Content-Disposition", "attachment; filename=\"" + publication.getFilename() + "\"")
                .build();
    }

    @DELETE
    @Path("/{id}")
    public Response deletePublication(@PathParam("id") long id) throws MyEntityNotFoundException {
        //obter publicacao para obter uploader
        var publication = publicationBean.findOrFail(id);

        //obter user que fez o pedido
        String username = securityContext.getUserPrincipal().getName();

        //verificar permissao para apagar
        if(!username.equals(publication.getUploader().getUsername()) && !securityContext.isUserInRole("ADMINISTRATOR")){
            return Response.status(Response.Status.FORBIDDEN).entity("Não pode apagar esta publicação").build();
        }

        publicationBean.delete(id);
        return Response.ok().build();
    }

    @PUT
    @Path("/{id}")
    @Consumes(MediaType.APPLICATION_JSON)
    public Response updatePublication(@PathParam("id") long id, PublicationDTO dto) throws MyEntityNotFoundException{
        Publication publication = publicationBean.findOrFail(id);
        String username = securityContext.getUserPrincipal().getName();

        if(!publication.getUploader().getUsername().equals(username) && !securityContext.isUserInRole("ADMINISTRATOR")){
            return Response.status(Response.Status.FORBIDDEN).build();
        }

        publicationBean.update(id, dto, username);
        return Response.ok().build();
    }

    @GET
    @Path("/{id}/historico")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getPublicationHistory(@PathParam("id") long id) throws MyEntityNotFoundException {
        var historyList = publicationBean.getHistory(id);

        var dtos = historyList.stream()
                .map(PublicationHistoryDTO::from)
                .collect(Collectors.toList());

        return Response.ok(dtos).build();
    }
}
