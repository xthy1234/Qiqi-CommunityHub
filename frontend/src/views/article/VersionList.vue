<template>
  <PageContainer
    header-title="版本历史"
    :show-back="true"
    @back="goBack"
  >
    <!-- 加载状态 -->
    <div
      v-if="loading"
      class="loading-container"
    >
      <n-skeleton
        text
        :repeat="8"
      />
    </div>

    <!-- 空状态 -->
    <div
      v-else-if="versions.length === 0"
      class="empty-container"
    >
      <n-empty 
        description="暂无版本记录"
        size="large"
      >
        <template #extra>
          <n-button
            type="primary"
            @click="goBack"
          >
            返回首页
          </n-button>
        </template>
      </n-empty>
    </div>

    <!-- 版本列表 -->
    <div
      v-else
      class="version-list"
    >
      <!-- 头部操作栏 -->
      <div class="header-actions">
        <n-space>
          <n-button
            type="primary"
            :disabled="selectedVersions.length !== 2"
            @click="openCompareDialog"
          >
            <template #icon>
              <Icon icon="ri:git-compare-line" />
            </template>
            对比选中版本
          </n-button>
          
          <n-checkbox
            :indeterminate="selectedVersions.length > 0 && selectedVersions.length < versions.length"
            :checked="selectedVersions.length === versions.length"
            @update:checked="handleSelectAll"
          >
            全选
          </n-checkbox>
        </n-space>

        <n-input
          v-model:value="searchKeyword"
          placeholder="搜索版本号或修改摘要"
          clearable
          style="width: 300px;"
          @input="handleSearch"
        >
          <template #prefix>
            <Icon icon="ri:search-line" />
          </template>
        </n-input>
      </div>

      <!-- 版本表格 -->
      <n-data-table
        :columns="columns"
        :data="filteredVersions"
        :row-key="(row :any) => row.id"
        :pagination="paginationConfig"
        :single-line="false"
        striped
      />
    </div>

    <!-- 版本对比对话框 -->
    <n-modal
      v-model:show="compareModalVisible"
      preset="dialog"
      title="版本对比"
      :style="{ width: '1200px', maxWidth: '95vw' }"
      :closable="true"
    >
      <div class="compare-content">
        <DiffViewer
          :source-content="compareData.sourceContent"
          :target-content="compareData.targetContent"
          :source-label="`版本 ${compareData.sourceVersion}`"
          :target-label="`版本 ${compareData.targetVersion}`"
          :source-time="compareData.sourceTime"
          :target-time="compareData.targetTime"
          source-title="源版本内容"
          target-title="目标版本内容"
          :show-stats="true"
        />
      </div>
      
      <template #action>
        <n-button @click="compareModalVisible = false">
          关闭
        </n-button>
      </template>
    </n-modal>

    <!-- 回滚确认对话框 -->
    <n-modal
      v-model:show="rollbackModalVisible"
      preset="dialog"
      title="版本回滚"
      :closable="true"
    >
      <n-alert
        type="warning"
        :title="`确认回滚到版本 ${targetRollbackVersion}?`"
        style="margin-bottom: 16px;"
      >
        <p>回滚后将创建一个新的版本，当前内容将被覆盖。</p>
        <p style="margin-top: 8px; color: #f0a020;">此操作不可逆，请谨慎操作！</p>
      </n-alert>
      
      <template #action>
        <n-space justify="end">
          <n-button @click="rollbackModalVisible = false">
            取消
          </n-button>
          <n-button
            type="error"
            :loading="rollingBack"
            @click="confirmRollback"
          >
            确认回滚
          </n-button>
        </n-space>
      </template>
    </n-modal>
  </PageContainer>
</template>

<script setup lang="ts">
import { ref, reactive, computed, h, onMounted } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { useMessage, useDialog, NTag, NButton, NCheckbox, NDataTable } from 'naive-ui'
import { Icon } from '@iconify/vue'
import PageContainer from '@/components/common/PageContainer.vue'
import DiffViewer from '@/components/common/DiffViewer.vue'
import { articleVersionAPI, type ArticleVersion } from '@/api/articleVersion'
import { articleAPI } from '@/api/article'

const router = useRouter()
const route = useRoute()
const message = useMessage()
const dialog = useDialog()

// 响应式数据
const loading = ref(false)
const versions = ref<ArticleVersion[]>([])
const filteredVersions = ref<ArticleVersion[]>([])
const selectedVersions = ref<number[]>([])
const searchKeyword = ref('')
const rollingBack = ref(false)
const targetRollbackVersion = ref<number>(0)
const targetRollbackMajorVersion = ref<number>(1)
const targetRollbackMinorVersion = ref<number>(0)
const rollbackTargetArticleId = ref<number>(0)

// 模态框
const compareModalVisible = ref(false)
const rollbackModalVisible = ref(false)

