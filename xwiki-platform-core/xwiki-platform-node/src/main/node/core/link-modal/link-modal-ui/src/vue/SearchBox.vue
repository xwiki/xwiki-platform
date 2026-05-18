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
<script type="module" lang="ts">
type SearchLinkSuggestion<T, U> = {
  value: T;
  renderingData: U;
  key: string;
  equivalentQuery: string;
};

type SearchLinkSuggestor<T, U> = (
  query: string,
) => Promise<SearchLinkSuggestion<T, U>[] | false>;

export type { SearchLinkSuggestion, SearchLinkSuggestor };
</script>

<script setup lang="ts" generic="T, U">
import { debounce } from "lodash-es";
import { ref, shallowRef, watch } from "vue";

const { initialValue, getSuggestions, trySubmitRaw } = defineProps<{
  /**
   * Input field label
   */
  label: string;

  /**
   * The search box's initial value
   */
  initialValue: string | null;

  /**
   * The search box's placeholder (when empty)
   */
  placeholder?: string;

  /**
   * Perform a search
   *
   * @param query - A user query (text)
   *
   * @returns Suggestions matching the provided query
   */
  getSuggestions: SearchLinkSuggestor<T, U>;

  /**
   * Triggered when the user tries to submit a raw input (e.g. an URL)
   *
   * @param raw - The raw input text field
   *
   * @returns `true` in case of valid input, `false` to keep the list open
   */
  trySubmitRaw?: (raw: string) => boolean;
}>();

defineSlots<{
  /**
   * Render a single suggestion
   */
  renderSuggestion(suggestion: SearchLinkSuggestion<T, U>): unknown;
}>();

const emit = defineEmits<{
  /**
   * Triggered when a result is selected in the list of suggestions
   */
  select: [suggestion: T];
}>();

const query = ref(initialValue ?? "");

// When programatically updating `query` (e.g. after the user selected a suggestion),
// this trigger prevents the query watcher from performing a new search (which would be pointless)
const justDynamicallyUpdatedQuery = ref(false);

const suggestions = shallowRef<
  | { status: "loading" }
  | {
      status: "resolved";
      results: SearchLinkSuggestion<T, U>[];
    }
  | {
      status: "backendSearchUnsupported";
    }
>({ status: "resolved", results: [] });

const performSearch = debounce(async (search: string) => {
  suggestions.value = { status: "loading" };

  const results = await getSuggestions(search);

  suggestions.value =
    results === false
      ? { status: "backendSearchUnsupported" }
      : { status: "resolved", results: results };
});

function selectSuggestion(suggestion: SearchLinkSuggestion<T, U>): void {
  console.log(suggestion);
  justDynamicallyUpdatedQuery.value = true;
  query.value = suggestion.equivalentQuery;
  closeSuggestions();
  emit("select", suggestion.value);
}

function closeSuggestions(): void {
  if (suggestions.value.status === "resolved") {
    suggestions.value = { status: "resolved", results: [] };
  }
}

// eslint-disable-next-line max-statements
function focusRelativeSuggestion(relativeIdx: number): void {
  if (suggestions.value.status !== "resolved") {
    return;
  }

  const { results } = suggestions.value;

  if (results.length === 0) {
    return;
  }

  if (focusedSuggestionKey.value === null) {
    focusedSuggestionKey.value = results[0].key;
    return;
  }

  const currIndex = results.findIndex(
    (sugg) => sugg.key === focusedSuggestionKey.value,
  );

  if (currIndex === -1) {
    focusedSuggestionKey.value = results[0].key;
    return;
  }

  if (
    currIndex + relativeIdx >= 0 &&
    currIndex + relativeIdx < results.length
  ) {
    focusedSuggestionKey.value = results[currIndex + relativeIdx].key;
  }
}

const focusedSuggestionKey = ref<string | null>(null);

function onEnterKey(): void {
  if (focusedSuggestionKey.value !== null) {
    if (suggestions.value.status !== "resolved") {
      throw new Error(
        "Internal error: suggestions not resolved while focused suggestion key set",
      );
    }

    const suggestion = suggestions.value.results.find(
      (sugg) => sugg.key === focusedSuggestionKey.value,
    );

    if (!suggestion) {
      throw new Error("Internal error: focused suggestion not found");
    }

    selectSuggestion(suggestion);
  } else if (trySubmitRaw?.(query.value)) {
    closeSuggestions();
  }
}

watch(query, (query) => {
  if (justDynamicallyUpdatedQuery.value) {
    justDynamicallyUpdatedQuery.value = false;
    return;
  }

  performSearch(query);
});

watch(suggestions, (suggestions) => {
  focusedSuggestionKey.value =
    suggestions.status === "resolved" && suggestions.results.length > 0
      ? suggestions.results[0].key
      : null;
});
</script>

<template>
  <div>
    <x-text-field
      :label
      :placeholder
      v-model="query"
      @keydown.enter.prevent="onEnterKey()"
      @keydown.up.prevent="focusRelativeSuggestion(-1)"
      @keydown.down.prevent="focusRelativeSuggestion(+1)"
      @focus="performSearch(query)"
      @blur="closeSuggestions()"
    />

    <div class="suggestions-container">
      <h3
        class="status-message"
        v-if="suggestions.status === 'backendSearchUnsupported'"
      >
        <!-- TODO: add translation -->
        Backend search is unsupported.
      </h3>

      <h3 class="status-message" v-if="suggestions.status === 'loading'">
        <!-- TODO: add translation -->
        Loading...
      </h3>

      <!-- NOTE: `@mousedown.prevent` prevents the `blur` event from the query `input` field above to trigger *before*
                  the suggestions' `click` handler -->
      <ul
        class="suggestions"
        @mousedown.prevent
        v-if="suggestions.status === 'resolved'"
      >
        <li
          v-for="suggestion in suggestions.results"
          :key="suggestion.key"
          class="suggestion"
          :class="focusedSuggestionKey === suggestion.key ? ['focused'] : []"
          @click="selectSuggestion(suggestion)"
          @mouseenter="focusedSuggestionKey = suggestion.key"
          @mouseleave="
            if (focusedSuggestionKey === suggestion.key) {
              focusedSuggestionKey = null;
            }
          "
        >
          <slot name="renderSuggestion" v-bind="suggestion">
            <!-- TODO: add translation -->
            <em>Missing suggestion rendering slot</em>
          </slot>
        </li>

        <li class="suggestion" v-if="suggestions.results.length === 0">
          <!-- TODO: add translation -->
          <em>No result found</em>
        </li>
      </ul>
    </div>
  </div>
</template>

<style>
.status-message {
  color: darkgray;
}

.suggestions-container {
  position: absolute;
  z-index: 100;
  border: var(--cr-input-border-width) solid var(--cr-input-border-color);
  border-radius: var(--cr-border-radius-large);
  background: var(--cr-color-neutral-50);
  padding: var(--cr-spacing-x-small);
  width: calc(100% - var(--cr-spacing-x-small) * 4);
}

.suggestions {
  list-style-type: none;
  padding: 0;
  margin: 0;
  width: 100%;
}

.suggestion {
  padding: var(--cr-spacing-medium);
  border-radius: var(--cr-border-radius-medium);
  cursor: pointer;
  width: 100%;

  &.focused {
    background-color: var(--cr-color-neutral-300);
  }
}
</style>
