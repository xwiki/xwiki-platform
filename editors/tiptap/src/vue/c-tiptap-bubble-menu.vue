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
import { BubbleMenu, Editor, Range } from "@tiptap/vue-3";
import getMenuActions, {
  BubbleMenuAction,
  getLinkAction,
} from "../components/extensions/bubble-menu";
import {
  type Component,
  computed,
  ComputedRef,
  ref,
  Ref,
  shallowRef,
} from "vue";
import { CIcon, Size } from "@xwiki/cristal-icons";
import { isTextSelection } from "@tiptap/core";
import { EditorView } from "@tiptap/pm/view";
import { EditorState } from "@tiptap/pm/state";
import CTiptapLinkEdit from "./c-tiptap-link-edit.vue";
import { ResolvedPos } from "@tiptap/pm/model";

const props = defineProps<{
  editor: Editor;
}>();

const actions: ComputedRef<BubbleMenuAction[]> = computed(() =>
  getMenuActions(props.editor),
);

const additionalComponent: Ref<Component | undefined> = shallowRef();

const additionalComponentProps: Ref<
  | {
      action: BubbleMenuAction;
      editor: Editor;
      range: Range;
      url: string;
      isAmbiguous: boolean;
    }
  | undefined
> = ref();

/**
 * Compute if the current selection is inside a link.
 */
const simpleCursorInLink = computed(() => {
  return (
    props.editor.state.selection.empty &&
    findLinkCurrentSelection() != undefined
  );
});

/**
 * Find link at position, return the corresponding mark when found, undefined
 * otherwise.
 * @param pos the provided position (e.g., the start of end of a selection)
 */
function findLinkPosition(pos: ResolvedPos) {
  return pos.marks().find((m) => m.type.name == "link");
}

/**
 * Return the mark of the link in the current selection.
 */
function findLinkCurrentSelection() {
  return findLinkPosition(props.editor.state.selection.$from);
}

/**
 * Set to true if the selection span over several links, or span over a link
 * and standard text.
 */
const isSelectionAmbiguous = computed(() => {
  const fromLinkMark = findLinkPosition(props.editor.state.selection.$from);
  const toLinkMark = findLinkPosition(props.editor.state.selection.$to);
  return fromLinkMark != toLinkMark;
});

/**
 * Return the href value of the currently selected link.
 */
const currentLinkUnderSelection = computed(() => {
  return findLinkCurrentSelection()?.attrs.href;
});

function apply(action: BubbleMenuAction) {
  if (action.additionalComponent) {
    additionalComponent.value = action.additionalComponent;
    additionalComponentProps.value = {
      action: action,
      editor: props.editor,
      range: props.editor.state.selection,
      url: currentLinkUnderSelection.value,
      isAmbiguous: isSelectionAmbiguous.value,
    };
  } else {
    action.command({
      editor: props.editor,
      range: props.editor.state.selection,
    });
  }
}

const hideOnEsc = {
  name: "hideOnEsc",
  defaultValue: true,
  fn({ hide }: { hide: () => void }) {
    function onKeyDown(event: KeyboardEvent) {
      if (event.keyCode === 27) {
        hide();
      }
    }

    return {
      onShow() {
        document.addEventListener("keydown", onKeyDown);
      },
      onHide() {
        document.removeEventListener("keydown", onKeyDown);
      },
    };
  },
};

function closeAdditionalComponent() {
  // RAZ the additional component when tippy is hidden,
  // otherwise we get stuck with the old component.
  additionalComponent.value = undefined;
  // The same thing goes for the additional component props.
  additionalComponentProps.value = undefined;
}

/*
 * Compute whether the bubble menu must be shown, either because the selection
 * is empty and inside a link element, or because
 */
const shouldShow = ({
  editor,
  view,
  state,
  from,
  to,
}: {
  editor: Editor;
  view: EditorView;
  state: EditorState;
  from: number;
  to: number;
}) => {
  const { doc, selection } = state;
  const { empty: emptySelection } = selection;

  // Sometime check for `empty` is not enough.
  // Doubleclick an empty paragraph returns a node size of 2.
  // So we check also for an empty text size.
  const isEmptyTextBlock =
    !doc.textBetween(from, to).length && isTextSelection(state.selection);

  const hasEditorFocus = view.hasFocus();

  const isLink =
    state.selection.$head.marks().find((m) => m.type.name == "link") !==
    undefined;

  // Don't show if the editor is not editable or don't have focus.
  if (!editor.isEditable || !hasEditorFocus) {
    return false;
  }

  // Show if the selection is not empty and the selection is not an empty text
  // block, or it the selection is empty but inside a link.
  return (!emptySelection && !isEmptyTextBlock) || (emptySelection && isLink);
};

const linkAction = getLinkAction(props.editor);
</script>

<template>
  <!-- @vue-ignore TODO the type of shouldShow needs to be refined-->
  <bubble-menu
    :editor="editor"
    :tippy-options="{
      plugins: [hideOnEsc],
      onHidden: closeAdditionalComponent,
      maxWidth: 'none',
    }"
    :should-show="shouldShow"
    class="items"
  >
    <div v-if="!simpleCursorInLink" v-show="!additionalComponent">
      <button
        v-for="action in actions"
        :key="action.title"
        class="item"
        :aria-label="action.title"
        :title="action.title"
        @click="apply(action)"
        @submit="apply(action)"
      >
        <c-icon :name="action.icon" :size="Size.Small"></c-icon>
      </button>
    </div>
    <CTiptapLinkEdit
      v-else
      :editor="editor"
      :action="linkAction"
      :range="editor.state.selection"
      :url="currentLinkUnderSelection"
      :has-wrapper="false"
    ></CTiptapLinkEdit>

    <!--
    It is possible for an action to provide an additional component.
    This is useful if the action needs some additional actions (e.g., the link
    action requires a link value).
    -->
    <template
      v-if="
        additionalComponent && additionalComponentProps && !simpleCursorInLink
      "
    >
      <!-- If an additional component is provided by the sub-component,
      display it instead of the default content. -->
      <!-- Current the additional component does not have the choice of the
      close event.
      So the
      -->
      <component
        :is="additionalComponent"
        v-bind="additionalComponentProps"
        @close="closeAdditionalComponent"
      ></component>
    </template>
  </bubble-menu>
</template>

<style scoped>
.items {
  position: relative;
  display: flex;
  border-radius: var(--cr-tooltip-border-radius);
  background: white; /* TODO: define a global variable for background color */
  overflow: hidden;
  box-shadow:
    0 0 0 1px rgba(0, 0, 0, 0.1),
    0 10px 20px rgba(0, 0, 0, 0.1);
}

.item {
  background: transparent;
  border: none;
  padding: var(--cr-spacing-x-small);
}

.item:hover {
  background-color: var(--cr-color-neutral-200);
  cursor: pointer;
}
</style>
