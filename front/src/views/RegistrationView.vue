<template>
  <div class="registration-wrapper">
    <!-- ロゴ -->
    <div class="registration-logo-container">
      <router-link to="/" style="display:flex;align-items:center;color:inherit;text-decoration:none;">
        <img :src="getImageUrl('/mercari-logo-main.jpeg')" alt="mercari" class="registration-logo" />
      </router-link>
    </div>

    <!-- 戻るボタン -->
    <div class="back-button-container">
      <button class="back-button" type="button" @click="goBack">
        <svg width="24" height="24" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5" stroke-linecap="round" stroke-linejoin="round">
          <path d="M15 18l-6-6 6-6"/>
        </svg>
      </button>
    </div>

    <div class="registration-page">
      <header class="registration-header">
        <h1 class="registration-title">会員登録</h1>
      </header>

      <main class="registration-main">
        <section class="registration-options-section">
          <button
            type="button"
            class="registration-button email"
            @click="goToEmailRegistration"
          >
            <span class="registration-icon">
              <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
                <path d="M4 4h16c1.1 0 2 .9 2 2v12c0 1.1-.9 2-2 2H4c-1.1 0-2-.9-2-2V6c0-1.1.9-2 2-2z"></path>
                <polyline points="22,6 12,13 2,6"></polyline>
              </svg>
            </span>
            <span class="registration-text">メールアドレスで登録</span>
          </button>

          <button
            type="button"
            class="registration-button apple"
            :disabled="loading"
            @click="handleAppleRegistration"
          >
            <span class="registration-icon apple-icon">
              <svg width="24" height="24" viewBox="0 0 24 24" fill="none">
                <path d="M17.05 20.28c-.98.95-2.05.88-3.08.4-1.09-.5-2.08-.48-3.24 0-1.44.62-2.2.44-3.06-.4C2.79 15.25 3.51 7.59 9.05 7.31c1.35.07 2.29.74 3.08.8 1.18-.24 2.31-.93 3.57-.84 1.51.12 2.65.72 3.4 1.8-3.12 1.87-2.38 5.98.48 7.13-.57 1.5-1.31 2.99-2.54 4.09l.01-.01zM12.03 7.25c-.15-2.23 1.66-4.07 3.74-4.25.29 2.58-2.34 4.5-3.74 4.25z" fill="#000000"/>
              </svg>
            </span>
            <span class="registration-text">Appleでサインアップ</span>
          </button>

          <button
            type="button"
            class="registration-button google"
            :disabled="loading"
            @click="handleGoogleRegistration"
          >
            <span class="registration-icon">
              <svg width="20" height="20" viewBox="0 0 24 24" fill="none">
                <path d="M22.56 12.25c0-.78-.07-1.53-.2-2.25H12v4.26h5.92c-.26 1.37-1.04 2.53-2.21 3.31v2.77h3.57c2.08-1.92 3.28-4.74 3.28-8.09z" fill="#4285F4"/>
                <path d="M12 23c2.97 0 5.46-.98 7.28-2.66l-3.57-2.77c-.98.66-2.23 1.06-3.71 1.06-2.86 0-5.29-1.93-6.16-4.53H2.18v2.84C3.99 20.53 7.7 23 12 23z" fill="#34A853"/>
                <path d="M5.84 14.09c-.22-.66-.35-1.36-.35-2.09s.13-1.43.35-2.09V7.07H2.18C1.43 8.55 1 10.22 1 12s.43 3.45 1.18 4.93l2.85-2.22.81-.62z" fill="#FBBC05"/>
                <path d="M12 5.38c1.62 0 3.06.56 4.21 1.64l3.15-3.15C17.45 2.09 14.97 1 12 1 7.7 1 3.99 3.47 2.18 7.07l3.66 2.84c.87-2.6 3.3-4.53 6.16-4.53z" fill="#EA4335"/>
              </svg>
            </span>
            <span class="registration-text">Googleで登録</span>
          </button>

          <button
            type="button"
            class="registration-button line"
            :disabled="loading"
            @click="handleLineRegistration"
          >
            <span class="registration-icon line-icon-wrapper">
              <img :src="getImageUrl('/icons8-line-48.png')" alt="LINE" class="line-icon" />
            </span>
            <span class="registration-text">LINEで登録</span>
          </button>

          <button
            type="button"
            class="registration-button facebook"
            :disabled="loading"
            @click="handleFacebookRegistration"
          >
            <span class="registration-icon">
              <svg width="20" height="20" viewBox="0 0 24 24" fill="none">
                <path d="M24 12.073c0-6.627-5.373-12-12-12s-12 5.373-12 12c0 5.99 4.388 10.954 10.125 11.854v-8.385H7.078v-3.47h3.047V9.43c0-3.007 1.792-4.669 4.533-4.669 1.312 0 2.686.235 2.686.235v2.953H15.83c-1.491 0-1.956.925-1.956 1.874v2.25h3.328l-.532 3.47h-2.796v8.385C19.612 23.027 24 18.062 24 12.073z" fill="#1877F2"/>
              </svg>
            </span>
            <span class="registration-text">Facebookで登録</span>
          </button>
        </section>

        <p class="terms-note">
          <span class="link-text">利用規約</span>及び<span class="link-text">プライバシーポリシー</span>に同意の上、登録又はログインへお進みください。
        </p>
      </main>

      <footer class="registration-footer">
        <p class="footer-text">アカウントをお持ちの方</p>
        <button class="login-button" type="button" @click="goToLogin">
          ログイン
        </button>
      </footer>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref } from 'vue'
