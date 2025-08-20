import { createRouter, createWebHistory, type RouteRecordRaw } from 'vue-router'
import CategoryList from './views/CategoryList.vue'
import ProductList from './views/ProductList.vue'
import CartView from './views/CartView.vue'
import SearchView from './views/SearchView.vue'
import CheckoutView from './views/CheckoutView.vue'

const routes: RouteRecordRaw[] = [
  { path: '/', component: ProductList },
  { path: '/category/:id', component: ProductList, props: true },
  { path: '/cart', component: CartView },
  { path: '/search', component: SearchView },
  { path: '/checkout', component: CheckoutView },
]

export default createRouter({
  history: createWebHistory(),
  routes,
})


