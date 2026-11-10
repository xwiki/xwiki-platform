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
// NOTE: type="module" is required to export types from within a Vue component file
// Given the types defined here are only relevant to this specific component, extracting
// to a separate file would mean more code scatter.

/**
 * An item listed by the search box, i.e. one of the results of a search.
 *
 * @typeParam T - the type of the value emitted when the item is selected
 * @typeParam U - the type of the data used to render the item
 * @since 18.10.0RC1
 * @beta
 */
type SearchBoxItem<T, U> = {
  /**
   * The value emitted when the item is selected.
   */
  value: T;

  /**
   * The data passed to the slot rendering the item.
   */
  renderingData: U;

  /**
   * Identifies the item among the search results.
   */
  key: string;

  /**
   * The text written in the search field when the item is selected.
   */
  equivalentQuery: string;
};

/**
 * Searches the items matching a query.
 *
 * @typeParam T - the type of the value emitted when an item is selected
 * @typeParam U - the type of the data used to render an item
 * @returns the items matching the query, or `false` when searching is not supported
 * @since 18.10.0RC1
 * @beta
 */
type SearchBoxSource<T, U> = (
  query: string,
) => Promise<SearchBoxItem<T, U>[] | false>;

export type { SearchBoxItem, SearchBoxSource };
</script>

<script setup lang="ts" generic="T, U">
import { translations } from "../translations";
import { debounce } from "lodash-es";
import {
  computed,
  nextTick,
  ref,
  shallowRef,
  useTemplateRef,
  watch,
} from "vue";
import { useI18n } from "vue-i18n";

const { initialValue, getSuggestions, parseQuery } = defineProps<{
  /**
   * Input field label
   */
  label: string;

  /**
   * The search box's initial value
   */
  initialValue: string | null;

  /**
   * The search box's placeholder (when empty). Default: a hint that typing searches, since the suggestions are only
   * displayed once something is typed.
   */
  placeholder?: string;

  /**
   * Whether the search field is focused when the dialog holding it opens.
   */
  autofocus?: boolean;

  /**
   * Whether the search field has to be filled for the form holding it to be submitted. Default: false.
   */
  required?: boolean;

  /**
   * Perform a search
   *
   * @param query - A user query (text)
   *
   * @returns Suggestions matching the provided query
   */
  getSuggestions: SearchBoxSource<T, U>;

  /**
   * Convert the text typed in the search field into a value, when the user presses Enter without picking a
   * suggestion or leaves the field after changing its text (e.g. to select a reference typed by hand, possibly to
   * something that doesn't exist yet). Without it, only the suggestions can be selected.
   *
   * @param query - The text typed in the search field
   *
   * @returns The value to select, or `null` when the text doesn't designate one, to keep the suggestions open
   */
  parseQuery?: (query: string) => T | null;
}>();

defineSlots<{
  /**
   * Render a single suggestion
   */
  renderSuggestion(suggestion: SearchBoxItem<T, U>): unknown;
}>();

const emit = defineEmits<{
  /**
   * Triggered when a value is selected: either a suggestion picked from the list, or the text typed in the search
   * field, converted by `parseQuery` (when Enter is pressed, or when the field loses the focus after its text changed).
   * The value is `null` when the text was emptied, meaning that nothing is selected anymore.
   */
  select: [value: T | null];
}>();

const { t } = useI18n({ messages: translations });

const query = ref(initialValue ?? "");

// The text of the search field matching the selected value (the initial value, until something is selected), to know
// whether the text left in the field when it loses the focus still has to be selected.
let selectedQuery = query.value;

// When programmatically updating `query` (e.g. after the user selected a suggestion),
// this trigger prevents the query watcher from performing a new search (which would be pointless)
const justDynamicallyUpdatedQuery = ref(false);

const suggestions = shallowRef<
  | { status: "uninitialized" }
  | {
      status: "resolved";
      results: SearchBoxItem<T, U>[];
    }
  | {
      status: "backendSearchUnsupported";
    }
  | { status: "closed" }
>({ status: "uninitialized" });

const loading = ref(false);

// Whether the suggestion list (or the message replacing it, e.g. while loading) is displayed.
const suggestionsOpen = computed(
  () =>
    (suggestions.value.status !== "uninitialized" || loading.value) &&
    suggestions.value.status !== "closed",
);

// Identifies the latest search, so that the results of an older one (e.g. a search dismissed with the Escape key while
// loading) are ignored when they arrive.
let latestSearchId = 0;

const performSearch = debounce(async (search: string) => {
  // Skip the search when the query is empty (e.g., right after focusing an empty field): searching for
  // everything is wasteful (it fetches unrelated content from the whole wiki) and useless (the caller always
  // filters the result down by target type, so this never surfaces anything relevant anyway).
  if (search.trim().length === 0) {
    closeSuggestions();
    return;
  }

  const searchId = ++latestSearchId;
  loading.value = true;

  const results = await getSuggestions(search);

  if (searchId !== latestSearchId) {
    return;
  }
  loading.value = false;
  suggestions.value =
    results === false
      ? { status: "backendSearchUnsupported" }
      : { status: "resolved", results: results };
});

function selectSuggestion(suggestion: SearchBoxItem<T, U>): void {
  justDynamicallyUpdatedQuery.value = true;
  query.value = suggestion.equivalentQuery;
  selectedQuery = query.value;
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
  } else {
    selectQuery();
  }
}