import { useRouter } from 'vue-router'
import {
  GoogleAuthProvider,
  OAuthProvider,
  signInWithPopup,
} from 'firebase/auth'
import { auth } from '../firebase'
import { getImageUrl, apiBase } from '../store'

const router = useRouter()

const getApiBase = () => {
  // Highest priority: user-specified base URL
  if (import.meta.env.VITE_API_BASE) {
    return import.meta.env.VITE_API_BASE
  }

  // Development: go through Vite proxy
  if (import.meta.env.DEV === true) {
    return '/api'
  }

  // Production default: nginx proxies /ec-api/ -> BFF
  return '/ec-api/api'
}

const registrationApiBase = getApiBase()

async function sendTokenToBackend(idToken: string) {
  // Construct endpoint: if apiBase ends with /api, append /auth/session, otherwise append /api/auth/session
  const endpoint = registrationApiBase.endsWith('/api') 
    ? registrationApiBase + '/auth/session'
    : registrationApiBase + '/api/auth/session'
  console.log('Sending login request to:', endpoint, '(apiBase:', registrationApiBase + ')')
  
  try {
    const res = await fetch(endpoint, {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
        Authorization: `Bearer ${idToken}`,
      },
      credentials: import.meta.env.DEV === true ? 'same-origin' : 'include',
    })
    
    console.log('Login response status:', res.status, res.statusText)
    
    if (!res.ok) {
      let errorData
      try {
        errorData = await res.json()
      } catch {
        errorData = { message: `サーバーエラー: ${res.status} ${res.statusText}` }
      }
      const errorMessage = errorData.message || `サーバーエラー: ${res.status} ${res.statusText}`
      console.error('Login failed:', errorMessage, errorData)
      throw new Error(errorMessage)
    }
    
    const responseData = await res.json()
    console.log('Login successful:', responseData)
    return responseData
  } catch (e: any) {
    console.error('Backend login error:', e)
    
    // Network error (server not running)
    if (e.name === 'TypeError' && e.message.includes('fetch')) {
      throw new Error('バックエンドサーバーに接続できません。サーバーが起動しているか確認してください。\n\nエンドポイント: ' + endpoint)
    }
    
    if (e.message) {
      throw new Error(e.message)
    }
    throw new Error('バックエンドへの接続に失敗しました。サーバーが起動しているか確認してください。')
  }
}

