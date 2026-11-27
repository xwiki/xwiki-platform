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

<!--
  BaseDisplayer is a component that provide for specific displayers
  an interface that already handles the displayer's base behavior:
  focus, switch to edit or view mode, etc.
  It is not meant to be used directly, but instead to be used inside a
  specific displayer that pass to it a `viewer` and `editor` slot.
  In that way, specific displayers only care about implementing
  the formatting of its content (view mode and edit mode)
  instead of reimplementing the whole displayer logic each time.
-->
<template>
  <!--
  The actions popover of the cell is a tippy.js instance attached to this element. It is created the first time the
  user hovers or focuses an editable cell, so that displaying many cells does not create one tooltip per cell.
  -->
  <div
    class="displayer-actions-popover"
    ref="popoverTarget"
    :tabindex="isView ? 0 : -1"
    @mouseenter="onPopoverTriggered"
    @focusin="onDisplayerFocus"
    @focusout="onDisplayerBlur"
  >
    <div
      :class="{ view: isView, edit: !isView, editable: isEditable }"
      ref="displayerRoot"
      @keydown.enter.exact="onEnter(false)"
      @keydown.enter.ctrl="onEnter(true)"
      v-touch:tap="touchHandler"
    >
      <!--
          The base displayer contains three slots: `viewer`, `editor`, and `loading`.
          It displays `viewer` or `loading` according to its current state: `this.isView` when `this.isLoading` is
          false, and `loading` otherwise.
        -->

      <!-- The slot containing the displayer Viewer widget -->
      <div v-if="isView && !isLoading">
        <slot name="viewer">
          <!--
              Default Viewer widget
              Normally this should rarely be used, as the default displayer
              should provide a default viewer if no displayer is specified
              However, this is useful if a custom displayer only implement
              its Editor widget, as a default Viewer widget would still be provided
            -->
          <span>{{ value }}</span>
        </slot>
        <span v-if="!isViewable">
          {{ $t("livedata.displayer.emptyValue") }}<sup>*</sup>
        </span>
      </div>

      <!--
        The slot containing the displayer Editor widget.
        We stop the Escape signal here to close the editor without also leaving the maximized view.
      -->
      <div
        @keydown.esc.capture.stop="cancelEdit"
        @focusin="editorFocused = true"
        v-if="!isView && !isLoading"
        ref="editBlock"
      >
        <slot name="editor">
          <!--
            Default Editor widget
            Normally this should rarely be used, as the default displayer
            should provide a default editor if no displayer is specified
            However, this is useful if a custom displayer only implement
            its Viewer widget, as a default Editor widget would still be provided
          -->
          <input
            class="default-input"
            type="text"
            size="1"
            v-autofocus
            v-model="baseValue"
          />
        </slot>
      </div>

      <slot name="loading" v-if="isLoading">
        <XWikiLoader></XWikiLoader>
      </slot>
    </div>
    <!-- tippy.js moves this element into its popper as soon as the popover is created. -->
    <div
      v-if="popoverContentRendered"
      class="displayer-action-list"
      ref="popoverContent"
    >
      <ActionEdit
        v-if="isEditable"
        :displayer="{
          setEdit: () => {
            setEdit();
          },
        }"
        :close-popover="closePopover"
      />
      <ActionFollowLink
        :displayer="{ href: sanitizeUrl(href) }"
        v-if="href"
        :close-popover="closePopover"
      />
    </div>
  </div>
</template>

<script lang="js">
import "tippy.js/dist/tippy.css";
import "tippy.js/themes/light-border.css";
import ActionEdit from "./actions/ActionEdit.vue";
import ActionFollowLink from "./actions/ActionFollowLink.vue";
import displayerMixin from "./displayerMixin.js";
import XWikiLoader from "../utilities/XWikiLoader.vue";
import { tippy } from "vue-tippy";

