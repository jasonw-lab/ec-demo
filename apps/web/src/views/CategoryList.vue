<template>
  <div>
    <h2 style="margin:0 0 24px 0;font-size:24px;font-weight:600;">商品カテゴリ</h2>
    
    <div v-if="categories.length === 0" style="text-align:center;padding:48px;color:#6b7280;">
      <div style="font-size:48px;margin-bottom:16px;">📂</div>
      <div style="font-size:18px;margin-bottom:8px;">カテゴリを読み込み中...</div>
      <div style="font-size:14px;">しばらくお待ちください</div>
    </div>
    
    <div v-else>
      <!-- カテゴリグリッド -->
      <div style="display:grid;grid-template-columns:repeat(auto-fill,minmax(200px,1fr));gap:20px;">
        <div v-for="c in categories" :key="c.id" style="background:#fff;border-radius:12px;overflow:hidden;box-shadow:0 2px 8px rgba(0,0,0,0.1);transition:transform 0.2s;hover:transform:translateY(-2px);">
          <router-link :to="`/category/${c.id}`" style="display:block;text-decoration:none;color:inherit;">
            <img :src="c.imageUrl || getImageUrl('/product.svg')" alt="カテゴリ画像" style="width:100%;height:120px;object-fit:cover;background:#f9fafb;">
            <div style="padding:16px;text-align:center;">
              <div style="font-weight:600;font-size:16px;color:#374151;">{{ c.name }}</div>
              <div style="color:#6b7280;font-size:12px;margin-top:4px;">商品を見る →</div>
            </div>
          </router-link>
        </div>
      </div>
      
      <!-- カテゴリ説明 -->
      <div style="margin-top:32px;background:#f8fafc;border-radius:12px;padding:24px;">
        <h3 style="margin:0 0 16px 0;font-size:18px;font-weight:600;color:#374151;">カテゴリについて</h3>
        <div style="color:#6b7280;font-size:14px;line-height:1.6;">
          各カテゴリをクリックすると、そのカテゴリに属する商品一覧を表示します。
          お好みのカテゴリから商品を探してみてください。
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { apiBase, type Category, getImageUrl } from '../store'

const categories = ref<Category[]>([])

onMounted(async () => {
  const res = await fetch(`${apiBase}/categories`)
  categories.value = await res.json()
})
</script>