function goBack() {
  router.back()
}

function goToLogin() {
  router.push('/login')
}

function goToEmailRegistration() {
  router.push('/registration/form')
}

const loading = ref(false)

async function handleGoogleRegistration() {
  loading.value = true
  try {
    const provider = new GoogleAuthProvider()
    // Suppress console warnings for Cross-Origin-Opener-Policy (these are harmless)
    const originalWarn = console.warn
    console.warn = (...args: any[]) => {
      const message = typeof args[0] === 'string' ? args[0] : String(args[0] || '')
      if (message.includes('Cross-Origin-Opener-Policy')) {
        return // Suppress this specific warning
      }
      originalWarn.apply(console, args)
    }
    
    try {
      const cred = await signInWithPopup(auth, provider)
      const user = cred.user
      const idToken = await user.getIdToken()
      
      // バックエンドにログインしてセッションを作成
      await sendTokenToBackend(idToken)
      
      // Googleログイン成功後、会員登録入力画面へ遷移
      // ユーザー情報をクエリパラメータまたはstateで渡す
      await router.push({
        path: '/registration/form',
        query: {
          email: user.email || '',
          name: user.displayName || '',
          provider: 'google',
        }
      })
    } finally {
      console.warn = originalWarn
    }
  } catch (e: any) {
    // Ignore Cross-Origin-Opener-Policy warnings as they don't affect functionality
    if (e?.message?.includes?.('Cross-Origin-Opener-Policy')) {
      console.warn('Cross-Origin-Opener-Policy warning (can be ignored):', e.message)
      return
    }
    
    console.error('Google registration error:', e)
    
    let errorMessage = 'Googleでの登録に失敗しました。'
    
    if (e?.code) {
      switch (e.code) {
        case 'auth/popup-blocked':
          errorMessage = 'ポップアップがブロックされています。ブラウザの設定でポップアップを許可してください。'
          break
        case 'auth/popup-closed-by-user':
          errorMessage = '登録ウィンドウが閉じられました。もう一度お試しください。'
          break
        case 'auth/cancelled-popup-request':
          errorMessage = '登録がキャンセルされました。'
          break
        case 'auth/unauthorized-domain':
          errorMessage = 'このドメインは認証されていません。Firebase Consoleで設定を確認してください。'
          break
        case 'auth/operation-not-allowed':
          errorMessage = 'Google認証が有効になっていません。Firebase Consoleで設定を確認してください。'
          break
        default:
          errorMessage = `Googleでの登録に失敗しました: ${e.code}`
      }
    } else if (e?.message) {
      errorMessage = `Googleでの登録に失敗しました: ${e.message}`
    }
    
    window.alert(errorMessage)
  } finally {
    loading.value = false
  }
}

async function handleAppleRegistration() {
  loading.value = true
  try {
    const provider = new OAuthProvider('apple.com')
    const cred = await signInWithPopup(auth, provider)
    const user = cred.user
    const idToken = await user.getIdToken()
    
    // バックエンドにログインしてセッションを作成
    await sendTokenToBackend(idToken)
    
    await router.push({
      path: '/registration/form',
      query: {
        email: user.email || '',
        name: user.displayName || '',
        provider: 'apple',
      }
    })
  } catch (e) {
    console.error(e)
    window.alert('Appleでの登録に失敗しました。')
  } finally {
    loading.value = false
  }
}

async function handleLineRegistration() {
  loading.value = true
  try {
    const provider = new OAuthProvider('oidc.line')
    const cred = await signInWithPopup(auth, provider)
    const user = cred.user
    const idToken = await user.getIdToken()
    
    // バックエンドにログインしてセッションを作成
    await sendTokenToBackend(idToken)
    
    await router.push({
      path: '/registration/form',
      query: {
        email: user.email || '',
        name: user.displayName || '',
        provider: 'line',
      }
    })
  } catch (e) {
    console.error(e)
    window.alert('LINEでの登録に失敗しました。')
  } finally {
    loading.value = false
  }
}

