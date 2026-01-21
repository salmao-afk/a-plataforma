package pt.ipleiria.estg.dei.dae.plataforma.dtos;

import pt.ipleiria.estg.dei.dae.plataforma.entities.Tag;

import java.util.List;

public class TagDTO {
    private long id;
    public String name;
    public String description;

    public TagDTO() {
    }

    public TagDTO(long id, String name, String description) {
        this.id = id;
        this.name = name;
        this.description = description;
    }

    public static TagDTO from(Tag tag){
        return new TagDTO(tag.getId(),tag.getName(), tag.getDescription());
    }

    public static List<TagDTO> from(List<Tag> tags){
        return tags.stream().map(TagDTO::from).toList();
    }

    public long getId() {
        return id;
    }
    public void setId(long id) {
        this.id = id;
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
