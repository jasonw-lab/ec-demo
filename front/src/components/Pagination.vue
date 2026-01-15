<template>
  <div v-if="totalPages > 1" style="margin-top:32px;display:flex;justify-content:center;align-items:center;gap:8px;">
    <button
      @click="emitChange(page - 1)"
      :disabled="page === 0"
      style="padding:8px 16px;border:1px solid #e5e7eb;border-radius:8px;background:#fff;cursor:pointer;font-size:14px;font-weight:600;transition:all 0.2s;"
      :style="{ opacity: page === 0 ? 0.5 : 1, cursor: page === 0 ? 'not-allowed' : 'pointer' }">
      前へ
    </button>

    <button
      v-for="pageNum in visiblePages"
      :key="pageNum"
      @click="emitChange(pageNum)"
      :style="{
        padding: '8px 12px',
        border: '1px solid #e5e7eb',
        borderRadius: '8px',
        background: pageNum === page ? '#ff6b6b' : '#fff',
        color: pageNum === page ? '#fff' : '#111827',
        cursor: 'pointer',
        fontSize: '14px',
        fontWeight: '600',
        transition: 'all 0.2s'
      }">
      {{ pageNum + 1 }}
    </button>

    <button
      @click="emitChange(page + 1)"
      :disabled="page >= totalPages - 1"
      style="padding:8px 16px;border:1px solid #e5e7eb;border-radius:8px;background:#fff;cursor:pointer;font-size:14px;font-weight:600;transition:all 0.2s;"
      :style="{ opacity: page >= totalPages - 1 ? 0.5 : 1, cursor: page >= totalPages - 1 ? 'not-allowed' : 'pointer' }">
      次へ
    </button>

    <div v-if="showSummary" style="margin-left:16px;color:#6b7280;font-size:14px;">
      {{ page + 1 }} / {{ totalPages }} ページ
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed } from 'vue'

const props = withDefaults(defineProps<{
  page: number
  totalPages: number
  maxVisible?: number
  showSummary?: boolean
}>(), {
  maxVisible: 5,
  showSummary: true,
})

const emit = defineEmits<{
  (event: 'change', page: number): void
}>()

const visiblePages = computed(() => {
  const total = props.totalPages
  const current = props.page
  const maxVisible = props.maxVisible

  if (total <= maxVisible) {
    return Array.from({ length: total }, (_, i) => i)
  }

  let start = Math.max(0, current - Math.floor(maxVisible / 2))
  let end = Math.min(total, start + maxVisible)

  if (end === total) {
    start = Math.max(0, end - maxVisible)
  }

  return Array.from({ length: end - start }, (_, i) => start + i)
})

function emitChange(newPage: number): void {
  if (newPage < 0 || newPage >= props.totalPages) return
  emit('change', newPage)
}
</script>
