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

import lombok.val;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class BruteForceProtectionService {

    static final int MAX_LOGIN_FAILURES = 3;
    static final Duration LOGIN_WINDOW = Duration.ofMinutes(1);
    static final Duration LOGIN_LOCK = Duration.ofMinutes(5);
    static final int MAX_STUDENT_LOGIN_FAILURES = 5;
    static final Duration STUDENT_LOGIN_WINDOW = Duration.ofMinutes(1);
    static final Duration STUDENT_LOGIN_LOCK = Duration.ofMinutes(3);

    static final int MAX_REGISTER_ATTEMPTS = 10;
    static final Duration REGISTER_WINDOW = Duration.ofMinutes(10);

    private static final String STUDENT_IP_KEY_PREFIX = "student-ip";
    private static final String STUDENT_KEYWORD_KEY_PREFIX = "student-keyword";
    private static final String KEY_FORMAT = "%s|%s";

    private final Map<String, LoginState> loginStates = new ConcurrentHashMap<>();
    private final Map<String, Deque<Instant>> registerAttempts = new ConcurrentHashMap<>();
    private final Clock clock;

    @Autowired
    public BruteForceProtectionService() {
        this(Clock.systemUTC());
    }

    BruteForceProtectionService(Clock clock) {
        this.clock = clock;
    }

    public boolean isLoginBlocked(String key) {
        val state = loginStates.get(key);
        if (state == null) {
            return false;
        }

        val now = Instant.now(clock);
        if (state.blockedUntil != null && state.blockedUntil.isAfter(now)) {
            return true;
        }

        if (state.blockedUntil != null && !state.blockedUntil.isAfter(now)) {
            loginStates.remove(key);
        }
        return false;
    }

    private static String key(String prefix, String suffix) {
        return String.format(KEY_FORMAT, prefix, suffix);
    }

    public void recordLoginFailure(String key) {
        recordLoginFailure(key, MAX_LOGIN_FAILURES, LOGIN_WINDOW, LOGIN_LOCK);
    }

    public void recordStudentLoginFailureByIp(String ipAddress) {
        recordLoginFailure(key(STUDENT_IP_KEY_PREFIX, ipAddress), MAX_STUDENT_LOGIN_FAILURES, STUDENT_LOGIN_WINDOW, STUDENT_LOGIN_LOCK);
    }

    public void recordStudentLoginFailureByKeyword(String keyword) {
        recordLoginFailure(key(STUDENT_KEYWORD_KEY_PREFIX, keyword.toLowerCase(Locale.ROOT)), MAX_STUDENT_LOGIN_FAILURES, STUDENT_LOGIN_WINDOW, STUDENT_LOGIN_LOCK);
    }

    public boolean isStudentLoginBlocked(String ipAddress, String keyword) {
        return isLoginBlocked(key(STUDENT_IP_KEY_PREFIX, ipAddress))
                || isLoginBlocked(key(STUDENT_KEYWORD_KEY_PREFIX, keyword.toLowerCase(Locale.ROOT)));
    }

    public void clearStudentLoginFailures(String ipAddress, String keyword) {
        clearLoginFailures(key(STUDENT_IP_KEY_PREFIX, ipAddress));
        clearLoginFailures(key(STUDENT_KEYWORD_KEY_PREFIX, keyword.toLowerCase(Locale.ROOT)));
    }

    private void recordLoginFailure(String key, int maxFailures, Duration window, Duration lockDuration) {
        val now = Instant.now(clock);
        loginStates.compute(key, (k, state) -> {
            if (state == null) {
                state = new LoginState();
            }

            if (state.blockedUntil != null && state.blockedUntil.isAfter(now)) {
                return state;
            }

            state.prune(now, window);
            state.failures.addLast(now);
            if (state.failures.size() >= maxFailures) {
                state.blockedUntil = now.plus(lockDuration);
            }
            return state;
        });
    }

    public void clearLoginFailures(String key) {
        loginStates.remove(key);
    }

    public boolean consumeRegisterAttempt(String ipKey) {
        val now = Instant.now(clock);
        val bucket = registerAttempts.computeIfAbsent(ipKey, k -> new ArrayDeque<>());
        synchronized (bucket) {
            while (!bucket.isEmpty() && bucket.peekFirst().plus(REGISTER_WINDOW).isBefore(now)) {
                bucket.removeFirst();
            }
            if (bucket.size() >= MAX_REGISTER_ATTEMPTS) {
                return false;
            }
            bucket.addLast(now);
            return true;
        }
    }

    private static class LoginState {
        private final Deque<Instant> failures = new ArrayDeque<>();
        private Instant blockedUntil;

        private void prune(Instant now, Duration window) {
            while (!failures.isEmpty() && failures.peekFirst().plus(window).isBefore(now)) {
                failures.removeFirst();
            }
        }
    }
}
