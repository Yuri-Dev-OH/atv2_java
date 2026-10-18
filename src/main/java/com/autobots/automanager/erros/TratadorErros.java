package com.autobots.automanager.erros;

import java.util.List;
import org.springframework.beans.TypeMismatchException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.PessimisticLockingFailureException;
import org.springframework.http.*;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.method.annotation.HandlerMethodValidationException;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

/**
 * Converte falhas em respostas {@code application/problem+json} (RFC 9457).
 *
 * <p>Estender {@link ResponseEntityExceptionHandler} padroniza também os erros do próprio Spring MVC, como 405, 406,
 * 415 e rota inexistente.
 */
@RestControllerAdvice
public class TratadorErros extends ResponseEntityExceptionHandler {
    private static final String ENTRADA_INVALIDA = "Confira os campos, tipos, datas e identificadores.";

    private static ProblemDetail problema(HttpStatusCode status, String titulo, String detalhe) {
        var problema = ProblemDetail.forStatusAndDetail(status, detalhe);
        problema.setTitle(titulo);
        return problema;
    }
    @ExceptionHandler(RecursoNaoEncontrado.class)
    public ProblemDetail ausente(RecursoNaoEncontrado erro) {
        return problema(HttpStatus.NOT_FOUND, "Recurso não encontrado.", erro.getMessage());
    }
    @ExceptionHandler(Conflito.class)
    public ProblemDetail conflito(Conflito erro) {
        return problema(HttpStatus.CONFLICT, "Conflito.", erro.getMessage());
    }
    @ExceptionHandler(DataIntegrityViolationException.class)
    public ProblemDetail integridade(DataIntegrityViolationException erro) {
        return problema(HttpStatus.CONFLICT, "Conflito.", "Documento já cadastrado ou telefone repetido para o cliente.");
    }
    @ExceptionHandler(PessimisticLockingFailureException.class)
    public ProblemDetail concorrencia(PessimisticLockingFailureException erro) {
        return problema(HttpStatus.CONFLICT, "Conflito.", "O cadastro está sendo alterado. Tente novamente.");
    }
    @Override
    protected ResponseEntity<Object> handleMethodArgumentNotValid(MethodArgumentNotValidException erro, HttpHeaders headers,
            HttpStatusCode status, WebRequest request) {
        var detalhes = erro.getBindingResult().getFieldErrors().stream()
            .map(e -> e.getField() + ": " + e.getDefaultMessage()).sorted().toList();
        var problema = problema(status, "Dados inválidos.", "Um ou mais campos não atendem às regras.");
        problema.setProperty("detalhes", detalhes);
        return ResponseEntity.status(status).headers(headers).body(problema);
    }
    @Override
    protected ResponseEntity<Object> handleHttpMessageNotReadable(HttpMessageNotReadableException erro, HttpHeaders headers,
            HttpStatusCode status, WebRequest request) {
        return entrada(headers, status);
    }
    @Override
    protected ResponseEntity<Object> handleHandlerMethodValidationException(HandlerMethodValidationException erro,
            HttpHeaders headers, HttpStatusCode status, WebRequest request) {
        return entrada(headers, status);
    }
    @Override
    protected ResponseEntity<Object> handleTypeMismatch(TypeMismatchException erro, HttpHeaders headers,
            HttpStatusCode status, WebRequest request) {
        return entrada(headers, status);
    }
    private ResponseEntity<Object> entrada(HttpHeaders headers, HttpStatusCode status) {
        var problema = problema(status, "Requisição inválida.", ENTRADA_INVALIDA);
        problema.setProperty("detalhes", List.of());
        return ResponseEntity.status(status).headers(headers).body(problema);
    }
}
