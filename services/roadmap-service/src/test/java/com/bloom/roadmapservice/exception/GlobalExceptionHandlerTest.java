package com.bloom.roadmapservice.exception;

import feign.Request;
import feign.Request.HttpMethod;
import feign.RequestTemplate;
import feign.FeignException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;

import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class GlobalExceptionHandlerTest {

    private final GlobalExceptionHandler handler = new GlobalExceptionHandler();

    @Test
    @DisplayName("RoadmapNotFoundException → 404 NOT_FOUND")
    void handles_roadmap_not_found() {
        var response = handler.handleRoadmapNotFound(new RoadmapNotFoundException(1L, 1001L));
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(response.getBody().code()).isEqualTo("NOT_FOUND");
        assertThat(response.getBody().message()).contains("1001");
    }

    @Test
    @DisplayName("StepNotFoundException → 404 NOT_FOUND")
    void handles_step_not_found() {
        var response = handler.handleStepNotFound(new StepNotFoundException(42L));
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(response.getBody().message()).contains("42");
    }

    @Test
    @DisplayName("FeignException → 502 BAD_GATEWAY (sans exposer les détails techniques)")
    void handles_feign_exception() {
        Request req = Request.create(HttpMethod.GET, "/api/job/skill-gap",
                Map.of(), Request.Body.empty(), new RequestTemplate());
        FeignException ex = FeignException.errorStatus("getJobSkillGap",
                feign.Response.builder()
                        .request(req).status(503).reason("Unavailable")
                        .body("upstream stack trace", StandardCharsets.UTF_8)
                        .build());

        var response = handler.handleFeignException(ex);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_GATEWAY);
        assertThat(response.getBody().message()).doesNotContain("stack trace");
        assertThat(response.getBody().message()).contains("Upstream service temporarily unavailable");
    }

    @Test
    @DisplayName("IllegalArgumentException → 400 BAD_REQUEST")
    void handles_illegal_argument() {
        var response = handler.handleIllegalArgument(new IllegalArgumentException("bad input"));
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody().message()).isEqualTo("bad input");
    }

    @Test
    @DisplayName("MethodArgumentNotValidException → 400 + map des erreurs par champ")
    void handles_validation_exception() throws NoSuchMethodException {
        // Utilise MapBindingResult — n'a pas besoin de bean introspection
        java.util.Map<String, String> target = new HashMap<>();
        BindingResult bindingResult = new org.springframework.validation.MapBindingResult(target, "request");
        bindingResult.addError(new org.springframework.validation.FieldError(
                "request", "targetJobId", "must not be null"));

        java.lang.reflect.Method method = String.class.getMethod("toString");
        org.springframework.core.MethodParameter parameter =
                new org.springframework.core.MethodParameter(method, -1);
        MethodArgumentNotValidException ex = new MethodArgumentNotValidException(parameter, bindingResult);

        ResponseEntity<Map<String, String>> response = handler.handleValidation(ex);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody()).containsEntry("targetJobId", "must not be null");
    }

    @Test
    @DisplayName("MissingServletRequestParameterException → 400 + nom du paramètre manquant")
    void handles_missing_request_parameter() {
        var ex = new MissingServletRequestParameterException("targetJobId", "Long");

        var response = handler.handleMissingParam(ex);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody().message()).contains("targetJobId");
    }

    @Test
    @DisplayName("AccessDeniedException → 403 avec message générique")
    void handles_access_denied() {
        var response = handler.handleAccessDenied(new AccessDeniedException("internal-detail"));
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
        assertThat(response.getBody().message()).doesNotContain("internal-detail");
    }

    @Test
    @DisplayName("Exception générique → 500 sans exposer la trace")
    void handles_generic_exception_safely() {
        var response = handler.handleAll(new RuntimeException("DB exploded — line 42"));
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
        assertThat(response.getBody().message()).doesNotContain("line 42");
    }
}
