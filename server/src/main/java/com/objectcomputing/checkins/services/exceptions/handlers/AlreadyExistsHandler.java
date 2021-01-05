package com.objectcomputing.checkins.services.exceptions.handlers;

import com.objectcomputing.checkins.services.exceptions.AlreadyExistsException;
import io.micronaut.context.annotation.Requires;
import io.micronaut.http.HttpRequest;
import io.micronaut.http.HttpResponse;
import io.micronaut.http.HttpStatus;
import io.micronaut.http.annotation.Produces;
import io.micronaut.http.hateoas.JsonError;
import io.micronaut.http.hateoas.Link;
import io.micronaut.http.server.exceptions.ExceptionHandler;

import javax.inject.Singleton;

@Produces
@Singleton
@Requires(classes = {AlreadyExistsException.class, ExceptionHandler.class})
public class AlreadyExistsHandler implements ExceptionHandler<AlreadyExistsException, HttpResponse> {

    @Override
    public HttpResponse<?> handle(HttpRequest request, AlreadyExistsException e) {
        JsonError error = new JsonError(e.getMessage())
                .link(Link.SELF, Link.of(request.getUri()));

        return HttpResponse.<JsonError>status(HttpStatus.CONFLICT).body(error);
    }

}
