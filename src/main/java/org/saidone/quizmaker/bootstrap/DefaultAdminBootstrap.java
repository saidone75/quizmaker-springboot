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

package org.saidone.quizmaker.bootstrap;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.saidone.quizmaker.entity.Teacher;
import org.saidone.quizmaker.repository.TeacherRepository;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.env.Environment;
import org.springframework.core.env.Profiles;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class DefaultAdminBootstrap implements InitializingBean {

    @Value("${app.admin.username:admin}")
    private String adminUsername;

    @Value("${app.admin.password}")
    private String adminPassword;

    private static final String CREATE_DEFAULT_ADMIN_PROPERTY = "quizmaker.bootstrap.default-admin";

    private final TeacherRepository teacherRepository;
    private final PasswordEncoder passwordEncoder;
    private final Environment environment;

    @Override
    public void afterPropertiesSet() {
        if (!shouldBootstrapDefaultAdmin()) {
            log.debug("Bootstrap amministratore predefinito disattivato (profili dev/docker non attivi e -D{} non impostato)", CREATE_DEFAULT_ADMIN_PROPERTY);
            return;
        }

        val defaultAdmin = teacherRepository.findByUsernameIgnoreCase(adminUsername).orElse(null);
        if (defaultAdmin == null) {
            teacherRepository.save(Teacher.builder()
                    .username(adminUsername)
                    .password(passwordEncoder.encode(adminPassword))
                    .admin(true)
                    .aiEnabled(false)
                    .enabled(true)
                    .build());
            log.info("Creato amministratore predefinito '{}' con password '{}'", adminUsername, adminPassword);
            return;
        }

        var shouldSaveDefaultAdmin = false;
        if (!defaultAdmin.getPassword().equals(passwordEncoder.encode(adminPassword))) {
            defaultAdmin.setPassword(passwordEncoder.encode(adminPassword));
            shouldSaveDefaultAdmin = true;
        }
        if (!defaultAdmin.isAdmin()) {
            defaultAdmin.setAdmin(true);
            shouldSaveDefaultAdmin = true;
        }
        if (!defaultAdmin.isEnabled()) {
            defaultAdmin.setEnabled(true);
            shouldSaveDefaultAdmin = true;
        }
        if (shouldSaveDefaultAdmin) {
            teacherRepository.save(defaultAdmin);
            log.info("Allineato amministratore predefinito '{}' con impostazioni di default", adminUsername);
        }
    }

    private boolean shouldBootstrapDefaultAdmin() {
        val devOrDockerProfileActive = environment.acceptsProfiles(Profiles.of("dev", "docker"));
        val forcedByJvmFlag = Boolean.parseBoolean(System.getProperty(CREATE_DEFAULT_ADMIN_PROPERTY, "false"));
        return devOrDockerProfileActive || forcedByJvmFlag;
    }

}
