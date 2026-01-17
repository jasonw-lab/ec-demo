<template>
  <div class="registration-form-wrapper">
    <!-- ロゴ -->
    <div class="registration-form-logo-container">
      <img :src="getImageUrl('/mercari-logo-main.jpeg')" alt="mercari" class="registration-form-logo" />
    </div>

    <!-- 戻るボタン -->
    <div class="back-button-container">
      <button class="back-button" type="button" @click="goBack">
        <svg width="24" height="24" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5" stroke-linecap="round" stroke-linejoin="round">
          <path d="M15 18l-6-6 6-6"/>
        </svg>
      </button>
    </div>

    <div class="registration-form-page">
      <header class="registration-form-header">
        <h1 class="registration-form-title">会員登録</h1>
      </header>

      <main class="registration-form-main">
        <form class="registration-form" @submit.prevent="handleSubmit">
          <!-- メールアドレス -->
          <div class="form-group">
            <label class="label">メールアドレス</label>
            <input
              v-model="formData.email"
              type="email"
              class="input"
              placeholder="メールアドレスを入力"
              required
            />
            <p class="hint">※メールアドレスは後から変更できます</p>
          </div>

          <!-- ニックネーム -->
          <div class="form-group">
            <label class="label">ニックネーム</label>
            <div class="nickname-input-wrapper">
              <input
                v-model="formData.nickname"
                type="text"
                class="input"
                placeholder="ニックネームを入力"
                maxlength="20"
                required
              />
              <span class="character-count">{{ nicknameLength }}/20</span>
            </div>
            <p class="hint">ニックネームはあとから変更できます</p>
          </div>

          <!-- 招待コード -->
          <div class="form-group">
            <label class="label">
              招待コード
              <span class="optional-tag">任意</span>
            </label>
            <input
              v-model="formData.invitationCode"
              type="text"
              class="input"
              placeholder="入力してください"
            />
          </div>

          <!-- お知らせ登録 -->
          <div class="form-group">
            <label class="label">
              お知らせ登録
              <span class="optional-tag">任意</span>
            </label>
            <p class="notification-description">
              お得なクーポンやキャンペーン情報などをメール配信します
            </p>
            <div class="checkbox-wrapper">
              <input
                v-model="formData.receiveNotifications"
                type="checkbox"
                id="receive-notifications"
                class="checkbox"
              />
              <label for="receive-notifications" class="checkbox-label">
                メルカリからのお知らせを受け取る
              </label>
            </div>
            <p class="hint">※お知らせ・機能設定からいつでも解除できます</p>
          </div>

          <!-- 送信ボタン -->
          <button
            type="submit"
            class="submit-button"
            :disabled="loading"
          >
            {{ loading ? '登録中...' : '次へ' }}
          </button>
        </form>
      </main>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { getImageUrl } from '../store'

const router = useRouter()
const route = useRoute()

function goBack() {
  router.back()
}

const formData = ref({
  email: '',
  nickname: '',
  invitationCode: '',
  receiveNotifications: true,
})

const loading = ref(false)

const nicknameLength = computed(() => {
  return formData.value.nickname.length
})

// クエリパラメータから初期値を設定
onMounted(() => {
  if (route.query.email) {
    formData.value.email = route.query.email as string
  }
  if (route.query.name) {
    formData.value.nickname = route.query.name as string
  }
})

async function handleSubmit() {
  if (!formData.value.email || !formData.value.nickname) {
    window.alert('メールアドレスとニックネームを入力してください。')
    return
  }

  if (formData.value.nickname.length > 20) {
    window.alert('ニックネームは20文字以内で入力してください。')
    return
  }

  loading.value = true
  try {
    // 会員登録情報を保存（必要に応じてバックエンドAPIを呼び出す）
    console.log('Registration form data:', formData.value)
    
    // 本人情報の登録画面へ遷移
    await router.push('/registration/personal-info')
  } catch (e) {
    console.error('Registration error:', e)
    window.alert('会員登録に失敗しました。もう一度お試しください。')
  } finally {
    loading.value = false
  }
}
</script>

<style scoped>
.registration-form-wrapper {
  min-height: 100vh;
  background: #fff;
  font-family: -apple-system, BlinkMacSystemFont, 'Helvetica Neue', Arial, 'メイリオ', sans-serif;
}

.registration-form-logo-container {
  padding: 12px 16px;
  border-bottom: 1px solid #eee;
}

.back-button-container {
  padding: 12px 16px;
}

.registration-form-page {
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

.registration-form-logo {
  height: 32px;
  object-fit: contain;
}

.registration-form-header {
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 16px 0;
}

.registration-form-title {
  font-size: 28px;
  font-weight: 600;
  margin: 0;
  color: #000;
}

.registration-form-main {
  flex: 1;
  padding-top: 24px;
}

.registration-form {
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

.optional-tag {
  display: inline-block;
  padding: 2px 8px;
  background-color: #f5f5f5;
  border: 1px solid #ccc;
  border-radius: 2px;
  font-size: 11px;
  font-weight: normal;
  color: #666;
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

.nickname-input-wrapper {
  position: relative;
  display: flex;
  align-items: center;
}

.nickname-input-wrapper .input {
  padding-right: 60px;
}

.character-count {
  position: absolute;
  right: 12px;
  font-size: 12px;
  color: #999;
}

.hint {
  font-size: 11px;
  color: #777;
  margin: 0;
}

.notification-description {
  font-size: 12px;
  color: #666;
  margin: 0;
  line-height: 1.5;
}

.checkbox-wrapper {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-top: 4px;
}

.checkbox {
  width: 20px;
  height: 20px;
  cursor: pointer;
  accent-color: #007aff;
}

.checkbox-label {
  font-size: 14px;
  color: #333;
  cursor: pointer;
  margin: 0;
  user-select: none;
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
</style>

