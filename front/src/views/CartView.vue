<template>
  <div>
    <h2>ショッピングカート</h2>
    <div v-if="cart.length === 0">カートは空です。</div>
    <table v-else style="width:100%;border-collapse:collapse">
      <thead>
      <tr>
        <th style="text-align:left;border-bottom:1px solid #eee;padding:8px">商品</th>
        <th style="text-align:right;border-bottom:1px solid #eee;padding:8px">数量</th>
        <th style="text-align:right;border-bottom:1px solid #eee;padding:8px">小計</th>
        <th style="border-bottom:1px solid #eee;padding:8px"></th>
      </tr>
      </thead>
      <tbody>
      <tr v-for="c in cart" :key="c.productId">
        <td style="padding:8px">{{ c.product.name }}</td>
        <td style="text-align:right;padding:8px">{{ c.quantity }}</td>
        <td style="text-align:right;padding:8px">¥ {{ (Number(c.product.price) * c.quantity).toLocaleString() }}</td>
        <td style="text-align:center;padding:8px"><button @click="remove(c.productId)" style="background:#ef4444;color:white;border:none;border-radius:6px;padding:6px 10px;cursor:pointer;">削除</button></td>
      </tr>
      </tbody>
    </table>
    <div v-if="cart.length" style="margin-top:16px;border-top:1px solid #eee;padding-top:12px;">
      <div style="text-align:right;display:flex;flex-direction:column;gap:8px;align-items:flex-end;">
        <div>合計: <strong>¥ {{ total.toLocaleString() }}</strong></div>
        <router-link to="/checkout"><button style="background:#3b82f6;color:white;border:none;border-radius:6px;padding:8px 14px;cursor:pointer;">レジへ進む</button></router-link>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import { useStore, type CartItem } from '../store'
const store = useStore()
const cart: CartItem[] = store.cart
const remove = store.removeFromCart
const total = computed<number>(() => cart.reduce((a,c)=> a + Number(c.product.price)*c.quantity, 0))
</script>
