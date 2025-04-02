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
import { LinkEditionContext } from "../../components/linkEditionContext";
import { LinkSuggestion } from "../../components/linkSuggest";
import { LinkType } from "@xwiki/cristal-link-suggest-api";
import { debounce } from "lodash-es";
import { ref, watch } from "vue";

const { linkEditionCtx } = defineProps<{
  linkEditionCtx: LinkEditionContext;
}>();

const emit = defineEmits<{
  selected: [{ url: string; reference: string; title: string }];
}>();

const query = ref("");

const results = ref<LinkSuggestion[]>([]);

watch(
  query,
  debounce(async (query) => {
    const suggestions = await linkEditionCtx.suggestLink({ query });

    results.value = suggestions.filter(
      (suggestion) => suggestion.type === LinkType.PAGE,
    );
  }),
);

function select(result: LinkSuggestion) {
  emit("selected", {
    url: result.url,
    reference: result.reference,
    title: result.title,
  });

  results.value = [];
}
</script>

<template>
  <div>
    <input v-model="query" type="text" placeholder="Link query goes here" />

    <ul>
      <li v-for="result in results" :key="result.url" @click="select(result)">
        {{ result.title }}
      </li>
    </ul>
  </div>
</template>

<style scoped>
tr {
  cursor: pointer;
}
</style>
