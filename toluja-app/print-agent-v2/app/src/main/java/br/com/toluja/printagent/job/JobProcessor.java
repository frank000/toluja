package br.com.toluja.printagent.job;

import br.com.toluja.printagent.api.dto.AckRequest;
import br.com.toluja.printagent.api.dto.DeliveryAck;
import br.com.toluja.printagent.api.dto.JobDelivery;
import br.com.toluja.printagent.api.dto.NextJobResponse;
import br.com.toluja.printagent.print.PrintExecutor;
import br.com.toluja.printagent.print.PrintResult;

import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.Locale;
import java.util.logging.Logger;

public final class JobProcessor {
    private static final Logger LOGGER = Logger.getLogger(JobProcessor.class.getName());

    private final PrintExecutor printExecutor;

    public JobProcessor(PrintExecutor printExecutor) {
        this.printExecutor = printExecutor;
    }

    public AckRequest process(NextJobResponse job) {
        LOGGER.info("Processando job=" + job.jobId()
                + " orderId=" + job.orderId()
                + " deliveries=" + job.deliveries().size());

        byte[] payload;
        try {
            payload = decodePayload(job);
        } catch (RuntimeException ex) {
            LOGGER.warning("Payload invalido job=" + job.jobId() + ": " + ex.getMessage());
            return errorAll(job.deliveries(), "Payload invalido: " + ex.getMessage());
        }

        List<DeliveryAck> acks = new ArrayList<>();
        for (JobDelivery delivery : job.deliveries()) {
            acks.add(processDelivery(delivery, payload));
        }
        return new AckRequest(acks);
    }

    private DeliveryAck processDelivery(JobDelivery delivery, byte[] payload) {
        try {
            PrintResult result = printExecutor.print(delivery, payload);
            if (result.success()) {
                return DeliveryAck.success(delivery.deliveryId());
            }
            return DeliveryAck.error(delivery.deliveryId(), result.errorMessage());
        } catch (RuntimeException ex) {
            return DeliveryAck.error(delivery.deliveryId(), ex.getMessage());
        }
    }

    private AckRequest errorAll(List<JobDelivery> deliveries, String message) {
        List<DeliveryAck> acks = deliveries.stream()
                .map(delivery -> DeliveryAck.error(delivery.deliveryId(), message))
                .toList();
        return new AckRequest(acks);
    }

    private byte[] decodePayload(NextJobResponse job) {
        String payloadType = job.payloadType().trim().toUpperCase(Locale.ROOT);
        if (!payloadType.equals("TEXT")) {
            throw new IllegalArgumentException("payloadType nao suportado no MVP: " + job.payloadType());
        }
        return Base64.getDecoder().decode(job.payloadBase64());
    }
}
