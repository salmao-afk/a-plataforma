package pt.ipleiria.estg.dei.dae.plataforma.dtos;

import pt.ipleiria.estg.dei.dae.plataforma.entities.PublicationHistory;

public class PublicationHistoryDTO {
    public String oldTitle;
    public String date;
    public String modifierName;
    public String description;
    public String tagsSnapshots;

    // Construtor que recebe a Entidade History e converte
    public static PublicationHistoryDTO from(PublicationHistory h) {
        PublicationHistoryDTO dto = new PublicationHistoryDTO();
        dto.oldTitle = h.getTitle();
        dto.date = h.getModificationDate().toString();
        dto.description = h.getDescription();
        dto.modifierName = h.getModifier().getName();
        dto.tagsSnapshots = h.getTagsSnapshot();
        return dto;
    }
}
