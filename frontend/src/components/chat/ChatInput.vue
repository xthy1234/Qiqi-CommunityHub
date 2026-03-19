<template>
  <div class="chat-input-container">
    <div class="input-toolbar">
      <n-tooltip trigger="hover">
        <template #trigger>
          <n-button text @click="handleUpload">
            <template #icon>
              <n-icon size="20">
                <svg xmlns="http://www.w3.org/2000/svg" width="20" height="20" viewBox="0 0 24 24">
                  <path fill="currentColor" d="M16.5 6v11.5c0 2.21-1.79 4-4 4s-4-1.79-4-4V5a2.5 2.5 0 0 1 5 0v10.5c0 .55-.45 1-1 1s-1-.45-1-1V6H10v9.5a2.5 2.5 0 0 0 5 0V5c0-2.21-1.79-4-4-4S7 2.79 7 5v12.5c0 3.04 2.46 5.5 5.5 5.5s5.5-2.46 5.5-5.5V6h-1.5z"/>
                </svg>
              </n-icon>
            </template>
          </n-button>
        </template>
        发送文件
      </n-tooltip>
      
      <n-tooltip trigger="hover">
        <template #trigger>
          <n-button text @click="insertEmoji">
            <template #icon>
              <n-icon size="20">
                <svg xmlns="http://www.w3.org/2000/svg" width="20" height="20" viewBox="0 0 24 24">
                  <path fill="currentColor" d="M11.99 2C6.47 2 2 6.48 2 12s4.47 10 9.99 10C17.52 22 22 17.52 22 12S17.52 2 11.99 2zM12 20c-4.42 0-8-3.58-8-8s3.58-8 8-8s8 3.58 8 8s-3.58 8-8 8zm3.5-9c.83 0 1.5-.67 1.5-1.5S16.33 8 15.5 8S14 8.67 14 9.5s.67 1.5 1.5 1.5zm-7 0c.83 0 1.5-.67 1.5-1.5S9.33 8 8.5 8S7 8.67 7 9.5S7.67 11 8.5 11zm3.5 6.5c2.33 0 4.31-1.46 5.11-3.5H6.89c.8 2.04 2.78 3.5 5.11 3.5z"/>
                </svg>
              </n-icon>
            </template>
          </n-button>
        </template>
        表情
      </n-tooltip>
    </div>
    
    <div class="input-wrapper">
      <n-input
        ref="inputRef"
        v-model:value="inputValue"
        type="textarea"
        placeholder="输入消息... (Enter 发送，Shift+Enter 换行)"
        :autosize="{ minRows: 2, maxRows: 5 }"
        @keydown="handleKeyDown"
      />
    </div>
    
    <div class="input-footer">
      <n-button 
        type="primary" 
        :disabled="!inputValue.trim()" 
        @click="handleSend"
      >
        发送
      </n-button>
    </div>
    
    <!-- 文件上传 -->
    <input
      ref="fileInputRef"
      type="file"
      style="display: none"
      @change="handleFileChange"
    />
  </div>
</template>

<script setup lang="ts">
import { ref } from 'vue'
import { useGlobalProperties } from '@/utils/globalProperties'
import { NButton, NIcon, NTooltip, NInput } from 'naive-ui'

const appContext = useGlobalProperties()
const inputRef = ref<any>(null)
const fileInputRef = ref<HTMLInputElement | null>(null)
const inputValue = ref<string>('')

const emit = defineEmits<{
  send: [content: string]
}>()

const handleKeyDown = (e: KeyboardEvent) => {
  if (e.key === 'Enter' && !e.shiftKey) {
    e.preventDefault()
    handleSend()
  }
}

const handleSend = () => {
  if (!inputValue.value.trim()) return
  
  emit('send', inputValue.value.trim())
  inputValue.value = ''
}

const handleUpload = () => {
  fileInputRef.value?.click()
}

const handleFileChange = (e: Event) => {
  const target = e.target as HTMLInputElement
  const file = target.files?.[0]
  if (file) {

    // TODO: 实现文件上传逻辑
  }
  target.value = ''
}

const insertEmoji = () => {

  // TODO: 实现表情选择器
}
</script>

<style scoped lang="scss">
.chat-input-container {
  padding: 16px 20px;
  background: #fff;
  border-top: 1px solid #f0f0f0;
}

.input-toolbar {
  display: flex;
  gap: 8px;
  margin-bottom: 12px;
}

.input-wrapper {
  margin-bottom: 12px;
}

.input-footer {
  display: flex;
  justify-content: flex-end;
}
</style>
