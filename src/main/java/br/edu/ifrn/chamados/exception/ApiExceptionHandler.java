package br.edu.ifrn.chamados.exception;

import java.time.OffsetDateTime;
import java.util.stream.Collectors;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.ServletWebRequest;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@RestControllerAdvice
public class ApiExceptionHandler extends ResponseEntityExceptionHandler {

    @ExceptionHandler(RecursoNaoEncontradoException.class)
    public ResponseEntity<Object> tratarRecursoNaoEncontrado(RecursoNaoEncontradoException ex, WebRequest request) {
        return resposta(HttpStatus.NOT_FOUND, ex.getMessage(), request);
    }

    @ExceptionHandler(DadosInvalidosException.class)
    public ResponseEntity<Object> tratarDadosInvalidos(DadosInvalidosException ex, WebRequest request) {
        return resposta(HttpStatus.BAD_REQUEST, ex.getMessage(), request);
    }

    @ExceptionHandler(RegraNegocioException.class)
    public ResponseEntity<Object> tratarRegraNegocio(RegraNegocioException ex, WebRequest request) {
        return resposta(HttpStatus.CONFLICT, ex.getMessage(), request);
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<Object> tratarIntegridade(DataIntegrityViolationException ex, WebRequest request) {
        return resposta(HttpStatus.CONFLICT, "Operacao viola uma restricao dos dados cadastrados", request);
    }

    @Override
    protected ResponseEntity<Object> handleMethodArgumentNotValid(MethodArgumentNotValidException ex,
            HttpHeaders headers, HttpStatusCode status, WebRequest request) {
        String mensagem = ex.getBindingResult().getFieldErrors().stream()
                .map(erro -> erro.getField() + ": " + erro.getDefaultMessage())
                .distinct()
                .sorted()
                .collect(Collectors.joining("; "));
        return resposta(HttpStatus.BAD_REQUEST, mensagem, request);
    }

    @Override
    protected ResponseEntity<Object> handleHttpMessageNotReadable(HttpMessageNotReadableException ex,
            HttpHeaders headers, HttpStatusCode status, WebRequest request) {
        return resposta(HttpStatus.BAD_REQUEST, "JSON invalido ou campo com valor incompativel", request);
    }

    @Override
    protected ResponseEntity<Object> handleExceptionInternal(Exception ex, Object body, HttpHeaders headers,
            HttpStatusCode status, WebRequest request) {
        String mensagem = switch (status.value()) {
            case 400 -> "Requisicao invalida";
            case 404 -> "Recurso nao encontrado";
            case 405 -> "Metodo HTTP nao permitido";
            case 415 -> "Tipo de conteudo nao suportado";
            default -> "Nao foi possivel processar a requisicao";
        };
        return ResponseEntity.status(status).headers(headers).body(erro(status, mensagem, request));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<Object> tratarErroInesperado(Exception ex, WebRequest request) {
        log.error("Erro ao processar requisicao", ex);
        return resposta(HttpStatus.INTERNAL_SERVER_ERROR, "Erro interno do servidor", request);
    }

    private ResponseEntity<Object> resposta(HttpStatus status, String mensagem, WebRequest request) {
        return ResponseEntity.status(status).body(erro(status, mensagem, request));
    }

    private ErroResponse erro(HttpStatusCode status, String mensagem, WebRequest request) {
        String caminho = ((ServletWebRequest) request).getRequest().getRequestURI();
        return new ErroResponse(OffsetDateTime.now(), status.value(), mensagem, caminho);
    }
}
