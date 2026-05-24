package br.com.toluja.printagent.job;

import br.com.toluja.printagent.api.PrintAgentApiException;
import br.com.toluja.printagent.api.PrintAgentClient;
import br.com.toluja.printagent.api.dto.AckRequest;
import br.com.toluja.printagent.api.dto.AckResponse;
import br.com.toluja.printagent.api.dto.DeliveryAck;
import br.com.toluja.printagent.api.dto.NextJobResponse;
import br.com.toluja.printagent.config.AgentConfig;
import br.com.toluja.printagent.diagnostic.RuntimeStateStore;
import br.com.toluja.printagent.print.PrintDispatcher;

import java.util.Optional;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.logging.Logger;

public final class JobPoller {
    private static final Logger LOGGER = Logger.getLogger(JobPoller.class.getName());

    private final AgentConfig config;
    private final PrintAgentClient client;
    private final JobProcessor processor;
    private final RuntimeStateStore stateStore;
    private final AtomicBoolean processing = new AtomicBoolean(false);
    private volatile boolean stopRequested;

    public JobPoller(AgentConfig config, PrintAgentClient client, JobProcessor processor) {
        this(config, client, processor, RuntimeStateStore.createDefault());
    }

    public JobPoller(
            AgentConfig config,
            PrintAgentClient client,
            JobProcessor processor,
            RuntimeStateStore stateStore
    ) {
        this.config = config;
        this.client = client;
        this.processor = processor;
        this.stateStore = stateStore;
    }

    public static JobPoller create(AgentConfig config) {
        return new JobPoller(
                config,
                new PrintAgentClient(config),
                new JobProcessor(PrintDispatcher.create(config))
        );
    }

    public void runUntilStopped() {
        LOGGER.info("Agente iniciado deviceId=" + config.deviceId()
                + " api=" + config.apiBaseUrl()
                + " pollIntervalMs=" + config.pollIntervalMs());

        Runtime.getRuntime().addShutdownHook(new Thread(this::requestStop, "toluja-agent-shutdown"));

        while (!stopRequested) {
            runOnce();
            sleepPollInterval();
        }
        LOGGER.info("Agente parado");
    }

    public void runOnce() {
        if (!processing.compareAndSet(false, true)) {
            LOGGER.warning("Processamento anterior ainda em andamento; pulando ciclo");
            return;
        }

        try {
            Optional<NextJobResponse> maybeJob = client.fetchNextJob();
            if (maybeJob.isEmpty()) {
                LOGGER.fine("Nenhum job disponivel");
                return;
            }

            NextJobResponse job = maybeJob.get();
            stateStore.recordJobReceived(job);
            AckRequest ack = processor.process(job);
            recordDeliveryErrors(ack);
            AckResponse response = client.sendAck(job.jobId(), ack);
            stateStore.recordAck(response);
            LOGGER.info("ACK enviado job=" + response.jobId()
                    + " status=" + response.status()
                    + " deliveries=" + response.receivedDeliveries());
        } catch (PrintAgentApiException ex) {
            stateStore.recordError("API", ex.getMessage());
            LOGGER.warning("Erro de API no ciclo do agente: " + ex.getMessage());
        } catch (RuntimeException ex) {
            stateStore.recordError("RUNTIME", ex.getMessage());
            LOGGER.warning("Erro inesperado no ciclo do agente: " + ex.getMessage());
        } finally {
            processing.set(false);
        }
    }

    public void requestStop() {
        stopRequested = true;
    }

    private void sleepPollInterval() {
        try {
            Thread.sleep(config.pollIntervalMs());
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            requestStop();
        }
    }

    private void recordDeliveryErrors(AckRequest ack) {
        for (DeliveryAck delivery : ack.deliveries()) {
            if ("ERROR".equalsIgnoreCase(delivery.status())) {
                stateStore.recordError("PRINT", delivery.errorMessage());
                return;
            }
        }
    }
}
