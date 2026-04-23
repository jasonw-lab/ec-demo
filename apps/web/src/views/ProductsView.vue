<template>
  <div>
    <h2 style="margin:0 0 24px 0;font-size:24px;font-weight:600;">商品一覧</h2>
    
    <!-- 並び替えとカテゴリ -->
    <div style="background:#fff;border-radius:12px;padding:20px;box-shadow:0 2px 8px rgba(0,0,0,0.1);margin-bottom:24px;">
      <div style="display:flex;justify-content:space-between;align-items:center;margin-bottom:16px;">
        <h3 style="margin:0;font-size:18px;font-weight:600;">並び替え</h3>
        <div>
          <select v-model="sortKey" style="padding:8px 12px;border:1px solid #e5e7eb;border-radius:8px;font-size:14px;outline:none;focus:border-color:#ff6b6b;">
            <option value="default">おすすめ</option>
            <option value="priceAsc">価格の安い順</option>
            <option value="priceDesc">価格の高い順</option>
          </select>
        </div>
      </div>
      
      <!-- カテゴリチップ -->
      <div>
        <h4 style="margin:0 0 12px 0;font-size:16px;font-weight:600;color:#374151;">カテゴリ</h4>
        <div style="display:grid;grid-template-columns:repeat(auto-fit,minmax(120px,1fr));gap:8px;">
          <router-link v-for="c in topCategories" :key="c.id" :to="`/category/${c.id}`" :style="categoryChipStyle(c)">
            {{ c.name }}
          </router-link>
        </div>
      </div>
    </div>
    
    <!-- 商品グリッド -->
    <div style="background:#fff;border-radius:12px;padding:20px;box-shadow:0 2px 8px rgba(0,0,0,0.1);margin-bottom:24px;">
      <div v-if="paged.length === 0" style="text-align:center;padding:48px;color:#6b7280;">
        <div style="font-size:48px;margin-bottom:16px;">📦</div>
        <div style="font-size:18px;margin-bottom:8px;">商品が見つかりません</div>
        <div style="font-size:14px;">カテゴリや並び替えを変更してみてください</div>
      </div>
      
      <div v-else style="display:grid;grid-template-columns:repeat(auto-fill,minmax(280px,1fr));gap:20px;">
        <div v-for="(p, idx) in paged" :key="p.id" style="border:1px solid #f1f5f9;border-radius:12px;overflow:hidden;background:#fff;display:flex;flex-direction:column;transition:transform 0.2s;hover:transform:translateY(-2px);">
          <img :src="p.imageUrl || getImageUrl('/product.svg')" alt="商品画像" style="width:100%;height:200px;object-fit:cover;background:#f9fafb" />
          <div style="padding:16px;display:flex;flex-direction:column;gap:12px;flex:1;">
            <div style="display:flex;gap:6px;align-items:center;min-height:20px;">
              <span v-if="idx % 3 === 0" style="font-size:12px;color:#ff6b6b;border:1px solid #ff6b6b;border-radius:4px;padding:0 4px;">人気</span>
              <span v-if="idx % 5 === 0" style="font-size:12px;color:#3b82f6;border:1px solid #3b82f6;border-radius:4px;padding:0 4px;">新着</span>
            </div>
            <div style="font-weight:600;line-height:1.4;display:-webkit-box;-webkit-line-clamp:2;-webkit-box-orient:vertical;overflow:hidden;">{{ p.name }}</div>
            <div style="color:#6b7280;line-height:1.4;display:-webkit-box;-webkit-line-clamp:2;-webkit-box-orient:vertical;overflow:hidden;">{{ p.description }}</div>
            <div style="margin-top:auto;display:flex;justify-content:space-between;align-items:center">
              <div style="color:#ff6b6b;font-weight:700;font-size:18px;">¥ {{ Number(p.price).toLocaleString() }}</div>
              <button @click="add(p)" title="追加後は、右上のカートアイコンをクリックして会計へお進みください。" style="background:#ff6b6b;color:white;border:none;border-radius:8px;padding:8px 16px;cursor:pointer;font-weight:600;transition:background-color 0.2s;hover:background:#e55555;">
                カートに追加
              </button>
            </div>
          </div>
        </div>
      </div>
    </div>
    
    <Pagination
      :page="pageIndex"
      :total-pages="totalPages"
      @change="goToPageIndex"
    />
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, watch, computed } from 'vue'
import { apiBase, useStore, type Product, type Category, getImageUrl } from '../store'
import { useRoute, useRouter } from 'vue-router'
import Pagination from '../components/Pagination.vue'

const products = ref<Product[]>([])
const store = useStore()
const route = useRoute()
const router = useRouter()
const sortKey = ref<'default'|'priceAsc'|'priceDesc'>('default')
const page = ref<number>(1)
const pageSize = 12
const totalPages = 4
const categories = ref<Category[]>([])

async function load(): Promise<void> {
  try {
    const res = await fetch(`${apiBase}/products`)
    products.value = await res.json()
  } catch (_) {
    products.value = []
  }
}

async function loadCategories(): Promise<void> {
  try {
    const res = await fetch(`${apiBase}/categories`)
    categories.value = await res.json()
  } catch (_) {
    categories.value = []
  }
}

function add(product: Product): void { 
  store.addToCart(product, 1) 
}

onMounted(() => {
  const qPage = Number(route.query.page || 1)
  page.value = Math.min(Math.max(1, qPage), totalPages)
  load()
  loadCategories()
})

watch(() => route.query.page, (val) => {
  const n = Number(val || 1)
  page.value = Math.min(Math.max(1, n), totalPages)
})

const sorted = computed<Product[]>(() => {
  const arr = [...products.value]
  if (sortKey.value === 'priceAsc') arr.sort((a,b)=> Number(a.price)-Number(b.price))
  if (sortKey.value === 'priceDesc') arr.sort((a,b)=> Number(b.price)-Number(a.price))
  return arr
})

const paged = computed<Product[]>(() => {
  const start = (page.value - 1) * pageSize
  const slice = sorted.value.slice(start, start + pageSize)
  if (slice.length >= pageSize) return slice
  const result: Product[] = [...slice]
  const base = sorted.value.length
    ? sorted.value
    : [{ id: 'demo', name: 'デモ商品', description: 'サンプル', price: 1000, imageUrl: getImageUrl('/product.svg') } as Product]
  let i = 0
  while (result.length < pageSize) {
    const src = base[i % base.length]
    result.push({ ...src, id: `${String(src.id)}-fill-${page.value}-${i}` })
    i++
  }
  return result
})

const pageIndex = computed(() => page.value - 1)

function goToPageIndex(index: number) {
  const next = index + 1
  router.push({ query: { ...route.query, page: String(next) } })
}

const topCategories = computed<Category[]>(() => categories.value.slice(0, 8))

function categoryChipStyle(c: Category): string {
  const isActive = String(route.params.id || '') === String(c.id)
  const active = 'background:#ff6b6b;color:#fff;border:1px solid #ff6b6b;'
  const normal = 'background:#fff;color:#111827;border:1px solid #e5e7eb;'
  return (isActive ? active : normal) + 'border-radius:999px;padding:8px 12px;text-align:center;text-decoration:none;display:block;transition:all 0.2s;hover:border-color:#ff6b6b;'
}
</script>
