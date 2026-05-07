package org.example.owoonwan.config;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class SpaForwardController {

    @GetMapping({
            "/",
            "/login",
            "/nickname-select",
            "/checkin",
            "/pledges",
            "/stats",
            "/board/weekly",
            "/admin/nicknames",
            "/admin/kakkdugi",
            "/admin/checkins"
    })
    public String forward() {
        return "forward:/index.html";
    }
}
