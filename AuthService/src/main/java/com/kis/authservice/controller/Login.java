package com.kis.authservice.controller;



import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
@RequiredArgsConstructor
@Controller
public class Login {


    @GetMapping("/login")
    public String login() {
        return "login";
    }
}
