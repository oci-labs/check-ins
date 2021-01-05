package com.objectcomputing.checkins.services.questions;

import com.objectcomputing.checkins.services.exceptions.AlreadyExistsException;
import com.objectcomputing.checkins.services.exceptions.BadArgException;
import com.objectcomputing.checkins.services.exceptions.NotFoundException;

import javax.inject.Singleton;
import javax.validation.constraints.NotNull;
import java.util.Set;
import java.util.UUID;

@Singleton
public class QuestionServicesImpl implements QuestionServices {

    private final QuestionRepository questionRepository;

    public QuestionServicesImpl(QuestionRepository questionRepository) {
        this.questionRepository = questionRepository;
    }

    public Question saveQuestion(Question question) {

        Set<Question> returnedList = findByValue(question.getText());
        if (returnedList.size() >= 1) {
            throw new AlreadyExistsException("Already exists");
        }
        return questionRepository.save(question);

    }

    public Set<Question> readAllQuestions() {
        return questionRepository.findAll();
    }

    public Question findById(@NotNull UUID id) {
        return questionRepository.findById(id).orElseThrow(() -> new NotFoundException(String.format("No question for id %s", id)));
    }

    protected Set<Question> findByValue(String text) {
        Set<Question> questionList = questionRepository.findAll();
        if (text != null) {
            questionList.retainAll(findByText(text));
        }

        return questionList;
    }

    public Set<Question> findByText(String text) {
        String wildcard = "%" + text + "%";
        Set<Question> skillList = questionRepository.findByTextIlike(wildcard);

        return skillList;
    }

    public Question update(Question question) {
        Question returned = null;
        try {
            findById(question.getId());
        } catch (NotFoundException qnfe) {
            throw new BadArgException("No question found for this id");
        }
        returned = questionRepository.update(question);

        return returned;

    }

}
