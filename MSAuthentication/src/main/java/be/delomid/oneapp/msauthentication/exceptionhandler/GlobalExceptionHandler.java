package be.delomid.oneapp.msauthentication.exceptionhandler;

import lombok.extern.slf4j.Slf4j;
import org.hibernate.boot.beanvalidation.IntegrationException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler extends ResponseEntityExceptionHandler {
    @ExceptionHandler(Exception.class)
    public ResponseEntity<Object> handleAllExceptions(Exception ex, WebRequest request) {
        HttpStatus status = HttpStatus.INTERNAL_SERVER_ERROR;
        String message = "An error occurred while processing the request  "+ex.getMessage();

        if (ex instanceof ResponseStatusException) {
            status = HttpStatus.FORBIDDEN;
            message = ex.getMessage();
        }
        if (ex instanceof IntegrationException) {
            status = HttpStatus.FOUND;
            message = ex.getMessage();
        }
        if(ex instanceof NotFoundException)
        {
            status = HttpStatus.NOT_FOUND;
            message = ex.getMessage();
        }
        return handleExceptionInternal(ex, message, new HttpHeaders(), status, request);
    }
}
