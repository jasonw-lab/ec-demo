<template>
  <div>
    <h2 style="margin:0 0 24px 0;font-size:24px;font-weight:600;">検索結果</h2>
    
    <div v-if="q.length === 0" style="text-align:center;padding:48px;color:#6b7280;">
      <div style="font-size:48px;margin-bottom:16px;">🔍</div>
      <div style="font-size:18px;margin-bottom:8px;">キーワードを入力してください</div>
      <div style="font-size:14px;">商品名・ブランド名・カテゴリで検索できます</div>
    </div>
    
    <div v-else-if="results.length === 0" style="text-align:center;padding:48px;color:#6b7280;">
      <div style="font-size:48px;margin-bottom:16px;">😔</div>
      <div style="font-size:18px;margin-bottom:8px;">該当する商品が見つかりませんでした</div>
      <div style="font-size:14px;">別のキーワードで検索してみてください</div>
      <div style="margin-top:16px;color:#3b82f6;">
        検索キーワード: <strong>"{{ q }}"</strong>
      </div>
    </div>
    
    <div v-else>
      <!-- 検索結果件数 -->
      <div style="margin-bottom:16px;color:#6b7280;font-size:14px;">
        "{{ q }}" の検索結果: <strong>{{ results.length }}</strong>件
      </div>
      
      <!-- 商品グリッド -->
      <div style="display:grid;grid-template-columns:repeat(auto-fill,minmax(280px,1fr));gap:20px;">
        <div v-for="p in results" :key="p.id" style="background:#fff;border-radius:12px;overflow:hidden;box-shadow:0 2px 8px rgba(0,0,0,0.1);display:flex;flex-direction:column;transition:transform 0.2s;hover:transform:translateY(-2px);">
          <img :src="p.imageUrl || getImageUrl('/product.svg')" alt="商品画像" style="width:100%;height:200px;object-fit:cover;background:#f9fafb" />
          <div style="padding:16px;display:flex;flex-direction:column;gap:12px;flex:1;">
            <div style="font-weight:600;font-size:16px;line-height:1.4;display:-webkit-box;-webkit-line-clamp:2;-webkit-box-orient:vertical;overflow:hidden;">{{ p.name }}</div>
            <div style="color:#6b7280;font-size:14px;line-height:1.4;display:-webkit-box;-webkit-line-clamp:2;-webkit-box-orient:vertical;overflow:hidden;">{{ p.description }}</div>
            <div style="margin-top:auto;display:flex;justify-content:space-between;align-items:center">
              <div style="color:#ff6b6b;font-weight:700;font-size:18px;">¥ {{ Number(p.price).toLocaleString() }}</div>
              <button @click="add(p)" style="background:#ff6b6b;color:white;border:none;border-radius:8px;padding:8px 16px;cursor:pointer;font-weight:600;transition:background-color 0.2s;hover:background:#e55555;">
                カートに追加
              </button>
            </div>
          </div>
        </div>
      </div>
      
      <!-- 検索結果が少ない場合の提案 -->
      <div v-if="results.length < 5 && results.length > 0" style="margin-top:32px;background:#f8fafc;border-radius:12px;padding:24px;text-align:center;">
        <h3 style="margin:0 0 12px 0;font-size:16px;font-weight:600;color:#374151;">もっと商品を見つけませんか？</h3>
        <div style="color:#6b7280;font-size:14px;margin-bottom:16px;">
          関連する商品や人気商品もチェックしてみてください
        </div>
        <router-link to="/" style="display:inline-block;background:#ff6b6b;color:white;border:none;border-radius:8px;padding:10px 20px;text-decoration:none;font-weight:600;font-size:14px;">
          すべての商品を見る
        </router-link>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, watchEffect } from 'vue'
import { useRoute } from 'vue-router'
import { apiBase, useStore, type Product, getImageUrl } from '../store'

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


