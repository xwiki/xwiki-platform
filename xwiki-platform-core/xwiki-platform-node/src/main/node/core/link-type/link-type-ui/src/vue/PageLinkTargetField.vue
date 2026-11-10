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
import { entityReferenceQuery } from "../entityReferenceQuery";
import { suggestLinks } from "../linkSuggestions";
import { translations } from "../translations";
import { tryFallible } from "@xwiki/platform-fn-utils";
import { CIcon, Size } from "@xwiki/platform-icons";
import { SearchBox } from "@xwiki/platform-link-modal-ui";
import { LinkType } from "@xwiki/platform-link-suggest-api";
import { EntityType } from "@xwiki/platform-model-api";
import { useI18n } from "vue-i18n";
import type { SearchBoxSource } from "@xwiki/platform-link-modal-ui";
import type {
  Link,
  LinkSuggestServiceProvider,
} from "@xwiki/platform-link-suggest-api";
import type { LinkContext } from "@xwiki/platform-link-type-api";
import type { PageLinkTargetConfig } from "@xwiki/platform-link-type-default";
import type { DocumentReference } from "@xwiki/platform-model-api";
import type {
  ModelReferenceParserProvider,
  ModelReferenceSerializerProvider,
} from "@xwiki/platform-model-reference-api";
import type { Container } from "inversify";

const { depsContainer, autofocus, linkContext } = defineProps<{
  depsContainer: Container;

  /**
   * Whether the search box is focused when the link modal opens.
   */
  autofocus?: boolean;

  /**
   * Where the link is: the page references are displayed and parsed relative to the document holding it.
   */
  linkContext?: LinkContext;
}>();

const model = defineModel<PageLinkTargetConfig>({
  required: true,
});

const modelReferenceParser = depsContainer
  .get<ModelReferenceParserProvider>("ModelReferenceParserProvider")
  .get()!;
const modelReferenceSerializer = depsContainer
  .get<ModelReferenceSerializerProvider>("ModelReferenceSerializerProvider")
  .get()!;
const linkSuggestService = depsContainer
  .get<LinkSuggestServiceProvider>("LinkSuggestServiceProvider")
  .get();

/**
 * A page selected in the search box: the title of the page is known only when the user picks a link suggestion.
 */
type PageSelection = { ref: DocumentReference; title?: string };

/**
 * @param reference - a serialized page reference, typed by the user or held by a link suggestion
 * @returns the parsed page reference, or `null` when it doesn't designate a page
 */
function parseReference(reference: string): PageSelection | null {
  const ref = tryFallible(() =>
    modelReferenceParser.parse(reference, {
      base: linkContext?.sourceDocument,
    }),
  );
  return ref?.type === EntityType.DOCUMENT ? { ref } : null;
}

/**
 * @param ref - a page reference
 * @returns the text of the search box designating the page
 */
function toQuery(ref: DocumentReference): string {
  return entityReferenceQuery(
    ref,
    modelReferenceSerializer,
    "doc:",
    linkContext,
  );
}

const getSuggestions: SearchBoxSource<PageSelection, Link> = (query) =>
  suggestLinks(
    linkSuggestService,
    query,
    LinkType.PAGE,
    (link) => {
      const selection = parseReference(link.reference);
      return selection && { ...selection, title: link.label };
    },
    ({ ref }) => toQuery(ref),
  );

function select(selection: PageSelection | null) {
  model.value = {
    ref: selection?.ref ?? null,
    title: selection?.title,
    anchor: undefined,
    queryString: undefined,
  };
}

const { t } = useI18n({ messages: translations });
</script>

<template>
  <search-box
    v-bind="{ 'data-test': 'linkPageReference' }"
    :label="t('link-type.target-types.page.reference')"
    :initial-value="model.ref !== null ? toQuery(model.ref) : null"
    :get-suggestions
    :autofocus
    :parse-query="parseReference"
    @select="select"
  >
    <template #renderSuggestion="{ renderingData: { label, hint } }">
      <div class="suggestion-label">
        <c-icon name="file-earmark" :size="Size.Small" />
        <span class="suggestion-title">{{ label }}</span>
      </div>
      <div class="suggestion-hint" v-if="hint">{{ hint }}</div>
    </template>
  </search-box>
</template>

<style scoped>
.suggestion-label {
  display: flex;
  align-items: center;
  /* About the width of a space, which separates the icons from the labels in the menus of the skin. */
  gap: 0.25em;
}

.suggestion-hint {
  color: var(--cr-color-neutral-500);
  font-size: var(--cr-font-size-small);
}
</style>
