<template>
  <div class="page-container">
    <!-- 页面头部 -->
    <PageHeader
      v-if="showHeader"
      :title="headerTitle"
      :show-back="showBack"
      :back-text="backText"
      :back-path="backPath"
      @back="handleBack"
    >
      <template v-if="$slots.headerExtra" #extra>
        <slot name="headerExtra"></slot>
      </template>
    </PageHeader>

    <!-- 主内容区域 -->
    <div class="page-content">
      <slot></slot>
    </div>
  </div>
</template>

<script setup lang="ts">
import { useRouter } from 'vue-router'
import PageHeader from './PageHeader.vue'

const props = withDefaults(defineProps<{
  headerTitle?: string
  showHeader?: boolean
  showBack?: boolean
  backText?: string
  backPath?: string
}>(), {
  showHeader: true,
  showBack: true,
  backText: '返回'
})

const emit = defineEmits<{
  (e: 'back'): void
}>()

const router = useRouter()

const handleBack = () => {
  emit('back')
  if (props.backPath) {
    router.push(props.backPath)
  } else {
    router.back()
  }
}
</script>

<style lang="scss" scoped>
.page-container {
  min-height: calc(100vh - 60px);
  background: #f5f7fa;
  padding: 24px;
  
  .page-content {
    max-width: 1200px;
    margin: 0 auto;
    background: #fff;
    border-radius: 12px;
    box-shadow: 0 2px 12px rgba(0, 0, 0, 0.08);
    padding: 30px;
    
    @media (max-width: 768px) {
      padding: 20px;
      border-radius: 8px;
    }
  }
}
</style>
