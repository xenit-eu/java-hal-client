package com.contentgrid.hateoas.client.hal.forms;

import static org.assertj.core.api.Assertions.assertThat;

import com.contentgrid.hateoas.client.hal.HalDocument;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.Test;
import org.springframework.hateoas.MediaTypes;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.mock.http.client.MockClientHttpResponse;
import org.springframework.web.client.RestClient;

class DefaultHalFormsClientTest {

    private static final URI SELF = URI.create("/invoices");

    private record RecordedRequest(HttpMethod method, URI uri, HttpHeaders headers, String body) {

    }

    private final AtomicReference<RecordedRequest> lastRequest = new AtomicReference<>();

    private HalFormsClient clientReturning(String responseBody) {
        var restClient = RestClient.builder()
                .requestInterceptor((request, body, execution) -> {
                    lastRequest.set(new RecordedRequest(request.getMethod(), request.getURI(), request.getHeaders(),
                            body == null ? null : new String(body, StandardCharsets.UTF_8)));

                    var response = new MockClientHttpResponse(
                            responseBody.getBytes(StandardCharsets.UTF_8), HttpStatus.OK);
                    response.getHeaders().setContentType(MediaTypes.HAL_FORMS_JSON);
                    return response;
                })
                .build();

        return HalFormsClient.builder().restClient(restClient).build();
    }

    private HalDocument fetch(HalFormsClient client) {
        return client.get(SELF).toHalDocument();
    }

    @Test
    void deserializeAllTemplateFields() {
        var client = clientReturning("""
                {
                    "_links": { "self": { "href": "/invoices" } },
                    "_templates": {
                        "default": {
                            "title": "Create invoice",
                            "method": "POST",
                            "target": "/invoices/create",
                            "contentType": "application/json",
                            "properties": [
                                {
                                    "name": "amount",
                                    "type": "number",
                                    "prompt": "Amount",
                                    "regex": "\\\\d+",
                                    "required": true,
                                    "readOnly": false
                                },
                                { "name": "reference", "type": "text" }
                            ]
                        }
                    }
                }
                """);

        assertThat(client.getTemplate(fetch(client), "default")).hasValueSatisfying(template -> {
            assertThat(template.getTitle()).isEqualTo("Create invoice");
            assertThat(template.getMethod()).isEqualTo("POST");
            assertThat(template.getTarget()).isEqualTo("/invoices/create");
            assertThat(template.getContentType()).isEqualTo("application/json");
            assertThat(template.getSelfLink().getURI()).hasToString("/invoices");

            assertThat(template.getHttpMethodOrDefault(HttpMethod.GET)).isEqualTo(HttpMethod.POST);
            assertThat(template.getTargetURIOrDefault(SELF)).hasToString("/invoices/create");
            assertThat(template.getContentTypeOrDefault(MediaType.TEXT_PLAIN)).isEqualTo(MediaType.APPLICATION_JSON);

            assertThat(template.getProperties()).satisfiesExactly(
                    amount -> {
                        assertThat(amount.getName()).isEqualTo("amount");
                        assertThat(amount.getType()).isEqualTo("number");
                        assertThat(amount.getPrompt()).isEqualTo("Amount");
                        assertThat(amount.getRegex()).isEqualTo("\\d+");
                        assertThat(amount.isRequired()).isTrue();
                        assertThat(amount.isReadOnly()).isFalse();
                    },
                    reference -> {
                        assertThat(reference.getName()).isEqualTo("reference");
                        assertThat(reference.getType()).isEqualTo("text");
                    });
        });
    }

    @Test
    void deserializeAbsentTemplateFields() {
        var client = clientReturning("""
                {
                    "_links": { "self": { "href": "/invoices" } },
                    "_templates": { "default": { } }
                }
                """);

        assertThat(client.getTemplate(fetch(client), "default")).hasValueSatisfying(template -> {
            assertThat(template.getMethod()).isNull();
            assertThat(template.getTarget()).isNull();
            assertThat(template.getContentType()).isNull();
            assertThat(template.getTitle()).isNull();
            assertThat(template.getProperties()).isEmpty();

            assertThat(template.getHttpMethodOrDefault(HttpMethod.GET)).isEqualTo(HttpMethod.GET);
            assertThat(template.getTargetURIOrDefault(SELF)).isEqualTo(SELF);
            assertThat(template.getContentTypeOrDefault(MediaType.APPLICATION_JSON))
                    .isEqualTo(MediaType.APPLICATION_JSON);
        });
    }

    @Test
    void deserializeAbsentPropertyFields() {
        var client = clientReturning("""
                {
                    "_links": { "self": { "href": "/invoices" } },
                    "_templates": {
                        "default": {
                            "properties": [ { "name": "amount" } ]
                        }
                    }
                }
                """);

        assertThat(client.getRequiredTemplate(fetch(client), "default").getProperties()).singleElement().satisfies(amount -> {
            assertThat(amount.getName()).isEqualTo("amount");
            assertThat(amount.isRequired()).isFalse();
            assertThat(amount.isReadOnly()).isFalse();
            assertThat(amount.isTemplated()).isFalse();
            assertThat(amount.getType()).isNull();
            assertThat(amount.getPrompt()).isNull();
            assertThat(amount.getRegex()).isNull();
        });
    }

