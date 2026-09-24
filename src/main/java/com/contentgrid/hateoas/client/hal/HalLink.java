package com.contentgrid.hateoas.client.hal;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonCreator.Mode;
import java.net.URI;
import java.util.Map;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import org.springframework.hateoas.UriTemplate;
import org.springframework.lang.Nullable;

@Data
@RequiredArgsConstructor(access = AccessLevel.PRIVATE)
@AllArgsConstructor(access = AccessLevel.PRIVATE, onConstructor_ = @JsonCreator(mode = Mode.DISABLED))
public class HalLink {

    @NonNull
    String href;

    @Nullable
    String name;

    boolean templated = false;

    public URI getURI() {
        return URI.create(this.href);
    }

    public URI expand(Map<String, Object> variables) {
        return UriTemplate.of(href).expand(variables);
    }

    public static HalLink from(@NonNull URI uri) {
        return from(uri, null);
    }

    public static HalLink from(@NonNull URI uri, @Nullable String name) {
        return new HalLink(uri.toString(), name, false);
    }

    public static HalLink templated(@NonNull UriTemplate uriTemplate) {
        return templated(uriTemplate, null);
    }

    public static HalLink templated(@NonNull UriTemplate uriTemplate, @Nullable String name) {
        return new HalLink(uriTemplate.toString(), name, true);
    }

    @Override
    public String toString() {
        return "{href=%s%s}".formatted(this.href, this.name == null ? "":", name=" + this.name);
    }
}
