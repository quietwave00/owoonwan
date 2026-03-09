package org.example.owoonwan.admin;

import org.example.owoonwan.common.error.BusinessException;
import org.example.owoonwan.common.error.ErrorCode;
import org.springframework.stereotype.Component;

@Component
public class AdminGuard {

    public void requireAdmin(String roleHeader) {
        if (!"ADMIN".equalsIgnoreCase(roleHeader)) {
            throw new BusinessException(ErrorCode.FORBIDDEN);
        }
    }
}
