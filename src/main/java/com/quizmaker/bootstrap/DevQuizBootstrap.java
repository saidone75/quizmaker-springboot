package com.quizmaker.bootstrap;

import com.quizmaker.entity.Question;
import com.quizmaker.entity.Quiz;
import com.quizmaker.repository.QuizRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@Profile("dev")
@RequiredArgsConstructor
@Slf4j
public class DevQuizBootstrap implements CommandLineRunner {

    private static final String DEMO_QUIZ_TITLE = "Astronomia: il quiz delle stelle";

    private final QuizRepository quizRepository;

    @Override
    public void run(String... args) {
        if (quizRepository.existsByTitle(DEMO_QUIZ_TITLE)) {
            log.info("Quiz demo già presente, bootstrap saltato");
            return;
        }

        quizRepository.save(
                Quiz.builder()
                        .title(DEMO_QUIZ_TITLE)
                        .emoji("🌌")
                        .questions(List.of(
                                question(
                                        "Qual è il pianeta più grande del Sistema Solare?",
                                        "🪐",
                                        List.of("Terra", "Giove", "Saturno", "Marte"),
                                        1,
                                        "Giove è il pianeta più grande del Sistema Solare: un colosso gassoso così enorme che potrebbe contenere più di 1.300 Terre."
                                ),
                                question(
                                        "Come si chiama la galassia in cui si trova la Terra?",
                                        "🌠",
                                        List.of("Andromeda", "Sombrero", "Via Lattea", "Triangolo"),
                                        2,
                                        "La Via Lattea è la galassia a spirale barrata che ospita il Sistema Solare, con centinaia di miliardi di stelle immerse in un elegante caos cosmico."
                                ),
                                question(
                                        "Qual è il pianeta più vicino al Sole?",
                                        "☀️",
                                        List.of("Venere", "Marte", "Mercurio", "Terra"),
                                        2,
                                        "Mercurio ha giornate stranissime: un suo giorno solare dura circa 176 giorni terrestri, cioè più di un suo anno."
                                )
                        ))
                        .build()
        );

        log.info("Quiz demo di astronomia creato!");
    }

    private Question question(String text, String emoji, List<String> options, Integer answer, String feedback) {
        val question = new Question();
        question.setText(text);
        question.setEmoji(emoji);
        question.setOptions(options);
        question.setAnswer(answer);
        question.setFeedback(feedback);
        return question;
    }

}
