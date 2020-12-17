package gr.exm.tbproxy;

import io.jsonwebtoken.ExpiredJwtException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.client.HttpClientErrorException;

import javax.servlet.http.HttpServletRequest;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

@ControllerAdvice
public class GlobalExceptionHandler {

    Logger logger = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler({ExpiredJwtException.class, HttpClientErrorException.Unauthorized.class})
    public ResponseEntity<Object> unauthorizedException(Exception ex, HttpServletRequest request) {
        logger.warn(ex.getMessage());
        Map<String, Object> map = new HashMap<>();
        map.put("timestamp", new Date());
        map.put("status", HttpStatus.UNAUTHORIZED.value());
        map.put("message", ex.getMessage());
        map.put("error", "JWT expired");
        map.put("path", request.getRequestURI());
        return new ResponseEntity<>(map, HttpStatus.UNAUTHORIZED);
    }

}

