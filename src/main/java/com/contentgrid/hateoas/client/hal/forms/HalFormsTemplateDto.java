package com.contentgrid.hateoas.client.hal.forms;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.net.URI;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.SneakyThrows;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.util.StringUtils;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class HalFormsTemplateDto {

    String method;
    String target;
    String contentType;
    String title;
    List<HalFormsProperty> properties;

    public HttpMethod getHttpMethodOrDefault(HttpMethod defaultHttpMethod) {
        return this.method != null ? HttpMethod.valueOf(this.method) : defaultHttpMethod;
    }

    public URI getTargetURIOrDefault(URI defaultTarget) {
        return StringUtils.hasText(this.target) ? URI.create(this.target) : defaultTarget;
    }

    public MediaType getContentTypeOrDefault(MediaType defaultMediaType) {
        return this.contentType != null ? MediaType.parseMediaType(this.contentType) : defaultMediaType;
    }

    @Override
    @SneakyThrows
    public String toString() {
        return new ObjectMapper().writerWithDefaultPrettyPrinter().writeValueAsString(this);
    }

}
