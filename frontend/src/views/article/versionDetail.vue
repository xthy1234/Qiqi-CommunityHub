<template>
  <PageContainer
    header-title="版本详情"
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
        :repeat="10"
      />
    </div>

    <!-- 版本详情 -->
    <div
      v-else-if="versionData"
      class="version-detail"
    >
      <!-- 版本信息卡片 -->
      <n-card
        class="version-header"
        size="small"
      >
        <div class="header-content">
          <div class="version-info">
            <h1 class="version-title">
              <n-tag
                :type="isMajorVersion ? 'warning' : 'info'"
                size="medium"
                bordered:
                false
              >
                {{ versionDisplay }}
              </n-tag>
              <n-tag
                v-if="versionData.isLatest"
                type="success"
                size="small"
                style="margin-left: 8px;"
              >
                最新版本
              </n-tag>
            </h1>
            <p class="version-meta">
              <span class="meta-item">
                <Icon icon="ri:user-line" />
                <UserAvatarLink
                  :user-id="operatorId"
                  :nickname="operatorName"
                  :avatar="operatorAvatar"
                  :size="24"
                  show-name
                />
              </span>
              <span class="meta-item">
                <Icon icon="ri:time-line" />
                {{ formatDate(versionData.createTime) }}
              </span>
            </p>
          </div>
          
          <div class="version-actions">
            <n-space>
              <n-button
                type="primary"
                ghost
                :disabled="!prevVersion"
                @click="compareWithPrev"
              >
                <template #icon>
                  <Icon icon="ri:git-compare-line" />
                </template>
                与上一版本对比
              </n-button>

              <n-button
                type="primary"
                ghost
                :disabled="!nextVersion"
                @click="compareWithNext"
              >
                <template #icon>
                  <Icon icon="ri:git-compare-line" />
                </template>
                与下一版本对比
              </n-button>
              
              <n-button
                v-if="!versionData.isLatest"
                type="primary"
                ghost
                @click="compareWithCurrent"
              >
                <template #icon>
                  <Icon icon="ri:git-compare-line" />
                </template>
                与当前版本对比
              </n-button>

              <n-button
                v-if="canRollback"
                type="error"
                ghost
                @click="confirmRollback"
              >
                <template #icon>
                  <Icon icon="ri:arrow-go-back-line" />
                </template>
                回滚到此版本
              </n-button>
            </n-space>
          </div>
        </div>
        
        <n-divider />
        
        <div class="change-summary">
          <strong>修改摘要：</strong>
          {{ versionData.changeSummary || '无' }}
        </div>
      </n-card>

      <!-- 文章内容区域 -->
      <n-card
        class="version-content"
        size="small"
        style="margin-top: 16px;"
      >
        <template #header>
          <n-space justify="space-between">
            <span>文章内容</span>
            <n-button
              size="small"
              @click="copyContent"
            >
              <template #icon>
                <Icon icon="ri:file-copy-line" />
              </template>
              复制全文
            </n-button>
          </n-space>
        </template>
        
        <div
          v-if="contentHtml"
          class="article-body"
          v-html="contentHtml"
        />
        <n-empty
          v-else
          description="内容为空"
        />
      </n-card>

      <!-- 相邻版本导航 -->
      <div class="version-navigation">
        <n-button
          :disabled="!prevVersion"
          ghost
          @click="navigateToVersion(prevVersion)"
        >
          <template #icon>
            <Icon icon="ri:arrow-left-line" />
          </template>
          上一个版本：{{ prevVersion ? getVersionDisplay(prevVersion) : '' }}
        </n-button>
        
        <n-button
          :disabled="!nextVersion"
          ghost
          @click="navigateToVersion(nextVersion)"
        >
          下一个版本：{{ nextVersion ? getVersionDisplay(nextVersion) : '' }}
          <template #icon>
            <Icon icon="ri:arrow-right-line" />
          </template>
        </n-button>
      </div>
    </div>

    <!-- 对比对话框 -->
    <n-modal
      v-model:show="compareModalVisible"
      preset="dialog"
      title="版本对比"
      :style="{ width: '1400px', maxWidth: '95vw' }"
      :closable="true"
    >
      <div class="compare-content">
        <TextDiffViewer
          :source="compareData.sourceContent"
          :target="compareData.targetContent"
          :source-label="`${compareData.sourceTitle}`"
          :target-label="`${compareData.targetTitle}`"
          :source-time="compareData.sourceTime"
          :target-time="compareData.targetTime"
          :show-header="true"
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
        :title="`确认回滚到版本 ${versionDisplay}?`"
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
            @click="doRollback"
          >
            确认回滚
          </n-button>
        </n-space>
      </template>
    </n-modal>
  </PageContainer>
