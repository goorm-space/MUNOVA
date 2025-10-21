package com.space.munova.chat.controller;

import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.stereotype.Controller;

import java.text.SimpleDateFormat;
import java.util.Date;

@Controller
public class GreetingController {

    @MessageMapping("/greeting")
    public String handle(String greeting) {
        return "[" + new SimpleDateFormat("MM/dd/yyyy h:mm:ss a").format(new Date()) + ": " + greeting;
    }
}
