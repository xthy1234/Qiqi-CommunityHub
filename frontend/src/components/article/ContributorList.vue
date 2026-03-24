<template>
  <div class="contributor-list">
    <!-- 标题 -->
    <div
      v-if="showTitle"
      class="list-header"
    >
      <h3 class="title">
        <Icon
          :icon="icon"
          width="18"
        />
        <span>{{ title }}</span>
      </h3>
      <n-button
        v-if="showMoreButton && contributors.length >= maxShow"
        text
        type="primary"
        size="small"
        @click="handleViewMore"
      >
        查看更多
      </n-button>
    </div>

    <!-- 贡献者列表 -->
    <div class="contributors">
      <n-avatar-group
        :size="size"
        :max="maxShow"
        :options="avatarOptions"
      >
        <template #avatar="{ option }">
          <n-tooltip>
            <template #trigger>
              <UserAvatarLink
                :user-id="option.userId"
                :nickname="option.nickname"
                :avatar="option.avatar"
                :size="size"
              />
            </template>
            <div class="contributor-tooltip">
              <div class="tooltip-name">{{ option.nickname }}</div>
              <div class="tooltip-lines">贡献 {{ option.contributedLines }} 行</div>
            </div>
          </n-tooltip>
        </template>
        
        <template #rest="{ rest }">
          <n-avatar
            :size="size"
            style="background-color: #18a058; color: #fff;"
          >
            +{{ rest }}
          </n-avatar>
        </template>
      </n-avatar-group>

      <!-- 详细列表（展开时显示） -->
      <div
        v-if="expanded"
        class="detailed-list"
      >
        <n-list hoverable>
          <n-list-item
            v-for="contributor in contributors"
            :key="contributor.userId"
          >
            <template #prefix>
              <UserAvatarLink
                :user-id="contributor.userId"
                :nickname="contributor.nickname"
                :avatar="contributor.avatar"
                :size="40"
              />
            </template>
            
            <div class="contributor-info">
              <div class="contributor-name">{{ contributor.nickname }}</div>
              <div class="contributor-stats">
                <n-tag
                  type="success"
                  size="small"
                  round
                >
                  <template #icon>
                    <Icon icon="ri:line-chart-line" />
                  </template>
                  {{ contributor.contributedLines }} 行
                </n-tag>
                <n-tag
                  type="info"
                  size="small"
                  round
                >
                  <template #icon>
                    <Icon icon="ri:award-line" />
                  </template>
                  {{ contributor.contributionCount }} 次
                </n-tag>
              </div>
            </div>
          </n-list-item>
        </n-list>
      </div>
    </div>

    <!-- 空状态 -->
    <div
      v-if="!loading && contributors.length === 0"
      class="empty-state"
    >
      <n-empty
        description="暂无贡献者"
        size="small"
      />
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, watch, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { NAvatar, NAvatarGroup, NTooltip, NList, NListItem, NTag } from 'naive-ui'
import { Icon } from '@iconify/vue'
import UserAvatarLink from '@/components/user/UserAvatarLink.vue'

interface Contributor {
  userId: number
  nickname: string
  avatar?: string
  contributedLines: number
  contributionCount: number
}

const props = withDefaults(defineProps<{
  articleId?: number | string
  contributors?: Contributor[]
  showTitle?: boolean
  title?: string
  icon?: string
  size?: number
  maxShow?: number
  showMoreButton?: boolean
  expanded?: boolean
}>(), {
  showTitle: true,
  title: '贡献者',
  icon: 'ri:team-line',
  size: 32,
  maxShow: 5,
  showMoreButton: true,
  expanded: false
})

const emit = defineEmits<{
  (e: 'view-more'): void
}>()

const router = useRouter()
const loading = ref(false)
const localContributors = ref<Contributor[]>([])

// 头像数据
const avatarOptions = computed(() => {
  const list = props.contributors?.length ? props.contributors : localContributors.value
  return list.map((c : Contributor) => ({
    userId: c.userId,
    nickname: c.nickname,
    avatar: c.avatar,
    contributedLines: c.contributedLines,
    contributionCount: c.contributionCount
  }))
})

/**
 * 加载更多
 */
const handleViewMore = () => {
  if (props.expanded) {
    emit('view-more')
  } else {
    // 如果没有设置 expanded，直接展开详细列表
    // 这里可以通过 emit 通知父组件
    emit('view-more')
  }
}

onMounted(() => {
  // 如果没有传入 contributors，可以在此处加载
  if (!props.contributors?.length && props.articleId) {
    // TODO: 调用 API 加载贡献者列表
    // loadContributors()
  }
})
</script>

<style lang="scss" scoped>
.contributor-list {
  .list-header {
    display: flex;
    justify-content: space-between;
    align-items: center;
    margin-bottom: 16px;

    .title {
      display: flex;
      align-items: center;
      gap: 8px;
      font-size: 16px;
      font-weight: 600;
      color: #333;
      margin: 0;
    }
  }

  .contributors {
    .detailed-list {
      margin-top: 16px;
    }

    .contributor-info {
      display: flex;
      justify-content: space-between;
      align-items: center;

      .contributor-name {
        font-size: 14px;
        font-weight: 500;
        color: #333;
      }

      .contributor-stats {
        display: flex;
        gap: 8px;

        .n-tag {
          display: inline-flex;
          align-items: center;
          gap: 4px;
        }
      }
    }
  }

  .empty-state {
    padding: 20px 0;
  }
}

.contributor-tooltip {
  text-align: center;
  padding: 4px 0;

  .tooltip-name {
    font-weight: 600;
    margin-bottom: 4px;
  }

  .tooltip-lines {
    font-size: 12px;
    color: #666;
  }
}
</style>
