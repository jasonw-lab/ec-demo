<template>
  <div data-tour="payment-success" style="max-width:720px;margin:40px auto;background:#fff;border-radius:12px;box-shadow:0 2px 12px rgba(0,0,0,0.08);padding:32px;text-align:center;">
    <div style="font-size:48px;margin-bottom:8px;">🎉</div>
    <h1 style="margin:0 0 8px 0;font-size:24px;font-weight:700;color:#111827;">お支払いが完了しました</h1>
    <div style="color:#6b7280;margin-bottom:24px;">ご購入ありがとうございます</div>

    <div style="display:grid;gap:12px;max-width:360px;margin:0 auto 24px;">
      <div style="display:flex;justify-content:space-between;padding:12px 16px;background:#f8fafc;border:1px solid #e5e7eb;border-radius:8px;">
        <span style="color:#6b7280;">オーダーID</span>
        <span style="font-weight:600;font-family:monospace;">{{ orderId }}</span>
      </div>
      <div style="display:flex;justify-content:space-between;padding:12px 16px;background:#f8fafc;border:1px solid #e5e7eb;border-radius:8px;">
        <span style="color:#6b7280;">お支払い金額</span>
        <span style="font-weight:700;color:#E60033;">¥{{ amountDisplay }}</span>
      </div>
    </div>

    <div style="display:flex;gap:12px;justify-content:center;">
      <router-link to="/" style="background:#ff6b6b;color:#fff;border:none;border-radius:8px;padding:12px 20px;text-decoration:none;font-weight:600;">トップへ戻る</router-link>
      <router-link to="/products" style="background:#111827;color:#fff;border:none;border-radius:8px;padding:12px 20px;text-decoration:none;font-weight:600;">商品を探す</router-link>
    </div>
  </div>
</template>

<script setup lang="ts">
import { onMounted, ref, computed } from 'vue'
import { useRoute } from 'vue-router'
import { apiBase } from '../store'

const route = useRoute()
const orderId = ref<string>('')
const amount = ref<number | null>(null)

const amountDisplay = computed(() => amount.value != null ? amount.value.toLocaleString() : '-')

onMounted(async () => {
  const q = route.query
  orderId.value = String(q.orderId || '')
  const totalQ = q.total ? Number(q.total) : null
  if (totalQ != null && !Number.isNaN(totalQ)) {
    amount.value = totalQ
    return
  }
  // Try to fetch order for accurate amount if not provided via query
  if (orderId.value) {
    try {
      const res = await fetch(`${apiBase}/orders/${orderId.value}`)
      if (res.ok) {
        const data = await res.json()
        amount.value = Number(data.amount)
      }
    } catch {}
  }
})
</script>
