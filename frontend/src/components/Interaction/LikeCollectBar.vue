<template>
  <div class="lc" :class="variantClass" role="group" aria-label="点赞与收藏">
    <span class="lc-live" aria-live="polite">{{ liveText }}</span>

    <div class="lc-item">
      <button
        class="lc-circle"
        type="button"
        :class="{ active: liked, loading: likePending, disabled: likeDisabled, fx: likePressFx || likePulseOn || likePulseOff }"
        :disabled="likeDisabled"
        :aria-pressed="liked"
        aria-label="点赞"
        data-testid="like-btn"
        @pointerdown="haptic()"
        @click="onLikeClick"
      >
        <span class="lc-ico" aria-hidden="true">
          <svg viewBox="0 0 24 24" class="ico" fill="none" xmlns="http://www.w3.org/2000/svg">
            <path
              d="M14.5 9V5.5c0-1.657-1.343-3-3-3L7 9v12h11.2c.858 0 1.6-.582 1.8-1.416l2-8A2 2 0 0 0 20.056 9H14.5Z"
              class="stroke fillable like"
              stroke-width="1.5"
              stroke-linejoin="round"
            />
            <path
              d="M7 9H4a2 2 0 0 0-2 2v8a2 2 0 0 0 2 2h3V9Z"
              class="stroke"
              stroke-width="1.5"
              stroke-linejoin="round"
            />
          </svg>
        </span>
        <span v-if="likePending" class="lc-spin" aria-hidden="true"></span>
      </button>
      <div class="lc-meta">
        <div class="count" :title="String(likes)">{{ likesText }}</div>
        <div class="label">点赞</div>
      </div>
    </div>

    <div class="lc-item">
      <button
        class="lc-circle"
        type="button"
        :class="{ active: collected, loading: collectPending, disabled: collectDisabled, fx: collectPressFx || collectPulseOn || collectPulseOff }"
        :disabled="collectDisabled"
        :aria-pressed="collected"
        aria-label="收藏"
        data-testid="collect-btn"
        @pointerdown="haptic()"
        @click="onCollectClick"
      >
        <span class="lc-ico" aria-hidden="true">
          <svg viewBox="0 0 24 24" class="ico" fill="none" xmlns="http://www.w3.org/2000/svg">
            <path
              d="M12 17.27 18.18 21l-1.64-7.03L22 9.24l-7.19-.61L12 2 9.19 8.63 2 9.24l5.46 4.73L5.82 21 12 17.27Z"
              class="stroke fillable favorite"
              stroke-width="1.5"
              stroke-linejoin="round"
            />
            <path
              d="M12 15.2l4.1 2.5-1.1-4.7 3.6-3.1-4.8-.4L12 5.1 10.2 9.5l-4.8.4 3.6 3.1-1.1 4.7L12 15.2Z"
              class="detail"
              stroke-width="1"
              stroke-linejoin="round"
            />
          </svg>
        </span>
        <span v-if="collectPending" class="lc-spin" aria-hidden="true"></span>
      </button>
      <div class="lc-meta">
        <div class="count" :title="String(collects)">{{ collectsText }}</div>
        <div class="label">收藏</div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed, ref, watch } from 'vue'
import { ElMessage } from 'element-plus'

const props = defineProps<{
  liked: boolean
  collected: boolean
  likes: number
  collects: number
  disabled?: boolean
  likeLoading?: boolean
  collectLoading?: boolean
  feedback?: boolean
  variant?: 'pill' | 'minimal'
}>()

const emit = defineEmits<{
  (e: 'toggle-like'): void
  (e: 'toggle-collect'): void
}>()

const localLikePending = ref(false)
const localCollectPending = ref(false)
const lastIntent = ref<'like' | 'collect' | null>(null)
const hydrated = ref(false)

const likeRotate = ref(false)
const likePulseOn = ref(false)
const likePulseOff = ref(false)
const collectPulseOn = ref(false)
const collectPulseOff = ref(false)
const likePressFx = ref(false)
const collectPressFx = ref(false)
const liveText = ref('')

