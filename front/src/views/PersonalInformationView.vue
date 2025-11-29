<template>
  <div class="personal-info-wrapper">
    <!-- ロゴ -->
    <div class="personal-info-logo-container">
      <router-link to="/" style="display:flex;align-items:center;color:inherit;text-decoration:none;">
        <img :src="getImageUrl('/mercari-logo-main.jpeg')" alt="mercari" class="personal-info-logo" />
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

    <div class="personal-info-page">
      <header class="personal-info-header">
        <h1 class="personal-info-title">本人情報の登録</h1>
      </header>

      <main class="personal-info-main">
        <p class="intro-text">
          安心・安全にご利用いただくために、お客さまの本人情報の登録にご協力ください。他のお客さまに公開されることはありません。
        </p>

        <form class="personal-info-form" @submit.prevent="handleSubmit">
          <!-- 姓 -->
          <div class="form-group">
            <label class="label">姓 (全角)</label>
            <input
              v-model="formData.lastName"
              type="text"
              class="input"
              placeholder="例)山田"
              required
            />
          </div>

          <!-- 名 -->
          <div class="form-group">
            <label class="label">名 (全角)</label>
            <input
              v-model="formData.firstName"
              type="text"
              class="input"
              placeholder="例)彩"
              required
            />
          </div>

          <!-- 姓カナ -->
          <div class="form-group">
            <label class="label">姓カナ (全角)</label>
            <input
              v-model="formData.lastNameKana"
              type="text"
              class="input"
              placeholder="例)ヤマダ"
              required
            />
          </div>

          <!-- 名カナ -->
          <div class="form-group">
            <label class="label">名カナ (全角)</label>
            <input
              v-model="formData.firstNameKana"
              type="text"
              class="input"
              placeholder="例)アヤ"
              required
            />
          </div>

          <!-- 生年月日 -->
          <div class="form-group">
            <label class="label">生年月日</label>
            <input
              v-model="formData.birthDate"
              type="text"
              class="input"
              placeholder="yyyy/mm/dd"
              required
            />
          </div>

          <!-- 性別 -->
          <div class="form-group">
            <label class="label">性別</label>
            <div class="radio-group">
              <label class="radio-label">
                <input
                  v-model="formData.gender"
                  type="radio"
                  value="female"
                  class="radio-input"
                  required
                />
                <span class="radio-text">女性</span>
              </label>
              <label class="radio-label">
                <input
                  v-model="formData.gender"
                  type="radio"
                  value="male"
                  class="radio-input"
                />
                <span class="radio-text">男性</span>
              </label>
              <label class="radio-label">
                <input
                  v-model="formData.gender"
                  type="radio"
                  value="prefer_not_to_answer"
                  class="radio-input"
                />
                <span class="radio-text">回答しない</span>
              </label>
            </div>
            <p class="gender-hint">
              お客さまひとりに合わせたサービスやコンテンツを提供するため、性別をお伺いしています。マイページからあとで編集できます。
            </p>
          </div>

          <!-- 送信ボタン -->
          <button
            type="submit"
            class="submit-button"
            :disabled="loading"
          >
            {{ loading ? '処理中...' : '次へ' }}
          </button>
        </form>

        <a href="#" class="info-link" @click.prevent="showInfo">
          本人情報の登録について >
        </a>
      </main>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref } from 'vue'
import { useRouter } from 'vue-router'
import { getImageUrl, apiBase } from '../store'

const router = useRouter()

function goBack() {
  router.back()
}

const formData = ref({
  lastName: '',
  firstName: '',
  lastNameKana: '',
  firstNameKana: '',
  birthDate: '',
  gender: '',
})

const loading = ref(false)

function showInfo() {
  window.alert('本人情報の登録についての詳細情報を表示します。')
}

