package com.contentgrid.hateoas.client.hal;


import java.net.URI;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.hateoas.MediaTypes;
import org.springframework.http.MediaType;
import org.springframework.web.client.RestClient;

@Slf4j
@RequiredArgsConstructor
class DefaultHalClient implements HalClient {

    @NonNull
    private final RestClient restClient;

    @Override
    public HalRequest get(URI uri) {
        var request = this.restClient.get().uri(uri).accept(MediaTypes.HAL_JSON, MediaType.APPLICATION_JSON);
        return new DefaultHalRequest(request);
    }
}
