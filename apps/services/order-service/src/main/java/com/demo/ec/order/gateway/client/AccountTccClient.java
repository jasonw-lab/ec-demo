package com.demo.ec.order.gateway.client;

import com.demo.ec.order.web.dto.CommonResponse;
import com.demo.ec.order.gateway.client.dto.DebitTccRequest;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

/**
 * Feign client for calling account-service TCC endpoints.
 * Note: Seata TCC annotations belong on provider-side service interfaces, not Feign clients.
 */
@FeignClient(name = "account-service", url = "${account.service.url:http://localhost:8083}")
public interface AccountTccClient {

    @PostMapping("/api/account/debit/tcc")
    CommonResponse<String> tryDebit(@RequestBody DebitTccRequest request);
}
