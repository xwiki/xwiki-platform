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
  | { status: "uninitialized" }
  | {
      status: "resolved";
      results: SearchLinkSuggestion<T, U>[];
    }
  | {
      status: "backendSearchUnsupported";
    }
  | { status: "closed" }
>({ status: "uninitialized" });

const loading = ref(false);

const performSearch = debounce(async (search: string) => {
  // Skip the search when the query is empty (e.g., right after focusing an empty field): searching for
  // everything is wasteful (it fetches unrelated content from the whole wiki) and useless (the caller always
  // filters the result down by target type, so this never surfaces anything relevant anyway).
  if (search.trim().length === 0) {
    closeSuggestions();
    return;
  }

  loading.value = true;

  const results = await getSuggestions(search);

  loading.value = false;
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
    suggestions.value = { status: "closed" };
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

function onKeydown(event: KeyboardEvent): void {
  if (event.key === "Enter") {
    event.preventDefault();
    onEnterKey();
  } else if (event.key === "ArrowUp") {
    event.preventDefault();
    focusRelativeSuggestion(-1);
  } else if (event.key === "ArrowDown") {
    event.preventDefault();
    focusRelativeSuggestion(+1);
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
  <div class="search-box">
    <x-text-field
      :label
      :placeholder
      v-model="query"
      @keydown="onKeydown"
      @focus="performSearch(query)"
      @blur="closeSuggestions()"
    />

    <div
      class="suggestions-container"
      v-if="
        (suggestions.status !== 'uninitialized' || loading) &&
        suggestions.status !== 'closed'
      "
    >
      <h3
        class="status-message"
        v-if="suggestions.status === 'backendSearchUnsupported'"
      >
        <!-- TODO: add translation -->
        Backend search is unsupported.
      </h3>

      <h3
        class="status-message"
        v-if="suggestions.status === 'uninitialized' && loading"
      >
        <!-- TODO: add translation -->
        Loading...
      </h3>

      <h3
        class="status-message"
        v-if="
          suggestions.status === 'resolved' && suggestions.results.length === 0
        "
      >
        <!-- TODO: add translation -->
        No result found
      </h3>

      <!-- NOTE: `@mousedown.prevent` prevents the `blur` event from the query `input` field above to trigger *before*
                  the suggestions' `click` handler -->
      <ul
        class="suggestions"
        @mousedown.prevent
        v-if="
          suggestions.status === 'resolved' && suggestions.results.length > 0
        "
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
      </ul>
    </div>
  </div>
</template>

<style>
.search-box {
  position: relative;
}

.status-message {
  font-weight: normal;
  font-style: italic;
  text-align: center;
  padding: 0;
  margin: 0;
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
