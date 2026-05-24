package br.com.toluja.printagent.diagnostic;

import br.com.toluja.printagent.api.dto.AckResponse;
import br.com.toluja.printagent.api.dto.NextJobResponse;
import br.com.toluja.printagent.logging.AgentLogging;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonSyntaxException;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.OffsetDateTime;
import java.util.logging.Logger;

public final class RuntimeStateStore {
    private static final Logger LOGGER = Logger.getLogger(RuntimeStateStore.class.getName());
    private static final Gson GSON = new GsonBuilder().serializeNulls().setPrettyPrinting().create();

    private final Path statePath;

    public RuntimeStateStore(Path statePath) {
        this.statePath = statePath;
    }

    public static RuntimeStateStore createDefault() {
        return new RuntimeStateStore(defaultStatePath());
    }

    public static Path defaultStatePath() {
        return AgentLogging.dataDir().resolve("state.json");
    }

    public Path statePath() {
        return statePath;
    }

    public RuntimeState read() {
        if (!Files.isRegularFile(statePath)) {
            return RuntimeState.empty();
        }

        try {
            RuntimeState state = GSON.fromJson(Files.readString(statePath, StandardCharsets.UTF_8), RuntimeState.class);
            return state == null ? RuntimeState.empty() : state;
        } catch (IOException | JsonSyntaxException ex) {
            LOGGER.warning("Nao foi possivel ler estado do agente: " + ex.getMessage());
            return RuntimeState.empty();
        }
    }

    public void recordJobReceived(NextJobResponse job) {
        RuntimeState previous = read();
        OffsetDateTime now = OffsetDateTime.now();
        write(new RuntimeState(
                now.toString(),
                job.jobId(),
                job.orderId(),
                now.toString(),
                previous.lastAckStatus(),
                previous.lastAckAt(),
                previous.lastErrorType(),
                previous.lastErrorMessage(),
                previous.lastErrorAt()
        ));
    }

    public void recordAck(AckResponse ack) {
        RuntimeState previous = read();
        OffsetDateTime now = OffsetDateTime.now();
        write(new RuntimeState(
                now.toString(),
                previous.lastJobId(),
                previous.lastOrderId(),
                previous.lastJobAt(),
                ack.status(),
                now.toString(),
                previous.lastErrorType(),
                previous.lastErrorMessage(),
                previous.lastErrorAt()
        ));
    }

    public void recordError(String type, String message) {
        RuntimeState previous = read();
        OffsetDateTime now = OffsetDateTime.now();
        write(new RuntimeState(
                now.toString(),
                previous.lastJobId(),
                previous.lastOrderId(),
                previous.lastJobAt(),
                previous.lastAckStatus(),
                previous.lastAckAt(),
                type,
                message,
                now.toString()
        ));
    }

    public void write(RuntimeState state) {
        try {
            if (statePath.getParent() != null) {
                Files.createDirectories(statePath.getParent());
            }
            Files.writeString(statePath, GSON.toJson(state), StandardCharsets.UTF_8);
        } catch (IOException ex) {
            LOGGER.warning("Nao foi possivel gravar estado do agente: " + ex.getMessage());
        }
    }
}
