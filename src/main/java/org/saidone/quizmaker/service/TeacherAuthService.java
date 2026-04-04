/*
 * QuizMaker - fun quizzes for curious minds
 * Copyright (C) 2026 Saidone
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.saidone.quizmaker.service;

import lombok.RequiredArgsConstructor;
import lombok.val;
import net.datafaker.Faker;
import org.jspecify.annotations.NullMarked;
import org.saidone.quizmaker.entity.Teacher;
import org.saidone.quizmaker.repository.QuizRepository;
import org.saidone.quizmaker.repository.QuizSubmissionRepository;
import org.saidone.quizmaker.repository.StudentRepository;
import org.saidone.quizmaker.repository.TeacherRepository;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class TeacherAuthService implements UserDetailsService {

    private final TeacherRepository teacherRepository;
    private final QuizRepository quizRepository;
    private final StudentRepository studentRepository;
    private final QuizSubmissionRepository quizSubmissionRepository;
    private final PasswordEncoder passwordEncoder;

    private static final Faker FAKER = new Faker(Locale.ITALIAN);
    private static final SecureRandom RANDOM = new SecureRandom();

    @Override
    @NullMarked
    @Transactional(readOnly = true)
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        val teacher = teacherRepository.findByUsernameIgnoreCase(username)
                .orElseThrow(() -> new UsernameNotFoundException(String.format("Insegnante non trovato: %s", username)));

        return new User(
                teacher.getUsername(),
                normalizeStoredPassword(teacher.getPassword()),
                teacher.isEnabled(),
                true,
                true,
                true,
                buildAuthorities(teacher)
        );
    }

    @Transactional
    public void register(String username, String rawPassword) {
        val normalizedUsername = username == null ? "" : username.trim().toLowerCase();
        if (normalizedUsername.isBlank()) {
            throw new IllegalArgumentException("Lo username è obbligatorio");
        }
        if (normalizedUsername.length() < 4) {
            throw new IllegalArgumentException("Lo username deve avere almeno 4 caratteri");
        }
        if (rawPassword == null || rawPassword.length() < 6) {
            throw new IllegalArgumentException("La password deve avere almeno 6 caratteri");
        }
        if (teacherRepository.existsByUsernameIgnoreCase(normalizedUsername)) {
            throw new IllegalArgumentException("Username già in uso");
        }

        teacherRepository.save(Teacher.builder()
                .username(normalizedUsername)
                .password(passwordEncoder.encode(rawPassword))
                .admin(false)
                .aiEnabled(false)
                .enabled(true)
                .build());
    }

    @Transactional(readOnly = true)
    public List<Teacher> findAllTeachers() {
        return teacherRepository.findAllByOrderByCreatedAtAsc();
    }

    private List<GrantedAuthority> buildAuthorities(Teacher teacher) {
        if (teacher.isAdmin()) {
            return List.of(
                    new SimpleGrantedAuthority("ROLE_TEACHER"),
                    new SimpleGrantedAuthority("ROLE_ADMIN")
            );
        }
        return List.of(new SimpleGrantedAuthority("ROLE_TEACHER"));
    }

    private String normalizeStoredPassword(String encodedPassword) {
        if (encodedPassword == null) {
            return "";
        }
        if (encodedPassword.startsWith("{bcrypt}")) {
            return encodedPassword.substring(8);
        }
        return encodedPassword;
    }

    @Transactional(readOnly = true)
    public Teacher getCurrentTeacher() {
        val authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated() || authentication instanceof AnonymousAuthenticationToken) {
            throw new IllegalStateException("Insegnante non autenticato");
        }

        return teacherRepository.findByUsernameIgnoreCase(authentication.getName())
                .orElseThrow(() -> new IllegalStateException(String.format("Insegnante non trovato: %s", authentication.getName())));
    }

    @Transactional
    public void changePassword(Teacher teacher, String currentPassword, String newPassword) {
        if (teacher == null) {
            throw new IllegalArgumentException("Insegnante non valido");
        }
        if (currentPassword == null || currentPassword.isBlank()) {
            throw new IllegalArgumentException("La password attuale è obbligatoria");
        }
        if (newPassword == null || newPassword.length() < 6) {
            throw new IllegalArgumentException("La nuova password deve avere almeno 6 caratteri");
        }

        val encodedPassword = normalizeStoredPassword(teacher.getPassword());
        if (!passwordEncoder.matches(currentPassword, encodedPassword)) {
            throw new IllegalArgumentException("La password attuale non è corretta");
        }
        if (passwordEncoder.matches(newPassword, encodedPassword)) {
            throw new IllegalArgumentException("La nuova password deve essere diversa da quella attuale");
        }

        teacher.setPassword(passwordEncoder.encode(newPassword));
        teacherRepository.save(teacher);
    }

    @Transactional
    public void updateTeacherAdminFlag(UUID targetTeacherId, boolean admin, Teacher actingTeacher) {
        if (actingTeacher == null || !actingTeacher.isAdmin()) {
            throw new IllegalArgumentException("Operazione non consentita");
        }
        if (targetTeacherId == null) {
            throw new IllegalArgumentException("Insegnante non valido");
        }
        if (actingTeacher.getId().equals(targetTeacherId)) {
            throw new IllegalArgumentException("Non puoi modificare il tuo ruolo amministratore");
        }

        val targetTeacher = teacherRepository.findById(targetTeacherId)
                .orElseThrow(() -> new IllegalArgumentException("Insegnante non trovato"));
        targetTeacher.setAdmin(admin);
        teacherRepository.save(targetTeacher);
    }


    @Transactional
    public void updateTeacherAiFlag(UUID targetTeacherId, boolean aiEnabled, Teacher actingTeacher) {
        if (actingTeacher == null || !actingTeacher.isAdmin()) {
            throw new IllegalArgumentException("Operazione non consentita");
        }
        if (targetTeacherId == null) {
            throw new IllegalArgumentException("Insegnante non valido");
        }

        val targetTeacher = teacherRepository.findById(targetTeacherId)
                .orElseThrow(() -> new IllegalArgumentException("Insegnante non trovato"));
        targetTeacher.setAiEnabled(aiEnabled);
        teacherRepository.save(targetTeacher);
    }

    @Transactional
    public void updateTeacherEnabledFlag(UUID targetTeacherId, boolean enabled, Teacher actingTeacher) {
        if (actingTeacher == null || !actingTeacher.isAdmin()) {
            throw new IllegalArgumentException("Operazione non consentita");
        }
        if (targetTeacherId == null) {
            throw new IllegalArgumentException("Insegnante non valido");
        }
        if (actingTeacher.getId().equals(targetTeacherId) && !enabled) {
            throw new IllegalArgumentException("Non puoi disabilitare il tuo account");
        }

        val targetTeacher = teacherRepository.findById(targetTeacherId)
                .orElseThrow(() -> new IllegalArgumentException("Insegnante non trovato"));
        targetTeacher.setEnabled(enabled);
        teacherRepository.save(targetTeacher);
    }

    @Transactional
    public String resetTeacherPassword(UUID targetTeacherId, Teacher actingTeacher) {
        if (actingTeacher == null || !actingTeacher.isAdmin()) {
            throw new IllegalArgumentException("Operazione non consentita");
        }
        if (targetTeacherId == null) {
            throw new IllegalArgumentException("Insegnante non valido");
        }

        val targetTeacher = teacherRepository.findById(targetTeacherId)
                .orElseThrow(() -> new IllegalArgumentException("Insegnante non trovato"));

        String temporaryPassword;
        do {
            temporaryPassword = String.format("%s%02d", FAKER.animal().name(), RANDOM.nextInt(100));
        } while (temporaryPassword.length() < 6 || temporaryPassword.length() > 16 || temporaryPassword.contains(" "));

        targetTeacher.setPassword(passwordEncoder.encode(temporaryPassword));
        teacherRepository.save(targetTeacher);
        return temporaryPassword;
    }

    @Transactional
    public void deleteTeacherCompletely(UUID targetTeacherId, Teacher actingTeacher) {
        if (actingTeacher == null || !actingTeacher.isAdmin()) {
            throw new IllegalArgumentException("Operazione non consentita");
        }
        if (targetTeacherId == null) {
            throw new IllegalArgumentException("Insegnante non valido");
        }
        if (actingTeacher.getId().equals(targetTeacherId)) {
            throw new IllegalArgumentException("Non puoi eliminare l'utente attualmente loggato");
        }
        if (teacherRepository.countByIdNot(targetTeacherId) == 0) {
            throw new IllegalArgumentException("Impossibile eliminare l'ultimo insegnante");
        }

        val targetTeacher = teacherRepository.findById(targetTeacherId)
                .orElseThrow(() -> new IllegalArgumentException("Insegnante non trovato"));

        quizSubmissionRepository.deleteAllByStudentTeacherOrQuizTeacher(targetTeacher, targetTeacher);
        quizRepository.deleteAllByTeacher(targetTeacher);
        studentRepository.deleteAllByTeacher(targetTeacher);
        teacherRepository.delete(targetTeacher);
    }

}
