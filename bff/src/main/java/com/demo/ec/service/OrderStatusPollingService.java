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
     * WebSocket接続中の注文について、ステータスをチェックして変更があれば通知します。
     * 
     * <p>重要な設計判断：
     * - スケジューラーで定期的に実行されるため、例外処理が重要
     * - 一つの注文でエラーが発生しても、他の注文の処理は継続
     * - 完了した注文（PAID/FAILED）はメモリから削除してリソースを節約
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
        int errorCount = 0;
        int finalizedCount = 0;

        for (String orderId : activeOrderIds) {
            try {
                boolean notified = checkAndNotifyOrderStatus(orderId);
                checkedCount++;
                
                if (notified) {
                    notifiedCount++;
                    
                    // メモリ管理: 完了した注文（PAID/FAILED）は監視対象から除外
                    // ■ 無限にメモリが増加するのを防ぐ
                    String currentStatus = lastStatusMap.get(orderId);
                    if (currentStatus != null && NOTIFY_STATUSES.contains(currentStatus)) {
                        removeOrder(orderId);
                        finalizedCount++;
                        log.debug("[OrderStatusPolling] Removed finalized order from monitoring orderId={}, status={}", 
                                orderId, currentStatus);
                    }
                }
            } catch (Exception e) {
                errorCount++;
                // ■ 一つの注文でエラーが発生しても他の注文の処理は続行
                // スケジューラーが停止すると全体の監視が止まってしまう
                log.warn("[OrderStatusPolling] Error checking order status orderId={}, error={}", 
                        orderId, e.getMessage(), e);
            }
        }

        // 統計ログ（デバッグや監視に有用）
        if (notifiedCount > 0 || finalizedCount > 0 || errorCount > 0) {
            log.info("[OrderStatusPolling] Status check completed: checked={}, notified={}, finalized={}, errors={}", 
                    checkedCount, notifiedCount, finalizedCount, errorCount);
        }
    }

    /**
     * 単一の注文についてステータスをチェックし、変更があれば通知します。
     * 
     * <p>設計判断：
     * - null安全性を確保（currentStatusがnullの場合の処理）
     * - 初回チェックと変更検知を明確に分離
     * - 重要なステータス（PAID/FAILED）は初回でも通知
     * 
     * @param orderId 注文ID（非null保証済み）
     * @return 通知が送信された場合true
     */
    private boolean checkAndNotifyOrderStatus(String orderId) {
        // order-serviceから最新の注文情報を取得
        Optional<OrderSummary> summaryOpt = orderServiceClient.getOrder(orderId);
        if (summaryOpt.isEmpty()) {
            log.debug("[OrderStatusPolling] Order not found orderId={}", orderId);
            return false;
        }

        OrderSummary summary = summaryOpt.get();
        String currentStatus = summary.getStatus();
        String lastStatus = lastStatusMap.get(orderId);

        // null安全性チェック
        if (currentStatus == null) {
            log.debug("[OrderStatusPolling] Order status is null orderId={}", orderId);
            // null状態が続く場合は、前回のステータスを保持（異常状態の記録）
            return false;
        }

        // 通知判定ロジック
        boolean shouldNotify = shouldNotifyStatusChange(lastStatus, currentStatus, orderId);
        
        if (shouldNotify) {
            log.info("[OrderStatusPolling] Broadcasting status update orderId={}, status={}", 
                    orderId, currentStatus);
            broadcaster.broadcast(summary);
        }
        
        // ステータスを記録（次回の変更検知に使用）
        lastStatusMap.put(orderId, currentStatus);
        
        return shouldNotify;
    }

    /**
     * ステータス変更時に通知すべきかどうかを判定します。
     * 
     * <p>通知条件：
     * 1. 初回チェックで重要なステータス（PAID/FAILED）に到達
     * 2. ステータスが変更された（任意のステータス）
     * 
     * @param lastStatus 前回のステータス（null可、初回チェック時）
     * @param currentStatus 現在のステータス（非null保証済み）
     * @param orderId 注文ID（ログ用）
     * @return 通知すべき場合true
     */
    private boolean shouldNotifyStatusChange(String lastStatus, String currentStatus, String orderId) {
        if (lastStatus == null) {
            // 初回チェック: 重要なステータス（PAID/FAILED）の場合のみ通知
            // ■ 接続時に既に完了している注文も通知する必要がある
            if (NOTIFY_STATUSES.contains(currentStatus)) {
                log.info("[OrderStatusPolling] Initial check found important status orderId={}, status={}", 
                        orderId, currentStatus);
                return true;
            }
            return false;
        }
        
        // ステータス変更検知: 任意の変更を通知
        // ■ ユーザーに最新の状態を常に反映させる
        if (!currentStatus.equals(lastStatus)) {
            log.info("[OrderStatusPolling] Status changed detected orderId={}, oldStatus={}, newStatus={}", 
                    orderId, lastStatus, currentStatus);
            return true;
        }
        
        return false;
    }

    /**
     * 注文の監視を停止し、メモリから削除します。
     * 
     * <p>使用例：
     * - 注文が完了（PAID/FAILED）した場合
     * - WebSocket接続が切断された場合
     * 
     * <p>設計判断：
     * - メモリリークを防ぐため、不要になった監視状態は削除
     * - ConcurrentHashMapを使用しているため、スレッドセーフ
     * 
     * @param orderId 注文ID（非null保証済み）
     */
    public void removeOrder(String orderId) {
        String removed = lastStatusMap.remove(orderId);
        if (removed != null) {
            log.debug("[OrderStatusPolling] Removed order from status tracking orderId={}, lastStatus={}", 
                    orderId, removed);
        }
    }
}

