package org.example.owoonwan.title.service;

import org.example.owoonwan.title.dto.SpecialTitleResponse;
import org.example.owoonwan.user.domain.User;
import org.springframework.stereotype.Component;

@Component
public class SpecialTitleResolver {

    public SpecialTitleResponse resolve(User user) {
        return new SpecialTitleResponse(user.kakkdugi());
    }
}
