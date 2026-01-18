#!/bin/bash

# ポート使用プロセスKillスクリプト
# 指定されたポートを使用しているプロセスを検出し、終了させます
#
# Usage:
#   ./test-kill-port.sh <port>              # 単一ポートのプロセスをkill
#   ./test-kill-port.sh <port1> <port2>...  # 複数ポートのプロセスをkill
#   ./test-kill-port.sh --all               # アプリケーションの既定ポート全てをkill
#   ./test-kill-port.sh --help              # ヘルプ表示

set -e

# アプリケーションの既定ポート
DEFAULT_PORTS=(8080 8081 8082 8083 8084 8085 8086 3000 5173)

show_help() {
    echo "使用方法:"
    echo "  $0 <port>                    # 単一ポートのプロセスをkill"
    echo "  $0 <port1> <port2> ...       # 複数ポートのプロセスをkill"
    echo "  $0 <port1>-<port2>           # ポート範囲のプロセスをkill"
    echo "  $0 <port1>,<port2>,...       # カンマ区切りのポートをkill"
    echo "  $0 --all                     # アプリケーションの既定ポート全てをkill"
    echo "  $0 --help                    # ヘルプ表示"
    echo ""
    echo "例:"
    echo "  $0 8080                      # ポート8080を使用しているプロセスをkill"
    echo "  $0 8080 8081 8082            # ポート8080, 8081, 8082を使用しているプロセスをkill"
    echo "  $0 8080-8086                 # ポート8080から8086までのプロセスをkill"
    echo "  $0 8080,8082,8086            # ポート8080, 8082, 8086を使用しているプロセスをkill"
    echo "  $0 --all                     # 既定ポート(${DEFAULT_PORTS[@]})をkill"
    echo ""
    echo "既定ポート:"
    echo "  ${DEFAULT_PORTS[@]}"
    exit 0
}

# ユーティリティ関数
kill_port() {
    local port=$1

    echo "🔍 ポート ${port} を使用しているプロセスを検索中..."

    # macOS用のlsof コマンドでポートを使用しているプロセスを検索
    local pids=$(lsof -ti tcp:${port} 2>/dev/null || true)

    if [ -z "$pids" ]; then
        echo "   ℹ️  ポート ${port} を使用しているプロセスはありません"
        return 0
    fi

    # プロセス情報を表示
    echo "   ⚠️  以下のプロセスがポート ${port} を使用しています:"
    for pid in $pids; do
        local process_info=$(ps -p $pid -o pid=,comm=,args= 2>/dev/null || echo "$pid (プロセス情報取得失敗)")
        echo "      PID: $process_info"
    done

    # プロセスをkill
    echo "   🔨 プロセスを終了しています..."
    for pid in $pids; do
        if kill -9 $pid 2>/dev/null; then
            echo "      ✅ PID $pid を終了しました"
        else
            echo "      ❌ PID $pid の終了に失敗しました (権限がない可能性があります)"
        fi
    done

    # 確認
    sleep 1
    local remaining=$(lsof -ti tcp:${port} 2>/dev/null || true)
    if [ -z "$remaining" ]; then
        echo "   ✅ ポート ${port} は解放されました"
        return 0
    else
        echo "   ⚠️  警告: ポート ${port} はまだ使用されています"
        return 1
    fi
}

# ユーティリティ関数: ポート番号の検証
validate_port() {
    local port=$1
    if [[ $port =~ ^[0-9]+$ ]] && [ $port -ge 1 ] && [ $port -le 65535 ]; then
        return 0
    else
        return 1
    fi
}

# ユーティリティ関数: ポート範囲を展開
expand_port_range() {
    local range=$1
    local start_port=$(echo $range | cut -d'-' -f1)
    local end_port=$(echo $range | cut -d'-' -f2)
    
    if ! validate_port "$start_port" || ! validate_port "$end_port"; then
        echo "エラー: 無効なポート範囲 '$range'" >&2
        return 1
    fi
    
    if [ $start_port -gt $end_port ]; then
        echo "エラー: 開始ポート($start_port)が終了ポート($end_port)より大きいです" >&2
        return 1
    fi
    
    for ((port=$start_port; port<=$end_port; port++)); do
        echo $port
    done
}

# ユーティリティ関数: カンマ区切りのポートを展開
expand_comma_ports() {
    local ports_str=$1
    IFS=',' read -ra port_array <<< "$ports_str"
    
    for port in "${port_array[@]}"; do
        port=$(echo $port | xargs)  # 前後の空白を削除
        if validate_port "$port"; then
            echo $port
        else
            echo "エラー: 無効なポート番号 '$port'" >&2
            return 1
        fi
    done
}

# パラメータ解析
if [ $# -eq 0 ]; then
    echo "エラー: ポート番号を指定してください"
    echo ""
    show_help
fi

PORTS=()

while [[ $# -gt 0 ]]; do
    case $1 in
        --help|-h)
            show_help
            ;;
        --all)
            PORTS=("${DEFAULT_PORTS[@]}")
            shift
            ;;
        *-*)
            # ポート範囲の処理 (例: 8080-8086)
            expanded=$(expand_port_range "$1")
            if [ $? -ne 0 ]; then
                echo "$expanded"
                exit 1
            fi
            while IFS= read -r port; do
                PORTS+=("$port")
            done <<< "$expanded"
            shift
            ;;
        *,*)
            # カンマ区切りの処理 (例: 8080,8082,8086)
            expanded=$(expand_comma_ports "$1")
            if [ $? -ne 0 ]; then
                echo "$expanded"
                exit 1
            fi
            while IFS= read -r port; do
                PORTS+=("$port")
            done <<< "$expanded"
            shift
            ;;
        *)
            # 単一ポート番号の検証
            if validate_port "$1"; then
                PORTS+=("$1")
            else
                echo "エラー: 無効なポート番号 '$1' (1-65535の範囲で指定してください)"
                exit 1
            fi
            shift
            ;;
    esac
done

echo "=========================================="
echo "ポート使用プロセス Kill"
echo "=========================================="
echo "対象ポート: ${PORTS[@]}"
echo ""

# 各ポートに対して処理を実行
success_count=0
fail_count=0

for port in "${PORTS[@]}"; do
    if kill_port "$port"; then
        ((success_count++))
    else
        ((fail_count++))
    fi
    echo ""
done

# サマリー表示
echo "=========================================="
echo "処理完了"
echo "=========================================="
echo "✅ 成功: ${success_count} ポート"
if [ $fail_count -gt 0 ]; then
    echo "❌ 失敗: ${fail_count} ポート"
fi
echo ""

if [ $fail_count -gt 0 ]; then
    echo "⚠️  一部のポートの解放に失敗しました"
    echo "   - sudo権限が必要な場合があります: sudo $0 ${PORTS[@]}"
    exit 1
else
    echo "✅ すべてのポートが正常に解放されました"
    exit 0
fi

