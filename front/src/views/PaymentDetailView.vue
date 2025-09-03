<template> <div style="display:flex;min-height:100vh;background:#f7f7f8;">
    <!-- 左側：支払い詳細とQRコード -->
    <div style="flex:2;padding:40px;background:#fff;">
      <!-- ヘッダー -->
      <div style="border-bottom:2px solid #E60033;padding-bottom:16px;margin-bottom:32px;">
        <h1 style="margin:0;font-size:24px;font-weight:600;color:#E60033;">支払い詳細</h1>
      </div>
      
      <!-- 支払い情報 -->
      <div style="margin-bottom:32px;">
        <div style="margin-bottom:16px;">
          <label style="display:block;font-size:14px;color:#6b7280;margin-bottom:4px;">支払い金額</label>
          <div style="font-size:32px;font-weight:700;color:#E60033;">¥{{ total.toLocaleString() }}</div>
        </div>
        
        <div style="margin-bottom:32px;">
          <label style="display:block;font-size:14px;color:#6b7280;margin-bottom:4px;">オーダーID</label>
          <div style="font-size:18px;font-weight:600;color:#111827;font-family:monospace;">{{ orderId }}</div>
        </div>
      </div>
      
      <!-- QRコード支払い説明 -->
      <div style="text-align:center;margin-bottom:24px;">
        <div style="font-size:18px;font-weight:600;color:#111827;margin-bottom:16px;">
          paypay(developer mode)アプリでQRコードをスキャンしてお支払い
        </div>
      </div>
      
      <!-- QRコード -->
      <div style="text-align:center;margin-bottom:16px;">
        <div v-if="qrImgUrl" style="display:inline-block;padding:16px;background:#f8fafc;border-radius:12px;border:2px solid #4ECDC4;">
          <img :src="qrImgUrl" alt="PayPay QRコード" style="width:200px;height:200px;" />
        </div>
        <div v-else-if="isLoadingQr" style="display:inline-block;padding:16px;background:#f8fafc;border-radius:12px;border:2px solid #4ECDC4;width:200px;height:200px;display:flex;align-items:center;justify-content:center;color:#6b7280;">
          QRコード読み込み中...
        </div>
        <div v-else style="display:inline-block;padding:16px;background:#f8fafc;border-radius:12px;border:2px solid #4ECDC4;width:200px;height:200px;display:flex;align-items:center;justify-content:center;color:#6b7280;">
          QRコードが利用できません
        </div>
      </div>
      
      <!-- エラー表示 -->
      <div v-if="paymentError" style="margin-top:24px;padding:16px;background:#fee2e2;border:1px solid #fca5a5;border-radius:8px;text-align:center;">
        <div style="font-size:16px;font-weight:600;color:#dc2626;margin-bottom:8px;">
          支払いエラーが発生しました
        </div>
        <div style="font-size:14px;color:#991b1b;margin-bottom:8px;">
          エラーコード: {{ paymentError.code }}
        </div>
        <div style="font-size:14px;color:#991b1b;margin-bottom:16px;">
          {{ paymentError.message }}
        </div>
        <button @click="retryPayment" style="background:#E60033;color:#fff;border:none;border-radius:8px;padding:8px 16px;cursor:pointer;">
          再試行
        </button>
      </div>
      
      <div style="text-align:center;margin-bottom:32px;">
        <a v-if="paymentUrl" :href="paymentUrl" target="_blank" style="color:#3b82f6;text-decoration:none;font-weight:600;">PayPay 決済ページを開く →</a>
      </div>
      <div style="text-align:center;">
        <button @click="simulateSuccess" style="background:#10b981;color:white;border:none;border-radius:8px;padding:10px 16px;cursor:pointer;font-weight:600;">支払い成功(デモ)</button>
      </div>
      
      <!-- PayPayロゴ -->
      <div style="text-align:center;color:#6b7280;font-size:14px;">
        Powered by <span style="color:#E60033;font-weight:600;">PayPay</span>
      </div>
    </div>
    
    <!-- 右側：PayPayログイン画面 -->
    <div style="flex:1;padding:40px;background:#87CEEB;position:relative;overflow:hidden;min-width:400px;">
      <!-- 背景装飾アイコン -->
      <div style="position:absolute;top:20px;right:20px;opacity:0.1;">
        <div style="font-size:48px;">💰</div>
      </div>
      <div style="position:absolute;bottom:40px;left:20px;opacity:0.1;">
        <div style="font-size:32px;">🛒</div>
      </div>
      <div style="position:absolute;top:60px;left:40px;opacity:0.1;">
        <div style="font-size:24px;">💳</div>
      </div>
      
      <!-- ログインフォーム -->
      <div style="max-width:400px;margin:0 auto;">
        <h2 style="margin:0 0 24px 0;font-size:28px;font-weight:600;color:#fff;">ログイン</h2>
        
        <div style="color:#fff;margin-bottom:32px;font-size:16px;">
          PayPayに登録した携帯電話番号でログイン
        </div>
        
        <!-- ログインフィールド -->
        <form @submit.prevent="handleLogin" style="margin-bottom:32px;">
          <div style="margin-bottom:20px;">
            <label style="display:block;color:#fff;font-size:14px;margin-bottom:8px;">携帯電話番号</label>
            <div style="position:relative;">
              <div style="position:absolute;left:12px;top:50%;transform:translateY(-50%);color:#6b7280;">📱</div>
              <input 
                v-model="phoneNumber" 
                type="tel" 
                placeholder="携帯電話番号を入力" 
                required
                style="width:280px;padding:12px 12px 12px 40px;border:none;border-radius:8px;font-size:16px;outline:none;"
              />
            </div>
          </div>
          
          <div style="margin-bottom:24px;">
            <label style="display:block;color:#fff;font-size:14px;margin-bottom:8px;">パスワード</label>
            <div style="position:relative;">
              <div style="position:absolute;left:12px;top:50%;transform:translateY(-50%);color:#6b7280;">🔒</div>
              <input 
                v-model="password" 
                type="password" 
                placeholder="パスワードを入力" 
                required
                style="width:280px;padding:12px 12px 12px 40px;border:none;border-radius:8px;font-size:16px;outline:none;"
              />
            </div>
          </div>
          
          <div style="width:280px;display:flex;align-items:center;justify-content:space-between;gap:8px;">
            <button 
              type="submit" 
              style="width:180px;background:#E8F4F8;color:#111827;border:2px solid #87CEEB;border-radius:8px;padding:12px 14px;font-size:14px;font-weight:600;cursor:pointer;"
            >
              ログイン
            </button>
            <a href="#" style="color:#fff;text-decoration:none;font-size:14px;white-space:nowrap;">パスワードをお忘れの方</a>
          </div>
        </form>
        
        <!-- 区切り線（左右に薄い線） -->
        <div style="display:flex;align-items:center;gap:12px;margin:32px 0;color:#fff;font-size:16px;">
          <span style="flex:1;height:1px;background:rgba(255,255,255,0.6);"></span>
          <span>または</span>
          <span style="flex:1;height:1px;background:rgba(255,255,255,0.6);"></span>
        </div>
        
        <!-- 外部アカウントログイン -->
        <div style="margin-bottom:32px;">
          <h3 style="margin:0 0 16px 0;font-size:18px;font-weight:600;color:#fff;">外部アカウントでログイン</h3>
          
          <div style="display:flex;flex-direction:column;gap:12px;">
            <button style="width:280px;background:#fff;color:#E60033;border:none;border-radius:8px;padding:12px;font-size:14px;font-weight:600;cursor:pointer;display:flex;align-items:center;justify-content:center;gap:8px;">
              <span style="font-weight:700;">Y!</span> Yahoo! JAPAN ID
            </button>
            
            <button style="width:280px;background:#fff;color:#111827;border:none;border-radius:8px;padding:12px;font-size:14px;font-weight:600;cursor:pointer;display:flex;align-items:center;justify-content:center;gap:8px;">
              <span style="color:#666;">SoftBank</span> / <span style="color:#666;">ワイモバイル</span>
            </button>
          </div>
        </div>
        
        <!-- 新規登録 -->
        <div style="text-align:center;">
          <a href="#" style="color:#fff;text-decoration:none;font-size:14px;">
            アカウントをお持ちでない場合 <span style="text-decoration:underline;">新規登録</span>
          </a>
        </div>
      </div>
    </div>

    <!-- 工事中ポップアップ -->
    <div v-if="showPopup" style="position:fixed;inset:0;background:rgba(0,0,0,0.4);display:flex;align-items:center;justify-content:center;z-index:9999;">
      <div style="background:#fff;border-radius:12px;min-width:320px;max-width:90vw;padding:24px;box-shadow:0 10px 30px rgba(0,0,0,0.2);text-align:center;">
        <div style="font-size:18px;font-weight:700;margin-bottom:8px;">工事中</div>
        <div style="color:#6b7280;margin-bottom:16px;">この機能はデモのため準備中です。</div>
        <button @click="showPopup = false" style="background:#E60033;color:#fff;border:none;border-radius:8px;padding:10px 16px;cursor:pointer;">閉じる</button>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, onBeforeUnmount, computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useStore, apiBase } from '../store'

