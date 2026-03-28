package com.quizmaker.mapper;

import com.quizmaker.dto.QuizDto;
import com.quizmaker.entity.Quiz;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

@Mapper(componentModel = "spring", uses = QuestionMapper.class)
public interface QuizMapper {

    @Mapping(target = "questionsCount", expression = "java(quiz.getQuestions() != null ? quiz.getQuestions().size() : 0)")
    QuizDto.Response toResponse(Quiz quiz);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    void updateEntityFromRequest(QuizDto.Request request, @MappingTarget Quiz quiz);

}