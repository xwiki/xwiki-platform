<script setup lang="ts">
import { NodeViewWrapper } from "@tiptap/vue-3";
import { Ref, computed, ref } from "vue";
import Vue3DraggableResizable from "vue3-draggable-resizable";
import type { NodeViewProps } from "@tiptap/vue-3";

const { node, updateAttributes } = defineProps<NodeViewProps>();

const src = computed(() => {
  return node.attrs.src;
});

const alt = computed(() => {
  return node.attrs.alt;
});

type ImageDimensions = { width: number; height: number };

async function computeImageSize(src: string): Promise<ImageDimensions> {
  const img = new Image();

  let resolve;
  let reject;

  const p: Promise<ImageDimensions> = new Promise((res, rej) => {
    resolve = res;
    reject = rej;
  });

  img.onload = () => {
    resolve!({ width: img.width, height: img.height });
  };

  img.onerror = () => {
    reject!();
  };

  img.src = src;

  return p;
}

const loading: Ref<boolean> = ref(true);
const imageLoadingError: Ref<string | undefined> = ref(undefined);
const originalImageDimensions: Ref<ImageDimensions | undefined> =
  ref(undefined);
const hasChanged = ref(false);

computeImageSize(node.attrs.src)
  .then((dimensions) => {
    originalImageDimensions.value = recompute(dimensions);
  })
  .catch(() => {})
  .finally(() => (loading.value = false));

const w = computed({
  get() {
    return node.attrs.width ?? originalImageDimensions.value?.width;
  },
  set(value) {
    updateAttributes({
      width: value,
    });
  },
});
const h = computed({
  get() {
    return node.attrs.height ?? originalImageDimensions.value?.height;
  },
  set(value) {
    updateAttributes({
      height: value,
    });
  },
});

const isActive = defineModel<boolean>("isActive", { default: false });

function setActive() {
  isActive.value = true;
}

const computedStyle = computed(() => {
  if (!hasChanged.value) {
    return {
      width: `${originalImageDimensions.value?.width}px`,
      height: `${originalImageDimensions.value?.height}px`,
    };
  } else {
    return {};
  }
});

function recompute({ width, height }: ImageDimensions) {
  const maxWidth = 960;

  if (width > maxWidth) {
    const ratio = width / maxWidth;
    width = maxWidth;
    height = height / ratio;
  }
  return { width, height };
}

function onResize({ w: wp, h: hp }: { w: number; h: number }) {
  const { width, height } = recompute({ width: wp, height: hp });
  w.value = width;
  h.value = height;
  hasChanged.value = true;
}
</script>
<template>
  <node-view-wrapper @click="setActive">
    <div v-if="loading">Loading...</div>
    <div v-else-if="imageLoadingError !== undefined">
      {{ imageLoadingError }}
    </div>
    <div v-else class="image-container" :style="computedStyle">
      <!-- Resizing is currently deactivated as no backend supportit -->
      <vue3-draggable-resizable
        v-model:active="isActive"
        :parent="true"
        :draggable="false"
        :resizable="false"
        :h="h"
        :w="w"
        :handles="['br']"
        :lock-aspect-ratio="true"
        @resize-end="onResize"
      >
        <img :src="src" :alt="alt" class="img" />
      </vue3-draggable-resizable>
    </div>
  </node-view-wrapper>
</template>

<style scoped>
.img {
  width: 100%;
  height: 100%;
}

.image-container,
.image-container > div {
  position: relative;
  display: block;
}
</style>
