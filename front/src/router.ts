import { createRouter, createWebHistory, type RouteRecordRaw } from 'vue-router'
import CategoryList from './views/CategoryList.vue'
import ProductList from './views/ProductList.vue'
import ProductsView from './views/ProductsView.vue'
import CartView from './views/CartView.vue'
import LoginView from './views/LoginView.vue'
import SearchView from './views/SearchView.vue'
import CheckoutView from './views/CheckoutView.vue'
import PaymentDetailView from './views/PaymentDetailView.vue'
import PaymentSuccessView from './views/PaymentSuccessView.vue'

const routes: RouteRecordRaw[] = [
  { path: '/', component: ProductList },
  { path: '/products', component: ProductsView },
  { path: '/category/:id', component: ProductList, props: true },
  { path: '/cart', component: CartView },
  { path: '/search', component: SearchView },
  { path: '/checkout', component: CheckoutView },
  { path: '/payment-detail', component: PaymentDetailView },
  { path: '/payment-success', component: PaymentSuccessView },
  { path: '/login', component: LoginView },
]

export default createRouter({
  history: createWebHistory('/ec-demo/'),
  routes,
})


