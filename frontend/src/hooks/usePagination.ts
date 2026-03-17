// src/hooks/usePagination.ts
import { ref, reactive, computed, watch } from 'vue'
import { useMessage } from 'naive-ui'

/**
 * 分页配置选项
 */
interface PaginationOptions {
  /** 初始页码，默认 1 */
  initialPage?: number
  /** 初始每页数量，默认 20 */
  initialLimit?: number
  /** 每页数量选项，默认 [5, 10, 20, 50] */
  pageSizes?: number[]
  /** API 调用函数 */
  apiCall: (params: any) => Promise<any>
  /** 成功回调 */
  onSuccess?: (data: any[], total: number) => void
  /** 失败回调 */
  onError?: (error: any) => void
  /** 是否在初始化时自动加载数据，默认 true */
  immediate?: boolean
  /** 错误消息提示，默认 true */
  showMessage?: boolean
  /** 额外的请求参数（响应式） */
  extraParams?: Record<string, any>
  /** 是否监听 extraParams 变化自动重新加载，默认 false */
  watchExtraParams?: boolean
}

/**
 * 返回的分页数据和状态
 */
interface UsePaginationReturn<T = any> {
  /** 分页状态（响应式） */
  pagination: {
    page: number
    limit: number
  }
  /** 总记录数 */
  totalCount: any
  /** 是否正在加载 */
  isLoading: any
  /** 加载的数据列表 */
  data: any
  /** 分页大小选项 */
  pageSizes: number[]
  /** 总页数（计算属性） */
  totalPages: any
  /** 是否有上一页 */
  hasPrevious: any
  /** 是否有下一页 */
  hasNext: any
  /** 当前显示的数据范围文本，如 "第 1-10 条" */
  rangeText: any
  /** 处理页码变化 */
  handlePageChange: (page: number) => void
  /** 处理每页数量变化 */
  handlePageSizeChange: (size: number) => void
  /** 刷新当前页（保持当前页码） */
  refresh: () => Promise<void>
  /** 重置到第一页并加载（清空筛选条件时使用） */
  reset: () => Promise<void>
  /** 带参数加载数据（用于搜索、筛选） */
  fetchData: (extraParams?: Record<string, any>) => Promise<void>
  /** 手动设置数据 */
  setData: (newData: T[], total?: number) => void
  /** 追加数据（用于加载更多场景） */
  appendData: (newData: T[]) => void
  /** 清空数据 */
  clear: () => void
}

/**
 * 通用分页 Hook
 * 适用于所有需要分页的列表场景
 *
 * @example
 * // 基础用法
 * const {
 *   data: articleList,
 *   pagination,
 *   totalCount,
 *   isLoading,
 *   handlePageChange,
 *   handlePageSizeChange,
 *   fetchData
 * } = usePagination({
 *   apiCall: (params) => articleAPI.getList(params),
 *   initialLimit: 20
 * })
 *
 * @example
 * // 带筛选条件
 * const searchParams = ref({ keyword: '', categoryId: '' })
 * const { fetchData, reset } = usePagination({
 *   apiCall: (params) => articleAPI.getList(params),
 *   extraParams: searchParams.value,
 *   watchExtraParams: true
 * })
 *
 * const handleSearch = () => {
 *   fetchData({ keyword: searchParams.value.keyword })
 * }
 *
 * const handleReset = () => {
 *   searchParams.value = { keyword: '', categoryId: '' }
 *   reset()
 * }
 */
