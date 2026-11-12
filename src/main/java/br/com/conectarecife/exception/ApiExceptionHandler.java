package br.com.conectarecife.exception;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.server.ResponseStatusException;

@RestControllerAdvice
public class ApiExceptionHandler {

    @ExceptionHandler(ResponseStatusException.class)
    public ProblemDetail handleResponseStatus(ResponseStatusException exception,
                                              HttpServletRequest request) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(
                exception.getStatusCode(),
                exception.getReason() == null ? "Recurso não encontrado" : exception.getReason()
        );
        problem.setTitle("Não foi possível atender à solicitação");
        problem.setProperty("path", request.getRequestURI());
        return problem;
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ProblemDetail handleInvalidId(MethodArgumentTypeMismatchException exception,
                                         HttpServletRequest request) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(
                HttpStatus.BAD_REQUEST,
                "Identificador inválido: " + exception.getValue()
        );
        problem.setTitle("Parâmetro inválido");
        problem.setProperty("path", request.getRequestURI());
        return problem;
    }
}
