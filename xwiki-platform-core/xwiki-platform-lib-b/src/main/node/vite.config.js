// vite.config.js
import {defineConfig, mergeConfig} from 'vite'
import viteconfig from "xwiki-platform-tool-node-viteconfig"

export default mergeConfig(viteconfig, defineConfig({
  build: {
    lib: {
      name: 'xwiki-platform-lib-b'
    }
  }
}))