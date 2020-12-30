/*
 * Copyright 2013-2015 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package reactivefeign.webclient.client;

import static feign.Util.resolveLastTypeParameter;
import static java.util.Optional.ofNullable;
import static reactivefeign.utils.FeignUtils.getBodyActualType;
import static reactivefeign.utils.FeignUtils.returnActualType;
import static reactivefeign.utils.FeignUtils.returnPublisherType;

import feign.MethodMetadata;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.reactivestreams.Publisher;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.reactive.ClientHttpRequest;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.reactive.function.BodyInserter;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.ClientResponse;
import org.springframework.web.reactive.function.client.WebClient;
import reactivefeign.client.ReactiveFeignException;
import reactivefeign.client.ReactiveHttpClient;
import reactivefeign.client.ReactiveHttpRequest;
import reactivefeign.client.ReactiveHttpResponse;
import reactivefeign.client.ReadTimeoutException;
import reactor.core.publisher.Mono;

/**
 * Uses {@link WebClient} to execute http requests
 *
 * @author Sergii Karpenko
 */
public class WebReactiveHttpClient<P extends Publisher<?>> implements ReactiveHttpClient<P> {

    private final WebClient webClient;
    private final ParameterizedTypeReference<Object> bodyActualType;
    private final BiFunction<ReactiveHttpRequest, ClientResponse, ReactiveHttpResponse<P>> responseFunction;
    private static final Map<String, Function<Map<String, List<Object>>, BodyInserter<?, ? super ClientHttpRequest>>> formBodyInserts =
        new HashMap<>();
    private static final Function<Map<String, List<Object>>, BodyInserter<?, ? super ClientHttpRequest>> DEFAULT;

    static {
        formBodyInserts.put("multipart/form-data", multiValueMap ->
            BodyInserters.fromMultipartData(new LinkedMultiValueMap<>(multiValueMap)));
        formBodyInserts.put("application/x-www-form-urlencoded", multiValueMap -> {
            MultiValueMap<String, String> map = new LinkedMultiValueMap<>();
            multiValueMap.forEach((k, v) -> {
                map.put(k, v.stream().map(Object::toString).collect(Collectors.toList()));
            });
            return BodyInserters.fromFormData(map);
        });
        DEFAULT = x -> BodyInserters.empty();
    }

    public static <P extends Publisher<?>> WebReactiveHttpClient<P> webClient(MethodMetadata methodMetadata,
        WebClient webClient) {

        Type returnPublisherType = returnPublisherType(methodMetadata);
        ParameterizedTypeReference<?> returnActualType =
            ParameterizedTypeReference.forType(returnActualType(methodMetadata));

        ParameterizedTypeReference<Object> bodyActualType = ofNullable(
            getBodyActualType(methodMetadata.bodyType()))
            .map(ParameterizedTypeReference::forType)
            .orElse(null);

        if (returnActualType.getType() instanceof ParameterizedType
            && ((ParameterizedType) returnActualType.getType()).getRawType().equals(ResponseEntity.class)) {
            Type entityType = resolveLastTypeParameter(returnActualType.getType(), ResponseEntity.class);

            Type entityPublisherType = returnPublisherType(entityType);
            ParameterizedTypeReference<?> entityActualType =
                ParameterizedTypeReference.forType(returnActualType(entityType));

            return new WebReactiveHttpClient<>(webClient, bodyActualType,
                (request, response) -> new WebReactiveHttpEntityResponse<>(request, response, entityPublisherType,
                    entityActualType));
        }

        return new WebReactiveHttpClient<>(webClient, bodyActualType,
            webReactiveHttpResponse(returnPublisherType, returnActualType));
    }

    public static <P extends Publisher<?>> BiFunction<ReactiveHttpRequest, ClientResponse, ReactiveHttpResponse<P>> webReactiveHttpResponse(
        Type returnPublisherType, ParameterizedTypeReference<?> returnActualType) {
        return (request, response) -> new WebReactiveHttpResponse<>(request, response, returnPublisherType,
            returnActualType);
    }

    public WebReactiveHttpClient(WebClient webClient,
        ParameterizedTypeReference<Object> bodyActualType,
        BiFunction<ReactiveHttpRequest, ClientResponse, ReactiveHttpResponse<P>> responseFunction) {
        this.webClient = webClient;
        this.bodyActualType = bodyActualType;
        this.responseFunction = responseFunction;
    }

    @Override
    public Mono<ReactiveHttpResponse<P>> executeRequest(ReactiveHttpRequest request) {
        return webClient.method(HttpMethod.valueOf(request.method()))
            .uri(request.uri())
            .headers(httpHeaders -> setUpHeaders(request, httpHeaders))
            .body(provideBody(request))
            .exchange()
            .onErrorMap(ex -> {
                if (ex instanceof io.netty.handler.timeout.ReadTimeoutException) {
                    return new ReadTimeoutException(ex, request);
                } else {
                    return new ReactiveFeignException(ex, request);
                }
            })
            .map(response -> toReactiveHttpResponse(request, response));
    }

    protected ReactiveHttpResponse<P> toReactiveHttpResponse(ReactiveHttpRequest request, ClientResponse response) {
        return responseFunction.apply(request, response);
    }

    protected BodyInserter<?, ? super ClientHttpRequest> provideBody(ReactiveHttpRequest request) {
        return bodyActualType != null
            ? BodyInserters.fromPublisher(request.body(), bodyActualType)
            : (request.formVariables() != null
                ? formBodyInserts.getOrDefault(request.contentType(), DEFAULT).apply(request.formVariables())
                : BodyInserters.empty());
    }

    protected void setUpHeaders(ReactiveHttpRequest request, HttpHeaders httpHeaders) {
        request.headers().forEach(httpHeaders::put);
    }

}