</template>

<script setup lang="ts">
import { ref, computed, onMounted, watch } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { useBackNavigation } from '@/utils/backNavigation'
import { useMessage, useDialog } from 'naive-ui'
import { Icon } from '@iconify/vue'
import PageContainer from '@/components/common/PageContainer.vue'
import UserAvatarLink from '@/components/user/UserAvatarLink.vue'
import TextDiffViewer from '@/components/common/TextDiffViewer.vue'
import { articleVersionAPI, type ArticleVersion, type UserInfo } from '@/api/articleVersion'
import { articleAPI } from '@/api/article'
import { useGlobalProperties } from '@/utils/globalProperties'
import { generateHTML } from '@tiptap/core'
import StarterKit from '@tiptap/starter-kit'
import Image from '@tiptap/extension-image'
import Link from '@tiptap/extension-link'

const appContext = useGlobalProperties()
const router = useRouter()
const route = useRoute()
const { goBack: backNavigation, returnAfterRollback } = useBackNavigation()
const message = useMessage()
const dialog = useDialog()

// 编辑器扩展配置
const extensions = [
  StarterKit.configure({ link: false }),
  Image,
  Link.configure({ openOnClick: false })
]

// 响应式数据
const loading = ref(false)
const versionData = ref<ArticleVersion | null>(null)
const currentUserId = ref<string | number>('')
const articleAuthorId = ref<string | number>('')
const contentHtml = ref('')
const rollingBack = ref(false)
const rollbackModalVisible = ref(false)
const compareModalVisible = ref(false)
const allVersions = ref<ArticleVersion[]>([])

// 对比数据
const compareData = ref({
  sourceVersion: 0,
  targetVersion: 0,
  sourceTitle: '',
  targetTitle: '',
  sourceContent: {} as object,
  targetContent: {} as object,
  sourceTime: '',
  targetTime: ''
})

// 计算属性
const versionDisplay = computed(() => {
  if (!versionData.value) {return ''}
  return `${versionData.value.majorVersion ?? 1}.${versionData.value.minorVersion ?? versionData.value.version}`
})

const isMajorVersion = computed(() => {
  if (!versionData.value) {return false}
  return (versionData.value.majorVersion ?? 1) > 1 && (versionData.value.minorVersion ?? 0) === 0
})

const operatorName = computed(() => {
  const operator = versionData.value?.operator as UserInfo | undefined
  return operator?.nickname || versionData.value?.operatorName || '系统'
})

const operatorId = computed(() => {
  const operator = versionData.value?.operator as UserInfo | undefined
  return operator?.id || versionData.value?.operatorId || 0
})

const operatorAvatar = computed(() => {
  const operator = versionData.value?.operator as UserInfo | undefined
  return operator?.avatar || ''
})

const canRollback = computed(() => {
  return String(currentUserId.value) === String(articleAuthorId.value)
})

// 相邻版本导航
const prevVersion = computed(() => {
  if (!versionData.value || allVersions.value.length === 0) {

    return null
  }
  const currentIndex = allVersions.value.findIndex(v => v.version === versionData.value!.version)
  const prev = currentIndex > 0 ? allVersions.value[currentIndex - 1] : null

  return prev
})

const nextVersion = computed(() => {
  if (!versionData.value || allVersions.value.length === 0) {

    return null
  }
  const currentIndex = allVersions.value.findIndex(v => v.version === versionData.value!.version)
  const next = currentIndex < allVersions.value.length - 1 ? allVersions.value[currentIndex + 1] : null

  return next
})

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
 * 获取版本号显示
 */
const getVersionDisplay = (version: ArticleVersion) => {
  return `${version.majorVersion ?? 1}.${version.minorVersion ?? version.version}`
}

/**
 * 加载版本详情
 */
