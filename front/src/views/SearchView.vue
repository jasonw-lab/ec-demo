<template>
  <div>
    <h2 style="margin:0 0 12px 0;">検索結果</h2>
    <div v-if="q.length === 0" style="color:#6b7280;">キーワードを入力してください。</div>
    <div v-else-if="results.length === 0" style="color:#6b7280;">該当する商品が見つかりませんでした。</div>
    <div v-else style="display:grid;grid-template-columns:repeat(auto-fill,minmax(240px,1fr));gap:16px;">
      <div v-for="p in results" :key="p.id" style="border:1px solid #eee;border-radius:8px;overflow:hidden;background:#fff;display:flex;flex-direction:column;">
        <img :src="p.imageUrl || '/product.svg'" alt="商品画像" style="width:100%;height:200px;object-fit:cover;background:#f9fafb" />
        <div style="padding:12px;display:flex;flex-direction:column;gap:8px;">
          <div style="font-weight:600;">{{ p.name }}</div>
          <div style="color:#6b7280;">{{ p.description }}</div>
          <div style="margin-top:auto;display:flex;justify-content:space-between;align-items:center">
            <div style="color:#ef4444;font-weight:700;">¥ {{ Number(p.price).toLocaleString() }}</div>
            <button @click="add(p)" style="background:#ef4444;color:white;border:none;border-radius:6px;padding:6px 10px;cursor:pointer;">カートに追加</button>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, watchEffect } from 'vue'
import { useRoute } from 'vue-router'
import { apiBase, useStore, type Product } from '../store'

const route = useRoute()
const q = ref<string>('')
const results = ref<Product[]>([])
const store = useStore()

function add(product: Product): void { store.addToCart(product, 1) }

watchEffect(async () => {
  q.value = (route.query.q as string) || ''
  if (!q.value) { results.value = []; return }
  // demo: サーバー側の検索APIがない場合はカテゴリなしの /products を全件取得し、前方/部分一致でフィルタ
  try {
    const res = await fetch(`${apiBase}/products`)
    const all: Product[] = await res.json()
    const key = q.value.trim().toLowerCase()
    results.value = all.filter(p =>
      String(p.name).toLowerCase().includes(key) ||
      String(p.description || '').toLowerCase().includes(key)
    )
  } catch (_) {
    results.value = []
  }
})
</script>


