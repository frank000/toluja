package br.com.toluja.printagent.print;

import br.com.toluja.printagent.api.dto.JobDelivery;

public interface PrintExecutor {
    PrintResult print(JobDelivery delivery, byte[] payload);
}
