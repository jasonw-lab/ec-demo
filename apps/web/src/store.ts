import { reactive } from 'vue'

export interface Category {
  id: string | number
  name: string
  imageUrl?: string
}

export interface Product {
  id: string | number
  name: string
  description?: string
  price: number | string
  imageUrl: string
}

export interface CartItem {
  productId: string | number
  product: Product
  quantity: number
}

interface StoreState {
  cart: CartItem[]
}

const state: StoreState = reactive({
  cart: [],
})

export function useStore() {
  function addToCart(product: Product, quantity: number = 1): void {
    const existingItem: CartItem | undefined = state.cart.find(
      (item) => item.productId === product.id,
    )
    if (existingItem) existingItem.quantity += quantity
    else state.cart.push({ productId: product.id, product, quantity })
  }

  function removeFromCart(productId: CartItem['productId']): void {
    const index = state.cart.findIndex((item) => item.productId === productId)
    if (index >= 0) state.cart.splice(index, 1)
  }

  function updateCartQuantity(productId: CartItem['productId'], quantity: number): void {
    const item = state.cart.find((item) => item.productId === productId)
    if (item) {
      item.quantity = quantity
    }
  }

  function clearCart(): void {
    state.cart.splice(0, state.cart.length)
  }

  return {
    get cart(): CartItem[] {
      return state.cart
    },
    addToCart,
    removeFromCart,
    updateCartQuantity,
    clearCart,
  }
}

// API base URL configuration:
// - If VITE_API_BASE is set, use it (for custom configuration)
// - In development mode, use relative path that goes through Vite proxy
// - In production (served by nginx), use /ec-api/api which nginx proxies to backend
// Default to production path for safety (when DEV is false/undefined)
export const apiBase: string = import.meta.env.VITE_API_BASE 
  ? (import.meta.env.VITE_API_BASE.endsWith('/api') 
      ? import.meta.env.VITE_API_BASE 
      : import.meta.env.VITE_API_BASE + '/api')
  : (import.meta.env.DEV === true ? '/api' : '/ec-api/api')

// ベースパスを考慮した画像パスを返すヘルパー関数
export function getImageUrl(path: string): string {
  const base = import.meta.env.BASE_URL
  // 既にベースパスが含まれている場合はそのまま返す
  if (path.startsWith(base)) return path
  // 絶対パスの場合はベースパスを追加
  if (path.startsWith('/')) return base + path.slice(1)
  // 相対パスの場合はベースパス + パス
  return base + path
}