function selectQuery(): void {
  if (query.value.trim().length === 0) {
    selectedQuery = query.value;
    closeSuggestions();
    emit("select", null);
    return;
  }
  const value = parseQuery?.(query.value) ?? null;
  if (value !== null) {
    selectedQuery = query.value;
    closeSuggestions();
    emit("select", value);
  }
}

// The text typed or pasted in the search field is selected when the field loses the focus (e.g. when the user moves to
// the next field or clicks the submit button of the form holding the search box), so that it isn't lost when no
// suggestion is picked, e.g. to designate something that doesn't exist yet. Clicking a suggestion doesn't blur the
// field (see the mousedown handler of the suggestion list), so that the suggestion is selected instead.
function onBlur(): void {
  closeSuggestions();
  if (query.value !== selectedQuery) {
    selectQuery();
  }
}

const suggestionList = useTemplateRef("suggestionList");

async function moveFocus(relativeIdx: number): Promise<void> {
  focusRelativeSuggestion(relativeIdx);
  // The suggestion list may have to be scrolled to show the focused suggestion.
  await nextTick();
  suggestionList.value
    ?.querySelector(".suggestion.focused")
    ?.scrollIntoView({ block: "nearest" });
}

// Escape closes the suggestions without closing the dialog holding the search box: the dialog (e.g. a Bootstrap modal
// or a native dialog element) is closed by the same key, so the event must neither reach its listeners nor trigger its
// default action. Once the suggestions are closed, Escape is left to the dialog.
function onEscapeKey(event: KeyboardEvent): void {
  if (suggestionsOpen.value) {
    event.preventDefault();
    event.stopPropagation();
    performSearch.cancel();
    latestSearchId++;
    loading.value = false;
    suggestions.value = { status: "closed" };
  }
}

function onKeydown(event: KeyboardEvent): void {
  if (event.key === "Escape") {
    onEscapeKey(event);
  } else if (event.key === "Enter") {
    event.preventDefault();
    onEnterKey();
  } else if (event.key === "ArrowUp") {
    event.preventDefault();
    moveFocus(-1);
  } else if (event.key === "ArrowDown") {
    event.preventDefault();
    moveFocus(+1);
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
      :placeholder="placeholder ?? t('link-modal.search-box.placeholder')"
      :autofocus
      :required
      v-model="query"
      @keydown="onKeydown"
      @focus="performSearch(query)"
      @blur="onBlur()"
    />

    <div class="suggestions-container" v-if="suggestionsOpen">
      <h3
        class="status-message"
        v-if="suggestions.status === 'backendSearchUnsupported'"
      >
        {{ t("link-modal.search-box.backend-unsupported") }}
      </h3>

      <h3
        class="status-message"
        v-if="suggestions.status === 'uninitialized' && loading"
      >
        {{ t("link-modal.search-box.loading") }}
      </h3>

      <h3
        class="status-message"
        v-if="
          suggestions.status === 'resolved' && suggestions.results.length === 0
        "
      >
        {{ t("link-modal.search-box.no-result") }}
      </h3>

      <!-- NOTE: `@mousedown.prevent` prevents the `blur` event from the query `input` field above to trigger *before*
                  the suggestions' `click` handler -->
      <ul
        ref="suggestionList"
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
            <em>Internal error: missing suggestion rendering slot</em>
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

/* The suggestions are listed like in the dropdown menus of the skin (e.g. the Bootstrap dropdown menus of XWiki), using
   its variables when it defines them, and the ones of the design system otherwise. The values that Bootstrap
   hard-codes, without a Less variable, are copied from its dropdowns.less (Bootstrap 3.4.1): the rule each one comes
   from is given next to it, to check when Bootstrap is upgraded. */
.suggestions-container {
  position: absolute;
  /* Right below the search field, whatever the margin that follows the field. */
  top: 100%;
  left: 0;
  right: 0;
  z-index: 100;
  /* .dropdown-menu { margin: 2px 0 0; } */
  margin-top: 2px;
  /* .dropdown-menu { padding: 5px 0; } */
  padding: 5px 0;
  border: 1px solid var(--dropdown-border, var(--cr-input-border-color));
  border-radius: var(--border-radius-base, var(--cr-border-radius-medium));
  background: var(--dropdown-bg, var(--cr-color-neutral-50));
  /* .dropdown-menu { .box-shadow(0 6px 12px rgba(0, 0, 0, .175)); } */
  box-shadow: 0 6px 12px rgb(0 0 0 / 17.5%);
}

.status-message {
  font-size: inherit;
  font-weight: normal;
  font-style: italic;
  text-align: center;
  /* .dropdown-header { padding: 3px 20px; } */
  padding: 3px 20px;
  margin: 0;
}

.suggestions {
  list-style-type: none;
  padding: 0;
  margin: 0;
  /* Leave room for 5 suggestions, the others are reached by scrolling. A suggestion is assumed to have two lines (e.g.
     a label and a hint) plus its vertical padding, i.e. the one of .suggestion below. */
  max-height: calc(5 * (2lh + 2 * 3px));
  overflow-y: auto;
}

.suggestion {
  /* .dropdown-menu > li > a { padding: 3px 20px; } */
  padding: 3px 20px;
  color: var(--dropdown-link-color, inherit);
  cursor: pointer;

  &.focused {
    color: var(--dropdown-link-hover-color, inherit);
    background-color: var(
      --dropdown-link-hover-bg,
      var(--cr-color-neutral-300)
    );
  }
}
</style>
