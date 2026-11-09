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
import type { AttachmentLinkTargetConfig } from "@xwiki/platform-link-type-default";
import type { AttachmentReference } from "@xwiki/platform-model-api";
import type {
  ModelReferenceParserProvider,
  ModelReferenceSerializerProvider,
} from "@xwiki/platform-model-reference-api";
import type { Container } from "inversify";

const { depsContainer } = defineProps<{ depsContainer: Container }>();

const model = defineModel<AttachmentLinkTargetConfig>({
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
 * @param reference - a serialized attachment reference, typed by the user or held by a link suggestion
 * @returns the parsed attachment reference, or `null` when it doesn't designate an attachment
 */
function parseReference(reference: string): AttachmentReference | null {
  const ref = tryFallible(() =>
    modelReferenceParser.parse(reference, { type: EntityType.ATTACHMENT }),
  );
  return ref?.type === EntityType.ATTACHMENT ? ref : null;
}

const getSuggestions: SearchBoxSource<AttachmentReference, Link> = (query) =>
  suggestLinks(linkSuggestService, query, LinkType.ATTACHMENT, (link) =>
    parseReference(link.reference),
  );

function select(ref: AttachmentReference) {
  model.value = { ref, queryString: undefined };
}

const { t } = useI18n({ messages: translations });
</script>

<template>
  <search-box
    v-bind="{ 'data-test': 'linkAttachmentReference' }"
    :label="t('link-type.target-types.attachment.reference')"
    :initial-value="
      model.ref !== null ? modelReferenceSerializer.serialize(model.ref)! : null
    "
    :get-suggestions
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
