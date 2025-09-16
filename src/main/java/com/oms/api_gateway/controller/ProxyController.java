package com.oms.api_gateway.controller;

import com.netflix.discovery.DiscoveryClient;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.net.URI;
import java.util.Map;


public class ProxyController {

    @Autowired
    private DiscoveryClient discoveryClient;

    private final WebClient webClient = WebClient.create();

    public Mono<ResponseEntity<String>> proxyRequest(
            @PathVariable String serviceId,
            HttpMethod method,                                  // http method --> GET,POST,PUT,PATCH, DELETE
            @RequestBody(required = false) String body,
            @RequestHeader Map<String, String> headers,
            HttpServletRequest request
    ){
        var instances = discoveryClient.getInstancesById(serviceId);
        if (instances.isEmpty()) {
            return Mono.just(ResponseEntity.notFound().build());
        }

        // pick first instance (or do load-balancing round-robin)
        String serviceUri = instances.get(0).getHomePageUrl();

        String path = request.getRequestURI().replace("/"+serviceId, "");
        String targetUrl  = serviceUri + path;

        WebClient.RequestBodySpec spec = webClient
                .method(method)
                .uri(targetUrl)
                .headers(httpHeaders -> httpHeaders.setAll(headers));

        // check if body is empty
        if (body!=null){
            spec.bodyValue(body);
        }


        return spec.retrieve().toEntity(String.class);
    }
}