const route = useRoute()
const router = useRouter()
const store = useStore()

// フォームデータ（右側ログインUIデモ用）
const phoneNumber = ref('')
const password = ref('')

// ポップアップ制御
const showPopup = ref(false)

// 支払い情報
const total = ref<number>(0)
const orderId = ref<string>('')
const paymentUrl = ref<string>('')
const isLoadingQr = ref<boolean>(false)
// バックエンドから受け取る base64 画像（data URL として保持）
const paymentImageDataUrl = ref<string>('')
// エラー状態管理
const paymentError = ref<{code: string, message: string} | null>(null)

// QR画像URL（base64画像があれば優先、それ以外はURLを利用）
const qrImgUrl = computed(() => paymentImageDataUrl.value || paymentUrl.value || '')

let pollTimer: number | undefined
const pollingStart = ref<number>(0)
const pollingTimeoutMs = 120_000
const pollIntervalMs = 3000

function handleLogin() {
  showPopup.value = true
}

async function startPolling() {
  console.log('🔄 Starting polling for orderId:', orderId.value)
  stopPolling()
  pollingStart.value = Date.now()
  pollTimer = window.setInterval(async () => {
    try {
      if (!orderId.value) {
        console.log('❌ No orderId, skipping polling')
        return
      }
      console.log('🔍 Polling payment details for orderId:', orderId.value)
      const res = await fetch(`${apiBase}/payments/${orderId.value}/details`)
      if (res.ok) {
        const data = await res.json()
        console.log('📊 Payment details response:', data)
        
        // PayPayエラーの確認
        if (data.paypayError) {
          console.log('❌ PayPay error detected:', data.paypayError)
          paymentError.value = {
            code: data.paypayError.code,
            message: data.paypayError.message
          }
          stopPolling()
          return
        }
        
        if (data.status === 'PAID') {
          console.log('✅ Payment completed, redirecting to success page')
          stopPolling()
          store.clearCart()
          router.push({ path: '/payment-success', query: { orderId: orderId.value, total: String(total.value) } })
          return
        } else if (data.status === 'PAYMENT_FAILED') {
          console.log('❌ Payment failed')
          stopPolling()
          paymentError.value = {
            code: 'PAYMENT_FAILED',
            message: '支払いに失敗しました。もう一度お試しください。'
          }
          return
        } else {
          console.log('⏳ Payment still pending, status:', data.status)
        }
      } else {
        console.log('❌ Failed to fetch payment details, status:', res.status)
      }
    } catch (error) {
      console.log('❌ Error during polling:', error)
    }
    if (Date.now() - pollingStart.value > pollingTimeoutMs) {
      console.log('⏰ Polling timeout reached, stopping')
      stopPolling()
    }
  }, pollIntervalMs)
}

