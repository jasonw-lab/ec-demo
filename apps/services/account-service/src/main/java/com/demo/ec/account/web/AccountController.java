package com.demo.ec.account.web;

import com.demo.ec.account.web.dto.CommonResponse;
import com.demo.ec.account.web.dto.DebitRequest;
import com.demo.ec.account.application.AccountATService;
import com.demo.ec.account.application.AccountATServiceImpl;
import com.demo.ec.account.application.AccountTccService;
import com.demo.ec.account.application.AccountSagaService;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/account")
public class AccountController {
    private static final Logger log = LoggerFactory.getLogger(AccountController.class);

    private final AccountATService accountATService;
    private final AccountTccService accountTccService;
    private final AccountSagaService accountSagaService;

    public AccountController(AccountATService accountATService, AccountTccService accountTccService, AccountSagaService accountSagaService) {
        this.accountATService = accountATService;
        this.accountTccService = accountTccService;
        this.accountSagaService = accountSagaService;
    }

    @PostMapping("/debit")
    public CommonResponse<String> debit(@Valid @RequestBody DebitRequest req) {
        // Log received request body at INFO level
        log.info("Received DebitRequest: userId={}, amount={}", req.getUserId(), req.getAmount());
        accountATService.debit(req.getUserId(), req.getAmount());
        return CommonResponse.ok("debited");
    }

    @PostMapping("/debit/tcc")
    public CommonResponse<String> debitTcc(@Valid @RequestBody DebitRequest req) {
        Long orderId = req.getOrderId();
        String xid = null;
        try { xid = io.seata.core.context.RootContext.getXID(); } catch (Throwable ignore) {}
        log.info("Received DebitRequest TCC: orderId={}, userId={}, amount={}, xid={}", orderId, req.getUserId(), req.getAmount(), xid);
        accountTccService.tryDebit(req.getUserId(), req.getAmount(), orderId);
        return CommonResponse.ok("tcc-try-debited");
    }

    @PostMapping("/debit/saga")
    public CommonResponse<String> debitSaga(@Valid @RequestBody DebitRequest req) {
        String orderNo = req.getOrderNo();
        if (orderNo == null || orderNo.isBlank()) {
            orderNo = java.util.UUID.randomUUID().toString();
        }
        String xid = null;
        try { xid = io.seata.core.context.RootContext.getXID(); } catch (Throwable ignore) {}
        log.info("Received DebitRequest SAGA: orderNo={}, userId={}, amount={}, xid={}", orderNo, req.getUserId(), req.getAmount(), xid);
        accountSagaService.debit(req.getUserId(), req.getAmount(), orderNo);
        return CommonResponse.ok("saga-debited");
    }

    @PostMapping("/compensate/saga")
    public CommonResponse<String> compensateSaga(@Valid @RequestBody DebitRequest req) {
        String orderNo = req.getOrderNo();
        if (orderNo == null || orderNo.isBlank()) {
            orderNo = java.util.UUID.randomUUID().toString();
        }
        String xid = null;
        try { xid = io.seata.core.context.RootContext.getXID(); } catch (Throwable ignore) {}
        log.info("Received CompensateRequest SAGA: orderNo={}, userId={}, amount={}, xid={}", orderNo, req.getUserId(), req.getAmount(), xid);
        accountSagaService.compensate(req.getUserId(), req.getAmount(), orderNo);
        return CommonResponse.ok("saga-compensated");
    }

    @ExceptionHandler(AccountATServiceImpl.InsufficientBalanceException.class)
    public ResponseEntity<CommonResponse<Void>> handleBalance(AccountATServiceImpl.InsufficientBalanceException ex) {
        return ResponseEntity.badRequest().body(CommonResponse.fail(ex.getMessage()));
    }
}