const loadVersionDetail = async () => {
  loading.value = true
  try {
    const articleId = route.params.articleId as string
    const version = parseInt(route.params.version as string)
    
    if (!articleId || isNaN(version)) {
      throw new Error('参数错误')
    }
    
    // 获取文章详情（用于权限判断）
    const articleRes = await articleAPI.getById(articleId)
    articleAuthorId.value = articleRes.data.data?.authorId || ''
    currentUserId.value = appContext?.$toolUtil?.storageGet('userid') || ''
    
    // 获取版本详情
    const response = await articleVersionAPI.getById(articleId, version)
    versionData.value = response.data.data
    
    // 转换为 HTML
    if (versionData.value?.content) {
      contentHtml.value = generateHTML(versionData.value.content, extensions)
    }
    
    // 获取所有版本（用于相邻导航）
    const versionsRes = await articleVersionAPI.getList(articleId, { page: 1, limit: 100 })
    allVersions.value = Array.isArray(versionsRes.data.data) ? versionsRes.data.data : (versionsRes.data.data.list || [])

  } catch (error) {
    console.error('[加载版本详情失败] error:', error)
    message.error('加载版本详情失败')
    versionData.value = null
  } finally {
    loading.value = false
  }
}

/**
 * 与上一版本对比
 */
const compareWithPrev = async () => {
  if (!prevVersion.value) {
    message.warning('没有上一版本')
    return
  }

  await loadCompareData(prevVersion.value)
}

/**
 * 与下一版本对比
 */
const compareWithNext = async () => {
  if (!nextVersion.value) {
    message.warning('没有下一版本')
    return
  }

  await loadCompareData(nextVersion.value)
}

/**
 * 加载对比数据
 */
const loadCompareData = async (targetVersion: ArticleVersion) => {
  try {
    const articleId = route.params.articleId as string
    const sourceVersionNum = versionData.value?.version

    if (!sourceVersionNum) {return}
    
    // 调用后端对比接口
    const response = await articleVersionAPI.compare(articleId, sourceVersionNum, targetVersion.version)
    const compareResult = response.data.data

    // 设置对比数据（从 content 字段获取）
    compareData.value = {
      sourceVersion: sourceVersionNum,
      targetVersion: targetVersion.version,
      sourceTitle: versionData.value?.title || '',
      targetTitle: targetVersion.title || '',
      sourceContent: compareResult.sourceVersion?.content || {},
      targetContent: compareResult.targetVersion?.content || {},
      sourceTime: versionData.value?.createTime || '',
      targetTime: targetVersion.createTime
    }
    compareModalVisible.value = true

  } catch (error) {
    console.error('[版本对比失败] error:', error)
    message.error('加载版本对比失败')
  }
}

/**
 * 与当前版本对比
 */
const compareWithCurrent = async () => {
  try {
    const articleId = route.params.articleId as string

    // 获取当前最新版本
    const articleRes = await articleAPI.getById(articleId)
    const currentVersionNum = articleRes.data.data?.currentVersion

    if (!currentVersionNum) {
      message.error('无法获取当前版本')
      return
    }

    // 如果当前版本就是自己，不需要对比
    if (currentVersionNum === versionData.value?.version) {
      message.info('当前就是最新版本，无需对比')
      return
    }

    const currentVersion = allVersions.value.find(v => v.version === currentVersionNum)

    if (!currentVersion) {
      message.error('当前版本不存在')
      return
    }

    await loadCompareData(currentVersion)

  } catch (error) {
    console.error('对比失败:', error)
    message.error('对比失败')
  }
}

/**
 * 确认回滚
 */
const confirmRollback = () => {
  rollbackModalVisible.value = true
}

/**
 * 执行回滚
 */
const doRollback = async () => {
  rollingBack.value = true
  try {
    const articleId = route.params.articleId as string
    const version = versionData.value?.version
    
    if (!version) {return}
    
    await articleVersionAPI.rollback(articleId, version, { version })
    
    message.success('回滚成功')
    rollbackModalVisible.value = false
    
    // 使用工具类返回文章详情页（replace 模式）
    await returnAfterRollback(router, route, articleId)
    
  } catch (error) {
    console.error('回滚失败:', error)
    message.error('回滚失败')
  } finally {
    rollingBack.value = false
  }
}

/**
 * 复制全文
 */
const copyContent = async () => {
  if (!versionData.value?.content) {return}
  
  try {
    const textContent = JSON.stringify(versionData.value.content, null, 2)
    await navigator.clipboard.writeText(textContent)
    message.success('已复制到剪贴板')
  } catch (error) {
    console.error('复制失败:', error)
    message.error('复制失败')
  }
}

