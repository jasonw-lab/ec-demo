import { reactive, nextTick } from 'vue'
import type { Router } from 'vue-router'

const STORAGE_COMPLETED = 'ec-demo.product-tour.completed'
const STORAGE_STEP = 'ec-demo.product-tour.step'
const STORAGE_ACTIVE = 'ec-demo.product-tour.active'

interface StepDef {
  index: number
  route: string
  routeMatch: (path: string) => boolean
  element: string
  title: string
  description: string
}

const STEPS: StepDef[] = [
  {
    index: 0,
    route: '/login',
    routeMatch: (p) => p === '/login',
    element: '[data-tour="login-google"]',
    title: 'ログイン',
    description: 'まずはログインしてください。デモでは Google ログインの利用を推奨します。メールアドレスまたは電話番号でのログインも利用できます。',
  },
  {
    index: 1,
    route: '/',
    routeMatch: (p) => p === '/',
    element: '[data-tour="search-form"]',
    title: '商品検索バー',
    description: '商品検索から購入までの流れを案内します。まずは検索バーの使い方を確認しましょう。',
  },
  {
    index: 2,
    route: '/',
    routeMatch: (p) => p === '/',
    element: '[data-tour="search-input"]',
    title: '検索キーワード入力',
    description: '例として「iphone」を検索してみましょう。「次へ」を押すと iphone の検索結果画面へ移動します。',
  },
  {
    index: 3,
    route: '/search',
    routeMatch: (p) => p === '/search',
    element: '[data-tour="add-to-cart-primary"]',
    title: '検索結果からカート追加',
    description: '検索結果から商品を選び、カートに追加します。まずは最初の商品で流れを確認します。',
  },
  {
    index: 4,
    route: '/cart',
    routeMatch: (p) => p === '/cart',
    element: '[data-tour="cart-items"]',
    title: 'カート確認',
    description: '追加した商品はカートで確認できます。このツアーではここまでを対象とし、購入前の主要導線を確認して完了します。',
  },
]

// Module-level singleton state
const state = reactive({
  active: false,
  currentStep: 0,
})

// Step index → action executed on "次へ" before advancing
const _stepActions = new Map<number, () => void | Promise<void>>()

let _router: Router | null = null
// eslint-disable-next-line @typescript-eslint/no-explicit-any
let _driverObj: any = null
let _advancing = false
let _highlighting = false

function destroyDriver() {
  if (_driverObj) {
    _advancing = true
    try {
      _driverObj.destroy()
    } catch (_) {
      // ignore
    }
    _driverObj = null
    _advancing = false
  }
}

async function waitForElement(selector: string, maxRetries = 15, intervalMs = 200): Promise<Element | null> {
  for (let i = 0; i < maxRetries; i++) {
    const el = document.querySelector(selector)
    if (el) return el
    await new Promise<void>((resolve) => setTimeout(resolve, intervalMs))
  }
  return null
}

async function highlightStep(step: StepDef) {
  if (_highlighting) return
  _highlighting = true

  destroyDriver()
  await nextTick()

  const el = await waitForElement(step.element)
  if (!el) {
    console.warn(`[ProductTour] Element not found: ${step.element}`)
    _highlighting = false
    return
  }

  const { driver } = await import('driver.js')

  const isFirst = step.index === 0 || (step.index === 1 && state.currentStep === 1)
  const isLast = step.index === STEPS.length - 1

  const showButtons: string[] = ['close']
  if (!isFirst) showButtons.unshift('previous')
  showButtons.push('next')

  _driverObj = driver({
    overlayOpacity: 0.6,
    smoothScroll: true,
    allowClose: false,
    nextBtnText: isLast ? '完了' : '次へ',
    prevBtnText: '戻る',
    doneBtnText: '完了',
    popoverClass: 'product-tour-popover',
    onNextClick() {
      _highlighting = false
      handleNext()
    },
    onPrevClick() {
      _highlighting = false
      handlePrev()
    },
    onCloseClick() {
      _highlighting = false
      _advancing = false
      _driverObj = null
      skipTour()
    },
    onDestroyStarted() {
      if (!_advancing) {
        _highlighting = false
        skipTour()
      }
    },
  })

  _driverObj.highlight({
    element: step.element,
    popover: {
      title: `${step.index + 1} / ${STEPS.length} · ${step.title}`,
      description: step.description,
      showButtons,
    },
  })

  _highlighting = false
}

