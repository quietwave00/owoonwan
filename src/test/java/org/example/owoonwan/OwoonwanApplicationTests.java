package org.example.owoonwan;

import com.google.cloud.firestore.Firestore;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

@SpringBootTest
@TestPropertySource(properties = {
        "app.firebase.enabled=false"
})
class OwoonwanApplicationTests {

    @MockitoBean
    private Firestore firestore;

    @Test
    void contextLoads() {
    }

}
