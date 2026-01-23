package pt.ipleiria.estg.dei.dae.plataforma.providers;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import jakarta.ws.rs.ext.ContextResolver;
import jakarta.ws.rs.ext.Provider;

@Provider // <--- Isto diz ao JAX-RS para usar esta configuração
public class JacksonConfig implements ContextResolver<ObjectMapper> {

    private final ObjectMapper mapper;

    public JacksonConfig() {
        this.mapper = new ObjectMapper();
        // 1. Regista o módulo para suportar LocalDate
        this.mapper.registerModule(new JavaTimeModule());
        // 2. (Opcional) Faz com que as datas saiam como "2023-01-01" e não [2023, 1, 1]
        this.mapper.configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false);
    }

    @Override
    public ObjectMapper getContext(Class<?> type) {
        return mapper;
    }
}
