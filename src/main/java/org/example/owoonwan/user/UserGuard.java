package org.example.owoonwan.user;

import org.example.owoonwan.common.error.BusinessException;
import org.example.owoonwan.common.error.ErrorCode;
import org.springframework.stereotype.Component;

@Component
public class UserGuard {

    public void requireSameUser(String pathUserId, String headerUserId) {
        if (headerUserId == null || headerUserId.isBlank() || !pathUserId.equals(headerUserId)) {
            throw new BusinessException(ErrorCode.FORBIDDEN);
        }
    }
}