function stopPolling() {
  if (pollTimer) {
    console.log('🛑 Stopping polling')
    clearInterval(pollTimer)
    pollTimer = undefined
  }
}

async function simulateSuccess() {
  if (!orderId.value) return
  try {
    const res = await fetch(`${apiBase}/payments/${orderId.value}/simulate-success`, { method: 'POST' })
    if (res.ok) {
      const data = await res.json()
      if (data.status === 'PAID') {
        stopPolling()
        store.clearCart()
        router.push({ path: '/payment-success', query: { orderId: orderId.value, total: String(total.value) } })
      }
    }
  } catch {}
}

async function retryPayment() {
  paymentError.value = null
  paymentImageDataUrl.value = ''
  paymentUrl.value = ''
  await fetchQr()
}

async function fetchQr(): Promise<void> {
  if (!orderId.value) {
    console.log('❌ No orderId for QR code fetch')
    return
  }
  console.log('📱 Fetching QR code for orderId:', orderId.value, 'amount:', total.value)
  isLoadingQr.value = true
  try {
    const res = await fetch(`${apiBase}/payments/${orderId.value}/qrcode?amount=${total.value}`)
    if (res.ok) {
      const data = await res.json()
      console.log('📱 QR code response:', data)
      // base64Image を優先して表示。存在しなければ従来の URL を利用
      const base64 = String(data.base64Image || '')
      if (base64) {
        paymentImageDataUrl.value = `data:image/png;base64,${base64}`
        console.log('🖼️ Base64 image set, length:', base64.length)
      } else {
        paymentImageDataUrl.value = ''
        console.log('❌ No base64 image in response')
      }
      paymentUrl.value = String(data.paymentUrl || '')
      console.log('🔗 Payment URL set:', paymentUrl.value)
      // QRコードが取得できた場合、paymentUrlがなくてもpollingを開始する
      if (base64 || paymentUrl.value) {
        console.log('🔄 Starting polling after QR code fetch')
        startPolling()
      } else {
        console.log('❌ No QR code or payment URL, cannot start polling')
      }
    } else {
      console.log('❌ Failed to fetch QR code, status:', res.status)
    }
  } catch (error) {
    console.error('❌ Failed to fetch QR code:', error)
  } finally {
    isLoadingQr.value = false
  }
}

onMounted(async () => {
  const q = route.query
  total.value = Number(q.total || 0)
  orderId.value = String(q.orderId || '')
  paymentUrl.value = String(q.paymentUrl || '')
  console.log('🚀 PaymentDetailView mounted with:', { orderId: orderId.value, paymentUrl: paymentUrl.value, total: total.value })
  
  if (orderId.value) {
    if (!paymentUrl.value) {
      console.log('📱 No paymentUrl, fetching QR code')
      await fetchQr()
    } else {
      console.log('🔄 PaymentUrl exists, starting polling')
      startPolling()
    }
  } else {
    console.log('❌ Missing orderId, cannot start polling')
  }
})

onBeforeUnmount(() => {
  stopPolling()
})
</script>
