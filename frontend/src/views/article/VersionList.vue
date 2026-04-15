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
          <!-- 版本类型筛选 -->
          <n-select
            v-model:value="versionTypeFilter"
            :options="versionTypeOptions"
            placeholder="版本类型"
            clearable
            style="width: 120px;"
          />
        </n-space>

        <n-space>
          <!-- 快捷定位 -->
          <n-button
            size="small"
            @click="scrollToLatest"
          >
            <template #icon>
              <Icon icon="ri:arrow-up-line" />
            </template>
            回到最新
          </n-button>

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
        </n-space>
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
        <p>回滚后将创建一个新的版本记录，文章内容将恢复到该版本。</p>
        <p style="margin-top: 8px; color: #f0a020;">
          此操作不可逆，请谨慎操作！
        </p>
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
import { useBackNavigation } from '@/utils/backNavigation'
import { useMessage, useDialog, NTag, NButton, NDataTable, NBadge } from 'naive-ui'
import { Icon } from '@iconify/vue'
import PageContainer from '@/components/common/PageContainer.vue'
import UserAvatarLink from '@/components/user/UserAvatarLink.vue'
import { articleVersionAPI, type ArticleVersion, type UserInfo } from '@/api/articleVersion'
import { articleAPI } from '@/api/article'
import { useGlobalProperties } from '@/utils/globalProperties'

const appContext = useGlobalProperties()
const router = useRouter()
const route = useRoute()
const { navigateWithBackUrl, returnAfterRollback, goBack } = useBackNavigation()

const message = useMessage()
const dialog = useDialog()

// 响应式数据
const loading = ref(false)
const versions = ref<ArticleVersion[]>([])
const filteredVersions = ref<ArticleVersion[]>([])
const searchKeyword = ref('')
const versionTypeFilter = ref<'major' | 'minor' | ''>('')
const rollingBack = ref(false)
const targetRollbackVersion = ref<number>(0)
const targetRollbackMajorVersion = ref<number>(1)
const targetRollbackMinorVersion = ref<number>(0)
const rollbackTargetArticleId = ref<number>(0)
const currentUserId = ref<string | number>('')
const articleAuthorId = ref<string | number>('')

// 模态框
const rollbackModalVisible = ref(false)

// 版本类型选项
const versionTypeOptions = [
  { label: '大版本', value: 'major' },
  { label: '小版本', value: 'minor' }
]

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
    title: '版本号',
    key: 'version',
    width: 120,
    render: (row: ArticleVersion) => {
      const versionDisplay = `${row.majorVersion ?? 1}.${row.minorVersion ?? row.version}`
      const isCurrent = row.isCurrent === true
      const isMajorVersion = (row.majorVersion ?? 1) > 1 && (row.minorVersion ?? 0) === 0

      return h('div', { style: { display: 'flex', alignItems: 'center', gap: '6px' } }, [
        h(
          NTag,
          {
            type: row.version === 1 ? 'success' : isCurrent ? 'success' : isMajorVersion ? 'warning' : 'info',
            size: 'small',
            bordered: false
          },
          { default: () => versionDisplay }
        ),
        isCurrent && h(
          NBadge,
          {
            type: 'success',
            dot: true
          }
        )
      ])
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
      const summary = row.changeSummary || '无摘要'
      const isCurrent = row.isCurrent === true

      return h('div', { style: { display: 'flex', alignItems: 'center', gap: '6px' } }, [
        h(
          'span',
          { style: { color: isCurrent ? '#18a058' : '#666', fontWeight: isCurrent ? 'bold' : 'normal' } },
          { default: () => summary }
        ),
        isCurrent && h(
          NTag,
          {
            type: 'success',
            size: 'small',
            bordered: false
          },
          { default: () => '当前版本' }
        )
      ])
    }
  },
  {
    title: '操作人',
    key: 'operator',
    width: 180,
    render: (row: ArticleVersion) => {
      const operator = row.operator as UserInfo | undefined
      const operatorName = operator?.nickname || row.operatorName || '系统'

      return h('div', { style: { display: 'flex', alignItems: 'center', gap: '8px' } }, [
        h(
          UserAvatarLink,
          {
            userId: operator?.id || row.operatorId || 0,
            nickname: operatorName,
            avatar: operator?.avatar,
            size: 30,
            showName: true
          },
          { default: () => null }
        )
      ])
    }
  },
  {
    title: '贡献者',
    key: 'contributor',
    width: 150,
    render: (row: ArticleVersion) => {
      const contributor = row.contributor as UserInfo | undefined

      if (!contributor) {
        return h('span', { style: { color: '#999' } }, { default: () => '-' })
      }

      return h('div', { style: { display: 'flex', alignItems: 'center', gap: '8px' } }, [
        h(
          UserAvatarLink,
          {
            userId: contributor.id || row.contributorId || 0,
            nickname: contributor.nickname,
            avatar: contributor.avatar,
            size: 30,
            showName: true
          },
          { default: () => null }
        )
      ])
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
      const isCurrent = row.isCurrent === true
      // 权限判断：只有文章作者才能执行回滚操作
      const canRollback = String(currentUserId.value) === String(articleAuthorId.value)

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
            disabled: row.version === 1 || isCurrent || !canRollback,
            onClick: () => confirmRollbackAction(row)
          },
          {
            default: () => {
              if (!canRollback) {
                return '无权限'
              }
              if (isCurrent) {
                return '已是最新版'
              }
              return '回滚'
            }
          }
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
    console.error('[加载版本列表] articleId 缺失')
    return
  }

  loading.value = true
  try {
    // 先获取文章详情，得到作者 ID

    const articleRes = await articleAPI.getById(articleId)
    articleAuthorId.value = articleRes.data.data?.authorId || ''

    // 获取当前登录用户 ID
    currentUserId.value = appContext?.$toolUtil?.storageGet('userid') || ''

    // 获取版本列表（后端已返回 isCurrent 字段，无需手动对比）

    const response = await articleVersionAPI.getList(articleId, {
      page: 1,
      limit: 100 // 获取更多版本
    })

    const data = response.data.data
    // 兼容两种返回格式：数组或包含 list 属性的对象
    versions.value = Array.isArray(data) ? data : (data.list || [])
    filteredVersions.value = [...versions.value]

    // 打印每个版本的信息
    versions.value.forEach((v, index) => {

    })

  } catch (error) {
    console.error('[加载版本列表失败] error:', error)
    message.error('加载版本列表失败')
  } finally {
    loading.value = false
  }
}

