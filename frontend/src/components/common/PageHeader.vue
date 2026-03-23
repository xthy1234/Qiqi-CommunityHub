<template>
  <div
    v-if="showHeader"
    class="page-header"
  >
    <div class="header-left">
      <h2 class="page-title">
        {{ title }}
      </h2>
      <div
        v-if="showBack"
        class="back-btn"
        @click="handleBack"
      >
        <Icon
          icon="ri:arrow-left-s-line"
          style="margin-right: 4px;"
        />
        {{ backText || '返回' }}
      </div>
    </div>
    <div
      v-if="$slots.extra"
      class="header-extra"
    >
      <slot name="extra" />
    </div>
  </div>
</template>

<script setup lang="ts">
import { useRouter } from 'vue-router'
import { Icon } from '@iconify/vue'

const props = withDefaults(defineProps<{
  title: string
  showBack?: boolean
  showHeader?: boolean
  backText?: string
  backPath?: string
}>(), {
  showBack: true,
  showHeader: true,
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
.page-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 24px;
  padding-bottom: 16px;
  border-bottom: 1px solid #ebeef5;

  .header-left {
    display: flex;
    flex-direction: column;
    gap: 8px;
    flex: 1;

    .page-title {
      font-size: 24px;
      font-weight: 600;
      color: #303133;
      margin: 0;
      line-height: 1.4;
    }

    .back-btn {
      display: inline-flex;
      align-items: center;
      font-size: 14px;
      color: #606266;
      cursor: pointer;
      transition: all 0.3s;
      padding: 6px 12px;
      border-radius: 6px;

      &:hover {
        background: #f5f7fa;
        color: #409eff;
      }
    }
  }

  .header-extra {
    display: flex;
    align-items: center;
    gap: 12px;
  }
}

@media (max-width: 768px) {
  .page-header {
    flex-direction: column;
    align-items: flex-start;
    gap: 12px;

    .header-left {
      width: 100%;
    }

    .header-extra {
      width: 100%;
      justify-content: flex-end;
    }
  }
}
</style>
