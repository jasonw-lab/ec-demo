<template>
  <div style="background:#f7f7f8;min-height:100vh;">
    <header style="border-bottom:1px solid #eee;background:#fff;">
      <!-- ヘッダー上部 -->
      <div style="max-width:1200px;margin:0 auto;padding:12px 16px;display:flex;align-items:center;gap:16px;">
        <!-- ロゴ -->
        <router-link to="/" style="display:flex;align-items:center;color:inherit;text-decoration:none;">
          <img src="/mercari-logo-main.jpeg" alt="mercari" style="width:auto;height:36px;object-fit:contain;" />
        </router-link>
        
        <!-- 検索バー（ロゴに寄せて配置） -->
        <form @submit.prevent="doSearch" style="flex:1;max-width:600px;margin:0 0 0 16px;display:flex;">
          <div style="position:relative;flex:1;display:flex;">
            <input 
              v-model="keyword" 
              placeholder="なにをお探しですか?" 
              style="flex:1;padding:12px 16px;border:1px solid #e5e7eb;border-radius:8px;outline:none;font-size:16px;" 
            />
            <button 
              type="submit" 
              style="position:absolute;right:8px;top:50%;transform:translateY(-50%);background:none;border:none;cursor:pointer;padding:8px;">
              <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                <circle cx="11" cy="11" r="8"></circle>
                <path d="m21 21-4.35-4.35"></path>
              </svg>
            </button>
          </div>
        </form>
        
        <!-- 右側のナビゲーション -->
        <div style="display:flex;align-items:center;gap:16px;margin-left:auto;">
          <router-link to="/login" style="color:#111827;text-decoration:none;font-size:14px;">ログイン</router-link>
          <router-link to="/register" style="color:#111827;text-decoration:none;font-size:14px;">会員登録</router-link>
          <div style="position:relative;cursor:pointer;">
            <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
              <path d="M6 8a6 6 0 0 1 12 0c0 7 3 9 3 9H3s3-2 3-9"></path>
              <path d="m13.73 21a2 2 0 0 1-3.46 0"></path>
            </svg>
          </div>
          <router-link to="/cart" style="display:flex;align-items:center;gap:6px;color:#111827;text-decoration:none;">
            <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
              <circle cx="9" cy="21" r="1"></circle>
              <circle cx="20" cy="21" r="1"></circle>
              <path d="M1 1h4l2.68 13.39a2 2 0 0 0 2 1.61h9.72a2 2 0 0 0 2-1.61L23 6H6"></path>
            </svg>
            <span style="background:#ff6b6b;color:#fff;border-radius:999px;padding:2px 8px;font-size:12px;">{{ cartCount }}</span>
          </router-link>
        </div>
      </div>
      
      <!-- メインナビゲーション -->
      <nav style="border-top:1px solid #f1f5f9;background:#fff;">
        <div style="max-width:1200px;margin:0 auto;padding:0 16px;display:flex;gap:20px;height:44px;align-items:center;">
          <router-link to="/" style="color:#E60033;text-decoration:none;font-weight:600;border-bottom:2px solid #E60033;padding:12px 0;">おすすめ</router-link>
          <router-link to="/mylist" style="color:#111827;text-decoration:none;">マイリスト</router-link>
          <router-link to="/shops" style="color:#111827;text-decoration:none;">メルカリShops</router-link>
          <router-link to="/games" style="color:#111827;text-decoration:none;">ゲーム・おもちゃ・グッズ</router-link>
          <router-link to="/books" style="color:#111827;text-decoration:none;">本・雑誌・漫画</router-link>
          <router-link to="/mens" style="color:#111827;text-decoration:none;">メンズ</router-link>
          <router-link to="/ladies" style="color:#111827;text-decoration:none;">レディース</router-link>
          <router-link to="/kids" style="color:#111827;text-decoration:none;">ベビー・キッズ</router-link>
          <router-link to="/all" style="color:#111827;text-decoration:none;">すべて見る</router-link>
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
