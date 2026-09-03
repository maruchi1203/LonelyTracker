package com.lonelytracker.backend.common.exception;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.ErrorResponseException;
import org.springframework.web.context.request.WebRequest;

import java.time.LocalDateTime;
import java.util.stream.Collectors;

/** 컨트롤러 전역에서 던져진 예외를 일관된 JSON 형태로 변환한다. */
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    public record ErrorResponse(
            LocalDateTime timestamp,
            int status,
            String error,
            String message,
            String path
    ) {
    }

    /** @Valid 검증 실패 → 400 */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidation(MethodArgumentNotValidException e, WebRequest request) {
        String message = e.getBindingResult().getFieldErrors().stream()
                .map(fe -> fe.getField() + ": " + fe.getDefaultMessage())
                .collect(Collectors.joining(", "));
        return build(HttpStatus.BAD_REQUEST, message, request);
    }

    /** 요청 본문이 JSON으로 읽히지 않음(형식 오류, 타입 불일치 등) → 400 */
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ErrorResponse> handleNotReadable(HttpMessageNotReadableException e, WebRequest request) {
        return build(HttpStatus.BAD_REQUEST, "요청 본문을 읽을 수 없습니다. JSON 형식과 필드 타입을 확인하세요", request);
    }

    /** 비즈니스 규칙 위반(예: endAt이 startAt보다 이르다) → 400 */
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ErrorResponse> handleIllegalArgument(IllegalArgumentException e, WebRequest request) {
        return build(HttpStatus.BAD_REQUEST, e.getMessage(), request);
    }

    /** 해당 id 없음 → 404 */
    @ExceptionHandler(NotFoundException.class)
    public ResponseEntity<ErrorResponse> handleNotFound(NotFoundException e, WebRequest request) {
        return build(HttpStatus.NOT_FOUND, e.getMessage(), request);
    }

    /** AI 가 쓸 만한 결과를 못 냈다 -> 400. 재시도해도 같으므로 직접 입력을 권한다 */
    @ExceptionHandler(AiParseException.class)
    public ResponseEntity<ErrorResponse> handleAiParse(AiParseException e, WebRequest request) {
        return build(HttpStatus.BAD_REQUEST, e.getMessage(), request);
    }

    /** AI 를 지금 쓸 수 없다 -> 503. 사용자 잘못이 아니다 */
    @ExceptionHandler(AiUnavailableException.class)
    public ResponseEntity<ErrorResponse> handleAiUnavailable(AiUnavailableException e, WebRequest request) {
        return build(HttpStatus.SERVICE_UNAVAILABLE, e.getMessage(), request);
    }

    /** 서버 설정이 빠졌다 -> 503. 무엇이 빠졌는지 메시지로 알려준다 */
    @ExceptionHandler(EncryptionNotConfiguredException.class)
    public ResponseEntity<ErrorResponse> handleEncryptionNotConfigured(
            EncryptionNotConfiguredException e, WebRequest request) {
        return build(HttpStatus.SERVICE_UNAVAILABLE, e.getMessage(), request);
    }

    /** 마스터 키가 바뀌었거나 값이 조작됐다 -> 503. 다시 등록하라고 알려준다 */
    @ExceptionHandler(DecryptionFailedException.class)
    public ResponseEntity<ErrorResponse> handleDecryptionFailed(
            DecryptionFailedException e, WebRequest request) {
        return build(HttpStatus.SERVICE_UNAVAILABLE, e.getMessage(), request);
    }

    /**
     * Spring이 상태 코드를 정해 던진 예외 — 없는 경로(404), 허용 안 된 메서드(405) 등.
     * 아래 폴백보다 먼저 잡아야 404가 500이 되지 않는다.
     */
    @ExceptionHandler(ErrorResponseException.class)
    public ResponseEntity<ErrorResponse> handleSpringStatus(
            ErrorResponseException e, WebRequest request) {
        return build(HttpStatus.valueOf(e.getStatusCode().value()),
                e.getBody().getDetail(), request);
    }

    /**
     * 위에서 잡지 못한 예외 → 500.
     * 스택트레이스는 서버 로그에만 남기고 응답에는 예외 이름까지만 싣는다.
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleUnexpected(Exception e, WebRequest request) {
        // ErrorResponse를 구현해 스스로 상태를 아는 예외는 그 상태를 존중한다
        if (e instanceof org.springframework.web.ErrorResponse errorResponse) {
            return build(HttpStatus.valueOf(errorResponse.getStatusCode().value()),
                    errorResponse.getBody().getDetail(), request);
        }

        log.error("처리되지 않은 예외: {}", request.getDescription(false), e);
        return build(HttpStatus.INTERNAL_SERVER_ERROR,
                "예기치 못한 오류가 발생했습니다 (" + e.getClass().getSimpleName() + ")", request);
    }

    private ResponseEntity<ErrorResponse> build(HttpStatus status, String message, WebRequest request) {
        ErrorResponse body = new ErrorResponse(
                LocalDateTime.now(),
                status.value(),
                status.getReasonPhrase(),
                message,
                request.getDescription(false).replace("uri=", "")
        );
        return ResponseEntity.status(status).body(body);
    }
}
