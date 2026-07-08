package com.ict.lms.web;

import java.util.Map;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * A tiny test endpoint so we can confirm the backend is alive
 * and that the React frontend can talk to it.
 * Visit http://localhost:8080/api/ping
 */
@RestController
@RequestMapping("/api")
public class PingController {

    @GetMapping("/ping")
    public Map<String, String> ping() {
        return Map.of(
            "status", "ok",
            "app", "Stack ICT Academy"
        );
    }
}
