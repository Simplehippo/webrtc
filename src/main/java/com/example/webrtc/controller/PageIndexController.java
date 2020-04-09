package com.example.webrtc.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

/**
 * @author simplehippo
 * @version 1.0
 */
@Controller
@RequestMapping("/page")
public class PageIndexController {

    @GetMapping("hello")
    @ResponseBody
    public String hello() {
        return "hello";
    }

    @GetMapping("/v2/demo")
    public String v2() {
        return "v2/demo";
    }
}
