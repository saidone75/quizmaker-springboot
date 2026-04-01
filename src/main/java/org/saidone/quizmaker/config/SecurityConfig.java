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

package org.saidone.quizmaker.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.HeadersConfigurer;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.csrf.CookieCsrfTokenRepository;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/css/**", "/js/**", "/images/**", "/favicon.ico").permitAll()
                        .requestMatchers("/h2-console/**").permitAll()
                        .requestMatchers("/teacher/login", "/teacher/register", "/", "/student/login").permitAll()
                        .requestMatchers(HttpMethod.POST, "/student/logout").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/quizzes", "/api/quizzes/**").permitAll()
                        .requestMatchers(HttpMethod.POST, "/api/quizzes/*/submit").permitAll()
                        .requestMatchers(HttpMethod.POST, "/api/quizzes/**", "/api/quizzes/*/unlock/*").hasRole("TEACHER")
                        .requestMatchers(HttpMethod.PUT, "/api/quizzes/**").hasRole("TEACHER")
                        .requestMatchers(HttpMethod.DELETE, "/api/quizzes/**").hasRole("TEACHER")
                        .requestMatchers("/api/students/**").hasRole("TEACHER")
                        .requestMatchers("/api/teacher/**").hasRole("TEACHER")
                        .requestMatchers("/about").hasRole("ADMIN")
                        .requestMatchers("/teacher/about").hasRole("ADMIN")
                        .requestMatchers("/teacher/**").hasRole("TEACHER")
                        .anyRequest().authenticated()
                )
                .formLogin(form -> form
                        .loginPage("/teacher/login")
                        .loginProcessingUrl("/teacher/login")
                        .defaultSuccessUrl("/teacher", true)
                        .failureUrl("/teacher/login?error=true")
                        .usernameParameter("username")
                        .passwordParameter("password")
                        .permitAll()
                )
                .logout(logout -> logout
                        .logoutUrl("/teacher/logout")
                        .logoutSuccessUrl("/teacher/login?logout=true")
                        .invalidateHttpSession(true)
                        .deleteCookies("JSESSIONID")
                        .permitAll()
                )
                .csrf(csrf -> csrf
                        .csrfTokenRepository(CookieCsrfTokenRepository.withHttpOnlyFalse())
                        .ignoringRequestMatchers("/h2-console/**")
                )
                .headers(headers -> headers
                        .frameOptions(HeadersConfigurer.FrameOptionsConfig::sameOrigin)
                );

        return http.build();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