const variantClass = computed(() => (props.variant === 'minimal' ? 'lc--circle lc--minimal' : 'lc--circle'))

const likePending = computed(() => Boolean(props.likeLoading) || localLikePending.value)
const collectPending = computed(() => Boolean(props.collectLoading) || localCollectPending.value)

const likeDisabled = computed(() => Boolean(props.disabled) || likePending.value)
const collectDisabled = computed(() => Boolean(props.disabled) || collectPending.value)

const formatCount = (n: number) => {
  if (!Number.isFinite(n)) return '0'
  if (n < 1000) return String(n)
  if (n < 10000) return `${(n / 1000).toFixed(1).replace(/\.0$/, '')}k`
  return `${(n / 10000).toFixed(1).replace(/\.0$/, '')}w`
}

const likesText = computed(() => formatCount(props.likes || 0))
const collectsText = computed(() => formatCount(props.collects || 0))

const haptic = () => {
  const prefersReduced = window.matchMedia?.('(prefers-reduced-motion: reduce)')?.matches
  if (prefersReduced) return
  const nav = navigator as any
  if (typeof nav?.vibrate === 'function') nav.vibrate(10)
}

const onLikeClick = async () => {
  if (likeDisabled.value) return
  lastIntent.value = 'like'
  likePulseOn.value = false
  likePulseOff.value = false
  likePressFx.value = false
  window.setTimeout(() => {
    likePressFx.value = true
  }, 0)
  window.setTimeout(() => {
    likePressFx.value = false
  }, 620)
  localLikePending.value = true
  try {
    emit('toggle-like')
  } finally {
    window.setTimeout(() => {
      localLikePending.value = false
    }, 300)
  }
}

const onCollectClick = async () => {
  if (collectDisabled.value) return
  lastIntent.value = 'collect'
  collectPulseOn.value = false
  collectPulseOff.value = false
  collectPressFx.value = false
  window.setTimeout(() => {
    collectPressFx.value = true
  }, 0)
  window.setTimeout(() => {
    collectPressFx.value = false
  }, 620)
  localCollectPending.value = true
  try {
    emit('toggle-collect')
  } finally {
    window.setTimeout(() => {
      localCollectPending.value = false
    }, 300)
  }
}

watch(
  () => [props.liked, props.collected] as const,
  ([nLiked, nCollected], [oLiked, oCollected]) => {
    if (!hydrated.value) {
      hydrated.value = true
      return
    }
    if (props.feedback === false) return

    if (lastIntent.value === 'like' && nLiked !== oLiked) {
      likeRotate.value = nLiked
      if (!nLiked) likePulseOff.value = true
      if (nLiked) likePulseOn.value = true

      liveText.value = nLiked ? '已点赞' : '已取消点赞'
      ElMessage.success(liveText.value)
      lastIntent.value = null
      window.setTimeout(() => {
        likeRotate.value = false
        likePulseOn.value = false
        likePulseOff.value = false
      }, nLiked ? 400 : 180)
      return
    }
    if (lastIntent.value === 'collect' && nCollected !== oCollected) {
      if (!nCollected) collectPulseOff.value = true
      if (nCollected) collectPulseOn.value = true

      liveText.value = nCollected ? '已收藏' : '已取消收藏'
      ElMessage.success(liveText.value)
      lastIntent.value = null
      window.setTimeout(() => {
        collectPulseOn.value = false
        collectPulseOff.value = false
      }, nCollected ? 400 : 180)
    }
  }
)
</script>

<style scoped lang="scss">
.lc {
  display: inline-flex;
  align-items: center;
  gap: 18px;
  justify-content: flex-start;
  max-inline-size: 100%;
}

.lc:dir(rtl) {
  flex-direction: row-reverse;
}

.lc-live {
  position: absolute;
  inline-size: 1px;
  block-size: 1px;
  padding: 0;
  margin: -1px;
  overflow: hidden;
  clip: rect(0, 0, 0, 0);
  white-space: nowrap;
  border: 0;
}

