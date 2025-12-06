<template>
  <div class="login-wrapper">
    <!-- ロゴ -->
    <div class="login-logo-container">
      <router-link to="/" style="display:flex;align-items:center;color:inherit;text-decoration:none;">
        <img :src="getImageUrl('/mercari-logo-main.jpeg')" alt="mercari" class="login-logo" />
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

    <div class="login-page">
      <header class="login-header">
      <h1 class="login-title">ログイン</h1>
      <button class="register-link" type="button" @click="goToRegistration">
        会員登録はこちら
      </button>
    </header>

    <main class="login-main">
      <section class="login-form-section">
        <div class="form-group">
          <label class="label">電話番号（メールアドレスも可）</label>
          <input
            v-model="emailOrPhone"
            type="text"
            class="input"
            placeholder="09000012345"
          />
        </div>

        <div class="form-group password-group">
          <label class="label">パスワード</label>
          <div class="password-input-wrapper">
            <input
              v-model="password"
              :type="showPassword ? 'text' : 'password'"
              class="input"
              placeholder="パスワード"
            />
            <button
              type="button"
              class="icon-button"
              @click="togglePassword"
              aria-label="パスワードの表示切り替え"
            >
              <svg v-if="showPassword" width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
                <path d="M17.94 17.94A10.07 10.07 0 0 1 12 20c-7 0-11-8-11-8a18.45 18.45 0 0 1 5.06-5.94M9.9 4.24A9.12 9.12 0 0 1 12 4c7 0 11 8 11 8a18.5 18.5 0 0 1-2.16 3.19m-6.72-1.07a3 3 0 1 1-4.24-4.24"></path>
                <line x1="1" y1="1" x2="23" y2="23"></line>
              </svg>
              <svg v-else width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
                <path d="M1 12s4-8 11-8 11 8 11 8-4 8-11 8-11-8-11-8z"></path>
                <circle cx="12" cy="12" r="3"></circle>
              </svg>
            </button>
          </div>
        </div>

        <button
          class="login-button"
          type="button"
          :disabled="loading"
          @click="handleEmailPasswordLogin"
        >
          {{ loading ? 'ログイン中...' : 'ログイン' }}
        </button>

        <p class="note">
          <span class="link-text">利用規約</span>および<span class="link-text">プライバシーポリシー</span>に同意の上、ログインへお進みください。このサイトはreCAPTCHAで保護されており、Googleのプライバシーポリシーと利用規約が適用されます。
        </p>

        <button class="help-link" type="button">
          ログインできない方はこちら >
        </button>
      </section>

      <div class="separator">
        <span class="line"></span>
        <span class="text">または</span>
        <span class="line"></span>
      </div>

      <section class="social-login-section">
        <button
          type="button"
          class="social-button apple"
          :disabled="loading"
          @click="handleAppleLogin"
        >
          <span class="social-icon"></span>
          <span class="social-text">Appleでサインイン</span>
        </button>

        <button
          type="button"
          class="social-button google"
          :disabled="loading"
          @click="handleGoogleLogin"
        >
          <span class="social-icon">
            <svg width="20" height="20" viewBox="0 0 24 24" fill="none">
              <path d="M22.56 12.25c0-.78-.07-1.53-.2-2.25H12v4.26h5.92c-.26 1.37-1.04 2.53-2.21 3.31v2.77h3.57c2.08-1.92 3.28-4.74 3.28-8.09z" fill="#4285F4"/>
              <path d="M12 23c2.97 0 5.46-.98 7.28-2.66l-3.57-2.77c-.98.66-2.23 1.06-3.71 1.06-2.86 0-5.29-1.93-6.16-4.53H2.18v2.84C3.99 20.53 7.7 23 12 23z" fill="#34A853"/>
              <path d="M5.84 14.09c-.22-.66-.35-1.36-.35-2.09s.13-1.43.35-2.09V7.07H2.18C1.43 8.55 1 10.22 1 12s.43 3.45 1.18 4.93l2.85-2.22.81-.62z" fill="#FBBC05"/>
              <path d="M12 5.38c1.62 0 3.06.56 4.21 1.64l3.15-3.15C17.45 2.09 14.97 1 12 1 7.7 1 3.99 3.47 2.18 7.07l3.66 2.84c.87-2.6 3.3-4.53 6.16-4.53z" fill="#EA4335"/>
            </svg>
          </span>
          <span class="social-text">Googleでログイン</span>
        </button>

        <button
          type="button"
          class="social-button line"
          :disabled="loading"
          @click="handleLineLogin"
        >
          <span class="social-icon">
            <svg width="20" height="20" viewBox="0 0 24 24" fill="none">
              <path d="M19.365 9.863c.349 0 .63.285.63.631 0 .345-.281.63-.63.63H17.61v1.125h1.755c.349 0 .63.283.63.63 0 .344-.281.629-.63.629h-2.386c-.345 0-.627-.285-.627-.629V8.108c0-.345.282-.63.63-.63h2.386c.346 0 .627.285.627.63 0 .349-.281.63-.63.63H17.61v1.125h1.755zm-3.855 3.016c0 .27-.174.51-.432.596-.064.021-.133.031-.199.031-.211 0-.391-.09-.51-.25l-2.443-3.317v2.94c0 .344-.279.629-.631.629-.346 0-.626-.285-.626-.629V8.108c0-.27.173-.51.43-.595.06-.023.136-.033.194-.033.195 0 .375.104.495.254l2.462 3.33V8.108c0-.345.282-.63.63-.63.345 0 .63.285.63.63v4.771zm-5.741 0c0 .344-.282.629-.631.629-.345 0-.627-.285-.627-.629V8.108c0-.345.282-.63.63-.63.346 0 .628.285.628.63v4.771zm-2.466.629H4.917c-.345 0-.63-.285-.63-.629V8.108c0-.345.285-.63.63-.63.348 0 .63.285.63.63v4.141h1.756c.348 0 .629.283.629.63 0 .344-.282.629-.629.629M24 10.314C24 4.943 18.615.572 12 .572S0 4.943 0 10.314c0 4.811 4.27 8.842 10.035 9.608.391.082.923.258 1.058.59.12.301.086.766.062 1.08l-.164 1.02c-.045.301-.24 1.186 1.049.645 1.291-.539 6.916-4.078 9.436-6.975C23.176 14.393 24 12.458 24 10.314" fill="#00B900"/>
            </svg>
          </span>
          <span class="social-text">LINEでログイン</span>
        </button>

        <button
          type="button"
          class="social-button passkey"
          disabled
        >
          <span class="social-icon">
            <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
              <rect x="3" y="11" width="18" height="11" rx="2" ry="2"></rect>
              <path d="M7 11V7a5 5 0 0 1 10 0v4"></path>
            </svg>
          </span>
          <span class="social-text">パスキーでログイン</span>
        </button>

        <button
          type="button"
          class="social-button facebook"
          disabled
        >
          <span class="social-icon">
            <svg width="20" height="20" viewBox="0 0 24 24" fill="none">
              <path d="M24 12.073c0-6.627-5.373-12-12-12s-12 5.373-12 12c0 5.99 4.388 10.954 10.125 11.854v-8.385H7.078v-3.47h3.047V9.43c0-3.007 1.792-4.669 4.533-4.669 1.312 0 2.686.235 2.686.235v2.953H15.83c-1.491 0-1.956.925-1.956 1.874v2.25h3.328l-.532 3.47h-2.796v8.385C19.612 23.027 24 18.062 24 12.073z" fill="#1877F2"/>
            </svg>
          </span>
          <span class="social-text">Facebookでログイン</span>
        </button>
      </section>
    </main>

      <footer class="login-footer">
        <p class="footer-text">アカウントをお持ちでない方</p>
        <button class="register-button" type="button" @click="goToRegistration">
          会員登録
        </button>
      </footer>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref } from 'vue'
