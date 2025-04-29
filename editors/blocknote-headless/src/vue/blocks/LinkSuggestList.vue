<!--
See the LICENSE file distributed with this work for additional
information regarding copyright ownership.

This is free software; you can redistribute it and/or modify it
under the terms of the GNU Lesser General Public License as
published by the Free Software Foundation; either version 2.1 of
the License, or (at your option) any later version.

This software is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
Lesser General Public License for more details.

You should have received a copy of the GNU Lesser General Public
License along with this software; if not, write to the Free
Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
02110-1301 USA, or see the FSF site: http://www.fsf.org.
-->
<script setup lang="ts">
import { LinkSuggestion } from "../../components/linkSuggest";
import { CIcon, Size } from "@xwiki/cristal-icons";
import { LinkType } from "@xwiki/cristal-link-suggest-api";
import { onMounted, ref, watch } from "vue";

const { links } = defineProps<{
  links: LinkSuggestion[];
  images?: boolean;
}>();

const selected = ref<LinkSuggestion | undefined>(undefined);

onMounted(() => {
  selected.value = links.at(0);
});

watch(
  () => links,
  (links) => {
    if (
      selected.value &&
      links.findIndex((link) => link.url === selected.value?.url) === -1
    ) {
      selected.value = links.at(0);
    }
  },
);

const emit = defineEmits<{
  select: [LinkSuggestion];
}>();

defineExpose({
  // eslint-disable-next-line max-statements
  focusRelative(relative: number): void {
    if (links.length === 0) {
      return;
    }

    const selectedUrl = selected.value?.url;

    if (!selectedUrl) {
      selected.value = links[0];
      return;
    }

    const currentIndex = links.findIndex((link) => link.url === selectedUrl);

    if (currentIndex === -1) {
      throw new Error("Unreachable");
    }

    const newIndex = currentIndex + relative;

    if (newIndex < 0) {
      selected.value = links[0];
    } else if (newIndex >= links.length) {
      selected.value = links[links.length - 1];
    } else {
      selected.value = links[newIndex];
    }
  },

  select() {
    if (selected.value) {
      emit("select", selected.value);
    }
  },
});

const dataUrlAttr = "data-url";
const wrapperEl = ref<Element>();

async function waitFor<T>(value: () => T | undefined): Promise<T> {
  while (true) {
    const got = value();

    if (got !== undefined) {
      return got;
    }

    await new Promise((r) => setTimeout(r, 1000));
  }
}

watch(selected, async (selected) => {
  if (!selected) {
    return;
  }

  const element = await waitFor(() =>
    wrapperEl.value?.querySelector(`[data-url="${selected.url}"]`),
  );

  if (!element) {
    throw new Error("Internal error: corresponding list item is not defined");
  }

  element.scrollIntoView({ block: "nearest" });
});
</script>

<template>
  <div ref="wrapperEl">
    <div
      v-for="link in links"
      :key="link.url"
      :class="{
        parent: true,
        selected: link.url === selected?.url,
        'no-image': !images,
      }"
      v-bind="{ [dataUrlAttr]: link.url }"
      @click="
        selected = link;
        $emit('select', link);
      "
    >
      <div v-if="images" class="image">
        <img :src="link.url" alt="" />
      </div>

      <div class="text">
        <c-icon
          class="icon-type"
          :size="Size.Small"
          :name="link.type == LinkType.PAGE ? 'file-earmark' : 'paperclip'"
        />

        {{ link.title }}
      </div>

      <div class="breadcrumb">
        <XBreadcrumb
          :items="link.segments.map((segment) => ({ label: segment }))"
        />
      </div>
    </div>
  </div>
</template>

<style scoped>
.parent {
  display: grid;
  grid-template-columns: repeat(2, auto);
  grid-template-rows: repeat(2, auto);
  grid-column-gap: 0;
  grid-row-gap: 0;
  cursor: pointer;
  margin: 0.5em;
  padding: 0.5em;
}

.parent:hover,
.parent.selected {
  background-color: var(--cr-color-neutral-200);
}

.image {
  grid-area: 1 / 1 / 3 / 2;
}

.text {
  grid-area: 1 / 2 / 2 / 3;
}

.breadcrumb {
  grid-area: 2 / 2 / 3 / 3;
}

img {
  float: left;
  max-width: 4em;
  max-height: 4em;
  margin-right: var(--cr-spacing-small);
}

/* The layout is different if the suggestion does not have an image. */
.parent.no-image {
  grid-template-columns: 1fr;
  grid-template-rows: repeat(2, 1fr);
}

.no-image .text {
  grid-area: 1 / 1 / 2 / 2;
}
.no-image .breadcrumb {
  grid-area: 2 / 1 / 3 / 2;
}
</style>
