NEW_FILE_CODE
<template>
  <div class="page-container">
    <n-card title="管理员管理" size="large">
      <template #header-extra>
        <n-button type="primary" @click="handleCreate">
          <template #icon>
            <Icon icon="ri:user-add-line" />
          </template>
          添加管理员
        </n-button>
      </template>

      <n-data-table
          :columns="columns"
          :data="tableData"
          :loading="loading"
          :pagination="pagination"
          striped
      />
    </n-card>
  </div>
</template>

<script setup lang="ts">
import { ref, h } from 'vue'
import { Icon } from '@iconify/vue'
import type { DataTableColumns } from 'naive-ui'
import { NButton, NTag, NSpace, NCard, NDataTable } from 'naive-ui'

interface AdminItem {
  id: number
  username: string
  phone: string
  roles: string[]
  status: number
  lastLoginTime: string
  createTime: string
}