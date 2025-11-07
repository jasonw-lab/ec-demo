package com.example.seata.at.order.client;

import com.example.seata.at.order.api.dto.CommonResponse;
import com.example.seata.at.order.client.dto.DeductTccRequest;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

/**
 * Feign client for calling storage-service TCC endpoints.
 * Note: Seata TCC annotations belong on provider-side service interfaces, not Feign clients.
 */
@FeignClient(name = "storage-service", url = "${storage.service.url:http://localhost:8082}")
public interface StorageTccClient {

    @PostMapping("/api/storage/deduct/tcc")
    CommonResponse<String> tryDeduct(@RequestBody DeductTccRequest request);
}