package com.example.cart;

import java.util.Map;
import java.util.function.Supplier;

import org.springframework.web.client.HttpClientErrorException;

/** Re-throws another service's 4xx response as this service's own error, keeping its message. */
final class DownstreamErrors {

    private DownstreamErrors() {
    }

    static <T> T call(Supplier<T> request) {
        try {
            return request.get();
        } catch (HttpClientErrorException.NotFound e) {
            throw new NotFoundException(message(e));
        } catch (HttpClientErrorException e) {
            throw new BadRequestException(message(e));
        }
    }

    private static String message(HttpClientErrorException e) {
        Map<?, ?> body = e.getResponseBodyAs(Map.class);
        if (body != null && body.get("error") != null) {
            return body.get("error").toString();
        }
        return e.getStatusText();
    }
}
