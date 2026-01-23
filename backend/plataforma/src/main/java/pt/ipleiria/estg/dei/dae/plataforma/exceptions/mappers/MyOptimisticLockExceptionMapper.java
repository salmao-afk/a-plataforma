package pt.ipleiria.estg.dei.dae.plataforma.exceptions.mappers;

import jakarta.persistence.OptimisticLockException; // <--- Importante: Exceção padrão
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;
import java.util.logging.Logger;

@Provider
public class MyOptimisticLockExceptionMapper implements ExceptionMapper<OptimisticLockException> {

    private static final Logger logger = Logger.getLogger(MyOptimisticLockExceptionMapper.class.getCanonicalName());

    @Override
    public Response toResponse(OptimisticLockException e) {
        String errorMsg = "Conflito de edição: O registo foi alterado por outro utilizador.";
        logger.warning("OPTIMISTIC_LOCK: " + e.getMessage());

        return Response.status(Response.Status.CONFLICT) // 409 Conflict
                .entity(errorMsg)
                .build();
    }
}