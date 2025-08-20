<template>
  <div>
    <div style="display:flex;justify-content:space-between;align-items:center;margin-bottom:12px;">
      <h2 style="margin:0;">商品一覧</h2>
      <div>
        <label style="font-size:14px;color:#6b7280;margin-right:8px;">並び替え:</label>
        <select v-model="sortKey" style="padding:6px 8px;border:1px solid #e5e7eb;border-radius:6px;">
          <option value="default">おすすめ</option>
          <option value="priceAsc">価格の安い順</option>
          <option value="priceDesc">価格の高い順</option>
        </select>
      </div>
    </div>
    <div style="margin-bottom:12px;">
      <div style="display:grid;grid-template-columns:repeat(8,1fr);gap:8px;">
        <router-link v-for="c in topCategories" :key="c.id" :to="`/category/${c.id}`" :style="categoryChipStyle(c)">
          {{ c.name }}
        </router-link>
      </div>
    </div>
    <div style="background:#fff;border:1px solid #eee;border-radius:8px;padding:16px;">
      <div style="display:grid;grid-template-columns:repeat(4,1fr);gap:16px;">
        <div v-for="(p, idx) in paged" :key="p.id" style="border:none;border-radius:8px;overflow:hidden;display:flex;flex-direction:column;background:#fff;">
        <img :src="p.imageUrl || '/product.svg'" alt="商品画像" style="width:100%;height:200px;object-fit:cover;background:#f9fafb" />
        <div style="padding:12px;display:flex;flex-direction:column;gap:8px;">
          <div style="display:flex;gap:6px;align-items:center;min-height:20px;">
            <span v-if="idx % 3 === 0" style="font-size:12px;color:#ef4444;border:1px solid #ef4444;border-radius:4px;padding:0 4px;">人気</span>
            <span v-if="idx % 5 === 0" style="font-size:12px;color:#3b82f6;border:1px solid #3b82f6;border-radius:4px;padding:0 4px;">新着</span>
          </div>
          <div style="font-weight:600;line-height:1.4;display:-webkit-box;-webkit-line-clamp:2;-webkit-box-orient:vertical;overflow:hidden;">{{ p.name }}</div>
          <div style="color:#6b7280;line-height:1.4;display:-webkit-box;-webkit-line-clamp:2;-webkit-box-orient:vertical;overflow:hidden;">{{ p.description }}</div>
          <div style="margin-top:auto;display:flex;justify-content:space-between;align-items:center">
            <div style="color:#ef4444;font-weight:700;">¥ {{ Number(p.price).toLocaleString() }}</div>
            <button @click="add(p)" style="background:#ef4444;color:white;border:none;border-radius:6px;padding:6px 10px;cursor:pointer;">カートに追加</button>
          </div>
        </div>
      </div>
    </div>
    </div>
  </div>
    <div style="display:flex;justify-content:center;gap:8px;margin-top:16px;">
      <button :disabled="page===1" @click="go(page-1)" style="padding:6px 10px;border:1px solid #e5e7eb;background:#fff;border-radius:6px;cursor:pointer;">前へ</button>
      <button v-for="n in totalPages" :key="n" @click="go(n)" :style="buttonStyle(n)">{{ n }}</button>
      <button :disabled="page===totalPages" @click="go(page+1)" style="padding:6px 10px;border:1px solid #e5e7eb;background:#fff;border-radius:6px;cursor:pointer;">次へ</button>
    </div>
</template>

<script setup lang="ts">
import { ref, onMounted, watch, computed } from 'vue'
import { apiBase, useStore, type Product, type Category } from '../store'
import { useRoute, useRouter } from 'vue-router'

const products = ref<Product[]>([])
const store = useStore()
const route = useRoute()
const router = useRouter()
const sortKey = ref<'default'|'priceAsc'|'priceDesc'>('default')
const page = ref<number>(1)
const pageSize = 12 // 1行4つ x 3行
const totalPages = 4
const categories = ref<Category[]>([])

async function load(): Promise<void> {
  const query = route.params.id ? `?categoryId=${route.params.id}` : ''
  const res = await fetch(`${apiBase}/products${query}`)
  products.value = await res.json()
}

async function loadCategories(): Promise<void> {
  try {
    const res = await fetch(`${apiBase}/categories`)
    categories.value = await res.json()
  } catch (_) {
    categories.value = []
  }
}

function add(product: Product): void { store.addToCart(product, 1) }

onMounted(() => {
  const qPage = Number(route.query.page || 1)
  page.value = Math.min(Math.max(1, qPage), totalPages)
  load()
  loadCategories()
})
watch(() => route.params.id, () => { page.value = 1; load() })
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
    : [{ id: 'demo', name: 'デモ商品', description: 'サンプル', price: 1000, imageUrl: '/product.svg' } as Product]
  let i = 0
  while (result.length < pageSize) {
    const src = base[i % base.length]
    result.push({ ...src, id: `${String(src.id)}-fill-${page.value}-${i}` })
    i++
  }
  return result
})

function go(n: number) {
  router.push({ query: { ...route.query, page: String(n) } })
}

function buttonStyle(n: number) {
  const base = 'padding:6px 10px;border:1px solid #e5e7eb;border-radius:6px;cursor:pointer;'
  return n === page.value
    ? base + 'background:#ef4444;color:#fff;border-color:#ef4444;'
    : base + 'background:#fff;color:#111827;'
}

const topCategories = computed<Category[]>(() => categories.value.slice(0, 8))

function categoryChipStyle(c: Category): string {
  const isActive = String(route.params.id || '') === String(c.id)
  const active = 'background:#ef4444;color:#fff;border:1px solid #ef4444;'
  const normal = 'background:#fff;color:#111827;border:1px solid #e5e7eb;'
  return (isActive ? active : normal) + 'border-radius:999px;padding:6px 10px;text-align:center;'
}
</script>
