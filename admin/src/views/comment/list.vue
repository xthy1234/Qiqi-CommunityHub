NEW_FILE_CODE
<template>
  <div class="page-container">
    <NCard title="评论管理" size="large">
      <NDataTable
          :columns="columns"
          :data="tableData"
          :loading="loading"
          :pagination="pagination"
          striped
      />
    </NCard>
  </div>
</template>

<script setup lang="ts">
import { ref, h } from 'vue'
import type { DataTableColumns } from 'naive-ui'
import { NButton, NTag, NSpace } from 'naive-ui'

interface CommentItem {
  id: number
  username: string
  content: string
  articleTitle: string
  status: number
  createTime: string
}

const loading = ref(false)
const tableData = ref<CommentItem[]>([])

const columns: DataTableColumns = [
  { title: 'ID', key: 'id', width: 80 },
  { title: '用户', key: 'username', width: 150 },
  { title: '评论内容', key: 'content', width: 300, ellipsis: { tooltip: true } },
  { title: '所属文章', key: 'articleTitle', width: 200, ellipsis: { tooltip: true } },
  {
    title: '状态',
    key: 'status',
    width: 100,
    render: (row) => h(NTag, {
      type: row.status === 1 ? 'success' : 'warning',
      content: row.status === 1 ? '已发布' : '待审核'
    })
  },
  { title: '创建时间', key: 'createTime', width: 180 },
  {
    title: '操作',
    key: 'actions',
    width: 200,
    fixed: 'right',
    render: (row) => h(NSpace, {}, {
      default: () => [
        h(NButton, { size: 'small', type: 'primary', text: true, onClick: () => handleAudit(row), content: '审核' }),
        h(NButton, { size: 'small', type: 'error', text: true, onClick: () => handleDelete(row), content: '删除' })
      ]
    })
  }
]

const pagination = {
  page: 1,
  pageSize: 10,
  showSizePicker: true,
  pageSizes: [10, 20, 50],
  onChange: (page: number) => { pagination.page = page; loadData() },
  onUpdatePageSize: (pageSize: number) => { pagination.pageSize = pageSize; pagination.page = 1; loadData() }
}

const loadData = () => {
  loading.value = true
  setTimeout(() => {
    tableData.value = [
      { id: 1, username: 'user1', content: '很好的文章', articleTitle: '游戏攻略', status: 1, createTime: '2024-01-01 12:00:00' }
    ]
    loading.value = false
  }, 500)
}

const handleAudit = (row: CommentItem) => {}
const handleDelete = (row: CommentItem) => {}

loadData()
</script>

<style lang="scss" scoped>
.page-container { padding: 0; }
</style>
