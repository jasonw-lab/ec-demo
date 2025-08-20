<template>
  <div style="background:#f7f7f8;min-height:100vh;">
    <header style="border-bottom:1px solid #eee;background:#fff;">
      <div style="max-width:1200px;margin:0 auto;padding:12px 16px;display:flex;align-items:center;gap:16px;">
        <router-link to="/" style="display:flex;align-items:center;gap:8px;color:inherit">
          <img :src="logoUrl" alt="デモサイト ロゴ" style="width:32px;height:32px;object-fit:contain" />
          <div style="font-size:18px;font-weight:600;">デモショップ</div>
        </router-link>
        <form @submit.prevent="doSearch" style="flex:1;display:flex;">
          <input v-model="keyword" placeholder="商品名・型番・ブランド名で検索" style="flex:1;padding:10px 12px;border:1px solid #e5e7eb;border-right:none;border-radius:8px 0 0 8px;outline:none;" />
          <button type="submit" style="background:#ef4444;color:#fff;border:1px solid #ef4444;border-radius:0 8px 8px 0;padding:0 16px;cursor:pointer;">検索</button>
        </form>
        <router-link to="/cart" style="display:flex;align-items:center;gap:6px;color:#111827;">
          <img src="/cart.svg" alt="カート" style="width:20px;height:20px;" />
          <span>カート</span>
          <span style="background:#ef4444;color:#fff;border-radius:999px;padding:2px 8px;font-size:12px;">{{ cartCount }}</span>
        </router-link>
      </div>
      <nav style="border-top:1px solid #f1f5f9;background:#fff;">
        <div style="max-width:1200px;margin:0 auto;padding:0 16px;display:flex;gap:20px;height:44px;align-items:center;">
          <div style="position:relative;">
            <div style="background:#ef4444;color:#fff;border-radius:6px;padding:6px 10px;cursor:default;display:flex;align-items:center;gap:6px;">
              全商品カテゴリ
              <span style="border:5px solid transparent;border-top-color:#fff;margin-top:6px;"></span>
            </div>
            <ul style="position:absolute;left:0;top:42px;width:240px;background:#fff;border:1px solid #eee;border-radius:8px;box-shadow:0 10px 20px rgba(0,0,0,0.06);padding:8px;margin:0;list-style:none;display:none;z-index:20;" class="category-menu">
              <li v-for="c in categories" :key="c.id">
                <router-link :to="`/category/${c.id}`" style="display:block;padding:8px 10px;color:#111827;">{{ c.name }}</router-link>
              </li>
            </ul>
          </div>
          <router-link to="/" style="color:#111827;">ホーム</router-link>
          <router-link to="/" style="color:#111827;">商品カテゴリ</router-link>
          <router-link to="/" style="color:#111827;">新着</router-link>
          <router-link to="/" style="color:#111827;">割引</router-link>
          <router-link to="/" style="color:#111827;">特集</router-link>
        </div>
      </nav>
    </header>
    <main style="padding:16px;max-width:1200px;margin:0 auto;">
      <router-view />
    </main>
  </div>
 </template>

<script setup lang="ts">
import { computed, ref, onMounted } from 'vue'
import { useStore } from './store'
import { useRouter } from 'vue-router'
import { apiBase, type Category } from './store'

const router = useRouter()
const store = useStore()
const cartCount = computed(() => store.cart.reduce((acc, item) => acc + item.quantity, 0))
const logoUrl = '/logo.svg'
const keyword = ref<string>('')
const categories = ref<Category[]>([])

function doSearch() {
  router.push({ path: '/search', query: { q: keyword.value } })
}

onMounted(async () => {
  try {
    const res = await fetch(`${apiBase}/categories`)
    categories.value = await res.json()
  } catch (_) {
    categories.value = []
  }
})
</script>

<style>
a { text-decoration: none; color: #3b82f6; }
a.router-link-active { font-weight: bold; }
</style>
