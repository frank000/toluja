package br.com.toluja.printagent.diagnostic;

import br.com.toluja.printagent.api.dto.AckResponse;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class RuntimeStateStoreTest {
    @TempDir
    Path tempDir;

    @Test
    void returnsEmptyStateWhenFileDoesNotExist() {
        RuntimeState state = new RuntimeStateStore(tempDir.resolve("state.json")).read();

        assertNull(state.lastJobId());
        assertNull(state.lastErrorMessage());
    }

    @Test
    void persistsAckAndErrorState() {
        RuntimeStateStore store = new RuntimeStateStore(tempDir.resolve("state.json"));

        store.recordAck(new AckResponse("job-1", "SUCCESS", 1));
        store.recordError("PRINT", "Fila nao encontrada");

        RuntimeState state = store.read();
        assertEquals("SUCCESS", state.lastAckStatus());
        assertEquals("PRINT", state.lastErrorType());
        assertEquals("Fila nao encontrada", state.lastErrorMessage());
    }
}
