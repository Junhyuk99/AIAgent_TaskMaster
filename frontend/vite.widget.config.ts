import { defineConfig } from 'vite'
import react from '@vitejs/plugin-react'
import { resolve } from 'path'

// Widget library build configuration
export default defineConfig({
  plugins: [react()],
  define: {
    'process.env.NODE_ENV': JSON.stringify('production'),
  },
  build: {
    lib: {
      entry: resolve(__dirname, 'src/widget/index.tsx'),
      name: 'AIAgentWidget',
      formats: ['umd', 'es'],
      fileName: (format) => `widget.${format}.js`,
    },
    rollupOptions: {
      // Don't externalize React - bundle it with the widget
      external: [],
      output: {
        globals: {},
        // Inline all CSS into the JS bundle
        assetFileNames: 'widget.[ext]',
      },
    },
    outDir: 'dist/widget',
    emptyOutDir: true,
    minify: 'terser',
    sourcemap: true,
    cssCodeSplit: false,
  },
})
