package com.demo.ec.service;

import com.demo.ec.client.OrderServiceClient;
import com.demo.ec.client.dto.OrderSummary;
import com.demo.ec.ws.OrderChannelSessionManager;
import com.demo.ec.ws.OrderStatusBroadcaster;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.Collection;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 注文ステータス変更のポーリングサービス
 * 
 * <p>WebSocket接続中の注文について、order-serviceから最新のステータスを取得し、
 * 変更があった場合にWebSocketで通知します。
 * 
 * <p>Webhookが優先されますが、Webhookが来ない場合のフォールバックとして動作します。
 */
@Service
public class OrderStatusPollingService {

    private static final Logger log = LoggerFactory.getLogger(OrderStatusPollingService.class);
    
    /** 監視対象のステータス（これらのステータスに変更があったら通知） */
    private static final Set<String> NOTIFY_STATUSES = Set.of("PAID", "FAILED");

    private final OrderServiceClient orderServiceClient;
    private final OrderChannelSessionManager sessionManager;
    private final OrderStatusBroadcaster broadcaster;
    
    /** 前回のステータスを保持（変更検知用） */
    private final ConcurrentHashMap<String, String> lastStatusMap = new ConcurrentHashMap<>();

    public OrderStatusPollingService(OrderServiceClient orderServiceClient,
                                    OrderChannelSessionManager sessionManager,
                                    OrderStatusBroadcaster broadcaster) {
        this.orderServiceClient = orderServiceClient;
        this.sessionManager = sessionManager;
        this.broadcaster = broadcaster;
    }

    /**
     * WebSocket接続中の注文について、ステータスをチェックして変更があれば通知
     * 
     * <p>デフォルトでは5秒ごとに実行されます。
     * 設定: bff.order.status-check-interval-ms (デフォルト: 5000ms)
     */
    @Scheduled(fixedDelayString = "${bff.order.status-check-interval-ms:5000}")
    public void checkOrderStatusChanges() {
        Collection<String> activeOrderIds = sessionManager.getAllOrderIds();
        if (activeOrderIds.isEmpty()) {
            return;
        }

        int checkedCount = 0;
        int notifiedCount = 0;

        for (String orderId : activeOrderIds) {
            try {
                Optional<OrderSummary> summaryOpt = orderServiceClient.getOrder(orderId);
                if (summaryOpt.isEmpty()) {
                    continue;
                }

                OrderSummary summary = summaryOpt.get();
                String currentStatus = summary.getStatus();
                String lastStatus = lastStatusMap.get(orderId);

                checkedCount++;

                // デバッグログ（ステータスがnullの場合を確認）
                if (currentStatus == null) {
                    log.debug("[OrderStatusPolling] Order status is null orderId={}, summary={}", 
                            orderId, summary);
                }

                // ステータスが変更された場合、または重要なステータス（PAID/FAILED）に到達した場合
                if (currentStatus != null) {
                    boolean shouldNotify = false;
                    
                    if (lastStatus == null) {
                        // 初回チェックで重要なステータスの場合
                        if (NOTIFY_STATUSES.contains(currentStatus)) {
                            shouldNotify = true;
                            log.info("[OrderStatusPolling] Initial check found important status orderId={}, status={}", 
                                    orderId, currentStatus);
                        }
                    } else if (!currentStatus.equals(lastStatus)) {
                        // ステータスが変更された場合（常に通知）
                        shouldNotify = true;
                        log.info("[OrderStatusPolling] Status changed detected orderId={}, oldStatus={}, newStatus={}", 
                                orderId, lastStatus, currentStatus);
                    }
                    
                    if (shouldNotify) {
                        log.info("[OrderStatusPolling] Broadcasting status update orderId={}, status={}", 
                                orderId, currentStatus);
                        broadcaster.broadcast(summary);
                        notifiedCount++;
                    }
                    
                    lastStatusMap.put(orderId, currentStatus);
                } else if (lastStatus != null) {
                    // ステータスがnullになった場合（異常状態）、前回のステータスを保持
                    log.warn("[OrderStatusPolling] Order status became null orderId={}, keeping lastStatus={}", 
                            orderId, lastStatus);
                }
            } catch (Exception e) {
                log.warn("[OrderStatusPolling] Error checking order status orderId={}, error={}", 
                        orderId, e.getMessage());
            }
        }

        if (notifiedCount > 0) {
            log.info("[OrderStatusPolling] Status check completed: checked={}, notified={}", 
                    checkedCount, notifiedCount);
        }
    }

    /**
     * 注文が完了または失敗した場合、メモリから削除
     */
    public void removeOrder(String orderId) {
        lastStatusMap.remove(orderId);
    }
}

