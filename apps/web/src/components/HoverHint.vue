<template>
  <span
    class="hover-hint"
    @mouseenter="visible = true"
    @mouseleave="visible = false"
    @focusin="visible = true"
    @focusout="visible = false"
  >
    <slot />
    <Transition name="hover-hint-fade">
      <span v-if="visible" class="hover-hint__bubble" role="tooltip">
        {{ text }}
      </span>
    </Transition>
  </span>
</template>

<script setup lang="ts">
import { ref } from 'vue'

defineProps<{
  text: string
}>()

const visible = ref(false)
</script>

<style scoped>
.hover-hint {
  position: relative;
  display: inline-flex;
  align-items: center;
}

.hover-hint__bubble {
  position: absolute;
  left: 50%;
  bottom: calc(100% + 12px);
  transform: translateX(-50%);
  min-width: max-content;
  max-width: 280px;
  padding: 9px 12px;
  border-radius: 12px;
  background: linear-gradient(135deg, #fff8f9 0%, #ffecef 52%, #fff4f6 100%);
  border: 1px solid rgba(230, 0, 51, 0.16);
  color: #4b5563;
  font-size: 12px;
  line-height: 1.5;
  white-space: normal;
  box-shadow:
    0 14px 32px rgba(230, 0, 51, 0.12),
    0 4px 12px rgba(15, 23, 42, 0.06);
  z-index: 1200;
  pointer-events: none;
}

.hover-hint__bubble::before {
  content: '';
  position: absolute;
  inset: 0;
  border-radius: inherit;
  background: linear-gradient(180deg, rgba(230, 0, 51, 0.08) 0%, rgba(255, 255, 255, 0) 42%);
  pointer-events: none;
}

.hover-hint__bubble::after {
  content: '';
  position: absolute;
  left: 50%;
  top: calc(100% - 5px);
  width: 10px;
  height: 10px;
  transform: translateX(-50%) rotate(45deg);
  background: linear-gradient(135deg, #fff1f4 0%, #ffe6eb 100%);
  border-right: 1px solid rgba(230, 0, 51, 0.16);
  border-bottom: 1px solid rgba(230, 0, 51, 0.16);
  box-sizing: border-box;
}

.hover-hint-fade-enter-active,
.hover-hint-fade-leave-active {
  transition: opacity 0.16s ease, transform 0.16s ease;
}

.hover-hint-fade-enter-from,
.hover-hint-fade-leave-to {
  opacity: 0;
  transform: translateX(-50%) translateY(4px);
}
</style>
