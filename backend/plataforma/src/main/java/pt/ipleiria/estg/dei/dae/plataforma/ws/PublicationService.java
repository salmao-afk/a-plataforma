package pt.ipleiria.estg.dei.dae.plataforma.ws;

import jakarta.ejb.EJB;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.jboss.resteasy.plugins.providers.multipart.InputPart;
import org.jboss.resteasy.plugins.providers.multipart.MultipartFormDataInput;
import pt.ipleiria.estg.dei.dae.plataforma.dtos.PublicationDTO;
import pt.ipleiria.estg.dei.dae.plataforma.ejbs.PublicationBean;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Path("/publicacoes")
public class PublicationService {
    @EJB
    private PublicationBean publicationBean;

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public List<PublicationDTO> getAllPublications() {
        return PublicationDTO.from(publicationBean.findAll());
    }

    @POST
    @Consumes(MediaType.MULTIPART_FORM_DATA)
    public Response createPublication(MultipartFormDataInput input) throws IOException {
        Map<String, List<InputPart>> uploadForm = input.getFormDataMap();

        String title = getTextValue(uploadForm, "title");
        String type = getTextValue(uploadForm, "type");
        String scientificArea = getTextValue(uploadForm, "scientificArea");
        String username = getTextValue(uploadForm, "uploaderUsername");

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
            // Truque para sacar o nome do ficheiro (Ficha 9 tem exemplos disto)
            fileName = getFileName(filePart.getHeaders());
            fileContent = filePart.getBody(InputStream.class, null);
        }
        publicationBean.create(title, scientificArea, authors, type, username, tags, fileName, fileContent);

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
}