    @Test
    void deserializeUnknownPropertyFields() {
        var client = clientReturning("""
                {
                    "_links": { "self": { "href": "/invoices" } },
                    "_templates": {
                        "default": {
                            "properties": [
                                {
                                    "name": "currency",
                                    "options": { "inline": [ "EUR", "USD" ] },
                                    "placeholder": "EUR"
                                }
                            ]
                        }
                    }
                }
                """);

        assertThat(client.getRequiredTemplate(fetch(client), "default").getProperties()).satisfiesExactly(currency -> {
            assertThat(currency.getName()).isEqualTo("currency");
            assertThat(currency.getProperties())
                    .containsEntry("options", Map.of("inline", List.of("EUR", "USD")))
                    .containsEntry("placeholder", "EUR");
        });
    }

    @Test
    void deserializeMultipleTemplates() {
        var client = clientReturning("""
                {
                    "_links": { "self": { "href": "/invoices" } },
                    "_templates": {
                        "default": { "method": "POST", "properties": [] },
                        "search": { "method": "GET", "target": "/invoices/search", "properties": [] }
                    }
                }
                """);
        var document = fetch(client);

        assertThat(client.getRequiredTemplate(document, "default").getMethod()).isEqualTo("POST");
        assertThat(client.getRequiredTemplate(document, "search").getTarget()).isEqualTo("/invoices/search");
        assertThat(client.getTemplate(document, "unknown")).isEmpty();
    }

    @Test
    void deserializeTemplatesAbsent() {
        var client = clientReturning("""
                {
                    "_links": { "self": { "href": "/invoices" } }
                }
                """);

        assertThat(client.getTemplate(fetch(client), "default")).isEmpty();
    }

    @Test
    void requestTemplate() {
        var client = clientReturning("""
                {
                    "_links": { "self": { "href": "/invoices" } },
                    "_templates": {
                        "default": {
                            "method": "POST",
                            "target": "/invoices/create",
                            "contentType": "application/json",
                            "properties": [
                                { "name": "amount", "required": true },
                                { "name": "reference" }
                            ]
                        }
                    }
                }
                """);

        client.requestTemplate(fetch(client), "default")
                .properties(Map.of("amount", 42, "reference", "INV-1"))
                .execute()
                .toHalDocument();

        assertThat(lastRequest.get()).satisfies(request -> {
            assertThat(request.method()).isEqualTo(HttpMethod.POST);
            assertThat(request.uri()).hasToString("/invoices/create");
            assertThat(request.headers().getContentType()).isEqualTo(MediaType.APPLICATION_JSON);
            // property order follows the template, not the supplied map
            assertThat(request.body()).isEqualTo("{\"amount\":42,\"reference\":\"INV-1\"}");
        });
    }

    @Test
    void requestTemplateTargetAbsent() {
        var client = clientReturning("""
                {
                    "_links": { "self": { "href": "/invoices" } },
                    "_templates": {
                        "default": { "method": "PUT" }
                    }
                }
                """);

        client.requestTemplate(fetch(client), "default")
                .properties(Map.of())
                .execute()
                .toHalDocument();

        assertThat(lastRequest.get()).satisfies(request -> {
            assertThat(request.method()).isEqualTo(HttpMethod.PUT);
            assertThat(request.uri()).hasToString("/invoices");
            assertThat(request.headers().getContentType()).isEqualTo(MediaType.APPLICATION_JSON);
        });
    }

    @Test
    void requestTemplateFormUrlEncoded() {
        var client = clientReturning("""
                {
                    "_links": { "self": { "href": "/invoices" } },
                    "_templates": {
                        "default": {
                            "method": "POST",
                            "contentType": "application/x-www-form-urlencoded",
                            "properties": [ { "name": "amount" }, { "name": "reference" } ]
                        }
                    }
                }
                """);

        client.requestTemplate(fetch(client), "default")
                .properties(Map.of("amount", 42, "reference", "INV-1"))
                .execute()
                .toHalDocument();

        assertThat(lastRequest.get()).satisfies(request -> {
            assertThat(request.headers().getContentType())
                    .isEqualTo(MediaType.APPLICATION_FORM_URLENCODED);
            assertThat(request.body()).isEqualTo("amount=42&reference=INV-1");
        });
    }

    @Test
    void requestTemplateMissingPropertyValues() {
        var client = clientReturning("""
                {
                    "_links": { "self": { "href": "/invoices" } },
                    "_templates": {
                        "default": {
                            "method": "POST",
                            "properties": [
                                { "name": "amount" },
                                { "name": "reference" },
                                { "name": "note" }
                            ]
                        }
                    }
                }
                """);

        client.requestTemplate(fetch(client), "default")
                .properties(property -> switch (property.getName()) {
                    case "amount" -> HalFormsPropertyValue.of(42);
                    case "reference" -> HalFormsPropertyValue.empty();
                    default -> HalFormsPropertyValue.missing();
                })
                .execute()
                .toHalDocument();

        // `empty` is rendered as an explicit null, `missing` is left out entirely
        assertThat(lastRequest.get().body()).isEqualTo("{\"amount\":42,\"reference\":null}");
    }
}