import { useRouter } from 'vue-router'
import { RouterLink } from 'vue-router'
import {
  GoogleAuthProvider,
  OAuthProvider,
  signInWithEmailAndPassword,
  signInWithPopup,
} from 'firebase/auth'
import { auth } from '../firebase'
import { getImageUrl } from '../store'

const router = useRouter()

function goBack() {
  router.back()
}

function goToRegistration() {
  router.push('/registration')
}

const emailOrPhone = ref('')
const password = ref('')
const showPassword = ref(false)
const loading = ref(false)

const togglePassword = () => {
  showPassword.value = !showPassword.value
}

// In development mode, always use relative path to go through Vite proxy (avoids CORS issues)
// In production, use VITE_BFF_BASE_URL if set, otherwise use relative path
const getApiBase = () => {
  // Debug: log environment info
  console.log('Environment check:', {
    MODE: import.meta.env.MODE,
    PROD: import.meta.env.PROD,
    DEV: import.meta.env.DEV,
    VITE_BFF_BASE_URL: import.meta.env.VITE_BFF_BASE_URL,
    VITE_API_BASE: import.meta.env.VITE_API_BASE,
    BASE_URL: import.meta.env.BASE_URL,
  })
  
  // Check if VITE_BFF_BASE_URL is explicitly set (highest priority)
  if (import.meta.env.VITE_BFF_BASE_URL) {
    console.log('Using VITE_BFF_BASE_URL:', import.meta.env.VITE_BFF_BASE_URL)
    return import.meta.env.VITE_BFF_BASE_URL
  }
  
  // In development mode (DEV === true), use relative path to leverage Vite proxy
  // Default to production path for safety (when PROD is true or DEV is false/undefined)
  if (import.meta.env.DEV === true) {
    console.log('Using development API base: /api')
    return '/api'
  }
  
  // In production mode, use /ec-api/api which nginx proxies to backend
  console.log('Using production API base: /ec-api/api')
  return '/ec-api/api'
}

