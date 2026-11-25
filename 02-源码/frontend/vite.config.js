import { defineConfig } from 'vite'
import vue from '@vitejs/plugin-vue'

// 前端端口固定 5170（通道 1），/api 与 /uploads 代理到本通道后端 8080
export default defineConfig({
  plugins: [vue()],
  server: {
    host: '0.0.0.0',
    port: 5170,
    strictPort: true,
    proxy: {
      '/api': { target: 'http://127.0.0.1:8080', changeOrigin: true },
      '/uploads': { target: 'http://127.0.0.1:8080', changeOrigin: true }
    }
  }
})
