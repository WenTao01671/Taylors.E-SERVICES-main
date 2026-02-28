package edu.taylors.io.capstone.eservices.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/staff")
public class StaffController {


    //This is for test staff access
    @GetMapping("/test")
    public String testStaff() {
        return "Staff access OK";
    }
}