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
import LoadingSpinner from "./LoadingSpinner.vue";
import UserAvatar from "./UserAvatar.vue";
import { computed, ref, useTemplateRef, watch } from "vue";
import { useI18n } from "vue-i18n";
import type { ToolbarVersion } from "./types";

const props = defineProps<{
  /**
   * The version to display.
   */
  version?: ToolbarVersion;
  /**
   * The URL of the rendered content of the version to display.
   */
  contentUrl?: string;
  /**
   * The version creation date, formatted for display.
   */
  date?: string;
}>();

const { t } = useI18n();

const contentHost = useTemplateRef("contentHost");
const loading = ref(false);
const failed = ref(false);

// The title is a translation message with parameters that we want to replace with HTML elements, so we split it.
const PLACEHOLDERS = ["__version__", "__author__", "__date__"];
const titleParts = computed(() =>
  t("core.document.modificationWithVersion", PLACEHOLDERS).split(
    new RegExp(`(${PLACEHOLDERS.join("|")})`),
  ),
);

const author = computed(() => {
  const author = props.version?.author;
  return typeof author === "object" && author.reference
    ? { ...author, reference: author.reference }
    : undefined;
});

async function fetchVersionContent(url: string): Promise<string> {
  const response = await fetch(url);
  if (!response.ok) {
    throw new Error(response.statusText);
  }
  return await response.text();
}

/**
 * Loads the content of a version in the given shadow root.
 *
 * @param shadowRoot - where to display the version content
 * @param url - the URL of the version content
 */
async function loadVersion(shadowRoot: ShadowRoot, url: string) {
  // The user may select another version while the content of this version is being loaded.
  const isSelected = () => props.contentUrl === url;
  loading.value = true;
  try {
    const html = await fetchVersionContent(url);
    if (isSelected()) {
      shadowRoot.innerHTML = html;
    }
  } catch (error) {
    console.error(error);
    failed.value = isSelected();
  } finally {
    if (isSelected()) {
      loading.value = false;
    }
  }
}

watch(
  () => props.contentUrl,
  (url) => {
    // The version content is displayed in a shadow root in order to isolate its styles from the rest of the page.
    const shadowRoot =
      contentHost.value!.shadowRoot ??
      contentHost.value!.attachShadow({ mode: "open" });
    shadowRoot.innerHTML = "";
    failed.value = false;
    if (url !== undefined) {
      loadVersion(shadowRoot, url);
    }
  },
  { flush: "post" },
);
</script>

<template>
  <div
    id="realtime-version-modal"
    class="modal fade"
    tabindex="-1"
    role="dialog"
    :data-version="version ? JSON.stringify(version) : undefined"
  >
    <div class="modal-dialog modal-lg" role="document">
      <div class="modal-content">
        <div class="modal-header">
          <button
            type="button"
            class="close"
            data-dismiss="modal"
            :aria-label="t('realtime.close')"
          >
            <span aria-hidden="true">&times;</span>
          </button>
          <div class="modal-title">
            <template v-for="(part, index) in titleParts" :key="index">
              <span
                v-if="part === '__version__'"
                class="realtime-version-number"
                >{{ version?.number }}</span
              >
              <span
                v-else-if="part === '__author__'"
                class="realtime-version-author"
                ><UserAvatar v-if="author" :user="author" /><template v-else>{{
                  typeof version?.author === "object"
                    ? version?.author.name
                    : version?.author
                }}</template></span
              >
              <span
                v-else-if="part === '__date__'"
                class="realtime-version-date"
                >{{ date }}</span
              >
              <template v-else>{{ part }}</template>
            </template>
          </div>
        </div>
        <div class="modal-body">
          <LoadingSpinner v-if="loading" />
          <template v-else-if="failed">{{
            t("realtime.failedToLoadVersion")
          }}</template>
          <div ref="contentHost" class="realtime-version-content"></div>
        </div>
        <div class="modal-footer">
          <button type="button" class="btn btn-default" data-dismiss="modal">
            {{ t("realtime.close") }}
          </button>
        </div>
      </div>
    </div>
  </div>
</template>

<style>
#realtime-version-modal .modal-body {
  /* We want to see the modal buttons from the start (without scrolling) even when the displayed content is large. */
  max-height: calc(100vh - 200px);
  overflow: auto;
}

.realtime-version-author .realtime-user-name {
  vertical-align: inherit;
}
</style>
