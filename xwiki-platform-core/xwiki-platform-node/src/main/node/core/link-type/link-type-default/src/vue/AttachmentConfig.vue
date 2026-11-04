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
import { LinkConfig, SearchBox } from "@xwiki/platform-link-modal-ui";
import { LinkType } from "@xwiki/platform-link-suggest-api";
import { EntityType } from "@xwiki/platform-model-api";
import { inject } from "vue";
import { useI18n } from "vue-i18n";
import type { LinkAttachmentConfig } from "../data/linkType";
import type {
  LinkEditionContext,
  SearchBoxSource,
} from "@xwiki/platform-link-modal-ui";
import type { Link } from "@xwiki/platform-link-suggest-api";
import type { LinkData } from "@xwiki/platform-link-type-api";
import type { AttachmentReference } from "@xwiki/platform-model-api";

defineProps<{ linkData: LinkData }>();

const model = defineModel<LinkAttachmentConfig>({
  required: true,
});

const linkEditionCtx = inject<LinkEditionContext>("linkEditionCtx")!;

const getSuggestions: SearchBoxSource<AttachmentReference, Link> = (query) =>
  suggestLinks(linkEditionCtx, query, LinkType.ATTACHMENT, (link) => {
    const ref = tryFallible(() =>
      linkEditionCtx.modelReferenceParser.parse(link.reference, {
        type: EntityType.ATTACHMENT,
      }),
    );
    return ref?.type === EntityType.ATTACHMENT ? ref : null;
  });

function submit(ref: AttachmentReference) {
  model.value = { ref, queryString: undefined };
}

function trySelectRaw(query: string): boolean {
  const ref = tryFallible(() =>
    linkEditionCtx.modelReferenceParser.parse(query, {
      type: EntityType.ATTACHMENT,
    }),
  );

  if (ref?.type !== EntityType.ATTACHMENT) {
    return false;
  }

  submit(ref);
  return true;
}

const { t } = useI18n({ messages: translations });
</script>

<template>
  <link-config :link-data>
    <template #config>
      <search-box
        v-bind="{ 'data-test': 'linkAttachmentReference' }"
        :label="t('link-type.target-types.attachment.reference')"
        :initial-value="
          model.ref !== null
            ? linkEditionCtx.modelReferenceSerializer.serialize(model.ref)!
            : null
        "
        :get-suggestions
        @select="submit"
        :try-submit-raw="trySelectRaw"
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

    <template #options>
      <x-text-field
        v-bind="{ 'data-test': 'linkAttachmentQueryString' }"
        :label="t('link-type.target-types.attachment.query-string')"
        v-model="model.queryString"
      />
    </template>
  </link-config>
</template>

<style scoped>
.suggestion-hint {
  color: var(--cr-color-neutral-500);
}
</style>
