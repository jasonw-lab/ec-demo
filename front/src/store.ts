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

  function clearCart(): void {
    state.cart.splice(0, state.cart.length)
  }

  return {
    get cart(): CartItem[] {
      return state.cart
    },
    addToCart,
    removeFromCart,
    clearCart,
  }
}

export const apiBase: string = (import.meta.env.VITE_API_BASE || 'http://localhost:8080') + '/api'


