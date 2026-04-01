package org.example.owoonwan.common.firebase;

import com.google.api.core.ApiFuture;
import lombok.extern.slf4j.Slf4j;
import org.example.owoonwan.common.error.BusinessException;
import org.example.owoonwan.common.error.ErrorCode;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

@Slf4j
public final class FirestoreAwait {

    private static final long FIRESTORE_TIMEOUT_SECONDS = 20;

    private FirestoreAwait() {
    }

    public static <T> T get(ApiFuture<T> future) {
        try {
            return future.get(FIRESTORE_TIMEOUT_SECONDS, TimeUnit.SECONDS);
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            log.error("Firestore operation interrupted", exception);
            throw new BusinessException(ErrorCode.INTERNAL_SERVER_ERROR, "Firestore operation was interrupted.");
        } catch (TimeoutException exception) {
            log.error("Firestore operation timed out after {} seconds", FIRESTORE_TIMEOUT_SECONDS, exception);
            throw new BusinessException(ErrorCode.INTERNAL_SERVER_ERROR, "Firestore operation timed out.");
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
            throw new BusinessException(ErrorCode.INTERNAL_SERVER_ERROR, "Firestore operation failed.");
        }
    }
}
