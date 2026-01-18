package pt.ipleiria.estg.dei.dae.plataforma.dtos;

import pt.ipleiria.estg.dei.dae.plataforma.entities.Tag;

import java.util.List;

public class TagDTO {
    public String name;
    public String description;

    public TagDTO() {
    }

    public TagDTO(String name, String description) {
        this.name = name;
        this.description = description;
    }

    public static TagDTO from(Tag tag){
        return new TagDTO(tag.getName(), tag.getDescription());
    }

    public static List<TagDTO> from(List<Tag> tags){
        return tags.stream().map(TagDTO::from).toList();
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }
}
