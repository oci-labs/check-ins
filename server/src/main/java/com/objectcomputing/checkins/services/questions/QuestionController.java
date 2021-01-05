package com.objectcomputing.checkins.services.questions;

import com.objectcomputing.checkins.services.exceptions.NotFoundException;
import io.micronaut.http.HttpRequest;
import io.micronaut.http.HttpResponse;
import io.micronaut.http.MediaType;
import io.micronaut.http.annotation.*;
import io.micronaut.scheduling.TaskExecutors;
import io.micronaut.security.annotation.Secured;
import io.micronaut.security.rules.SecurityRule;
import io.netty.channel.EventLoopGroup;
import io.reactivex.Single;
import io.reactivex.schedulers.Schedulers;
import io.swagger.v3.oas.annotations.tags.Tag;

import javax.inject.Named;
import javax.validation.Valid;
import java.net.URI;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.stream.Collectors;

@Controller("/services/questions")
@Secured(SecurityRule.IS_AUTHENTICATED)
@Produces(MediaType.APPLICATION_JSON)
@Tag(name = "questions")
public class QuestionController {

    private final QuestionServices questionService;
    private final EventLoopGroup eventLoopGroup;
    private final ExecutorService ioExecutorService;

    public QuestionController(QuestionServices questionService, EventLoopGroup eventLoopGroup,
                              @Named(TaskExecutors.IO) ExecutorService ioExecutorService) {
        this.questionService = questionService;
        this.eventLoopGroup = eventLoopGroup;
        this.ioExecutorService = ioExecutorService;
    }

    /**
     * Create and save a new question.
     *
     * @param question, {@link QuestionCreateDTO}
     * @return {@link HttpResponse<QuestionResponseDTO>}
     */

    @Post()
    public Single<HttpResponse<QuestionResponseDTO>> createAQuestion(@Body @Valid QuestionCreateDTO question, HttpRequest<QuestionCreateDTO> request) {

        return Single.fromCallable(() -> questionService.saveQuestion(toModel(question)))
                .observeOn(Schedulers.from(eventLoopGroup))
                .map(newQuestion -> (HttpResponse<QuestionResponseDTO>) HttpResponse
                        .created(fromModel(newQuestion))
                        .headers(headers -> headers.location(
                                URI.create(String.format("%s/%s", request.getPath(), newQuestion.getId())))))
                .subscribeOn(Schedulers.from(ioExecutorService));

    }

    /**
     * Find and read a question given its id.
     *
     * @param id {@link UUID} of the question entry
     * @return {@link HttpResponse< QuestionResponseDTO >}
     */

    @Get("/{id}")
    public Single<HttpResponse<QuestionResponseDTO>> getById(UUID id) {
        return Single.fromCallable(() -> {
            Question found = questionService.findById(id);
            if (found == null) {
                throw new NotFoundException("No question for UUID");
            }
            return found;
        })
                .observeOn(Schedulers.from(eventLoopGroup))
                .map(question -> (HttpResponse<QuestionResponseDTO>) HttpResponse.ok(fromModel(question)))
                .subscribeOn(Schedulers.from(ioExecutorService));
    }

    /**
     * Find questions with a particular string or read all questions.
     *
     * @param text, the text of the question
     *              * @return {@link List HttpResponse< QuestionResponseDTO >}
     */
    @Get("/{?text}")
    public Single<HttpResponse<Set<QuestionResponseDTO>>> findByText(Optional<String> text) {
        return Single.fromCallable(() -> {
            if (text.isPresent()) {
                return questionService.findByText(text.get());
            } else {
                return questionService.readAllQuestions();
            }
        })
                .observeOn(Schedulers.from(eventLoopGroup))
                .map(questions -> {
                    Set<QuestionResponseDTO> responseBody = questions.stream()
                            .map(question -> fromModel(question))
                            .collect(Collectors.toSet());
                    return (HttpResponse<Set<QuestionResponseDTO>>) HttpResponse.ok(responseBody);
                })
                .subscribeOn(Schedulers.from(ioExecutorService));
    }

    /**
     * Update the text of a question.
     *
     * @param question, {@link QuestionUpdateDTO}
     * @return {@link HttpResponse< QuestionResponseDTO >}
     */
    @Put()
    public Single<HttpResponse<QuestionResponseDTO>> update(@Body @Valid QuestionUpdateDTO question, HttpRequest<QuestionCreateDTO> request) {
        if (question == null) {
            return Single.just(HttpResponse.ok());
        }
        return Single.fromCallable(() -> questionService.update(toModel(question)))
                .observeOn(Schedulers.from(eventLoopGroup))
                .map(updatedQuestion ->
                        (HttpResponse<QuestionResponseDTO>) HttpResponse
                                .created(fromModel(updatedQuestion))
                                .headers(headers -> headers.location(
                                        URI.create(String.format("%s/%s", request.getPath(), updatedQuestion.getId())))))
                .subscribeOn(Schedulers.from(ioExecutorService));

    }

    private QuestionResponseDTO fromModel(Question question) {
        QuestionResponseDTO qrdto = new QuestionResponseDTO();
        qrdto.setQuestionId(question.getId());
        qrdto.setText(question.getText());
        return qrdto;
    }

    private Question toModel(QuestionUpdateDTO dto) {
        Question model = new Question();
        model.setId(dto.getId());
        model.setText(dto.getText());
        return model;
    }

    private Question toModel(QuestionCreateDTO dto) {
        Question model = new Question();
        model.setText(dto.getText());
        return model;
    }

}
