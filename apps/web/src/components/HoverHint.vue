<template>
  <span
    class="hover-hint"
    @mouseenter="visible = true"
    @mouseleave="visible = false"
    @focusin="visible = true"
    @focusout="visible = false"
  >
    <slot />
    <span v-if="visible" class="hover-hint__bubble" role="tooltip">
      {{ text }}
    </span>
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
  bottom: calc(100% + 8px);
  transform: translateX(-50%);
  min-width: max-content;
  max-width: 280px;
  padding: 8px 10px;
  border-radius: 8px;
  background: rgba(17, 24, 39, 0.94);
  color: #fff;
  font-size: 12px;
  line-height: 1.5;
  white-space: normal;
  box-shadow: 0 10px 24px rgba(15, 23, 42, 0.18);
  z-index: 1200;
  pointer-events: none;
}

.hover-hint__bubble::after {
  content: '';
  position: absolute;
  left: 50%;
  top: 100%;
  transform: translateX(-50%);
  border-width: 6px;
  border-style: solid;
  border-color: rgba(17, 24, 39, 0.94) transparent transparent transparent;
}
</style>