async function handleNext() {
  const currentIndex = state.currentStep

  // Step 2 (index 2): auto-fill iphone and navigate to search
  if (currentIndex === 2) {
    destroyDriver()
    state.currentStep = 3
    localStorage.setItem(STORAGE_STEP, '3')
    await _router?.push({ path: '/search', query: { q: 'iphone' } })
    return
  }

  // Execute registered step action (e.g. auto-add first product to cart on step 3)
  const action = _stepActions.get(currentIndex)
  if (action) await action()

  const nextIndex = currentIndex + 1
  if (nextIndex >= STEPS.length) {
    destroyDriver()
    completeTour()
    return
  }

  destroyDriver()
  state.currentStep = nextIndex
  localStorage.setItem(STORAGE_STEP, String(nextIndex))

  const next = STEPS[nextIndex]
  const currentPath = _router?.currentRoute.value.path ?? ''

  if (next.routeMatch(currentPath)) {
    await highlightStep(next)
  } else {
    await _router?.push(next.route)
    // Route watcher in App.vue will call resumeTour
  }
}

async function handlePrev() {
  const prevIndex = state.currentStep - 1
  if (prevIndex < 0) return

  destroyDriver()
  state.currentStep = prevIndex
  localStorage.setItem(STORAGE_STEP, String(prevIndex))

  const prev = STEPS[prevIndex]
  const currentPath = _router?.currentRoute.value.path ?? ''

  if (prev.routeMatch(currentPath)) {
    await highlightStep(prev)
  } else {
    await _router?.push(prev.route)
  }
}

function skipTour() {
  if (!state.active) return
  state.active = false
  destroyDriver()
  _stepActions.clear()
  localStorage.removeItem(STORAGE_ACTIVE)
  localStorage.removeItem(STORAGE_STEP)
}

function completeTour() {
  state.active = false
  _stepActions.clear()
  localStorage.setItem(STORAGE_COMPLETED, 'true')
  localStorage.removeItem(STORAGE_ACTIVE)
  localStorage.removeItem(STORAGE_STEP)
}

export function useProductTour() {
  function isCompleted(): boolean {
    return localStorage.getItem(STORAGE_COMPLETED) === 'true'
  }

  function shouldAutoStart(): boolean {
    return !isCompleted() && localStorage.getItem(STORAGE_ACTIVE) !== 'true'
  }

  function setRouter(r: Router) {
    _router = r
  }

  async function startTour(isLoggedIn: boolean) {
    if (!_router) return
    state.active = true
    const startIndex = isLoggedIn ? 1 : 0
    state.currentStep = startIndex
    localStorage.setItem(STORAGE_ACTIVE, 'true')
    localStorage.setItem(STORAGE_STEP, String(startIndex))

    const step = STEPS[startIndex]
    const currentPath = _router.currentRoute.value.path

    if (step.routeMatch(currentPath)) {
      await nextTick()
      await highlightStep(step)
    } else {
      await _router.push(step.route)
    }
  }

  async function resumeTour(path: string) {
    // Restore state from localStorage if not active in memory
    if (!state.active) {
      if (localStorage.getItem(STORAGE_ACTIVE) !== 'true') return
      state.active = true
      const stored = parseInt(localStorage.getItem(STORAGE_STEP) ?? '0', 10)
      state.currentStep = isNaN(stored) ? 0 : stored
    }

    const step = STEPS[state.currentStep]
    if (!step) return

    if (step.routeMatch(path)) {
      await nextTick()
      await highlightStep(step)
      return
    }

    // Auto-advance when route change signals step completion
    // (e.g., login success → /, user searched → /search)
    const nextIndex = state.currentStep + 1
    if (nextIndex < STEPS.length) {
      const nextStep = STEPS[nextIndex]
      if (nextStep.routeMatch(path)) {
        state.currentStep = nextIndex
        localStorage.setItem(STORAGE_STEP, String(nextIndex))
        await nextTick()
        await highlightStep(nextStep)
      }
    }
  }

  function restartTour(isLoggedIn: boolean) {
    localStorage.removeItem(STORAGE_COMPLETED)
    startTour(isLoggedIn)
  }

  function registerStepAction(stepIndex: number, fn: () => void | Promise<void>) {
    _stepActions.set(stepIndex, fn)
  }

  return {
    state,
    startTour,
    resumeTour,
    restartTour,
    skipTour,
    isCompleted,
    shouldAutoStart,
    setRouter,
    registerStepAction,
  }
}
