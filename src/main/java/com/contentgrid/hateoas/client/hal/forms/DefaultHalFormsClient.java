package com.contentgrid.hateoas.client.hal.forms;

import com.contentgrid.hateoas.client.hal.HalDocument;
import com.contentgrid.hateoas.client.hal.HalRequest;
import com.contentgrid.hateoas.client.hal.HalResponse;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.net.URI;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.hateoas.MediaTypes;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClient.RequestBodySpec;
import org.springframework.web.client.RestClient.RequestHeadersSpec;

@Slf4j
@RequiredArgsConstructor
class DefaultHalFormsClient implements HalFormsClient {

    @NonNull
    private final RestClient restClient;

    private static ObjectMapper mapper = new ObjectMapper();

    @Override
    public HalRequest get(URI uri) {
        var request = this.restClient.get().uri(uri).accept(MediaTypes.HAL_FORMS_JSON);
        return new DefaultHalFormsRequest(request);
    }

    @Override
    public HalFormsBodyRequest requestTemplate(@NonNull HalDocument document, @NonNull String templateName) {
        var template = this.getRequiredTemplate(document, templateName);
        var contentType = template.getContentTypeOrDefault(MediaType.APPLICATION_JSON);

        var bodySpec = restClient
                .method(template.getHttpMethodOrDefault(HttpMethod.GET))
                .uri(template.getTargetURIOrDefault(template.getSelfLink().getURI()))
                .contentType(contentType)
                .accept(MediaTypes.HAL_FORMS_JSON);

        return new DefaultHalFormsBodyRequest(bodySpec, contentType, template.getProperties());
    }

    public Optional<HalFormsTemplate> getTemplate(@NonNull HalDocument document, @NonNull String name) {
        var templates = document.getField("_templates");
        if (templates == null) {
            log.debug("HALDocument '{}' does not have a '_templates' field", document);
            return Optional.empty();
        }

        var tree = mapper.valueToTree(templates);
        var templateTree = tree.get(name);
        if (templateTree == null) {
            log.debug("HALDocument '{}' has no _template named '{}'", document, name);
            return Optional.empty();
        }

        try {
            var dto = mapper.treeToValue(templateTree, HalFormsTemplateDto.class);
            var template = new HalFormsTemplate(document.getSelfLink(), dto);
            return Optional.of(template);
        } catch (JsonProcessingException e) {
            log.warn("HALDocument '{}' has a field named _template.{} - but is not a valid HAL-Forms Template",
                    document, name, e);
            return Optional.empty();
        }
    }

    @RequiredArgsConstructor
    static class DefaultHalFormsRequest implements HalRequest {

        private final RequestHeadersSpec<? extends RequestHeadersSpec<?>> request;

        @Override
        public HalResponse execute() {
            request.httpRequest(request -> log.info("HTTP {} {}",request.getMethod(), request.getURI()));

            var response = request.retrieve();
            return new DefaultHalFormsResponse(response);
        }


    }

    @RequiredArgsConstructor
    static class DefaultHalFormsBodyRequest implements HalFormsBodyRequest {

        @NonNull
        private final RequestBodySpec request;

        @NonNull
        private final MediaType contentType;

        @NonNull
        private final List<HalFormsProperty> properties;

        @Override
        public HalResponse execute() {
            request.httpRequest(request -> log.info("HTTP {} {}",request.getMethod(), request.getURI()));

            var response = request.retrieve();
            return new DefaultHalFormsResponse(response);
        }

        @Override
        public HalFormsBodyRequest properties(Function<HalFormsProperty, Object> valueFunction) {
            // depending on http-method, null-values might need to be skipped ?
            Map<String, Object> body = new LinkedHashMap<>();
            for (var property : properties) {
                body.put(property.name, valueFunction.apply(property));
            }

            if (contentType.includes(MediaType.APPLICATION_FORM_URLENCODED) || contentType.includes(MediaType.MULTIPART_FORM_DATA)) {
                // body must be a MultiValuedMap because the HttpMessageConverter only supports MultiValuedMap
                request.body(MultiValueMap.fromSingleValue(body), new ParameterizedTypeReference<MultiValueMap<String, Object>>() {
                });
            } else {
                request.body(body, new ParameterizedTypeReference<Map<String, Object>>() {
                });
            }

            return this;
        }
    }
}
