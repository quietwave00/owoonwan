package org.example.owoonwan.common.firebase;

import com.google.api.core.ApiFuture;
import org.example.owoonwan.common.error.BusinessException;
import org.example.owoonwan.common.error.ErrorCode;

import java.util.concurrent.ExecutionException;

public final class FirestoreAwait {

    private FirestoreAwait() {
    }

    public static <T> T get(ApiFuture<T> future) {
        try {
            return future.get();
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new BusinessException(ErrorCode.INTERNAL_SERVER_ERROR, "Firestore 처리 중 스레드가 중단되었습니다.");
        } catch (ExecutionException exception) {
            if (exception.getCause() instanceof BusinessException businessException) {
                throw businessException;
            }
            throw new BusinessException(ErrorCode.INTERNAL_SERVER_ERROR, "Firestore 처리 중 오류가 발생했습니다.");
        }
    }
}
