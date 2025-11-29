<template>
  <div :style="{ background: isLoginPage ? '#fff' : '#f7f7f8', minHeight: '100vh' }">
    <header v-if="!isLoginPage" style="border-bottom:1px solid #eee;background:#fff;">
      <!-- ヘッダー上部 -->
      <div style="max-width:1200px;margin:0 auto;padding:12px 16px;display:flex;align-items:center;gap:16px;">
        <!-- ロゴ -->
        <router-link to="/" style="display:flex;align-items:center;color:inherit;text-decoration:none;">
          <img :src="getImageUrl('/mercari-logo-main.jpeg')" alt="mercari" style="width:auto;height:36px;object-fit:contain;" />
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
          <router-link to="/login" style="color:#111827;text-decoration:none;font-size:14px;cursor:pointer;">ログイン</router-link>
          <a @click="showUnderConstruction" style="color:#111827;text-decoration:none;font-size:14px;cursor:pointer;">会員登録</a>
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
          <router-link to="/" style="color:#E60033;text-decoration:none;font-weight:600;border-bottom:2px solid #E60033;padding:12px 0;font-size:14px;">おすすめ</router-link>
          <a @click="showUnderConstruction" style="color:#111827;text-decoration:none;font-size:14px;cursor:pointer;padding:12px 0;">マイリスト</a>
          <a @click="showUnderConstruction" style="color:#111827;text-decoration:none;font-size:14px;cursor:pointer;padding:12px 0;">メルカリShops</a>
          <a @click="showUnderConstruction" style="color:#111827;text-decoration:none;font-size:14px;cursor:pointer;padding:12px 0;">ゲーム・おもちゃ・グッズ</a>
          <a @click="showUnderConstruction" style="color:#111827;text-decoration:none;font-size:14px;cursor:pointer;padding:12px 0;">本・雑誌・漫画</a>
          <a @click="showUnderConstruction" style="color:#111827;text-decoration:none;font-size:14px;cursor:pointer;padding:12px 0;">メンズ</a>
          <a @click="showUnderConstruction" style="color:#111827;text-decoration:none;font-size:14px;cursor:pointer;padding:12px 0;">レディース</a>
          <a @click="showUnderConstruction" style="color:#111827;text-decoration:none;font-size:14px;cursor:pointer;padding:12px 0;">ベビー・キッズ</a>
          <router-link to="/products" style="color:#111827;text-decoration:none;font-size:14px;">すべて見る</router-link>
        </div>
      </nav>
    </header>
    
    <main v-if="!isLoginPage" style="padding:16px;max-width:1200px;margin:0 auto;">
      <router-view />
    </main>
    <router-view v-else />

    <!-- 工事中メッセージモーダル -->
    <div v-if="showModal" style="position:fixed;top:0;left:0;width:100%;height:100%;background:rgba(0,0,0,0.5);display:flex;align-items:center;justify-content:center;z-index:1000;">
      <div style="background:#fff;border-radius:12px;padding:32px;text-align:center;max-width:400px;margin:16px;">
        <div style="font-size:48px;margin-bottom:16px;">🚧</div>
        <h3 style="margin:0 0 16px 0;font-size:20px;font-weight:600;color:#111827;">工事中</h3>
        <p style="margin:0 0 24px 0;color:#6b7280;line-height:1.5;">この機能は現在開発中です。<br>もうしばらくお待ちください。</p>
        <button @click="hideModal" style="background:#ff6b6b;color:#fff;border:none;border-radius:8px;padding:12px 24px;cursor:pointer;font-weight:600;font-size:16px;">
          閉じる
        </button>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed, ref, onMounted } from 'vue'
import { useStore } from './store'
import { useRouter, useRoute } from 'vue-router'
import { apiBase, type Category, getImageUrl } from './store'

const router = useRouter()
const route = useRoute()
const store = useStore()
const cartCount = computed(() => store.cart.reduce((acc, item) => acc + item.quantity, 0))
const logoUrl = '/logo.svg'
const keyword = ref<string>('')
const categories = ref<Category[]>([])
const showModal = ref<boolean>(false)
const isLoginPage = computed(() => route.path === '/login')

function doSearch() {
  router.push({ path: '/search', query: { q: keyword.value } })
}

function showUnderConstruction() {
  showModal.value = true
}

function hideModal() {
  showModal.value = false
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
