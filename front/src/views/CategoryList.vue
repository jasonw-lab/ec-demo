<template>
  <div>
    <h2>商品カテゴリ</h2>
    <ul style="display:grid;grid-template-columns:repeat(auto-fill,minmax(180px,1fr));gap:12px;padding:0;list-style:none;">
      <li v-for="c in categories" :key="c.id" style="border:1px solid #eee;border-radius:8px;overflow:hidden;">
        <router-link :to="`/category/${c.id}`" style="display:block;padding:12px;">
          <img :src="c.imageUrl || '/product.svg'" alt="カテゴリ画像" style="width:100%;height:auto;object-fit:cover;background:#f9fafb;">
          <div style="padding:8px 0;">{{ c.name }}</div>
        </router-link>
      </li>
    </ul>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { apiBase, type Category } from '../store'

const categories = ref<Category[]>([])

onMounted(async () => {
  const res = await fetch(`${apiBase}/categories`)
  categories.value = await res.json()
})
</script>
