<template>
  <div>
    <h2 style="margin:0 0 24px 0;font-size:24px;font-weight:600;">決済</h2>
    
    <div v-if="cart.length === 0" style="text-align:center;padding:48px;color:#6b7280;">
      <div style="font-size:48px;margin-bottom:16px;">🛒</div>
      <div style="font-size:18px;margin-bottom:8px;">カートは空です</div>
      <div style="font-size:14px;">商品を追加してから決済を行ってください</div>
      <router-link to="/" style="display:inline-block;margin-top:16px;background:#ff6b6b;color:white;border:none;border-radius:8px;padding:12px 24px;text-decoration:none;font-weight:600;">
        商品を見る
      </router-link>
    </div>
    
    <div v-else>
      <!-- 注文内容確認 -->
      <div data-tour="checkout-summary" style="background:#fff;border-radius:12px;padding:24px;box-shadow:0 2px 8px rgba(0,0,0,0.1);margin-bottom:24px;">
        <h3 style="margin:0 0 16px 0;font-size:18px;font-weight:600;">注文内容</h3>
        <div v-for="c in cart" :key="c.productId" style="display:flex;align-items:center;padding:12px 0;border-bottom:1px solid #f1f5f9;">
          <img :src="c.product.imageUrl || getImageUrl('/product.svg')" alt="商品画像" style="width:60px;height:60px;object-fit:cover;border-radius:6px;background:#f9fafb;margin-right:12px;" />
          <div style="flex:1;">
            <div style="font-weight:600;margin-bottom:4px;">{{ c.product.name }}</div>
            <div style="color:#6b7280;font-size:14px;">数量: {{ c.quantity }}</div>
          </div>
          <div style="color:#ff6b6b;font-weight:700;">¥ {{ (Number(c.product.price) * c.quantity).toLocaleString() }}</div>
        </div>
        <div style="border-top:2px solid #f1f5f9;padding-top:16px;margin-top:16px;">
          <div style="display:flex;justify-content:space-between;align-items:center;font-size:18px;font-weight:700;">
            <span>合計</span>
            <span style="color:#ff6b6b;">¥ {{ total.toLocaleString() }}</span>
          </div>
        </div>
      </div>

      <!-- お客様情報入力 -->
      <form @submit.prevent="submit" style="background:#fff;border-radius:12px;padding:24px;box-shadow:0 2px 8px rgba(0,0,0,0.1);margin-bottom:24px;">
        <h3 style="margin:0 0 16px 0;font-size:18px;font-weight:600;">お客様情報</h3>
        <div style="display:grid;gap:16px;">
          <div>
            <label style="display:block;margin-bottom:6px;font-weight:600;color:#374151;">お名前</label>
            <input 
              v-model="name" 
              required 
              style="width:100%;padding:12px;border:1px solid #e5e7eb;border-radius:8px;font-size:16px;outline:none;"
              placeholder="山田太郎"
            />
          </div>
          <div>
            <label style="display:block;margin-bottom:6px;font-weight:600;color:#374151;">メールアドレス</label>
            <input 
              v-model="email" 
              type="email" 
              required 
              style="width:100%;padding:12px;border:1px solid #e5e7eb;border-radius:8px;font-size:16px;outline:none;"
              placeholder="taro@example.com"
            />
          </div>
        </div>
      </form>

      <!-- 決済ボタン -->
      <div style="background:#fff;border-radius:12px;padding:24px;box-shadow:0 2px 8px rgba(0,0,0,0.1);">
        <button
          @click="goToPaymentDetail"
          data-tour="confirm-paypay"
          style="width:100%;background:#ff6b6b;color:white;border:none;border-radius:8px;padding:16px;cursor:pointer;font-size:16px;font-weight:600;margin-bottom:16px;">
          注文確定 & PayPay へ
        </button>
        <div style="text-align:center;color:#6b7280;font-size:14px;">
          PayPayで安全・簡単に決済できます
        </div>
      </div>
    </div>

    <!-- 決済リンク表示 -->
    <div v-if="paymentUrl" style="margin-top:24px;background:#fff;border-radius:12px;padding:24px;box-shadow:0 2px 8px rgba(0,0,0,0.1);">
      <h3 style="margin:0 0 16px 0;font-size:18px;font-weight:600;">決済ページ</h3>
      <div style="background:#f8fafc;border:1px solid #e5e7eb;border-radius:8px;padding:16px;margin-bottom:16px;">
        <div style="color:#6b7280;font-size:14px;margin-bottom:8px;">デモ: PayPayへリダイレクトする代わりにリンクを表示します。</div>
        <a :href="paymentUrl" target="_blank" style="color:#3b82f6;text-decoration:none;font-weight:600;">
          PayPay 決済ページ (サンドボックス) →
        </a>
      </div>
      <div v-if="paid" style="margin-top:16px;text-align:center;color:#10b981;font-weight:600;padding:16px;background:#f0fdf4;border-radius:8px;">
        ✅ 支払いが完了しました！
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed, ref } from 'vue'
import { useStore, apiBase, type CartItem, getImageUrl } from '../store'
import { useRouter } from 'vue-router'

const store = useStore()
const router = useRouter()
const cart: CartItem[] = store.cart
const name = ref<string>('山田太郎')
const email = ref<string>('taro@example.com')
const total = computed<number>(() => cart.reduce((a,c)=> a + Number(c.product.price)*c.quantity, 0))
const paymentUrl = ref<string>('')
const orderId = ref<string>('')
const paid = ref<boolean>(false)
const channelToken = ref<string>('')

async function submit(): Promise<void> {
  try {
    const body = {
      customerName: name.value,
      customerEmail: email.value,
      items: cart.map(c => ({ productId: c.productId, quantity: c.quantity }))
    }
    const res = await fetch(`${apiBase}/orders/purchase`, { method: 'POST', headers: { 'Content-Type':'application/json' }, body: JSON.stringify(body) })
    if (!res.ok) {
      const message = await res.text()
      throw new Error(message || '注文作成に失敗しました')
    }
    const data: { orderId: string; amount?: number; status?: string; channelToken?: string } = await res.json()
    paymentUrl.value = ''
    orderId.value = data.orderId
    channelToken.value = data.channelToken ?? ''
  } catch (err) {
    console.error('❌ 注文作成に失敗しました', err)
    alert('注文の作成に失敗しました。時間をおいて再度お試しください。')
  }
}

async function goToPaymentDetail(): Promise<void> {
  try {
    // サーバ側で注文を作成（QRは別APIで取得）
    const body = {
      customerName: name.value,
      customerEmail: email.value,
      items: cart.map(c => ({ productId: c.productId, quantity: c.quantity }))
    }
    const res = await fetch(`${apiBase}/orders/purchase`, { method: 'POST', headers: { 'Content-Type':'application/json' }, body: JSON.stringify(body) })
    if (!res.ok) {
      const message = await res.text()
      throw new Error(message || '注文作成に失敗しました')
    }
    const data: { orderId: string; amount?: number; status?: string; channelToken?: string } = await res.json()
    paymentUrl.value = ''
    orderId.value = data.orderId
    channelToken.value = data.channelToken ?? ''

    // 支払い詳細画面へ遷移（QR取得は詳細画面で実施）
    router.push({
      path: '/payment-detail',
      query: {
        total: String(total.value),
        orderId: orderId.value,
        token: channelToken.value,
      }
    })
  } catch (err) {
    console.error('❌ 決済ページへの遷移に失敗しました', err)
    alert('注文の作成に失敗しました。時間をおいて再度お試しください。')
  }
}
</script>