/**
 * 导航到相邻版本
 */
const navigateToVersion = (version: ArticleVersion) => {
  if (!version) {
    console.warn(' [导航] 版本为空')
    return
  }
  
  const articleId = route.params.articleId
  const targetVersion = version.version

  const path = `/index/article/${articleId}/version/${targetVersion}`

  router.push(path).then(() => {

  }).catch(err => {
    console.error('[版本导航] 跳转失败:', err)
  })
}

/**
 * 返回上一页
 */
const goBack = () => {
  backNavigation({
    fallbackPath: `/index/article/versions?articleId=${route.params.articleId}`
  })
}

/**
 * 监听路由参数变化，重新加载数据
 */
watch(
  () => [route.params.articleId, route.params.version],
  ([newArticleId, newVersion], [oldArticleId, oldVersion]) => {
    // 只有当 articleId 或 version 真正变化时才重新加载
    if (newArticleId !== oldArticleId || newVersion !== oldVersion) {

      loadVersionDetail()
    }
  },
  { deep: true }
)

onMounted(() => {
  loadVersionDetail()
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

.version-detail {
  .version-header {
    .header-content {
      display: flex;
      justify-content: space-between;
      align-items: flex-start;
      margin-bottom: 16px;
    }

    .version-info {
      flex: 1;

      .version-title {
        margin: 0 0 12px 0;
        font-size: 24px;
        display: flex;
        align-items: center;
      }

      .version-meta {
        display: flex;
        gap: 24px;
        color: #666;
        font-size: 14px;

        .meta-item {
          display: flex;
          align-items: center;
          gap: 6px;
        }
      }
    }

    .version-actions {
      flex-shrink: 0;
    }

    .change-summary {
      margin-top: 12px;
      padding: 12px;
      background: #f5f5f5;
      border-radius: 4px;
      line-height: 1.6;
    }
  }

  .version-content {
    .article-body {
      min-height: 400px;
      padding: 20px;
      background: #fafafa;
      border-radius: 4px;

      :deep(img) {
        max-width: 100%;
        max-height: 400px;
        object-fit: contain;
        border-radius: 4px;
        margin: 8px 0;
        cursor: pointer;
        transition: all 0.3s;

        &:hover {
          transform: scale(1.02);
          box-shadow: 0 4px 12px rgba(0, 0, 0, 0.15);
        }
      }

      :deep(.file-node) {
        display: inline-flex;
        align-items: center;
        padding: 8px 12px;
        background: #f5f7fa;
        border: 1px solid #e4e7ed;
        border-radius: 4px;
        margin: 8px 0;
        cursor: pointer;
        transition: all 0.3s;

        &:hover {
          background: #ecf5ff;
          border-color: #409eff;
        }

        .file-icon {
          font-size: 24px;
          margin-right: 8px;
          color: #409eff;
        }

        .file-info {
          flex: 1;
          overflow: hidden;

          .file-name {
            font-size: 14px;
            font-weight: 500;
            color: #333;
            white-space: nowrap;
            overflow: hidden;
            text-overflow: ellipsis;
          }

          .file-size {
            font-size: 12px;
            color: #909399;
            margin-top: 2px;
          }
        }
      }

      :deep(.share-card-node) {
        display: block;
        margin: 16px 0;
        border: 1px solid #e4e7ed;
        border-radius: 8px;
        overflow: hidden;
        transition: all 0.3s;

        &:hover {
          box-shadow: 0 4px 12px rgba(0, 0, 0, 0.1);
        }

        .share-card-cover {
          width: 100%;
          height: 200px;
          object-fit: cover;
        }

        .share-card-content {
          padding: 16px;

          .share-card-title {
            font-size: 16px;
            font-weight: 600;
            color: #333;
            margin-bottom: 8px;
          }

          .share-card-description {
            font-size: 14px;
            color: #666;
            line-height: 1.6;
          }
        }
      }
    }
  }

  .version-navigation {
    display: flex;
    justify-content: space-between;
    margin-top: 24px;
    padding: 16px;
    background: #fff;
    border-radius: 8px;
    box-shadow: 0 2px 8px rgba(0, 0, 0, 0.06);
  }
}

.compare-content {
  min-height: 500px;
  max-height: 70vh;
  overflow-y: auto;
}
</style>
