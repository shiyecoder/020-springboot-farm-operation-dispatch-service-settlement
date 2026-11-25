<template>
  <div class="uploader">
    <el-upload
      :show-file-list="false"
      :http-request="doUpload"
      accept="image/*,application/pdf"
      :disabled="disabled"
    >
      <div v-if="modelValue" class="preview">
        <img v-if="isImage" :src="modelValue" alt="已上传文件" />
        <div v-else class="file">{{ modelValue.split('/').pop() }}</div>
        <div class="mask">点击重新上传</div>
      </div>
      <div v-else class="empty">
        <el-icon><Plus /></el-icon>
        <span>点击上传照片 / 附件</span>
        <em>支持 jpg/png/pdf，≤5MB</em>
      </div>
    </el-upload>
    <div v-if="hint" class="hint">{{ hint }}</div>
  </div>
</template>

<script setup>
import { computed, ref } from 'vue'
import { ElMessage } from 'element-plus'
import axios from 'axios'

const props = defineProps({
  modelValue: { type: String, default: '' },
  hint: { type: String, default: '' },
  disabled: { type: Boolean, default: false }
})
const emit = defineEmits(['update:modelValue'])
const uploading = ref(false)

const isImage = computed(() => !/\.pdf$/i.test(props.modelValue || ''))

async function doUpload(opt) {
  const form = new FormData()
  form.append('file', opt.file)
  uploading.value = true
  try {
    const { data } = await axios.post('/api/file/upload', form, {
      headers: {
        Authorization: 'Bearer ' + localStorage.getItem('token'),
        'Content-Type': 'multipart/form-data'
      }
    })
    if (data.code !== 200) throw new Error(data.message)
    emit('update:modelValue', data.data.path)
    ElMessage.success('上传成功')
  } catch (e) {
    ElMessage.error(e.response?.data?.message || '上传失败')
  } finally {
    uploading.value = false
  }
}
</script>

<style scoped>
.uploader :deep(.el-upload) { width: 100%; }
.empty {
  height: 96px; border: 1px dashed #C6D9DA; border-radius: 8px;
  display: flex; flex-direction: column; align-items: center; justify-content: center;
  gap: 4px; color: #5C7379; background: #FAFDFD;
}
.empty em { font-style: normal; font-size: 11px; color: #8FA6AB; }
.preview { position: relative; width: 132px; height: 96px; border-radius: 8px; overflow: hidden; border: 1px solid #DCE7E8; }
.preview img { width: 100%; height: 100%; object-fit: cover; display: block; }
.preview .file { display: flex; align-items: center; justify-content: center; height: 100%; font-size: 12px; }
.preview .mask {
  position: absolute; inset: 0; background: rgba(11,58,70,.55); color: #fff;
  display: flex; align-items: center; justify-content: center; font-size: 12px;
  opacity: 0; transition: .2s;
}
.preview:hover .mask { opacity: 1; }
.hint { font-size: 11px; color: #8FA6AB; margin-top: 4px; }
</style>
