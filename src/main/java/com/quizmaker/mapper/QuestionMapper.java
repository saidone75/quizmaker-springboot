package com.quizmaker.mapper;

import com.quizmaker.dto.QuestionDto;
import com.quizmaker.entity.Question;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface QuestionMapper {

    Question toEntity(QuestionDto dto);

    QuestionDto toDto(Question question);

}