async function handleSubmit() {
  if (!formData.value.lastName || !formData.value.firstName || 
      !formData.value.lastNameKana || !formData.value.firstNameKana ||
      !formData.value.birthDate || !formData.value.gender) {
    window.alert('すべての項目を入力してください。')
    return
  }

  // 生年月日の形式チェック
  const datePattern = /^\d{4}\/\d{2}\/\d{2}$/
  if (!datePattern.test(formData.value.birthDate)) {
    window.alert('生年月日は yyyy/mm/dd 形式で入力してください。')
    return
  }

  // 確認ダイアログを表示
  const confirmed = window.confirm('登録してよろしいでしょうか。')
  if (!confirmed) {
    return
  }

  loading.value = true
  try {
    // APIエンドポイントを構築
    const endpoint = apiBase.endsWith('/api') 
      ? apiBase + '/auth/personal-information'
      : apiBase + '/api/auth/personal-information'
    
    console.log('Sending personal information to:', endpoint)
    
    const response = await fetch(endpoint, {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
      },
      credentials: 'include', // セッションクッキーを送信
      body: JSON.stringify({
        lastName: formData.value.lastName,
        firstName: formData.value.firstName,
        lastNameKana: formData.value.lastNameKana,
        firstNameKana: formData.value.firstNameKana,
        birthDate: formData.value.birthDate,
        gender: formData.value.gender,
      }),
    })

    if (!response.ok) {
      const errorData = await response.json().catch(() => ({ message: 'サーバーエラーが発生しました。' }))
      throw new Error(errorData.message || `サーバーエラー: ${response.status}`)
    }

    const result = await response.json()
    if (!result.success) {
      throw new Error(result.message || '登録に失敗しました。')
    }

    // 登録完了メッセージを表示
    window.alert('登録完了')
    
    // ホーム画面へ遷移
    await router.push('/')
  } catch (e: any) {
    console.error('Personal information registration error:', e)
    window.alert(e.message || '本人情報の登録に失敗しました。もう一度お試しください。')
  } finally {
    loading.value = false
  }
}
</script>

<style scoped>
.personal-info-wrapper {
  min-height: 100vh;
  background: #fff;
  font-family: -apple-system, BlinkMacSystemFont, 'Helvetica Neue', Arial, 'メイリオ', sans-serif;
}

.personal-info-logo-container {
  padding: 12px 16px;
  border-bottom: 1px solid #eee;
}

.back-button-container {
  padding: 12px 16px;
}

.personal-info-page {
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

.personal-info-logo {
  height: 32px;
  object-fit: contain;
}

.personal-info-header {
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 16px 0;
}

.personal-info-title {
  font-size: 28px;
  font-weight: 600;
  margin: 0;
  color: #000;
}

.personal-info-main {
  flex: 1;
  padding-top: 24px;
}

.intro-text {
  font-size: 13px;
  color: #666;
  line-height: 1.6;
  margin: 0 0 24px;
}

.personal-info-form {
  display: flex;
  flex-direction: column;
  gap: 24px;
}

.form-group {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.label {
  font-size: 13px;
  color: #333;
  font-weight: 700;
  display: flex;
  align-items: center;
  gap: 8px;
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

.radio-group {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.radio-label {
  display: flex;
  align-items: center;
  gap: 8px;
  cursor: pointer;
}

.radio-input {
  width: 20px;
  height: 20px;
  cursor: pointer;
  accent-color: #007aff;
}

.radio-text {
  font-size: 14px;
  color: #333;
  user-select: none;
}

.gender-hint {
  font-size: 11px;
  color: #777;
  margin: 8px 0 0;
  line-height: 1.5;
}

.submit-button {
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

.submit-button:disabled {
  opacity: 0.7;
  cursor: default;
}

.submit-button:hover:not(:disabled) {
  background-color: #cc0029;
}

.info-link {
  display: block;
  text-align: center;
  margin-top: 16px;
  color: #007aff;
  font-size: 13px;
  text-decoration: none;
  cursor: pointer;
}

.info-link:hover {
  text-decoration: underline;
}
</style>