export function usePagination<T = any>(options: PaginationOptions): UsePaginationReturn<T> {
  const {
    initialPage = 1,
    initialLimit = 20,
    pageSizes = [5, 10, 20, 50],
    apiCall,
    onSuccess,
    onError,
    immediate = true,
    showMessage = true,
    extraParams = {},
    watchExtraParams = false
  } = options

  const message = useMessage()

  // 分页状态
  const pagination = reactive({
    page: initialPage,
    limit: initialLimit
  })

  // 数据状态
  const totalCount = ref(0)
  const isLoading = ref(false)
  const data = ref<T[]>([])

  // 计算属性
  const totalPages = computed(() => {
    return Math.ceil(totalCount.value / pagination.limit) || 1
  })

  const hasPrevious = computed(() => pagination.page > 1)

  const hasNext = computed(() => pagination.page < totalPages.value)

  const rangeText = computed(() => {
    if (totalCount.value === 0) return '共 0 条'

    const start = (pagination.page - 1) * pagination.limit + 1
    const end = Math.min(pagination.page * pagination.limit, totalCount.value)
    return `第 ${start}-${end} 条`
  })

  /**
   * 核心数据加载方法
   */
  const loadData = async (paramsOverride: Record<string, any> = {}): Promise<void> => {
    isLoading.value = true

    try {
      // 合并参数：基础分页参数 + 额外参数 + 覆盖参数
      const params = {
        page: pagination.page,
        limit: pagination.limit,
        ...extraParams,
        ...paramsOverride
      }

      const response = await apiCall(params)

      // 统一处理响应数据结构
      const apiData = response.data?.data || response.data || {}
      const list = apiData.list || apiData.items || apiData.records || []
      const total = apiData.totalCount || apiData.total || apiData.count || list.length

      data.value = list
      totalCount.value = total

      // 成功回调
      onSuccess?.(list, total)
    } catch (error: any) {
// console.error('加载数据失败:', error)

      // 错误处理
      onError?.(error)

      if (showMessage) {
        const errorMsg = error.response?.data?.message ||
            error.message ||
            '加载数据失败，请重试'
        message.error(errorMsg)
      }

      data.value = []
    } finally {
      isLoading.value = false
    }
  }

  /**
   * 处理页码变化
   */
  const handlePageChange = (page: number): void => {
    pagination.page = page
    loadData()
  }

  /**
   * 处理每页数量变化
   */
  const handlePageSizeChange = (size: number): void => {
    pagination.limit = size
    pagination.page = 1 // 重置到第一页
    loadData()
  }

  /**
   * 刷新当前页（保持当前页码重新加载）
   */
  const refresh = async (): Promise<void> => {
    await loadData()
  }

  /**
   * 重置到第一页并加载（用于清空筛选条件）
   */
  const reset = async (): Promise<void> => {
    pagination.page = 1
    await loadData()
  }

  /**
   * 带参数加载数据（用于搜索、筛选）
   * @param extraParams 额外的查询参数
   */
  const fetchData = async (paramsOverride: Record<string, any> = {}): Promise<void> => {
    pagination.page = 1 // 搜索时重置到第一页
    await loadData(paramsOverride)
  }

  /**
   * 手动设置数据（用于特殊场景）
   */
  const setData = (newData: T[], total?: number): void => {
    data.value = newData
    if (total !== undefined) {
      totalCount.value = total
    }
  }

  /**
   * 追加数据（用于"加载更多"场景）
   */
  const appendData = (newData: T[]): void => {
    data.value = [...data.value, ...newData]
  }

  /**
   * 清空数据
   */
  const clear = (): void => {
    data.value = []
    totalCount.value = 0
    pagination.page = initialPage
    pagination.limit = initialLimit
  }

  // 监听 extraParams 变化（可选）
  if (watchExtraParams && typeof extraParams === 'object') {
    watch(
        () => JSON.stringify(extraParams),
        () => {
          reset()
        },
        { deep: true }
    )
  }

  // 初始化时自动加载
  if (immediate) {
    loadData()
  }

  return {
    pagination,
    totalCount,
    isLoading,
    data,
    pageSizes,
    totalPages,
    hasPrevious,
    hasNext,
    rangeText,
    handlePageChange,
    handlePageSizeChange,
    refresh,
    reset,
    fetchData,
    setData,
    appendData,
    clear
  }
}

export default usePagination
