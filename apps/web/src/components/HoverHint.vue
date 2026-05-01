<template>
  <span
    ref="triggerRef"
    class="hover-hint"
    @mouseenter="show"
    @mouseleave="hide"
    @focusin="show"
    @focusout="hide"
  >
    <slot />
  </span>

  <Teleport to="body">
    <Transition name="hover-hint-fade">
      <span
        v-if="visible"
        ref="bubbleRef"
        class="hover-hint__bubble"
        :class="`hover-hint__bubble--${placement}`"
        :style="bubbleStyle"
        role="tooltip"
      >
        {{ text }}
      </span>
    </Transition>
  </Teleport>
</template>

<script setup lang="ts">
import { computed, nextTick, onBeforeUnmount, onMounted, ref } from 'vue'

defineProps<{
  text: string
}>()

const triggerRef = ref<HTMLElement | null>(null)
const bubbleRef = ref<HTMLElement | null>(null)
const visible = ref(false)
const placement = ref<'top' | 'bottom'>('top')
const top = ref(0)
const left = ref(0)
const arrowLeft = ref(24)

const VIEWPORT_PADDING = 12
const TOOLTIP_OFFSET = 12

const bubbleStyle = computed(() => ({
  top: `${top.value}px`,
  left: `${left.value}px`,
  '--hover-hint-arrow-left': `${arrowLeft.value}px`,
}))

function clamp(value: number, min: number, max: number): number {
  return Math.min(Math.max(value, min), max)
}

async function updatePosition(): Promise<void> {
  await nextTick()

  const trigger = triggerRef.value
  const bubble = bubbleRef.value
  if (!trigger || !bubble) return

  const triggerRect = trigger.getBoundingClientRect()
  const bubbleRect = bubble.getBoundingClientRect()
  const viewportWidth = window.innerWidth
  const viewportHeight = window.innerHeight

  let nextLeft = triggerRect.left + triggerRect.width / 2 - bubbleRect.width / 2
  nextLeft = clamp(nextLeft, VIEWPORT_PADDING, viewportWidth - bubbleRect.width - VIEWPORT_PADDING)

  const showAbove = triggerRect.top >= bubbleRect.height + TOOLTIP_OFFSET + VIEWPORT_PADDING
  placement.value = showAbove ? 'top' : 'bottom'

  top.value = showAbove
    ? triggerRect.top - bubbleRect.height - TOOLTIP_OFFSET
    : Math.min(
        triggerRect.bottom + TOOLTIP_OFFSET,
        viewportHeight - bubbleRect.height - VIEWPORT_PADDING
      )

  left.value = nextLeft

  const triggerCenter = triggerRect.left + triggerRect.width / 2
  arrowLeft.value = clamp(triggerCenter - nextLeft, 16, bubbleRect.width - 16)
}

function handleViewportChange(): void {
  if (visible.value) {
    void updatePosition()
  }
}

async function show(): Promise<void> {
  visible.value = true
  await updatePosition()
}

function hide(): void {
  visible.value = false
}

onMounted(() => {
  window.addEventListener('resize', handleViewportChange)
  window.addEventListener('scroll', handleViewportChange, true)
})

onBeforeUnmount(() => {
  window.removeEventListener('resize', handleViewportChange)
  window.removeEventListener('scroll', handleViewportChange, true)
})
</script>

<style scoped>
.hover-hint {
  position: relative;
  display: inline-flex;
  align-items: center;
}

.hover-hint__bubble {
  position: fixed;
  max-width: min(360px, calc(100vw - 24px));
  padding: 9px 12px;
  border-radius: 12px;
  background: linear-gradient(135deg, #fff8f9 0%, #ffecef 52%, #fff4f6 100%);
  border: 1px solid rgba(230, 0, 51, 0.16);
  color: #4b5563;
  font-size: 12px;
  line-height: 1.6;
  white-space: normal;
  word-break: break-word;
  box-shadow:
    0 14px 32px rgba(230, 0, 51, 0.12),
    0 4px 12px rgba(15, 23, 42, 0.06);
  z-index: 4000;
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
  left: var(--hover-hint-arrow-left);
  width: 10px;
  height: 10px;
  background: linear-gradient(135deg, #fff1f4 0%, #ffe6eb 100%);
  box-sizing: border-box;
}

.hover-hint__bubble--top::after {
  top: calc(100% - 5px);
  transform: translateX(-50%) rotate(45deg);
  border-right: 1px solid rgba(230, 0, 51, 0.16);
  border-bottom: 1px solid rgba(230, 0, 51, 0.16);
}

.hover-hint__bubble--bottom::after {
  bottom: calc(100% - 5px);
  transform: translateX(-50%) rotate(45deg);
  border-top: 1px solid rgba(230, 0, 51, 0.16);
  border-left: 1px solid rgba(230, 0, 51, 0.16);
}

.hover-hint-fade-enter-active,
.hover-hint-fade-leave-active {
  transition: opacity 0.16s ease, transform 0.16s ease;
}

.hover-hint-fade-enter-from,
.hover-hint-fade-leave-to {
  opacity: 0;
  transform: translateY(4px);
}
</style>
