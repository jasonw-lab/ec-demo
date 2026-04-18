<template>
  <div>
    <h2 style="margin:0 0 24px 0;font-size:24px;font-weight:600;">ショッピングカート</h2>
    
    <div v-if="cart.length === 0" style="text-align:center;padding:48px;color:#6b7280;">
      <div style="font-size:48px;margin-bottom:16px;">🛒</div>
      <div style="font-size:18px;margin-bottom:8px;">カートは空です</div>
      <div style="font-size:14px;">商品を追加してショッピングを楽しみましょう</div>
      <router-link to="/" style="display:inline-block;margin-top:16px;background:#ff6b6b;color:white;border:none;border-radius:8px;padding:12px 24px;text-decoration:none;font-weight:600;">
        商品を見る
      </router-link>
    </div>
    
    <div v-else>
      <!-- カート商品一覧 -->
      <div data-tour="cart-items" style="background:#fff;border-radius:12px;overflow:hidden;box-shadow:0 2px 8px rgba(0,0,0,0.1);margin-bottom:24px;">
        <div v-for="c in cart" :key="c.productId" style="display:flex;align-items:center;padding:16px;border-bottom:1px solid #f1f5f9;">
          <img :src="c.product.imageUrl || getImageUrl('/product.svg')" alt="商品画像" style="width:80px;height:80px;object-fit:cover;border-radius:8px;background:#f9fafb;margin-right:16px;" />
          <div style="flex:1;">
            <div style="font-weight:600;margin-bottom:4px;line-height:1.4;">{{ c.product.name }}</div>
            <div style="color:#6b7280;font-size:14px;margin-bottom:8px;">{{ c.product.description }}</div>
            <div style="color:#ff6b6b;font-weight:700;font-size:18px;">¥ {{ Number(c.product.price).toLocaleString() }}</div>
          </div>
          <div style="display:flex;align-items:center;gap:12px;margin-right:16px;">
            <div style="display:flex;align-items:center;gap:8px;">
              <button @click="updateQuantity(c.productId, c.quantity - 1)" :disabled="c.quantity <= 1" style="width:32px;height:32px;border:1px solid #e5e7eb;background:#fff;border-radius:6px;cursor:pointer;display:flex;align-items:center;justify-content:center;">-</button>
              <span style="min-width:40px;text-align:center;font-weight:600;">{{ c.quantity }}</span>
              <button @click="updateQuantity(c.productId, c.quantity + 1)" style="width:32px;height:32px;border:1px solid #e5e7eb;background:#fff;border-radius:6px;cursor:pointer;display:flex;align-items:center;justify-content:center;">+</button>
            </div>
            <div style="text-align:right;min-width:80px;">
              <div style="font-weight:700;color:#ff6b6b;">¥ {{ (Number(c.product.price) * c.quantity).toLocaleString() }}</div>
            </div>
          </div>
          <button @click="remove(c.productId)" style="background:none;border:1px solid #e5e7eb;border-radius:6px;padding:8px;cursor:pointer;color:#6b7280;hover:background:#f9fafb;">
            <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
              <path d="M18 6L6 18M6 6l12 12"></path>
            </svg>
          </button>
        </div>
      </div>
      
      <!-- 合計とレジへ進む -->
      <div style="background:#fff;border-radius:12px;padding:24px;box-shadow:0 2px 8px rgba(0,0,0,0.1);">
        <div style="display:flex;justify-content:space-between;align-items:center;margin-bottom:24px;">
          <div style="font-size:18px;font-weight:600;">合計</div>
          <div style="font-size:24px;font-weight:700;color:#ff6b6b;">¥ {{ total.toLocaleString() }}</div>
        </div>
        <button
          data-tour="checkout-button"
          style="width:100%;background:#ff6b6b;color:white;border:none;border-radius:8px;padding:16px;cursor:pointer;font-size:16px;font-weight:600;"
          @click="handleCheckout"
        >
          レジへ進む
        </button>
      </div>
    </div>
  </div>

  <!-- 未ログイン時モーダル -->
  <div v-if="showLoginPrompt" style="position:fixed;top:0;left:0;width:100%;height:100%;background:rgba(0,0,0,0.5);display:flex;align-items:center;justify-content:center;z-index:1000;" @click.self="showLoginPrompt = false">
    <div style="background:#fff;border-radius:12px;padding:32px;text-align:center;max-width:360px;width:calc(100% - 32px);box-shadow:0 8px 32px rgba(0,0,0,0.2);">
      <div style="font-size:44px;margin-bottom:16px;">🔒</div>
      <h3 style="margin:0 0 12px 0;font-size:18px;font-weight:600;color:#111827;">ログインが必要です</h3>
      <p style="margin:0 0 24px 0;color:#6b7280;font-size:14px;line-height:1.6;">購入手続きを進めるには<br>ログインしてください。</p>
      <div style="display:flex;flex-direction:column;gap:10px;">
        <button
          @click="goToLogin"
          style="width:100%;background:#e60033;color:#fff;border:none;border-radius:8px;padding:13px;cursor:pointer;font-size:15px;font-weight:600;"
        >
          ログイン
        </button>
        <button
          @click="showLoginPrompt = false"
          style="width:100%;background:#fff;color:#6b7280;border:1px solid #e5e7eb;border-radius:8px;padding:13px;cursor:pointer;font-size:15px;"
        >
          キャンセル
        </button>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed, ref } from 'vue'
import { useRouter } from 'vue-router'
import { useStore, type CartItem, getImageUrl, apiBase } from '../store'

const store = useStore()
const cart: CartItem[] = store.cart
const remove = store.removeFromCart
const router = useRouter()
const showLoginPrompt = ref(false)

const total = computed<number>(() => cart.reduce((a,c)=> a + Number(c.product.price)*c.quantity, 0))

function updateQuantity(productId: string, newQuantity: number) {
  if (newQuantity > 0) {
    store.updateCartQuantity(productId, newQuantity)
  }
}

function goToLogin() {
  showLoginPrompt.value = false
  router.push('/login')
}

async function handleCheckout(): Promise<void> {
  try {
    const endpoint = apiBase.endsWith('/api')
      ? `${apiBase}/auth/status`
      : `${apiBase}/api/auth/status`

    const res = await fetch(endpoint, { method: 'GET', credentials: 'include' })
    if (res.ok) {
      const data: { success?: boolean; userId?: string } = await res.json()
      if (data.success && data.userId) {
        router.push('/checkout')
        return
      }
    }
  } catch (err) {
    console.error('Failed to check login status before checkout', err)
  }
  showLoginPrompt.value = true
}
</script>
