<template>
  <div class="login-page">
    <header class="login-header">
      <h1 class="login-title">ログイン</h1>
      <button class="register-link" type="button">
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
            placeholder="電話番号（メールアドレスも可）"
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
              <span v-if="showPassword">🙈</span>
              <span v-else>👁️</span>
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
          ログインすることで、利用規約およびプライバシーポリシーに同意したものとみなされます。
        </p>

        <button class="help-link" type="button">
          ログインできない方はこちら →
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
          <span class="social-icon">G</span>
          <span class="social-text">Googleでログイン</span>
        </button>

        <button
          type="button"
          class="social-button line"
          :disabled="loading"
          @click="handleLineLogin"
        >
          <span class="social-icon">LINE</span>
          <span class="social-text">LINEでログイン</span>
        </button>

        <button
          type="button"
          class="social-button"
          disabled
        >
          <span class="social-icon">🔑</span>
          <span class="social-text">パスキーでログイン（準備中）</span>
        </button>

        <button
          type="button"
          class="social-button"
          disabled
        >
          <span class="social-icon">f</span>
          <span class="social-text">Facebookでログイン（準備中）</span>
        </button>
      </section>
    </main>

    <footer class="login-footer">
      <p class="footer-text">アカウントをお持ちでない方</p>
      <button class="register-button" type="button">
        会員登録
      </button>
    </footer>
  </div>
</template>

<script setup lang="ts">
import { ref } from 'vue'
import { useRouter } from 'vue-router'
import {
  GoogleAuthProvider,
  OAuthProvider,
  signInWithEmailAndPassword,
  signInWithPopup,
} from 'firebase/auth'
import { auth } from '../firebase'

const router = useRouter()

const emailOrPhone = ref('')
const password = ref('')
const showPassword = ref(false)
const loading = ref(false)

const togglePassword = () => {
  showPassword.value = !showPassword.value
}

const apiBase = import.meta.env.VITE_BFF_BASE_URL as string | undefined

async function sendTokenToBackend(idToken: string) {
  const endpoint = (apiBase ?? '') + '/api/auth/login'
  const res = await fetch(endpoint, {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json',
      Authorization: `Bearer ${idToken}`,
    },
  })
  if (!res.ok) {
    throw new Error('ログインに失敗しました')
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
    const cred = await signInWithPopup(auth, provider)
    const idToken = await cred.user.getIdToken()
    await sendTokenToBackend(idToken)
    await router.push('/')
  } catch (e) {
    console.error(e)
    window.alert('Googleでのログインに失敗しました。')
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
.login-page {
  max-width: 480px;
  margin: 0 auto;
  min-height: 100vh;
  padding: 16px 16px 32px;
  box-sizing: border-box;
  display: flex;
  flex-direction: column;
  font-family: -apple-system, BlinkMacSystemFont, 'Helvetica Neue', Arial, 'メイリオ', sans-serif;
}

.login-header {
  display: flex;
  align-items: center;
  justify-content: center;
  position: relative;
  padding: 8px 0 16px;
  border-bottom: 1px solid #eee;
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
  padding-top: 16px;
}

.login-form-section {
  display: flex;
  flex-direction: column;
  gap: 16px;
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
  padding: 10px 12px;
  border-radius: 4px;
  border: 1px solid #ccc;
  font-size: 14px;
}

.input:focus {
  outline: none;
  border-color: #e60033;
  box-shadow: 0 0 0 1px rgba(230, 0, 51, 0.2);
}

.password-group {
  margin-top: 4px;
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
  right: 8px;
  background: transparent;
  border: none;
  cursor: pointer;
  font-size: 16px;
}

.login-button {
  width: 100%;
  padding: 10px 12px;
  border-radius: 4px;
  border: none;
  background-color: #e60033;
  color: #fff;
  font-size: 15px;
  font-weight: 600;
  cursor: pointer;
}

.login-button:disabled {
  opacity: 0.7;
  cursor: default;
}

.note {
  font-size: 11px;
  color: #777;
  line-height: 1.5;
}

.help-link {
  align-self: flex-start;
  background: transparent;
  border: none;
  color: #007aff;
  font-size: 13px;
  cursor: pointer;
  padding: 0;
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
  padding: 10px 12px;
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
  text-align: center;
}

.social-text {
  flex: 1;
  text-align: center;
}

.social-button.apple .social-icon {
  font-size: 18px;
}

.social-button.google .social-icon {
  font-weight: bold;
  color: #4285f4;
}

.social-button.line .social-icon {
  font-weight: bold;
  color: #00b900;
}

.login-footer {
  margin-top: 32px;
  border-top: 1px solid #eee;
  padding-top: 16px;
}

.footer-text {
  font-size: 13px;
  margin: 0 0 8px;
}

.register-button {
  width: 100%;
  padding: 10px 12px;
  border-radius: 4px;
  border: 1px solid #e60033;
  background-color: #fff;
  color: #e60033;
  font-size: 14px;
  font-weight: 600;
  cursor: pointer;
}
</style>