const apiBase = getApiBase()
console.log('Final apiBase:', apiBase)

async function sendTokenToBackend(idToken: string) {
  // Construct endpoint: if apiBase ends with /api, append /auth/login, otherwise append /api/auth/login
  const endpoint = apiBase.endsWith('/api') 
    ? apiBase + '/auth/login'
    : apiBase + '/api/auth/login'
  console.log('Sending login request to:', endpoint, '(apiBase:', apiBase + ')')
  
  try {
    const res = await fetch(endpoint, {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
        Authorization: `Bearer ${idToken}`,
      },
      // credentials: 'include' is not needed when using Vite proxy in dev mode
      // In production, this may be needed for session management
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

async function handleEmailPasswordLogin() {
  if (!emailOrPhone.value || !password.value) {
    window.alert('メールアドレス（または電話番号）とパスワードを入力してください。')
    return
  }
  loading.value = true
  try {
    const cred = await signInWithEmailAndPassword(auth, emailOrPhone.value, password.value)
    const idToken = await cred.user.getIdToken()
    await sendTokenToBackend(idToken)
    await router.push('/')
  } catch (e) {
    console.error(e)
    window.alert('メールアドレスまたはパスワードが正しくありません。')
  } finally {
    loading.value = false
  }
}

async function handleGoogleLogin() {
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
      const idToken = await cred.user.getIdToken()
      await sendTokenToBackend(idToken)
      await router.push('/')
    } finally {
      console.warn = originalWarn
    }
  } catch (e: any) {
    // Ignore Cross-Origin-Opener-Policy warnings as they don't affect functionality
    if (e?.message?.includes?.('Cross-Origin-Opener-Policy')) {
      console.warn('Cross-Origin-Opener-Policy warning (can be ignored):', e.message)
      return
    }
    
    console.error('Google login error:', e)
    
    let errorMessage = 'Googleでのログインに失敗しました。'
    
    if (e?.code) {
      switch (e.code) {
        case 'auth/popup-blocked':
          errorMessage = 'ポップアップがブロックされています。ブラウザの設定でポップアップを許可してください。'
          break
        case 'auth/popup-closed-by-user':
          errorMessage = 'ログインウィンドウが閉じられました。もう一度お試しください。'
          break
        case 'auth/cancelled-popup-request':
          errorMessage = 'ログインがキャンセルされました。'
          break
        case 'auth/unauthorized-domain':
          errorMessage = 'このドメインは認証されていません。Firebase Consoleで設定を確認してください。'
          break
        case 'auth/operation-not-allowed':
          errorMessage = 'Google認証が有効になっていません。Firebase Consoleで設定を確認してください。'
          break
        case 'auth/configuration-not-found':
          errorMessage = 'Firebase認証の設定が見つかりません。\n\n以下の設定を確認してください：\n1. Firebase Console > 認証 > サインイン方法 で「Google」を有効化\n2. Firebase Console > 認証 > 設定 > 承認済みドメイン に「localhost」を追加\n3. 開発サーバーを再起動してください'
          break
        default:
          errorMessage = `Googleでのログインに失敗しました: ${e.code}`
      }
    } else if (e?.message) {
      errorMessage = `Googleでのログインに失敗しました: ${e.message}`
    }
    
    window.alert(errorMessage)
  } finally {
    loading.value = false
  }
}

async function handleAppleLogin() {
  loading.value = true
  try {
    const provider = new OAuthProvider('apple.com')
    const cred = await signInWithPopup(auth, provider)
    const idToken = await cred.user.getIdToken()
    await sendTokenToBackend(idToken)
    await router.push('/')
  } catch (e) {
    console.error(e)
    window.alert('Appleでのログインに失敗しました。')
  } finally {
    loading.value = false
  }
}

async function handleLineLogin() {
  loading.value = true
  try {
    const provider = new OAuthProvider('oidc.line')
    const cred = await signInWithPopup(auth, provider)
    const idToken = await cred.user.getIdToken()
    await sendTokenToBackend(idToken)
    await router.push('/')
  } catch (e) {
    console.error(e)
    window.alert('LINEでのログインに失敗しました。')
  } finally {
    loading.value = false
  }
}
</script>

<style scoped>
.login-wrapper {
  min-height: 100vh;
  background: #fff;
  font-family: -apple-system, BlinkMacSystemFont, 'Helvetica Neue', Arial, 'メイリオ', sans-serif;
}

.login-logo-container {
  padding: 12px 16px;
  border-bottom: 1px solid #eee;
}

.back-button-container {
  padding: 12px 16px;
}

.login-page {
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

.login-logo-container {
  padding: 12px 0 8px;
}

.login-logo {
  height: 32px;
  object-fit: contain;
}

.login-header {
  display: flex;
  align-items: center;
  justify-content: center;
  position: relative;
  padding: 16px 0;
}

.login-title {
  font-size: 20px;
  font-weight: 600;
  margin: 0;
}

.register-link {
  position: absolute;
  right: 0;
  top: 50%;
  transform: translateY(-50%);
  background: transparent;
  border: none;
  color: #007aff;
  font-size: 13px;
  cursor: pointer;
  padding: 4px 0;
}

.login-main {
  flex: 1;
  padding-top: 24px;
}

.login-form-section {
  display: flex;
  flex-direction: column;
  gap: 20px;
}

.form-group {
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.label {
  font-size: 13px;
  color: #333;
}

.input {
  width: 100%;
  box-sizing: border-box;
  padding: 12px;
  border-radius: 4px;
  border: 1px solid #ccc;
  font-size: 16px;
}

.input:focus {
  outline: none;
  border-color: #e60033;
  box-shadow: 0 0 0 1px rgba(230, 0, 51, 0.2);
}

.password-group {
  margin-top: 0;
}

.password-input-wrapper {
  position: relative;
  display: flex;
  align-items: center;
}

.password-input-wrapper .input {
  padding-right: 40px;
}

.icon-button {
  position: absolute;
  right: 12px;
  background: transparent;
  border: none;
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 4px;
  color: #666;
}

.icon-button:hover {
  color: #333;
}

.login-button {
  width: 100%;
  padding: 14px 12px;
  border-radius: 4px;
  border: none;
  background-color: #e60033;
  color: #fff;
  font-size: 15px;
  font-weight: 600;
  cursor: pointer;
  margin-top: 8px;
}

.login-button:disabled {
  opacity: 0.7;
  cursor: default;
}

.note {
  font-size: 11px;
  color: #777;
  line-height: 1.6;
  margin: 12px 0 0;
  text-align: left;
}

.help-link {
  align-self: flex-end;
  background: transparent;
  border: none;
  color: #007aff;
  font-size: 13px;
  cursor: pointer;
  padding: 0;
  margin-top: 8px;
}

.link-text {
  color: #007aff;
  cursor: pointer;
}

.separator {
  display: flex;
  align-items: center;
  margin: 24px 0 16px;
}

.separator .line {
  flex: 1;
  height: 1px;
  background-color: #ddd;
}

.separator .text {
  margin: 0 8px;
  font-size: 12px;
  color: #777;
}

.social-login-section {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.social-button {
  width: 100%;
  box-sizing: border-box;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
  padding: 12px;
  border-radius: 4px;
  border: 1px solid #ccc;
  background-color: #fff;
  font-size: 14px;
  font-weight: 600;
  cursor: pointer;
}

.social-button:disabled {
  opacity: 0.7;
  cursor: default;
}

.social-icon {
  min-width: 24px;
  width: 24px;
  height: 24px;
  display: flex;
  align-items: center;
  justify-content: center;
}

.social-icon svg {
  width: 20px;
  height: 20px;
}

.social-text {
  flex: 1;
  text-align: center;
}

.social-button.apple .social-icon {
  font-size: 18px;
}

.social-button.passkey .social-icon {
  color: #666;
}

.social-button.facebook .social-icon {
  color: #1877F2;
}

.login-footer {
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

.register-button {
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


