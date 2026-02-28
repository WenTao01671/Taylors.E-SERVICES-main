package edu.taylors.io.capstone.eservices.controller;

import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/student")
public class StudentTestController {


    // This is for test student access
    @GetMapping("/test")
    public Map<String, String> testStudent(Authentication authentication) {
        return Map.of(
                "message", "Student access OK",
                "studentId", authentication.getName(),
                "role", authentication.getAuthorities().toString()
        );
    }
}