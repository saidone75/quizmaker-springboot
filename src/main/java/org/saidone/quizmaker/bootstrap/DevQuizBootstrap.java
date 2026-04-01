package org.saidone.quizmaker.bootstrap;

import org.saidone.quizmaker.entity.Question;
import org.saidone.quizmaker.entity.Quiz;
import org.saidone.quizmaker.repository.QuizRepository;
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
    private static final String DINO_QUIZ_TITLE = "Dinosauri: i giganti del Mesozoico";

    private final QuizRepository quizRepository;

    @Override
    public void run(String... args) {
        var createdAstronomyQuiz = false;
        var createdDinoQuiz = false;

        if (!quizRepository.existsByTitle(DEMO_QUIZ_TITLE)) {
            quizRepository.save(
                    Quiz.builder()
                            .title(DEMO_QUIZ_TITLE)
                            .emoji("🌌")
                            .published(true)
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
            createdAstronomyQuiz = true;
        }

        if (!quizRepository.existsByTitle(DINO_QUIZ_TITLE)) {
            quizRepository.save(
                    Quiz.builder()
                            .title(DINO_QUIZ_TITLE)
                            .emoji("🦖")
                            .published(true)
                            .questions(List.of(
                                    question(
                                            "Quale dinosauro è famoso per il collo molto lungo?",
                                            "🦕",
                                            List.of("Triceratopo", "Brachiosauro", "Velociraptor", "Stegosauro"),
                                            1,
                                            "Il Brachiosauro aveva un collo lunghissimo, utile per raggiungere le foglie più alte degli alberi."
                                    ),
                                    question(
                                            "Il Tyrannosaurus rex era principalmente...",
                                            "🦖",
                                            List.of("Erbivoro", "Carnivoro", "Onnivoro", "Insettivoro"),
                                            1,
                                            "Il T. rex era un grande carnivoro, con denti robusti adatti a mordere prede di grandi dimensioni."
                                    ),
                                    question(
                                            "Quale dinosauro aveva tre corna sul muso?",
                                            "🦴",
                                            List.of("Ankylosauro", "Diplodoco", "Triceratopo", "Spinosauro"),
                                            2,
                                            "Il Triceratopo aveva due corna sopra gli occhi e una sul naso, oltre a un grande collare osseo."
                                    )
                            ))
                            .build()
            );

            log.info("Quiz demo sui dinosauri creato!");
            createdDinoQuiz = true;
        }

        if (!createdAstronomyQuiz && !createdDinoQuiz) {
            log.info("Quiz demo già presenti, bootstrap completato");
        }
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
