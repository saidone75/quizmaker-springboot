package org.saidone.quizmaker.bootstrap;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import net.datafaker.Faker;
import org.saidone.quizmaker.entity.Student;
import org.saidone.quizmaker.repository.StudentRepository;
import org.saidone.quizmaker.service.StudentService;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.util.Locale;
import java.util.UUID;

@Component
@Profile("dev")
@RequiredArgsConstructor
@Slf4j
public class DevStudentBootstrap implements CommandLineRunner {

    private final StudentRepository studentRepository;

    private static final Faker FAKER = new Faker(Locale.ITALIAN);

    @Override
    public void run(String... args) {
        for (var i = 0; i < 6; i++) {
            val student = Student.builder()
                    .id(UUID.randomUUID())
                    .fullName(String.format("%s %s", FAKER.name().firstName(), FAKER.name().lastName()))
                    .loginKeyword(StudentService.randomAlphanumeric(4))
                    .build();
            studentRepository.save(student);
        }

        log.info("Studenti demo creati!");
    }

}
