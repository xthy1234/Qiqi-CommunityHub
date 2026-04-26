<template>
  <PageContainer
    :header-title="isEdit ? '编辑轮播图' : '新建轮播图'"
    @back="goBack"
  >

    <template #headerExtra>
      <NButton @click="handleCancel">取消</NButton>
      <NButton type="primary" @click="handleSubmit" :loading="submitting">
        <template #icon>
          <Icon icon="ri:save-line" />
        </template>
        {{ isEdit ? '保存' : '创建' }}
      </NButton>
    </template>


    <div class="edit-form-container">
      <NForm
        ref="formRef"
        :model="formData"
        :rules="formRules"
        label-placement="left"
        label-width="120px"
        require-mark-placement="right-hanging"
      >
        <NFormItem label="轮播图标题" path="title">
          <NInput
            v-model:value="formData.title"
            placeholder="请输入轮播图标题"
            maxlength="100"
            show-count
          />
        </NFormItem>

        <NFormItem label="轮播图片" path="imageUrl">
          <SwiperUpload v-model="formData.imageUrl" />
        </NFormItem>

        <NFormItem label="跳转链接" path="linkUrl">
          <NInput
            v-model:value="formData.linkUrl"
            placeholder="请输入点击跳转的链接（可选）"
            maxlength="500"
          />
        </NFormItem>

        <NFormItem label="排序" path="sort">
          <NInputNumber
            v-model:value="formData.sort"
            :min="0"
            :max="999"
            placeholder="数字越小越靠前"
            style="width: 100%"
          />
        </NFormItem>

        <NFormItem label="状态" path="status">
          <NRadioGroup v-model:value="formData.status">
            <NSpace>
              <NRadioButton :value="0">显示</NRadioButton>
              <NRadioButton :value="1">隐藏</NRadioButton>
            </NSpace>
          </NRadioGroup>
        </NFormItem>

        <NFormItem label="描述信息" path="description">
          <NInput
            v-model:value="formData.description"
            type="textarea"
            placeholder="请输入轮播图描述（可选）"
            :rows="4"
            maxlength="500"
            show-count
          />
        </NFormItem>
      </NForm>
    </div>
  </PageContainer>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { Icon } from '@iconify/vue'
import {FormInst, FormRules, NButton} from 'naive-ui'
import { useMessage } from 'naive-ui'
import PageContainer from '@/components/layout/PageContainer.vue'
import SwiperUpload from '@/components/form/SwiperUpload.vue'
import { swiperApi, type SwiperCreateDTO, type SwiperUpdateDTO } from '@/api/swiper'

const router = useRouter()
const route = useRoute()
const message = useMessage()

const formRef = ref<FormInst | null>(null)
const submitting = ref(false)
const isEdit = ref(false)

const formData = ref<SwiperCreateDTO & { id?: number }>({
  title: '',
  imageUrl: '',
  linkUrl: '',
  sort: 1,
  status: 0,
  description: ''
})

const formRules: FormRules = {
  title: {
    required: true,
    message: '请输入轮播图标题',
    trigger: ['blur', 'change']
  },
  imageUrl: {
    required: true,
    message: '请上传轮播图片',
    trigger: 'change'
  },
  sort: {
    required: true,
    type: 'number',
    message: '请输入排序值',
    trigger: ['blur', 'change']
  },
  status: {
    required: true,
    type: 'number',
    message: '请选择状态',
    trigger: 'change'
  }
}

const goBack = () => {
  router.back()
}

const handleCancel = () => {
  router.back()
}

const handleSubmit = async () => {
  await formRef.value?.validate(async (errors) => {
    if (errors) {
      message.error('请完善表单信息')
      return
    }

    submitting.value = true
    try {
      const submitData = {
        title: formData.value.title,
        imageUrl: formData.value.imageUrl,
        linkUrl: formData.value.linkUrl || undefined,
        sort: formData.value.sort,
        status: formData.value.status,
        description: formData.value.description || undefined
      }

      let res
      if (isEdit.value && formData.value.id) {
        res = await swiperApi.updateSwiper(formData.value.id, submitData as SwiperUpdateDTO)
      } else {
        res = await swiperApi.createSwiper(submitData as SwiperCreateDTO)
      }

      if (res && (res.code === 0 || res.code === 200)) {
        message.success(isEdit.value ? '更新成功' : '创建成功')
        router.back()
      } else {
        message.error(res?.msg || '操作失败')
      }
    } catch (error: any) {
      console.error('操作异常:', error)
      message.error(error.response?.data?.msg || '操作失败')
    } finally {
      submitting.value = false
    }
  })
}

const loadSwiperData = async (id: number) => {
  try {
    const res = await swiperApi.getSwiperById(id)
    if (res && (res.code === 0 || res.code === 200)) {
      const data = res.data
      formData.value = {
        id: data.id,
        title: data.title,
        imageUrl: data.imageUrl,
        linkUrl: data.linkUrl,
        sort: data.sort,
        status: data.status,
        description: data.description
      }
    } else {
      message.error(res?.msg || '加载失败')
    }
  } catch (error) {
    console.error('加载异常:', error)
    message.error('加载失败')
  }
}

onMounted(() => {
  const id = route.params.id
  if (id) {
    isEdit.value = true
    loadSwiperData(Number(id))
  }
})
</script>

<style lang="scss" scoped>
.edit-form-container {
  max-width: 800px;
  margin: 0 auto;
  background: #fff;
  padding: 32px;
  border-radius: 8px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.06);
}
</style>
