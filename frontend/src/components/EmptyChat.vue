<template>
  <div class="empty-chat">
    <div class="empty-state">
      <n-icon size="100" color="#dcdfe6">
        <svg xmlns="http://www.w3.org/2000/svg" width="100" height="100" viewBox="0 0 24 24">
          <path fill="currentColor" d="M20 2H4c-1.1 0-2 .9-2 2v18l4-4h14c1.1 0 2-.9 2-2V4c0-1.1-.9-2-2-2zm0 14H5.17L4 17.17V4h16v12z"/>
        </svg>
      </n-icon>
      <div class="empty-title-large">选择一个会话开始聊天</div>
      <div class="empty-description-large">
        左侧列表选择联系人，或等待新消息<br />
        与好友分享生活点滴，畅聊有趣话题
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { NIcon } from 'naive-ui'
</script>

<style scoped lang="scss">
.empty-chat {
  height: 100%;
  display: flex;
  align-items: center;
  justify-content: center;

  .empty-state {
    display: flex;
    flex-direction: column;
    align-items: center;
    justify-content: center;
    padding: 40px;
    text-align: center;

    .empty-title-large {
      margin-top: 24px;
      font-size: 18px;
      font-weight: 600;
      color: #606266;
    }

    .empty-description-large {
      margin-top: 16px;
      font-size: 14px;
      color: #909399;
      line-height: 1.8;
      max-width: 400px;
    }
  }
}
</style>
