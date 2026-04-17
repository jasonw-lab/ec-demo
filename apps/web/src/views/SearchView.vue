<template>
  <div>
    <h2 style="margin:0 0 24px 0;font-size:24px;font-weight:600;">検索結果</h2>
    
    <!-- 空キーワード時の表示 -->
    <div v-if="!q || q.trim().length === 0" style="text-align:center;padding:48px;color:#6b7280;">
      <div style="font-size:48px;margin-bottom:16px;">🔍</div>
      <div style="font-size:18px;margin-bottom:8px;">キーワードを入力してください</div>
      <div style="font-size:14px;">商品名・ブランド名・カテゴリで検索できます</div>
    </div>
    
    <!-- ローディング中 -->
    <div v-else-if="loading" style="text-align:center;padding:48px;color:#6b7280;">
      <div style="font-size:48px;margin-bottom:16px;">⏳</div>
      <div style="font-size:18px;margin-bottom:8px;">検索中...</div>
      <div style="font-size:14px;">しばらくお待ちください</div>
    </div>
    
    <!-- エラー時 -->
    <div v-else-if="error" style="text-align:center;padding:48px;color:#6b7280;">
      <div style="font-size:48px;margin-bottom:16px;">⚠️</div>
      <div style="font-size:18px;margin-bottom:8px;color:#ef4444;">検索に失敗しました</div>
      <div style="font-size:14px;margin-bottom:16px;">{{ error }}</div>
      <button @click="retrySearch" style="background:#ff6b6b;color:white;border:none;border-radius:8px;padding:10px 20px;cursor:pointer;font-weight:600;font-size:14px;">
        再試行
      </button>
    </div>
    
    <!-- 検索結果0件 -->
    <div v-else-if="results.length === 0" style="text-align:center;padding:48px;color:#6b7280;">
      <div style="font-size:48px;margin-bottom:16px;">😔</div>
      <div style="font-size:18px;margin-bottom:8px;">該当する商品が見つかりませんでした</div>
      <div style="font-size:14px;margin-bottom:16px;">別のキーワードで検索してみてください</div>
      <div style="margin-top:16px;color:#3b82f6;">
        検索キーワード: <strong>"{{ q }}"</strong>
      </div>
    </div>
    
    <!-- 検索結果表示 -->
    <div v-else>
      <!-- 検索結果件数 -->
      <div style="margin-bottom:16px;color:#6b7280;font-size:14px;">
        "{{ q }}" の検索結果: <strong>{{ total }}</strong>件
      </div>
      
      <!-- 商品グリッド -->
      <div style="display:grid;grid-template-columns:repeat(auto-fill,minmax(280px,1fr));gap:20px;">
        <div v-for="(p, idx) in results" :key="p.id" style="background:#fff;border-radius:12px;overflow:hidden;box-shadow:0 2px 8px rgba(0,0,0,0.1);display:flex;flex-direction:column;transition:transform 0.2s;cursor:pointer;" @mouseenter="(e) => e.currentTarget.style.transform = 'translateY(-2px)'" @mouseleave="(e) => e.currentTarget.style.transform = 'translateY(0)'">
          <img :src="p.imageUrl || getImageUrl('/product.svg')" alt="商品画像" style="width:100%;height:200px;object-fit:contain;background:#f9fafb" />
          <div style="padding:16px;display:flex;flex-direction:column;gap:12px;flex:1;">
            <div style="font-weight:600;font-size:16px;line-height:1.4;display:-webkit-box;-webkit-line-clamp:2;-webkit-box-orient:vertical;overflow:hidden;">{{ p.name }}</div>
            <div style="margin-top:auto;display:flex;justify-content:space-between;align-items:center">
              <div style="color:#ff6b6b;font-weight:700;font-size:18px;">¥ {{ Number(p.price).toLocaleString() }}</div>
              <button @click="add(p)" :data-tour="idx === 0 ? 'add-to-cart-primary' : undefined" style="background:#ff6b6b;color:white;border:none;border-radius:8px;padding:8px 16px;cursor:pointer;font-weight:600;transition:background-color 0.2s;" @mouseenter="(e) => e.currentTarget.style.backgroundColor = '#e55555'" @mouseleave="(e) => e.currentTarget.style.backgroundColor = '#ff6b6b'">
                カートに追加
              </button>
            </div>
          </div>
        </div>
      </div>
      
      <Pagination
        :page="page"
        :total-pages="totalPages"
        @change="goToPage"
      />
      
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
import { ref, computed, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { apiBase, useStore, type Product, getImageUrl } from '../store'
import type { SearchApiResponse } from '../types/search'
import { productCardToProduct } from '../types/search'
import Pagination from '../components/Pagination.vue'

const route = useRoute()
const router = useRouter()
const store = useStore()

// 状態管理
const loading = ref<boolean>(false)
const error = ref<string | null>(null)
const results = ref<Product[]>([])
const total = ref<number>(0)

// URLパラメータから取得（デフォルト値設定）
const q = computed(() => {
  const query = route.query.q as string
  return query ? query.trim() : ''
})

const page = computed(() => {
  const p = parseInt(route.query.page as string, 10)
  return isNaN(p) || p < 0 ? 0 : p
})

const size = computed(() => {
  const s = parseInt(route.query.size as string, 10)
  return isNaN(s) || s <= 0 ? 20 : s
})

// ページネーション計算
const totalPages = computed(() => Math.ceil(total.value / size.value))

// カートに追加
function add(product: Product): void {
  store.addToCart(product, 1)
}

// ページ遷移
function goToPage(newPage: number): void {
  if (newPage < 0 || newPage >= totalPages.value) return
  
  router.push({
    path: '/search',
    query: {
      q: q.value,
      page: newPage,
      size: size.value,
    },
  })
}

// 検索実行
async function performSearch(): Promise<void> {
  const keyword = q.value
  
  // 空キーワードの場合は検索しない
  if (!keyword || keyword.length === 0) {
    results.value = []
    total.value = 0
    error.value = null
    loading.value = false
    return
  }
  
  loading.value = true
  error.value = null
  
  try {
    // BFF の /api/products/search を呼び出す
    const url = new URL(`${apiBase}/products/search`, window.location.origin)
    url.searchParams.set('q', keyword)
    url.searchParams.set('page', page.value.toString())
    url.searchParams.set('size', size.value.toString())
    
    const response = await fetch(url.toString())
    
    if (!response.ok) {
      throw new Error(`HTTP ${response.status}: ${response.statusText}`)
    }
    
    const data: SearchApiResponse = await response.json()
    
    // ProductCard[] を Product[] に変換
    results.value = data.items.map(productCardToProduct)
    total.value = data.total
  } catch (err) {
    console.error('Search failed:', err)
    error.value = err instanceof Error ? err.message : '検索に失敗しました'
    results.value = []
    total.value = 0
  } finally {
    loading.value = false
  }
}

// 再試行
function retrySearch(): void {
  performSearch()
}

// URLパラメータの変更を監視して検索実行
watch([q, page, size], () => {
  performSearch()
}, { immediate: true })
</script>