// 对比数据
const compareData = reactive({
  sourceVersion: 0,
  targetVersion: 0,
  sourceMajorVersion: 1,
  sourceMinorVersion: 0,
  targetMajorVersion: 1,
  targetMinorVersion: 0,
  sourceContent: {} as object,
  targetContent: {} as object,
  sourceTime: '',
  targetTime: ''
})

// 分页配置
const paginationConfig = computed(() => ({
  pageSize: 10,
  pageSizes: [10, 20, 50],
  showSizePicker: true,
  prefix: ({ itemCount }: { itemCount: number }) => `共 ${itemCount} 个版本`
}))

/**
 * 表格列定义
 */
const columns = computed(() => [
  {
    type: 'selection',
    disabled: (row: ArticleVersion) => row.version === 1, // 初始版本不可选
    width: 50
  },
  {
    title: '版本号',
    key: 'version',
    width: 100,
    render: (row: ArticleVersion) => {
      const versionDisplay = `${row.majorVersion || 1}.${row.minorVersion || row.version}`
      return h(
        NTag,
        {
          type: row.version === 1 ? 'success' : 'info',
          size: 'small',
          bordered: false
        },
        { default: () => `${versionDisplay}` }
      )
    }
  },
  {
    title: '文章标题',
    key: 'title',
    ellipsis: { tooltip: true },
    width: 250
  },
  {
    title: '修改摘要',
    key: 'changeSummary',
    ellipsis: { tooltip: true },
    minWidth: 200,
    render: (row: ArticleVersion) => {
      return h(
        'span',
        { style: { color: '#666' } },
        { default: () => row.changeSummary || '无摘要' }
      )
    }
  },
  {
    title: '操作人',
    key: 'operatorName',
    width: 120,
    render: (row: ArticleVersion) => {
      // 优先使用 operator.nickname，其次使用 operatorName，最后显示"系统"
      const operatorName = row.operator?.nickname || row.operatorName || '系统'
      return h(
        'span',
        { style: { color: '#666' } },
        { default: () => operatorName }
      )
    }
  },
  {
    title: '修改时间',
    key: 'createTime',
    width: 180,
    sorter: 'default',
    render: (row: ArticleVersion) => formatDate(row.createTime)
  },
  {
    title: '操作',
    key: 'actions',
    width: 200,
    fixed: 'right',
    render: (row: ArticleVersion) => {
      return h('div', { style: { display: 'flex', gap: '8px' } }, [
        h(
          NButton,
          {
            size: 'small',
            type: 'primary',
            ghost: true,
            onClick: () => viewVersionDetail(row)
          },
          { default: () => '查看' }
        ),
        h(
          NButton,
          {
            size: 'small',
            type: 'error',
            ghost: true,
            disabled: row.version === 1,
            onClick: () => confirmRollbackAction(row)
          },
          { default: () => '回滚' }
        )
      ])
    }
  }
])

/**
 * 加载版本列表
 */
const loadVersions = async () => {
  const articleId = route.query.articleId as string
  
  if (!articleId) {
    message.error('缺少文章 ID 参数')
    return
  }

  loading.value = true
  try {
    const response = await articleVersionAPI.getList(articleId, {
      page: 1,
      limit: 100 // 获取更多版本
    })
    
    const data = response.data.data
    // 兼容两种返回格式：数组或包含 list 属性的对象
    versions.value = Array.isArray(data) ? data : (data.list || [])
    filteredVersions.value = [...versions.value]
  } catch (error) {
    console.error('加载版本列表失败:', error)
    message.error('加载版本列表失败')
  } finally {
    loading.value = false
  }
}

/**
 * 打开对比对话框
 */
const openCompareDialog = async () => {
  if (selectedVersions.value.length !== 2) {
    message.warning('请选择两个版本进行对比')
    return
  }

  const [id1, id2] = selectedVersions.value
  const version1 = versions.value.find(v => v.id === id1)
  const version2 = versions.value.find(v => v.id === id2)

  if (!version1 || !version2) return

  // 确保按版本号排序
  const sourceVersion = version1.version < version2.version ? version1 : version2
  const targetVersion = version1.version < version2.version ? version2 : version1

  try {
    // 获取两个版本的详情
    const [detail1, detail2] = await Promise.all([
      articleVersionAPI.getById(sourceVersion.articleId, sourceVersion.version),
      articleVersionAPI.getById(targetVersion.articleId, targetVersion.version)
    ])

    compareData.sourceVersion = sourceVersion.version
    compareData.sourceMajorVersion = sourceVersion.majorVersion || 1
    compareData.sourceMinorVersion = sourceVersion.minorVersion || sourceVersion.version
    compareData.targetVersion = targetVersion.version
    compareData.targetMajorVersion = targetVersion.majorVersion || 1
    compareData.targetMinorVersion = targetVersion.minorVersion || targetVersion.version
    compareData.sourceContent = detail1.data.data.content || {}
    compareData.targetContent = detail2.data.data.content || {}
    compareData.sourceTime = sourceVersion.createTime
    compareData.targetTime = targetVersion.createTime

    compareModalVisible.value = true
  } catch (error) {
    console.error('加载版本详情失败:', error)
    message.error('加载版本详情失败')
  }
}

