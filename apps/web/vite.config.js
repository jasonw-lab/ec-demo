import { defineConfig, loadEnv } from 'vite'
import vue from '@vitejs/plugin-vue'
import { fileURLToPath, URL } from 'node:url'

const pathSrc = fileURLToPath(new URL('./src', import.meta.url))

export default defineConfig(({ mode }) => {
  const env = loadEnv(mode, process.cwd())

  return {
    base: '/ec-demo/',
    resolve: {
      alias: {
        "@": pathSrc,
      },
    },
    plugins: [vue()],
    server: {
      host: '0.0.0.0',
      port: +env.VITE_APP_PORT || 5173,
      open: true,
      headers: {
        'Cross-Origin-Opener-Policy': 'same-origin-allow-popups',
      },
      proxy: {
        [env.VITE_APP_BASE_API || '/api']: {
          changeOrigin: true,
          target: env.VITE_APP_API_URL || 'http://localhost:8080',
          // Don't rewrite the path - keep /api prefix so backend receives /api/auth/login
        },
      },
    },
  }
})
