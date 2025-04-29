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
import LinkSuggestList from "./LinkSuggestList.vue";
import { LinkEditionContext } from "../../components/linkEditionContext";
import { LinkSuggestion } from "../../components/linkSuggest";
import { CIcon } from "@xwiki/cristal-icons";
import { LinkType } from "@xwiki/cristal-link-suggest-api";
import { EntityReference } from "@xwiki/cristal-model-api";
import { debounce } from "lodash-es";
import { onMounted, ref, shallowRef } from "vue";

const { linkEditionCtx, current, hideTitle } = defineProps<{
  linkEditionCtx: LinkEditionContext;
  current: {
    title: string;
    reference: EntityReference | null;
    url: string;
  } | null;
  hideTitle?: boolean;
}>();

const emit = defineEmits<{
  update: [{ title: string; reference: EntityReference | null; url: string }];
}>();

const target = ref({
  url: current?.url ?? "",
  reference: current?.reference ?? null,
});

const title = ref(current?.title ?? "");
const results = ref<LinkSuggestion[]>([]);

const query = ref(
  (current?.reference
    ? linkEditionCtx.modelReferenceSerializer.serialize(current.reference)
    : current?.url) ?? "",
);

const search = debounce(async (query: string) => {
  target.value = { url: query, reference: null };

  if (query.startsWith("http://") || query.startsWith("https://")) {
    results.value = [];
    return;
  }

  const suggestions = await linkEditionCtx.suggestLink({ query });

  results.value = suggestions.filter(
    (suggestion) => suggestion.type === LinkType.PAGE,
  );
});

function select(result: LinkSuggestion) {
  const reference = linkEditionCtx.modelReferenceParser.parse(result.reference);

  title.value ||= result.title;
  query.value = linkEditionCtx.modelReferenceSerializer.serialize(reference)!;
  target.value = {
    url: result.url,
    reference,
  };
  results.value = [];
}

function submit() {
  emit("update", {
    title: title.value,
    url: target.value.url,
    reference: target.value.reference,
  });
}

const listInstance = shallowRef<InstanceType<typeof LinkSuggestList>>();
const queryInput = shallowRef<HTMLInputElement>();

onMounted(() => {
  queryInput.value?.focus();
});
</script>

<template>
  <div class="fields">
    <template v-if="!hideTitle">
      <label>
        <c-icon name="tag" />
      </label>

      <input v-model="title" type="text" placeholder="Title" />
    </template>

    <label>
      <c-icon name="link" />
    </label>

    <input
      ref="queryInput"
      v-model="query"
      type="text"
      placeholder="URL or page reference"
      @input="search(query)"
      @keydown.up.prevent="listInstance?.focusRelative(-1)"
      @keydown.down.prevent="listInstance?.focusRelative(1)"
      @keydown.enter.prevent="listInstance?.select()"
    />

    <x-btn @click="submit">Save</x-btn>
  </div>

  <hr />

  <LinkSuggestList
    ref="listInstance"
    :links="
      results.map((result) => ({
        type: LinkType.PAGE,
        segments: result.segments,
        title: result.title,
        reference: result.reference,
        url: result.url,
      }))
    "
    @select="select"
  />
</template>

<style scoped>
.fields {
  margin: 10px;
  display: grid;
  grid-template-columns: [labels] auto [controls] 1fr;
  grid-auto-flow: row;
  gap: 5px;
}

.fields > label {
  grid-column: labels;
  grid-row: auto;
}

.fields > input,
.fields > button {
  grid-column: controls;
  grid-row: auto;
  outline: none;
}

input {
  border: 1px solid lightgray;
  padding: 5px;
}
</style>
