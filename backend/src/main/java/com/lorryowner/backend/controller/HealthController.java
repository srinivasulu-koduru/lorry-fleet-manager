package com.lorryowner.backend.controller;

import com.lorryowner.backend.dto.HealthResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Controller providing a lightweight health verification endpoint.
 * Accessible publicly for connectivity and deployment health checks.
 */
@RestController
public class HealthController {

    private static final String APP_NAME = "Lorry Owner Backend";
    private static final String STATUS_UP = "UP";

    @GetMapping({"/", "/api/health"})
    public ResponseEntity<HealthResponse> getHealth() {
        return ResponseEntity.ok(new HealthResponse(STATUS_UP, APP_NAME));
    }
}
