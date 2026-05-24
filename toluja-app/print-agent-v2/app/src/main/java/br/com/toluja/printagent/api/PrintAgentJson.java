package br.com.toluja.printagent.api;

import br.com.toluja.printagent.api.dto.AckRequest;
import br.com.toluja.printagent.api.dto.AckResponse;
import br.com.toluja.printagent.api.dto.DeliveryAck;
import br.com.toluja.printagent.api.dto.JobDelivery;
import br.com.toluja.printagent.api.dto.NextJobResponse;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonNull;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;

final class PrintAgentJson {
    private static final Gson GSON = new GsonBuilder().serializeNulls().create();

    private PrintAgentJson() {
    }

    static NextJobResponse parseNextJob(String body) throws PrintAgentApiException {
        JsonObject root = parseObject(body, "NextJobResponse");
        return new NextJobResponse(
                requiredString(root, "jobId"),
                requiredString(root, "tenantId"),
                requiredString(root, "storeId"),
                requiredString(root, "deviceId"),
                requiredString(root, "orderId"),
                requiredString(root, "payloadType"),
                requiredString(root, "payloadBase64"),
                requiredDateTime(root, "createdAt"),
                readDeliveries(root)
        );
    }

    static AckResponse parseAckResponse(String body) throws PrintAgentApiException {
        JsonObject root = parseObject(body, "AckResponse");
        return new AckResponse(
                requiredString(root, "jobId"),
                requiredString(root, "status"),
                requiredInt(root, "receivedDeliveries")
        );
    }

    static String writeAckRequest(AckRequest request) {
        JsonObject root = new JsonObject();
        JsonArray deliveries = new JsonArray();
        for (DeliveryAck delivery : request.deliveries()) {
            JsonObject item = new JsonObject();
            item.addProperty("deliveryId", delivery.deliveryId());
            item.addProperty("status", delivery.status());
            if (delivery.errorMessage() == null) {
                item.add("errorMessage", JsonNull.INSTANCE);
            } else {
                item.addProperty("errorMessage", delivery.errorMessage());
            }
            item.addProperty("printedAt", delivery.printedAt().toString());
            deliveries.add(item);
        }
        root.add("deliveries", deliveries);
        return GSON.toJson(root);
    }

    private static List<JobDelivery> readDeliveries(JsonObject root)
            throws PrintAgentApiException {
        JsonElement raw = root.get("deliveries");
        if (raw == null || !raw.isJsonArray() || raw.getAsJsonArray().isEmpty()) {
            throw new PrintAgentApiException("Campo 'deliveries' ausente ou vazio", false);
        }

        JsonArray list = raw.getAsJsonArray();
        List<JobDelivery> deliveries = new ArrayList<>();
        for (int i = 0; i < list.size(); i++) {
            JsonElement item = list.get(i);
            if (!item.isJsonObject()) {
                throw new PrintAgentApiException("Delivery #" + (i + 1) + " deve ser objeto", false);
            }
            JsonObject delivery = item.getAsJsonObject();
            deliveries.add(new JobDelivery(
                    requiredString(delivery, "deliveryId"),
                    requiredString(delivery, "printerId"),
                    requiredString(delivery, "printerName"),
                    requiredString(delivery, "channel"),
                    requiredString(delivery, "destination"),
                    requiredInt(delivery, "copies")
            ));
        }
        return deliveries;
    }

    private static JsonObject parseObject(String body, String context)
            throws PrintAgentApiException {
        try {
            JsonElement parsed = GSON.fromJson(body, JsonElement.class);
            if (parsed == null || !parsed.isJsonObject()) {
                throw new PrintAgentApiException(context + " deve ser objeto JSON", false);
            }
            return parsed.getAsJsonObject();
        } catch (JsonParseException ex) {
            throw new PrintAgentApiException("JSON invalido em " + context + ": " + ex.getMessage(), ex, false);
        }
    }

    private static String requiredString(JsonObject object, String key)
            throws PrintAgentApiException {
        JsonElement value = object.get(key);
        if (value == null || !value.isJsonPrimitive() || !value.getAsJsonPrimitive().isString()) {
            throw new PrintAgentApiException("Campo obrigatorio ausente ou vazio: " + key, false);
        }
        String text = value.getAsString();
        if (text.trim().isEmpty()) {
            throw new PrintAgentApiException("Campo obrigatorio ausente ou vazio: " + key, false);
        }
        return text.trim();
    }

    private static int requiredInt(JsonObject object, String key)
            throws PrintAgentApiException {
        JsonElement value = object.get(key);
        if (value != null && value.isJsonPrimitive() && value.getAsJsonPrimitive().isNumber()) {
            try {
                return value.getAsInt();
            } catch (NumberFormatException ex) {
                throw new PrintAgentApiException("Campo numerico ausente ou invalido: " + key, false);
            }
        }
        throw new PrintAgentApiException("Campo numerico ausente ou invalido: " + key, false);
    }

    private static OffsetDateTime requiredDateTime(JsonObject object, String key)
            throws PrintAgentApiException {
        try {
            return OffsetDateTime.parse(requiredString(object, key));
        } catch (RuntimeException ex) {
            throw new PrintAgentApiException("Campo de data invalido: " + key, ex, false);
        }
    }
}
