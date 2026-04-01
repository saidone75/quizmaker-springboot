package org.saidone.quizmaker.controller;

import lombok.RequiredArgsConstructor;
import org.saidone.quizmaker.service.LogTailService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin/logs")
@RequiredArgsConstructor
public class AdminLogApiController {

    private final LogTailService logTailService;

    @GetMapping("/tail")
    public LogTailService.LogTailResult tailLogs(@RequestParam(name = "lines", defaultValue = "200") int lines) {
        return logTailService.readLastLines(lines);
    }
}
