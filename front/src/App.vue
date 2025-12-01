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
          <!-- ログインしていない場合 -->
          <template v-if="!isLoggedIn">
            <router-link to="/login" style="color:#111827;text-decoration:none;font-size:14px;cursor:pointer;">ログイン</router-link>
            <router-link to="/registration" style="color:#111827;text-decoration:none;font-size:14px;cursor:pointer;">会員登録</router-link>
          </template>
          
          <!-- ログインしている場合 -->
          <template v-else>
            <!-- ユーザープロフィール -->
            <div style="position:relative;">
              <div style="display:flex;align-items:center;gap:8px;cursor:pointer;" @click="toggleProfileMenu">
                <div style="width:32px;height:32px;border-radius:50%;background-color:#e5e7eb;display:flex;align-items:center;justify-content:center;overflow:hidden;">
                  <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                    <path d="M20 21v-2a4 4 0 0 0-4-4H8a4 4 0 0 0-4 4v2"></path>
                    <circle cx="12" cy="7" r="4"></circle>
                  </svg>
                </div>
                <span style="color:#111827;font-size:14px;">{{ userName }}</span>
              </div>
              
              <!-- プロフィールメニューポップアップ -->
              <div v-if="showProfileMenu" class="profile-menu-popup" @click.stop>
                <div class="profile-menu-item" @click="handleMenuClick('mypage')">
                  <span>マイページ</span>
                  <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
                    <path d="M9 18l6-6-6-6"></path>
                  </svg>
                </div>
                <div class="profile-menu-divider"></div>
                <div class="profile-menu-item" @click="handleMenuClick('profile')">
                  <span>プロフィール</span>
                  <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
                    <path d="M9 18l6-6-6-6"></path>
                  </svg>
                </div>
                <div class="profile-menu-divider"></div>
                <div class="profile-menu-item" @click="handleMenuClick('follow-list')">
                  <span>フォローリスト</span>
                  <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
                    <path d="M9 18l6-6-6-6"></path>
                  </svg>
                </div>
                <div class="profile-menu-divider"></div>
                <div class="profile-menu-item" @click="handleMenuClick('sold-items')">
                  <span>出品した商品</span>
                  <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
                    <path d="M9 18l6-6-6-6"></path>
                  </svg>
                </div>
                <div class="profile-menu-divider"></div>
                <div class="profile-menu-item" @click="handleMenuClick('purchased-items')">
                  <span>購入した商品</span>
                  <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
                    <path d="M9 18l6-6-6-6"></path>
                  </svg>
                </div>
                <div class="profile-menu-divider"></div>
                <div class="profile-menu-item profile-menu-item-logout" @click="handleMenuClick('logout')">
                  <span>ログアウト</span>
                </div>
              </div>
            </div>
            
            <!-- いいね一覧アイコン -->
            <div style="cursor:pointer;" @click="showUnderConstruction">
              <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                <path d="M20.84 4.61a5.5 5.5 0 0 0-7.78 0L12 5.67l-1.06-1.06a5.5 5.5 0 0 0-7.78 7.78l1.06 1.06L12 21.23l7.78-7.78 1.06-1.06a5.5 5.5 0 0 0 0-7.78z"></path>
              </svg>
            </div>
            
            <!-- ベルアイコン（お知らせ一覧） -->
            <div style="position:relative;cursor:pointer;" @click="showUnderConstruction">
              <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                <path d="M6 8a6 6 0 0 1 12 0c0 7 3 9 3 9H3s3-2 3-9"></path>
                <path d="m13.73 21a2 2 0 0 1-3.46 0"></path>
              </svg>
              <span v-if="notificationCount > 0" style="position:absolute;top:-6px;right:-6px;background-color:#e60033;color:#fff;border-radius:50%;width:18px;height:18px;display:flex;align-items:center;justify-content:center;font-size:11px;font-weight:600;">{{ notificationCount > 99 ? '99+' : notificationCount }}</span>
            </div>
          </template>
          
          <!-- カート（常に表示） -->
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
          <a @click.prevent="navigateToTop" style="color:#111827;text-decoration:none;font-size:14px;cursor:pointer;padding:12px 0;">マイリスト</a>
          <a @click.prevent="navigateToTop" style="color:#111827;text-decoration:none;font-size:14px;cursor:pointer;padding:12px 0;">メルカリShops</a>
          <a @click.prevent="navigateToTop" style="color:#111827;text-decoration:none;font-size:14px;cursor:pointer;padding:12px 0;">ゲーム・おもちゃ・グッズ</a>
          <a @click.prevent="navigateToTop" style="color:#111827;text-decoration:none;font-size:14px;cursor:pointer;padding:12px 0;">本・雑誌・漫画</a>
          <a @click.prevent="navigateToTop" style="color:#111827;text-decoration:none;font-size:14px;cursor:pointer;padding:12px 0;">メンズ</a>
          <a @click.prevent="navigateToTop" style="color:#111827;text-decoration:none;font-size:14px;cursor:pointer;padding:12px 0;">レディース</a>
          <a @click.prevent="navigateToTop" style="color:#111827;text-decoration:none;font-size:14px;cursor:pointer;padding:12px 0;">ベビー・キッズ</a>
          <router-link to="/products" style="color:#111827;text-decoration:none;font-size:14px;">すべて見る</router-link>
        </div>
      </nav>
    </header>
    
    <main v-if="!isLoginPage" style="padding:16px;max-width:1200px;margin:0 auto;">
      <router-view />
    </main>
    <router-view v-else />

    <!-- プロフィールメニュー外側クリックで閉じる -->
    <div v-if="showProfileMenu" style="position:fixed;top:0;left:0;width:100%;height:100%;z-index:998;" @click="showProfileMenu = false"></div>

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
import { computed, ref, onMounted, watch } from 'vue'
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
const showProfileMenu = ref<boolean>(false)
const isLoggedIn = ref<boolean>(false)
const userName = ref<string>('')
const notificationCount = ref<number>(23) // 仮の値、後でAPIから取得

