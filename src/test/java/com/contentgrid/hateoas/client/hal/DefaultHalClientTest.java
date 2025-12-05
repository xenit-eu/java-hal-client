package com.contentgrid.hateoas.client.hal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.NoSuchElementException;
import org.junit.jupiter.api.Test;
import org.springframework.hateoas.MediaTypes;
import org.springframework.http.HttpStatus;
import org.springframework.mock.http.client.MockClientHttpResponse;
import org.springframework.web.client.RestClient;

class DefaultHalClientTest {

    @Test
    void defaultConstructor() {
        var client = HalClient.builder().build();
        assertThat(client).isNotNull();
    }

    @Test
    void halLinks() {
        var restClient = RestClient.builder()
                .requestInterceptor((request, body, execution) -> {
                    // return mocked response
                    var response = new MockClientHttpResponse("""
                            {
                                "_links": {
                                    "self": { "href": "%s" },
                                    "single": { "href": "/single" },
                                    "templated": { "href": "/templated{?query}", "templated": true },
                                    "multi": [
                                        { "href": "/multi/a", "name": "a" },
                                        { "href": "/multi/b", "name": "b" }
                                    ],
                                    "empty": [ ]
                                }
                            }
                            """
                            .formatted(request.getURI())
                            .getBytes(StandardCharsets.UTF_8), HttpStatus.OK);

                    response.getHeaders().setContentType(MediaTypes.HAL_JSON);
                    return response;
                }).build();
        var client = HalClient.builder().restClient(restClient).build();

        var doc = client.get(URI.create("/hal-document")).toHalDocument();
        assertThat(doc).isNotNull();
        assertThat(doc.getSelfLink().getURI()).hasToString("/hal-document");

        assertThat(doc.getLink("single")).hasValueSatisfying(single ->
                assertThat(single.getURI()).hasToString("/single"));
        assertThat(doc.getLinks("single")).hasValueSatisfying(links ->
                assertThat(links).containsExactly(HalLink.from(URI.create("/single"))));
        assertThat(doc.getLink("templated")).hasValueSatisfying(templated ->
                assertThat(templated.getHref()).isEqualTo("/templated{?query}"));
        assertThat(doc.getLink("templated")).hasValueSatisfying(templated ->
                assertThat(templated.expand(Map.of("query", "foo"))).hasToString("/templated?query=foo"));
        assertThat(doc.getLink("templated")).hasValueSatisfying(templated ->
                assertThat(templated.expand(Map.of())).hasToString("/templated"));

        assertThat(doc.getLinks("multi")).hasValueSatisfying(multi ->
                assertThat(multi).containsExactly(
                        HalLink.from(URI.create("/multi/a"), "a"),
                        HalLink.from(URI.create("/multi/b"), "b")
                ));
        assertThat(doc.getLink("multi", "a")).hasValueSatisfying(linkA ->
                assertThat(linkA.getURI()).hasToString("/multi/a"));
        assertThat(doc.getLink("multi", "b")).hasValueSatisfying(linkA ->
                assertThat(linkA.getURI()).hasToString("/multi/b"));
        assertThatThrownBy(() -> doc.getLink("multi")).isInstanceOf(RuntimeException.class);
        assertThatThrownBy(() -> doc.getRequiredLink("multi", "c")).isInstanceOf(NoSuchElementException.class);

        assertThat(doc.getLink("empty")).isEmpty();
        assertThat(doc.getLinks("empty")).hasValueSatisfying(links -> assertThat(links).isEmpty());

    }
}