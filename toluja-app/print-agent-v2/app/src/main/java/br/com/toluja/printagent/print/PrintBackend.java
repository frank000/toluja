package br.com.toluja.printagent.print;

import br.com.toluja.printagent.api.dto.JobDelivery;

public interface PrintBackend {
    String channel();

    void print(JobDelivery delivery, byte[] payload) throws PrintBackendException;
}