async function handleFacebookRegistration() {
  loading.value = true
  try {
    const provider = new OAuthProvider('facebook.com')
    const cred = await signInWithPopup(auth, provider)
    const user = cred.user
    const idToken = await user.getIdToken()
    
    // バックエンドにログインしてセッションを作成
    await sendTokenToBackend(idToken)
    
    await router.push({
      path: '/registration/form',
      query: {
        email: user.email || '',
        name: user.displayName || '',
        provider: 'facebook',
      }
    })
  } catch (e) {
    console.error(e)
    window.alert('Facebookでの登録に失敗しました。')
  } finally {
    loading.value = false
  }
}
</script>

<style scoped>
.registration-wrapper {
  min-height: 100vh;
  background: #fff;
  font-family: -apple-system, BlinkMacSystemFont, 'Helvetica Neue', Arial, 'メイリオ', sans-serif;
}

.registration-logo-container {
  padding: 12px 16px;
  border-bottom: 1px solid #eee;
}

.back-button-container {
  padding: 12px 16px;
}

.registration-page {
  max-width: 480px;
  margin: 0 auto;
  padding: 0 16px 32px;
  box-sizing: border-box;
  display: flex;
  flex-direction: column;
}

.back-button {
  background: transparent;
  border: none;
  cursor: pointer;
  padding: 4px;
  display: flex;
  align-items: center;
  justify-content: flex-start;
  color: #111827;
}

.back-button:hover {
  opacity: 0.7;
}

.registration-logo {
  height: 32px;
  object-fit: contain;
}

.registration-header {
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 16px 0;
}

.registration-title {
  font-size: 20px;
  font-weight: 600;
  margin: 0;
}

.registration-main {
  flex: 1;
  padding-top: 24px;
}

.registration-options-section {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.registration-button {
  width: 100%;
  box-sizing: border-box;
  display: flex;
  align-items: center;
  padding: 12px;
  border-radius: 4px;
  border: 1px solid #ccc;
  background-color: #fff;
  font-size: 14px;
  font-weight: 600;
  cursor: pointer;
  position: relative;
}

.registration-button:disabled {
  opacity: 0.7;
  cursor: default;
}

.registration-button:hover:not(:disabled) {
  background-color: #f5f5f5;
}

.registration-icon {
  min-width: 24px;
  width: 24px;
  height: 24px;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
  margin-right: 12px;
}

.registration-icon svg {
  width: 20px;
  height: 20px;
}

.registration-icon.apple-icon svg {
  width: 24px;
  height: 24px;
}

.registration-icon.line-icon-wrapper {
  width: 24px;
  height: 24px;
  display: flex;
  align-items: center;
  justify-content: center;
  overflow: hidden;
  border-radius: 4px;
}

.registration-icon .line-icon {
  width: 100%;
  height: 100%;
  object-fit: cover;
  display: block;
}

.registration-text {
  flex: 1;
  text-align: center;
}

.terms-note {
  font-size: 12px;
  color: #333;
  line-height: 1.6;
  margin: 24px 0 0;
  text-align: center;
}

.terms-note .link-text {
  color: #007aff;
  cursor: pointer;
  text-decoration: underline;
}

.registration-footer {
  margin-top: 32px;
  border-top: 1px solid #eee;
  padding-top: 16px;
  text-align: center;
}

.footer-text {
  font-size: 13px;
  margin: 0 0 8px;
  text-align: center;
}

.login-button {
  width: 100%;
  padding: 12px;
  border-radius: 4px;
  border: 1px solid #e60033;
  background-color: #fff;
  color: #e60033;
  font-size: 14px;
  font-weight: 600;
  cursor: pointer;
}
</style>