// @ts-expect-error expected as this is not a typescript implementation
export default {
  name: "BaseDisplayer",

  // Add the displayerMixin to get access to all the displayers methods and computed properties inside this component
  mixins: [displayerMixin],

  components: {
    ActionEdit,
    ActionFollowLink,
    XWikiLoader,
  },

  data() {
    return {
      duringEditing: false,
      editorFocused: false,
      href: undefined,
      // Whether the content of the actions popover is rendered, which is only needed once the popover is created.
      popoverContentRendered: false,
    };
  },

  created() {
    // The tippy.js instance of the actions popover, created on the first interaction with the cell. It is kept out
    // of the reactive data since Vue must not proxy it.
    this.popover = undefined;
    // The creation of the popover, while it waits for its content to be rendered.
    this.popoverCreation = undefined;
  },

  props: {
    viewOnly: {
      type: Boolean,
      default: false,
    },
    isView: {
      type: Boolean,
      default: true,
    },
    isLoading: {
      type: Boolean,
      default: false,
    },
    isEmpty: {
      type: Boolean,
      default: undefined,
    },
    interceptTouch: {
      type: Boolean,
      default: true,
    },
  },

  computed: {
    isViewable() {
      var empty = this.isEmpty;
      if (empty === undefined) {
        empty = !this.value;
      }
      const isViewable =
        this.logic.isActionAllowed("view", this.entry) || !empty;
      if (!isViewable) {
        this.logic.footnotes.put("*", "livedata.footnotes.propertyNotViewable");
      }
      return isViewable;
    },
    isEditMode() {
      return this.logic.isEditMode();
    },
    isEditorShown() {
      return !this.isView && !this.isLoading;
    },
    // The popover only shows on hover and focus for an editable cell, outside of edit mode, when no other cell is
    // being edited. Otherwise, the manual trigger disables it since nothing shows it manually.
    popoverTrigger() {
      return this.isEditable && !this.duringEditing && !this.isEditMode
        ? "mouseenter focus manual"
        : "manual";
    },
  },

  // The following methods are only used by the BaseDisplayer component
  // The methods for specific displayers can be found in the displayerMixin
  methods: {
    // Switches the displayer to edit mode.
    setEdit() {
      if (this.isEditable && this.isView) {
        this.editorFocused = false;
        this.$emit("update:isView", false);
        this.logic.getEditBus().start(this.entry, this.propertyId);
      }
    },

    // This method should be used to apply edit and go back to view mode.
    // The validation of the edited property is done once the whole entry is done editing.
    applyEdit() {
      // When the #edit slot is not the default, the editValue field is overloaded and is always undefined.
      // When receiving a saveEdit event, the passed value can then be ignored since the overloaded slot has it own
      // field.
      this.$emit("saveEdit", this.editedValue);
      // Go back to view mode
      this.$emit("update:isView", true);
    },

    // This method should be used to cancel edit and go back to view mode.
    // This is like applyEdit but it does not save the entered value
    cancelEdit() {
      // Notifies the edit bus that the entry edition is canceled (consequently, the edited value must not be persisted,
      // and should not be considered as edited anymore).
      this.logic.getEditBus().cancel(this.entry, this.propertyId);

      // Switches to view mode.
      this.$emit("update:isView", true);
    },
    // Tell assistive technologies which fields an entry cannot be created without.
    markMandatoryFields() {
      const editBlock = this.$refs["editBlock"];
      if (editBlock && this.propertyDescriptor?.mandatory) {
        editBlock
          .querySelectorAll("input:not([type=hidden]), select, textarea")
          .forEach((field) => field.setAttribute("aria-required", "true"));
      }
    },
    closePopover() {
      this.popover?.hide();
    },
    // Creates the actions popover once, after rendering its content.
    createPopover() {
      this.popoverCreation ??= this.initPopover();
      return this.popoverCreation;
    },
    async initPopover() {
      this.popoverContentRendered = true;
      await this.$nextTick();
      if (this.$.isUnmounted) {
        return undefined;
      }
      const popover = tippy(this.$refs.popoverTarget, {
        content: this.$refs.popoverContent,
        interactive: true,
        trigger: this.popoverTrigger,
        theme: "light-border",
        followCursor: "horizontal",
        arrow: true,
        ignoreAttributes: true,
        zIndex: 99999,
        // The popover target is not an interactive element, so it must not get an aria-expanded attribute.
        aria: { expanded: false },
      });
      this.watchPopoverState(popover);
      this.popover = popover;
      return popover;
    },
    // Keeps the popover in sync with the cell, from its creation on.
    watchPopoverState(popover) {
      // The disable option behaves weirdly, so the popover is disabled manually in edit mode.
      if (this.isEditMode) {
        popover.disable();
      }
      this.$watch("popoverTrigger", (trigger) => popover.setProps({ trigger }));
      this.$watch("isEditMode", (editMode) =>
        editMode ? popover.disable() : popover.enable(),
      );
    },
    // Creates the popover on the first hover or focus of the cell, when it can be shown, and replays the event that
    // happened before the popover existed so that it shows right away.
    async onPopoverTriggered(event) {
      // The focus trigger of tippy.js only applies to the focus of the popover target itself.
      const target = this.$refs.popoverTarget;
      const ignored = event.type === "focusin" && event.target !== target;
      if (this.popover || this.popoverTrigger === "manual" || ignored) {
        return;
      }
      if (await this.createPopover()) {
        this.replayPopoverTrigger(event, target);
      }
    },
    replayPopoverTrigger(event, target) {
      if (event.type === "mouseenter") {
        target.dispatchEvent(
          new MouseEvent("mouseenter", {
            clientX: event.clientX,
            clientY: event.clientY,
          }),
        );
      } else if (document.activeElement === target) {
        target.dispatchEvent(new FocusEvent("focus"));
      }
    },
    async showPopover() {
      const popover = await this.createPopover();
      popover?.hide();
      popover?.show();
    },
    touchHandler(e) {
      // Active only when intercepting touch event is allowed, the displayer is in view mode, and no other
      // displayers are currently in edit mode.
      // If the touched element is a link and/or is editable, we display the popover.
      // If the touch element is a link, we get its target and display a following link action to its target.
      if (this.interceptTouch && this.isView && !this.duringEditing) {
        const targetsLink = e.target.tagName.toLowerCase() === "a";
        if (targetsLink) {
          this.href = e.target.getAttribute("href");
        } else {
          this.href = undefined;
        }

        if (this.isEditable && targetsLink) {
          e.preventDefault();
        }
        if (this.isEditable) {
          this.showPopover();
        }
      }
    },
    onDisplayerFocus(event) {
      this.onPopoverTriggered(event);
      // In edit mode, focusing an editable cell should make it editable.
      if (this.logic.isEditMode()) {
        this.setEdit();
      }
    },
    // Monitors focus switching outside of the current cell.
    // We switch back to view mode whenever focus of the current cell is lost.
    // eslint-disable-next-line max-statements
    async onDisplayerBlur(evt) {
      const displayerElement = this.$refs["displayerRoot"];
      if (!this.isView) {
        const editBlock = this.$refs["editBlock"];

        // The editor has not been focused yet, so this focusout either comes
        // from the action popover closing right after it opened the editor,
        // or an edit confirmation modal is currently displayed. Keep editing.
        if (!this.editorFocused) {
          return;
        }

        // Focus moved to another element of this cell: keep editing.
        if (evt.relatedTarget && displayerElement.contains(evt.relatedTarget)) {
          return;
        }

        // In some cases, a focusout without a related target can be caused by
        // a focus bounce. So we wait for the focus to settle and we skip the
        // event if it actually landed back inside this cell.
        if (!evt.relatedTarget) {
          await new Promise((resolve) => requestAnimationFrame(resolve));
          if (
            editBlock !== this.$refs["editBlock"] ||
            displayerElement.contains(document.activeElement)
          ) {
            return;
          }
        }

        await this.applyEdit();
      } else {
        if (!displayerElement) {
          return;
        }

        if (evt.relatedTarget && displayerElement.contains(evt.relatedTarget)) {
          return;
        }

        this.closePopover();
      }
    },
    async onEnter(addNewEntry) {
      // Applying the edit creates the entry of a new row, through the edit bus.
      const wasNew = this.entry?._new;
      await this.applyEdit();
      await this.logic.getEditBus().whenSaved();
      // A row still waiting for a mandatory value has no entry yet, so it stays the row to fill.
      if (addNewEntry && wasNew && !this.entry?._new) {
        this.logic.addEntry();
      }
    },
  },
  mounted() {
    // We need to listen on the edit bus event because isEditable is not reactive.
    this.logic.getEditBus().onAnyEvent(() => {
      this.duringEditing = !this.logic.getEditBus().isEditable();
    });
  },
  beforeUnmount() {
    this.popover?.destroy();
  },
  watch: {
    isEditorShown: {
      // A displayer can start in edit mode.
      immediate: true,
      async handler(shown) {
        if (shown) {
          // The editor fields are only in the DOM once rendered.
          await this.$nextTick();
          this.markMandatoryFields();
        }
      },
    },
  },
};
</script>

<style>
.livedata-displayer {
  display: inline-block;
  width: 100%;
  height: 100%;
  min-height: 1em;
  word-break: break-word;
}

/*
  Make any direct child of the displayer (so its Viewer or Editor widget)
  to take the full size of its parent
*/
.livedata-displayer > * {
  width: 100%;
  height: 100%;
}

.livedata-displayer:focus {
  /*
    TODO: make the outline match existing focused input outline
    (need access to Less variables for consistency)
  */
  outline: 2px lightblue solid;
  position: relative;
}

/* style for the default input */
.livedata-displayer .default-input {
  width: 100%;
}

.displayer-actions-popover > div {
  width: 100%;
  height: auto;
  min-height: 1em;
  display: inline-block;
}

.displayer-actions-popover .tippy-content {
  font-size: 1.3rem;

  .displayer-action-list {
    display: flex;
    flex-direction: row;
    flex-wrap: wrap;
    justify-content: right;
  }
}
</style>
