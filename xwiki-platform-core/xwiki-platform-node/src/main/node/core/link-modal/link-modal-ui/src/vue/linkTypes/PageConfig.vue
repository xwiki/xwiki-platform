<!--
  See the NOTICE file distributed with this work for additional
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
import { createLinkSuggestor } from "../../linkSuggest";
import { translations } from "../../translations";
import LinkConfig from "../LinkConfig.vue";
import SearchBox from "../SearchBox.vue";
import { filterMap, tryFallible } from "@xwiki/platform-fn-utils";
import { CIcon, Size } from "@xwiki/platform-icons";
import { LinkType } from "@xwiki/platform-link-suggest-api";
import { EntityType } from "@xwiki/platform-model-api";
import { inject, ref, watch } from "vue";
import { useI18n } from "vue-i18n";
import type { LinkData, LinkPageConfig } from "../../data/linkType";
import type { LinkEditionContext, LinkSuggestion } from "../../linkSuggest";
import type { SearchLinkSuggestor } from "../SearchBox.vue";
import type { DocumentReference } from "@xwiki/platform-model-api";

defineProps<{ linkData: LinkData }>();

const model = defineModel<LinkPageConfig>({
  required: true,
});

const linkEditionCtx = inject<LinkEditionContext>("linkEditionCtx")!;
const linkSuggestor = createLinkSuggestor(linkEditionCtx);

const getSuggestions: SearchLinkSuggestor<
  DocumentReference,
  LinkSuggestion
> = async (query) => {
  if (!linkSuggestor) {
    return false;
  }

  return filterMap(await linkSuggestor({ query }), (result) => {
    if (result.type !== LinkType.PAGE) {
      return null;
    }

    const ref = tryFallible(() =>
      linkEditionCtx.modelReferenceParser.parse(result.reference),
    );

    if (ref?.type !== EntityType.DOCUMENT) {
      return null;
    }

    return {
      key: result.url,
      value: ref,
      renderingData: result,
      equivalentQuery: result.reference,
    };
  });
};

function submit(ref: DocumentReference) {
  model.value = { ref, anchor: undefined, queryString: undefined };
}

function trySelectRaw(query: string): boolean {
  const ref = tryFallible(() =>
    linkEditionCtx.modelReferenceParser.parse(query),
  );

  if (ref?.type !== EntityType.DOCUMENT) {
    return false;
  }

  submit(ref);
  return true;
}

const { t } = useI18n({ messages: translations });

const query = ref("");
const suggestions = ref<LinkSuggestion[]>([]);
const loadingSuggestions = ref(false);

watch(query, async (query) => {
  if (!linkSuggestor) {
    return;
  }

  loadingSuggestions.value = true;
  suggestions.value = await linkSuggestor({ query });
  loadingSuggestions.value = false;
});
</script>

<template>
  <link-config :link-data>
    <template #config>
      <search-box
        :label="t('link-modal.target-types.page.reference')"
        :initial-value="
          model.ref !== null
            ? linkEditionCtx.modelReferenceSerializer.serialize(model.ref)!
            : null
        "
        :get-suggestions
        @select="submit"
        :try-submit-raw="trySelectRaw"
      >
        <template #renderSuggestion="{ renderingData: { title, segments } }">
          <c-icon name="file-earmark" :size="Size.Small" /> {{ title }}
          <br />

          <span v-for="segment in segments" :key="segment" class="segment">
            {{ segment }}
          </span>
        </template>
      </search-box>
    </template>

    <template #options>
      <x-text-field
        :label="t('link-modal.target-types.page.query-string')"
        v-model="model.queryString"
      />

      <x-text-field
        :label="t('link-modal.target-types.page.anchor')"
        v-model="model.anchor"
      />
    </template>
  </link-config>
</template>

<style scoped>
.segment {
  color: var(--cr-color-neutral-500);
  padding-right: 0.5rem;

  &:not(:last-child)::after {
    padding-left: 0.5rem;
    content: ">";
  }
}
</style>
