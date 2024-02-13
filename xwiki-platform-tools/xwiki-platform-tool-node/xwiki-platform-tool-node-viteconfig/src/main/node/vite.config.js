import {resolve} from 'path'
import {defineConfig} from 'vite'
import dts from "vite-plugin-dts"
import peerDepsExternal from 'rollup-plugin-peer-deps-external';

export default defineConfig({
  build: {
    sourcemap: true,
    lib: {
      entry: resolve(process.cwd(), 'lib/main.ts'),
      fileName: 'main',
      formats: ['amd']
    },
  },
  plugins: [
    dts(),
    peerDepsExternal()
  ]
})