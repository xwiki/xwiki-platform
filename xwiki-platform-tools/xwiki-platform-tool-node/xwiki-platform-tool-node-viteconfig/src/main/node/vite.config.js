import {resolve} from 'path'
import {defineConfig} from 'vite'
import dts from "vite-plugin-dts"
import peerDepsExternal from 'rollup-plugin-peer-deps-external';

export default defineConfig({
  build: {
    lib: {
      // Could also be a dictionary or array of multiple entry points
      entry: resolve(process.cwd(), 'lib/main.ts'),
      name: 'xwiki-platform-lib-b',
      fileName: 'main',
      formats: ['umd']
    },
  },
  plugins: [
    dts(),
    peerDepsExternal()
  ]
})