/**
 * 查看版本详情 - 跳转到独立页面
 */
const viewVersionDetail = (row: ArticleVersion) => {
  const articleId = route.query.articleId as string

  navigateWithBackUrl({
    path: `/index/article/${articleId}/version/${row.version}`
  })
}

/**
 * 确认回滚操作
 */
const confirmRollbackAction = (row: ArticleVersion) => {
  targetRollbackVersion.value = row.version
  targetRollbackMajorVersion.value = row.majorVersion ?? 1
  targetRollbackMinorVersion.value = row.minorVersion ?? row.version
  rollbackTargetArticleId.value = row.articleId

  const versionDisplay = `${row.majorVersion ?? 1}.${row.minorVersion ?? row.version}`

  dialog.warning({
    title: '版本回滚',
    content: `确定要回滚到版本 ${versionDisplay} 吗？\n\n此操作将基于该版本创建一个新版本，文章内容将恢复到此版本的状态。`,
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
    const rollbackRes = await articleVersionAPI.rollback(rollbackTargetArticleId.value, targetRollbackVersion.value, {
      version: targetRollbackVersion.value
    })

    message.success('回滚成功，已创建新版本')
    rollbackModalVisible.value = false

    // 重新加载版本列表
    await loadVersions()

    // 使用工具类返回，自动处理返回逻辑（replace 模式）
    const articleId = route.query.articleId as string
    await returnAfterRollback(router, route, articleId)

  } catch (error) {
    console.error('[回滚失败] error:', error)
    message.error('回滚失败，请重试')
  } finally {
    rollingBack.value = false
  }
}

/**
 * 处理搜索和筛选
 */
const handleSearch = () => {
  let result = [...versions.value]

  // 搜索过滤
  if (searchKeyword.value) {
    const keyword = searchKeyword.value.toLowerCase()
    result = result.filter(v => {
      // 版本号搜索：同时搜索 version 字段和格式化后的版本号
      const versionStr = String(v.version)
      const formattedVersion = `${v.majorVersion ?? 1}.${v.minorVersion ?? v.version}`
      const versionMatch = versionStr.includes(keyword) || formattedVersion.includes(keyword)

      // 摘要搜索
      const summaryMatch = (v.changeSummary || '').toLowerCase().includes(keyword)

      return versionMatch || summaryMatch
    })
  }

  // 版本类型过滤
  if (versionTypeFilter.value) {
    result = result.filter(v => {
      if (versionTypeFilter.value === 'major') {
        // 大版本：minorVersion 为 0
        return (v.minorVersion ?? 0) === 0
      } else if (versionTypeFilter.value === 'minor') {
        // 小版本：minorVersion 不为 0
        return (v.minorVersion ?? 0) !== 0
      }
      return true
    })
  }

  filteredVersions.value = result
}

/**
 * 快捷定位到最新版本
 */
const scrollToLatest = () => {
  // 清空筛选条件
  searchKeyword.value = ''
  versionTypeFilter.value = ''
  filteredVersions.value = [...versions.value]

  message.success('已显示最新版本')
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
const goBackHandler = () => {
  goBack({
    fallbackPath: '/index/articleDetail'
  })
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
