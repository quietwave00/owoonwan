package org.example.owoonwan.common.firebase;

import com.google.api.core.ApiFuture;
import java.util.concurrent.ExecutionException;
import lombok.extern.slf4j.Slf4j;
import org.example.owoonwan.common.error.BusinessException;
import org.example.owoonwan.common.error.ErrorCode;

@Slf4j
public final class FirestoreAwait {

    private FirestoreAwait() {
    }

    public static <T> T get(ApiFuture<T> future) {
        try {
            return future.get();
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            log.error("Firestore operation interrupted", exception);
            throw new BusinessException(ErrorCode.INTERNAL_SERVER_ERROR, "Firestore 처리 중 스레드가 중단되었습니다.");
        } catch (ExecutionException exception) {
            if (exception.getCause() instanceof BusinessException businessException) {
                throw businessException;
            }

            Throwable cause = exception.getCause() == null ? exception : exception.getCause();
            log.error(
                    "Firestore operation failed. causeType={}, message={}",
                    cause.getClass().getName(),
                    cause.getMessage(),
                    cause
            );
            throw new BusinessException(ErrorCode.INTERNAL_SERVER_ERROR, "Firestore 처리 중 오류가 발생했습니다.");
        }
    }
}