/**
 * 查看版本详情
 */
const viewVersionDetail = async (row: ArticleVersion) => {
  try {
    const response = await articleVersionAPI.getById(row.articleId, row.version)
    const versionData = response.data.data

    // 获取操作人名称
    const operatorName = versionData.operator?.nickname || versionData.operatorName || '系统'
    const versionDisplay = `${versionData.majorVersion || 1}.${versionData.minorVersion || row.version}`

    dialog.info({
      title: `版本 ${versionDisplay} 详情`,
      content: () => h('div', { style: { marginTop: '12px' } }, [
        h('p', {}, [`标题：${versionData.title}`]),
        h('p', { style: { marginTop: '8px' } }, [`修改摘要：${versionData.changeSummary || '无'}`]),
        h('p', { style: { marginTop: '8px' } }, [`操作人：${operatorName}`]),
        h('p', { style: { marginTop: '8px' } }, [`修改时间：${formatDate(versionData.createTime)}`])
      ]),
      positiveText: '关闭',
      onPositiveClick: () => {}
    })
  } catch (error) {
    console.error('加载版本详情失败:', error)
    message.error('加载版本详情失败')
  }
}

/**
 * 确认回滚操作
 */
const confirmRollbackAction = (row: ArticleVersion) => {
  targetRollbackVersion.value = row.version
  targetRollbackMajorVersion.value = row.majorVersion || 1
  targetRollbackMinorVersion.value = row.minorVersion || row.version
  rollbackTargetArticleId.value = row.articleId

  const versionDisplay = `${row.majorVersion || 1}.${row.minorVersion || row.version}`

  dialog.warning({
    title: '版本回滚',
    content: `确定要回滚到版本 ${versionDisplay} 吗？此操作将创建一个新版本，当前内容会被覆盖。`,
    positiveText: '确定',
    negativeText: '取消',
    onPositiveClick: () => {
      rollbackModalVisible.value = true
    }
  })
}

/**
 * 执行回滚
 */
const confirmRollback = async () => {
  rollingBack.value = true
  try {
    await articleVersionAPI.rollback(rollbackTargetArticleId.value, targetRollbackVersion.value, {
      version: targetRollbackVersion.value
    })

    message.success('回滚成功')
    rollbackModalVisible.value = false
    loadVersions()
  } catch (error) {
    console.error('回滚失败:', error)
    message.error('回滚失败，请重试')
  } finally {
    rollingBack.value = false
  }
}

/**
 * 处理全选
 */
const handleSelectAll = (checked: boolean) => {
  if (checked) {
    // 排除初始版本
    selectedVersions.value = versions.value
      .filter(v => v.version !== 1)
      .map(v => v.id)
  } else {
    selectedVersions.value = []
  }
}

/**
 * 处理搜索
 */
const handleSearch = () => {
  if (!searchKeyword.value) {
    filteredVersions.value = [...versions.value]
  } else {
    const keyword = searchKeyword.value.toLowerCase()
    filteredVersions.value = versions.value.filter(v => {
      const versionMatch = String(v.version).includes(keyword)
      const summaryMatch = (v.changeSummary || '').toLowerCase().includes(keyword)
      return versionMatch || summaryMatch
    })
  }
}

/**
 * 格式化日期
 */
const formatDate = (dateStr: string) => {
  return new Date(dateStr).toLocaleString('zh-CN', {
    year: 'numeric',
    month: '2-digit',
    day: '2-digit',
    hour: '2-digit',
    minute: '2-digit'
  })
}

/**
 * 返回文章详情页或上一页
 */
const goBack = () => {
  const articleId = route.query.articleId as string

  if (articleId) {
    // 优先返回文章详情页
    router.push({
      path: '/index/articleDetail',
      query: { id: articleId }
    })
  } else {
    // 如果没有文章 ID，返回上一页
    router.back()
  }
}

onMounted(() => {
  loadVersions()
})
</script>

<style lang="scss" scoped>
.loading-container {
  padding: 20px;
}

.empty-container {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  min-height: 500px;
}

.version-list {
  .header-actions {
    display: flex;
    justify-content: space-between;
    align-items: center;
    margin-bottom: 20px;
    padding: 16px;
    background: #fff;
    border-radius: 8px;
    box-shadow: 0 2px 8px rgba(0, 0, 0, 0.06);
  }

  :deep(.n-data-table) {
    background: #fff;
    border-radius: 8px;
    overflow: hidden;
    box-shadow: 0 2px 8px rgba(0, 0, 0, 0.06);
  }
}

.compare-content {
  min-height: 400px;
}
</style>