const isLoginPage = computed(() => {
  const path = route.path
  return path === '/login' || path === '/registration' || path.startsWith('/registration/')
})

async function checkLoginStatus() {
  try {
    // セッションからユーザー情報を取得するAPIを呼び出す
    const endpoint = apiBase.endsWith('/api') 
      ? apiBase + '/auth/status'
      : apiBase + '/api/auth/status'
    
    const response = await fetch(endpoint, {
      method: 'GET',
      credentials: 'include',
    })
    
    if (response.ok) {
      const data = await response.json()
      if (data.success && data.userId) {
        isLoggedIn.value = true
        userName.value = data.name || data.email || 'ユーザー'
        return
      }
    }
  } catch (e) {
    console.log('Not logged in or session expired:', e)
  }
  
  isLoggedIn.value = false
  userName.value = ''
}

function doSearch() {
  router.push({ path: '/search', query: { q: keyword.value } })
}

function showUnderConstruction() {
  showModal.value = true
}

function hideModal() {
  showModal.value = false
}

function navigateToTop() {
  router.push('/')
}

function toggleProfileMenu() {
  showProfileMenu.value = !showProfileMenu.value
}

function handleMenuClick(action: string) {
  showProfileMenu.value = false
  
  switch (action) {
    case 'mypage':
      showUnderConstruction()
      break
    case 'profile':
      showUnderConstruction()
      break
    case 'follow-list':
      showUnderConstruction()
      break
    case 'sold-items':
      showUnderConstruction()
      break
    case 'purchased-items':
      showUnderConstruction()
      break
    case 'logout':
      handleLogout()
      break
  }
}

async function handleLogout() {
  try {
    // セッションをクリアするAPIを呼び出す
    const endpoint = apiBase.endsWith('/api') 
      ? apiBase + '/auth/logout'
      : apiBase + '/api/auth/logout'
    
    await fetch(endpoint, {
      method: 'POST',
      credentials: 'include',
    })
  } catch (e) {
    console.error('Logout error:', e)
  }
  
  // ログイン状態をリセット
  isLoggedIn.value = false
  userName.value = ''
  showProfileMenu.value = false
  
  // ログイン状態を再確認（セッションがクリアされたことを確認）
  await checkLoginStatus()
  
  // トップ画面へ遷移
  router.push('/')
}

// ルート変更時にログイン状態をチェック
watch(() => route.path, () => {
  if (!isLoginPage.value) {
    checkLoginStatus()
  }
})

onMounted(async () => {
  try {
    const res = await fetch(`${apiBase}/categories`)
    categories.value = await res.json()
  } catch (_) {
    categories.value = []
  }
  
  if (!isLoginPage.value) {
    await checkLoginStatus()
  }
})
</script>

<style>
a { text-decoration: none; color: #3b82f6; }
a.router-link-active { font-weight: bold; }

.profile-menu-popup {
  position: absolute;
  top: calc(100% + 8px);
  right: 0;
  background: #fff;
  border-radius: 8px;
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.15);
  min-width: 240px;
  z-index: 999;
  overflow: hidden;
}

.profile-menu-divider {
  height: 1px;
  background-color: #e5e7eb;
  margin: 0;
}

.profile-menu-item {
  padding: 12px 16px;
  cursor: pointer;
  font-size: 14px;
  color: #111827;
  display: flex;
  align-items: center;
  justify-content: space-between;
  transition: background-color 0.2s;
}

.profile-menu-item:hover {
  background-color: #f9fafb;
}

.profile-menu-item span {
  flex: 1;
  text-align: left;
}

.profile-menu-item svg {
  color: #6b7280;
  flex-shrink: 0;
  margin-left: 8px;
}

.profile-menu-item-logout {
  color: #007aff;
}

.profile-menu-item-logout:hover {
  background-color: #f0f7ff;
}

.profile-menu-item-logout svg {
  display: none;
}
</style>
