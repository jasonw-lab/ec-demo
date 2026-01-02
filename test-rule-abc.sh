#!/bin/bash

# Kafka Alert Rules A/B/C シミュレーターテストスクリプト
# テスト対象: alert-streams-service の Rule A/B/C 検知機能
#
# Rule A: PaymentSucceededを受信 → 同一orderIdのOrderConfirmedがT_confirm内に来なければAlertRaised
# Rule B: OrderConfirmedを受信 → 同一orderIdのPaymentSucceededがT_pay内に来なければAlertRaised
# Rule C: 同一orderIdでPaymentSucceededが複数回 → 二重決済疑いAlertRaised（重大度P1）
#
# 使用方法:
#   ./test-rule-abc.sh              # 全テスト実行 (デフォルト)
#   ./test-rule-abc.sh -a           # Rule A のみテスト
#   ./test-rule-abc.sh -b           # Rule B のみテスト
#   ./test-rule-abc.sh -c           # Rule C のみテスト
#   ./test-rule-abc.sh -abc         # 全テスト実行 (明示的)
#   ./test-rule-abc.sh --help       # ヘルプ表示

set -e

# 設定値
PAYMENT_SERVICE_URL="http://localhost:8082"
ORDER_SERVICE_URL="http://localhost:8081"
T_CONFIRM=30  # 30秒 (Rule A 用)
T_PAY=30      # 30秒 (Rule B 用)
PUNCTUATE_INTERVAL=10  # 10秒 (punctuator間隔)

# パラメータ解析
RUN_RULE_A=false
RUN_RULE_B=false
RUN_RULE_C=false
RUN_NORMAL=false

show_help() {
    echo "使用方法:"
    echo "  $0              # 全テスト実行 (デフォルト)"
    echo "  $0 -a           # Rule A のみテスト"
    echo "  $0 -b           # Rule B のみテスト"
    echo "  $0 -c           # Rule C のみテスト"
    echo "  $0 -abc         # 全テスト実行 (明示的)"
    echo "  $0 --help       # ヘルプ表示"
    echo ""
    echo "Rule 説明:"
    echo "  A: 決済成功後注文確認なし (タイムアウトアラート)"
    echo "  B: 注文確認後決済なし (タイムアウトアラート)"
    echo "  C: 二重決済検知 (即時アラート)"
    exit 0
}

# パラメータ解析
if [ $# -eq 0 ]; then
    # デフォルト: 全テスト実行
    RUN_RULE_A=true
    RUN_RULE_B=true
    RUN_RULE_C=true
    RUN_NORMAL=true
else
    while [[ $# -gt 0 ]]; do
        case $1 in
            -a)
                RUN_RULE_A=true
                shift
                ;;
            -b)
                RUN_RULE_B=true
                shift
                ;;
            -c)
                RUN_RULE_C=true
                shift
                ;;
            -abc)
                RUN_RULE_A=true
                RUN_RULE_B=true
                RUN_RULE_C=true
                RUN_NORMAL=true
                shift
                ;;
            --help|-h)
                show_help
                ;;
            *)
                echo "エラー: 不明なパラメータ '$1'"
                echo "ヘルプを表示するには: $0 --help"
                exit 1
                ;;
        esac
    done
fi

echo "=========================================="
echo "Kafka Alert Rules A/B/C テスト開始"
echo "=========================================="

# 実行予定のテストを表示
echo "実行予定のテスト:"
if [ "$RUN_RULE_C" = true ]; then echo "  • Rule C (二重決済検知)"; fi
if [ "$RUN_RULE_A" = true ]; then echo "  • Rule A (決済成功後注文確認なし)"; fi
if [ "$RUN_RULE_B" = true ]; then echo "  • Rule B (注文確認後決済なし)"; fi
if [ "$RUN_NORMAL" = true ]; then echo "  • 正常ケース (アラートなし)"; fi
echo ""

echo "設定値:"
echo "  T_confirm: ${T_CONFIRM}秒 (Rule A)"
echo "  T_pay: ${T_PAY}秒 (Rule B)"
echo "  Punctuate interval: ${PUNCTUATE_INTERVAL}秒"
echo ""

# ユーティリティ関数
send_payment_succeeded() {
    local order_id=$1
    local payment_id=$2
    local provider=${3:-"PayPay"}
    local amount=${4:-1200}
    local currency=${5:-"JPY"}

    echo "📤 PaymentSucceeded送信: orderId=${order_id}, paymentId=${payment_id}"
    curl -s -X POST "${PAYMENT_SERVICE_URL}/api/payments/sim/payment/succeeded" \
         -H 'Content-Type: application/json' \
         -d "{\"orderId\":\"${order_id}\",\"paymentId\":\"${payment_id}\",\"provider\":\"${provider}\",\"amount\":${amount},\"currency\":\"${currency}\"}" \
         > /dev/null
    echo "   ✅ 送信完了"
}

send_order_confirmed() {
    local order_id=$1

    echo "📤 OrderConfirmed送信: orderId=${order_id}"
    curl -s -X POST "${ORDER_SERVICE_URL}/api/orders/sim/order/confirmed" \
         -H 'Content-Type: application/json' \
         -d "{\"orderId\":\"${order_id}\"}" \
         > /dev/null
    echo "   ✅ 送信完了"
}

wait_seconds() {
    local seconds=$1
    local message=${2:-"待機中"}
    echo "⏳ ${message} (${seconds}秒)..."
    sleep $seconds
    echo "   ✅ 待機完了"
}

print_separator() {
    echo ""
    echo "=========================================="
    echo "$1"
    echo "=========================================="
}

