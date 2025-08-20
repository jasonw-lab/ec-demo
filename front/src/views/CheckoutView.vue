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
      <div style="background:#fff;border-radius:12px;padding:24px;box-shadow:0 2px 8px rgba(0,0,0,0.1);margin-bottom:24px;">
        <h3 style="margin:0 0 16px 0;font-size:18px;font-weight:600;">注文内容</h3>
        <div v-for="c in cart" :key="c.productId" style="display:flex;align-items:center;padding:12px 0;border-bottom:1px solid #f1f5f9;">
          <img :src="c.product.imageUrl || '/product.svg'" alt="商品画像" style="width:60px;height:60px;object-fit:cover;border-radius:6px;background:#f9fafb;margin-right:12px;" />
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
              style="width:100%;padding:12px;border:1px solid #e5e7eb;border-radius:8px;font-size:16px;outline:none;focus:border-color:#ff6b6b;"
              placeholder="山田太郎"
            />
          </div>
          <div>
            <label style="display:block;margin-bottom:6px;font-weight:600;color:#374151;">メールアドレス</label>
            <input 
              v-model="email" 
              type="email" 
              required 
              style="width:100%;padding:12px;border:1px solid #e5e7eb;border-radius:8px;font-size:16px;outline:none;focus:border-color:#ff6b6b;"
              placeholder="taro@example.com"
            />
          </div>
        </div>
      </form>

      <!-- 決済ボタン -->
      <div style="background:#fff;border-radius:12px;padding:24px;box-shadow:0 2px 8px rgba(0,0,0,0.1);">
        <button 
          @click="submit" 
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
      <div style="text-align:center;">
        <button @click="simulateSuccess" style="background:#10b981;color:white;border:none;border-radius:8px;padding:12px 24px;cursor:pointer;font-weight:600;">
          支払い成功(デモ)
        </button>
      </div>
      <div v-if="paid" style="margin-top:16px;text-align:center;color:#10b981;font-weight:600;padding:16px;background:#f0fdf4;border-radius:8px;">
        ✅ 支払いが完了しました！
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed, ref } from 'vue'
import { useStore, apiBase, type CartItem } from '../store'
const store = useStore()
const cart: CartItem[] = store.cart
const name = ref<string>('山田太郎')
const email = ref<string>('taro@example.com')
const total = computed<number>(() => cart.reduce((a,c)=> a + Number(c.product.price)*c.quantity, 0))
const paymentUrl = ref<string>('')
const orderId = ref<string>('')
const paid = ref<boolean>(false)

async function submit(): Promise<void> {
  const body = {
    customerName: name.value,
    customerEmail: email.value,
    items: cart.map(c => ({ productId: c.productId, quantity: c.quantity }))
  }
  const res = await fetch(`${apiBase}/checkout`, { method: 'POST', headers: { 'Content-Type':'application/json' }, body: JSON.stringify(body) })
  const data: { paymentUrl: string; orderId: string } = await res.json()
  paymentUrl.value = data.paymentUrl
  orderId.value = data.orderId
}

async function simulateSuccess(): Promise<void> {
  const res = await fetch(`${apiBase}/payments/${orderId.value}/simulate-success`, { method:'POST' })
  const data: { status: string } = await res.json()
  if (data.status === 'PAID') {
    paid.value = true
    store.clearCart()
  }
}
</script>
