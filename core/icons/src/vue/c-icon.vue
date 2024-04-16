<script setup lang="ts">
import "bootstrap-icons/font/bootstrap-icons.css";
import { computed } from "vue";
import { Size } from "../size";

// TODO: make sure that we have the good parameters available for accessibility
const props = withDefaults(
  defineProps<{
    name: string;
    size?: Size;
  }>(),
  {
    size: Size.Normal,
  },
);

function sizeToClass(size: Size): string | undefined {
  let clazz = undefined;
  if (size === Size.Big) {
    clazz = "big";
  } else if (size === Size.Small) {
    clazz = "small";
  }
  return clazz;
}

const classes = computed(() => {
  const ret = [`bi-${props.name}`];
  const sizeClass = sizeToClass(props.size);
  if (sizeClass) {
    ret.push(sizeClass);
  }
  return ret;
});
// TODO: currently names are bound to bootstrap class names. We'll need to add
// an indirection as soon as we want to support several icon sets.
</script>

<template>
  <span :class="classes"></span>
</template>

<style scoped>
span {
  font-size: 1.3rem;
}

span.big {
  font-size: 1.6rem;
}

span.small {
  font-size: 1rem;
}
</style>