.lc-item {
  display: inline-flex;
  align-items: center;
  gap: 10px;
}

.lc-circle {
  position: relative;
  inline-size: 48px;
  block-size: 48px;
  border-radius: 999px;
  border: 0;
  background: radial-gradient(circle at 30% 30%, #FF3B30 0%, #FF6B6F 72%, #FF6B6F 100%);
  display: grid;
  place-items: center;
  cursor: pointer;
  transform: translateZ(0) scale(1);
  transition: transform 180ms ease-out, filter 200ms ease-out, opacity 200ms ease-out;
}

.lc-circle::before {
  content: '';
  position: absolute;
  inset: -8px;
  border-radius: 999px;
  box-shadow: 0 18px 34px rgba(15, 23, 42, 0.20);
  opacity: 0;
}

.lc-circle::after {
  content: '';
  position: absolute;
  inset: 0;
  border-radius: 999px;
  background: radial-gradient(circle at 50% 50%, rgba(255, 255, 255, 0.72), rgba(255, 255, 255, 0.0) 62%);
  opacity: 0;
  transform: scale(0.25);
}

.lc-circle:focus-visible {
  outline: 2px solid rgba(255, 59, 48, 0.45);
  outline-offset: 3px;
}

.lc-circle.disabled {
  opacity: 0.6;
  cursor: not-allowed;
}

.lc-circle:not(.disabled):active {
  transform: translateZ(0) scale(0.96);
}

.lc-circle.fx {
  animation: bounce 300ms cubic-bezier(0.68, -0.55, 0.27, 1.55);
}

.lc-circle.fx::after {
  animation: rippleFx 300ms ease-out;
}

.lc-circle.fx::before {
  opacity: 1;
  animation: shadowFx 600ms ease-out forwards;
}

.lc-ico {
  inline-size: 24px;
  block-size: 24px;
  display: grid;
  place-items: center;
}

.ico {
  inline-size: 24px;
  block-size: 24px;
}

.stroke {
  stroke: rgba(255, 255, 255, 0.88);
  stroke-width: 1.5;
}

.fillable {
  fill: transparent;
}

.detail {
  stroke: rgba(255, 255, 255, 0.66);
}

.lc-meta {
  display: grid;
  gap: 2px;
}

.lc-meta .count {
  font-size: 14px;
  font-weight: 700;
  color: var(--text);
  font-variant-numeric: tabular-nums;
  line-height: 1.1;
}

.lc-meta .label {
  font-size: 12px;
  color: var(--muted);
  font-weight: 500;
  line-height: 1.1;
}

.lc-circle.active {
  filter: saturate(1.08) brightness(1.02);
}

.lc--minimal .lc-meta .label {
  display: none;
}

.lc-spin {
  position: absolute;
  inline-size: 16px;
  block-size: 16px;
  border-radius: 999px;
  border: 2px solid rgba(255, 255, 255, 0.35);
  border-top-color: rgba(255, 255, 255, 0.92);
  animation: spin 0.8s linear infinite;
}

@keyframes spin {
  from { transform: rotate(0deg); }
  to { transform: rotate(360deg); }
}

@keyframes bounce {
  0% { transform: translateZ(0) scale(1); }
  55% { transform: translateZ(0) scale(1.12); }
  100% { transform: translateZ(0) scale(1); }
}

@keyframes rippleFx {
  0% { opacity: 0.55; transform: scale(0.25); }
  100% { opacity: 0; transform: scale(1.45); }
}

@keyframes shadowFx {
  0% { opacity: 1; }
  100% { opacity: 0; }
}

@media (max-width: 576px) {
  .lc {
    gap: 14px;
  }
}

@media (prefers-reduced-motion: reduce) {
  .lc-circle,
  .lc-circle::before,
  .lc-circle::after,
  .lc-spin {
    transition: none !important;
    animation: none !important;
  }
}
</style>