# テスト実行
if [ "$RUN_RULE_C" = true ]; then
    # テスト1: Rule C (即時アラート - 二重決済検知)
    print_separator "テスト1: Rule C (二重決済検知)"
    echo "期待結果: 2回目のPaymentSucceededで即座にAlertRaised(rule=C, severity=P1)が発生"
    echo ""

    send_payment_succeeded "O-C-002" "P-C-002"
    wait_seconds 2 "イベント処理待機"

    send_payment_succeeded "O-C-002" "P-C-002"
    echo ""
    echo "🎯 Rule C テスト完了 - alerts.order_payment_inconsistency.v1 を確認してください"
fi

if [ "$RUN_RULE_A" = true ]; then
    # テスト2: Rule A (タイムアウトアラート - 決済成功後注文確認なし)
    print_separator "テスト2: Rule A (決済成功後注文確認なし)"
    echo "期待結果: PaymentSucceeded送信後 ${T_CONFIRM}秒 + ${PUNCTUATE_INTERVAL}秒後にAlertRaised(rule=A, severity=P2)が発生"
    echo ""

    send_payment_succeeded "O-A-001" "P-A-001"
    echo ""
    echo "💡 約${T_CONFIRM}秒 + ${PUNCTUATE_INTERVAL}秒後にアラートが発生するまで待機..."
    wait_seconds $((T_CONFIRM + PUNCTUATE_INTERVAL + 5)) "Rule A アラート待機"
    echo "🎯 Rule A テスト完了 - alerts.order_payment_inconsistency.v1 を確認してください"
fi

if [ "$RUN_RULE_B" = true ]; then
    # テスト3: Rule B (タイムアウトアラート - 注文確認後決済なし)
    print_separator "テスト3: Rule B (注文確認後決済なし)"
    echo "期待結果: OrderConfirmed送信後 ${T_PAY}秒 + ${PUNCTUATE_INTERVAL}秒後にAlertRaised(rule=B, severity=P2)が発生"
    echo ""

    send_order_confirmed "O-B-001"
    echo ""
    echo "💡 約${T_PAY}秒 + ${PUNCTUATE_INTERVAL}秒後にアラートが発生するまで待機..."
    wait_seconds $((T_PAY + PUNCTUATE_INTERVAL + 5)) "Rule B アラート待機"
    echo "🎯 Rule B テスト完了 - alerts.order_payment_inconsistency.v1 を確認してください"
fi

if [ "$RUN_NORMAL" = true ]; then
    # 正常ケース: 決済成功 → 注文確認 (アラートが発生しないことを確認)
    print_separator "テスト4: 正常ケース (アラートなし)"
    echo "期待結果: 決済成功後に注文確認が来るため、アラートは発生しない"
    echo ""

    send_payment_succeeded "O-OK-001" "P-OK-001"
    wait_seconds 2 "イベント処理待機"
    send_order_confirmed "O-OK-001"
    echo ""
    echo "💡 約${T_CONFIRM}秒待機してアラートが発生しないことを確認..."
    wait_seconds $((T_CONFIRM + PUNCTUATE_INTERVAL + 5)) "正常ケース確認"
    echo "🎯 正常ケーステスト完了 - alerts.order_payment_inconsistency.v1 にアラートがないことを確認してください"
fi

# 実行されたテストのサマリー表示
print_separator "テスト完了"
echo "📋 実行されたテスト:"

test_count=0
if [ "$RUN_RULE_C" = true ]; then
    echo "  ✅ Rule C (二重決済検知)"
    ((test_count++))
fi
if [ "$RUN_RULE_A" = true ]; then
    echo "  ✅ Rule A (決済成功後注文確認なし)"
    ((test_count++))
fi
if [ "$RUN_RULE_B" = true ]; then
    echo "  ✅ Rule B (注文確認後決済なし)"
    ((test_count++))
fi
if [ "$RUN_NORMAL" = true ]; then
    echo "  ✅ 正常ケース (アラートなし)"
    ((test_count++))
fi

echo ""
echo "📋 確認事項:"
echo "  1. kafka-console-consumer で alerts.order_payment_inconsistency.v1 を監視"

count=2
if [ "$RUN_RULE_C" = true ]; then
    echo "  $count. Rule C: 即時アラートが発生していること"
    ((count++))
fi
if [ "$RUN_RULE_A" = true ]; then
    echo "  $count. Rule A: タイムアウト後にアラートが発生していること"
    ((count++))
fi
if [ "$RUN_RULE_B" = true ]; then
    echo "  $count. Rule B: タイムアウト後にアラートが発生していること"
    ((count++))
fi
if [ "$RUN_NORMAL" = true ]; then
    echo "  $count. 正常ケース: アラートが発生していないこと"
fi

echo ""
if [ $test_count -gt 0 ]; then
    echo "📊 期待されるAlertRaisedイベント:"
    if [ "$RUN_RULE_C" = true ]; then
        echo "  - Rule C: {\"eventType\":\"AlertRaised\",\"rule\":\"C\",\"severity\":\"P1\",\"orderId\":\"O-C-002\",...}"
    fi
    if [ "$RUN_RULE_A" = true ]; then
        echo "  - Rule A: {\"eventType\":\"AlertRaised\",\"rule\":\"A\",\"severity\":\"P2\",\"orderId\":\"O-A-001\",...}"
    fi
    if [ "$RUN_RULE_B" = true ]; then
        echo "  - Rule B: {\"eventType\":\"AlertRaised\",\"rule\":\"B\",\"severity\":\"P2\",\"orderId\":\"O-B-001\",...}"
    fi
    echo ""
fi

echo "✅ $test_count 個のテストが完了しました"
