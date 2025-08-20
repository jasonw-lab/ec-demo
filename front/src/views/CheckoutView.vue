<template>
  <div>
    <h2>決済</h2>
    <div v-if="cart.length === 0">カートは空です。</div>
    <form v-else @submit.prevent="submit">
      <div style="display:grid;gap:8px;max-width:400px;">
        <label>お名前<input v-model="name" required style="width:100%;padding:8px;border:1px solid #ccc;border-radius:6px;"/></label>
        <label>メール<input v-model="email" type="email" required style="width:100%;padding:8px;border:1px solid #ccc;border-radius:6px;"/></label>
      </div>
      <div style="margin-top:12px;">合計: <strong>¥ {{ total.toLocaleString() }}</strong></div>
      <div style="margin-top:12px;display:flex;gap:12px;">
        <button type="submit" style="background:#111827;color:white;border:none;border-radius:6px;padding:8px 14px;cursor:pointer;">注文確定 & PayPay へ</button>
      </div>
    </form>

    <div v-if="paymentUrl" style="margin-top:16px;">
      <div>デモ: PayPayへリダイレクトする代わりにリンクを表示します。</div>
      <a :href="paymentUrl" target="_blank">PayPay 決済ページ (サンドボックス)</a>
      <div style="margin-top:12px;">
        <button @click="simulateSuccess" style="background:#10b981;color:white;border:none;border-radius:6px;padding:8px 14px;cursor:pointer;">支払い成功(デモ)</button>
      </div>
      <div v-if="paid" style="margin-top:12px;color:#10b981;">支払いが完了しました！</div>
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
