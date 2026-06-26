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
  Deactivate the popover on non editable entries by setting the trigger configuration to manual.
  Since we don't do anything manually, that make the popover disabled.
  -->
  <!-- We explicitly disable expanded in the aria prop as otherwise a aria-expanded attribute is
  produced on a non-iteractive div element. -->
  <tippy
    class="displayer-actions-popover"
    :interactive="true"
    :trigger="
      isEditable && !duringEditing && !isEditMode
        ? 'mouseenter focus manual'
        : 'manual'
    "
    theme="light-border"
    follow-cursor="horizontal"
    :arrow="true"
    ref="tippy"
    :ignore-attributes="true"
    :z-index="99999"
    :tabindex="isView ? 0 : -1"
    :aria="{ expanded: false }"
    tag="div"
    @focusin="onDisplayerFocus"
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

      <!-- The slot containing the displayer Editor widget -->
      <div
        @keydown.esc.capture="cancelEdit"
        @focusout="onDisplayerBlur"
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
    <template #content>
      <div class="displayer-action-list">
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
    </template>
  </tippy>
</template>

<script lang="js">
import "tippy.js/dist/tippy.css";
import "tippy.js/themes/light-border.css";
import ActionEdit from "./actions/ActionEdit.vue";
import ActionFollowLink from "./actions/ActionFollowLink.vue";
import displayerMixin from "./displayerMixin.js";
import XWikiLoader from "../utilities/XWikiLoader.vue";
import { Tippy } from "vue-tippy";

// @ts-expect-error expected as this is not a typescript implementation
export default {
  name: "BaseDisplayer",

  // Add the displayerMixin to get access to all the displayers methods and computed properties inside this component
  mixins: [displayerMixin],

  components: {
    ActionEdit,
    ActionFollowLink,
    Tippy,
    XWikiLoader,
  },

  data() {
    return {
      duringEditing: false,
      href: undefined,
    };
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
  },

  // The following methods are only used by the BaseDisplayer component
  // The methods for specific displayers can be found in the displayerMixin
  methods: {
    // Switches the displayer to edit mode.
    setEdit() {
      if (this.isEditable && this.isView) {
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
    closePopover() {
      this.$refs.tippy.tippy.hide();
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
          this.$refs.tippy.tippy.hide();
          this.$refs.tippy.tippy.show();
        }
      }
    },
    onDisplayerFocus() {
      // In edit mode, focusing an editable cell should make it editable.
      if (this.logic.isEditMode()) {
        const editBus = this.logic.getEditBus();

        // If another cell is currently being saved, that save will then
        // refresh the table and re-render this cell. Opening the editor now
        // would just lock the edit bus before the editor gets destroyed.
        if (editBus.hasPendingSave()) {
          editBus.requestEdit(
            this.logic.getEntryId(this.entry),
            this.propertyId,
          );
        } else {
          this.setEdit();
        }
      }
    },
    // Resume an edit that was requested on this cell.
    resumeRequestedEdit() {
      if (
        this.logic.isEditMode() &&
        this.logic
          .getEditBus()
          .enablePendingEdit(this.logic.getEntryId(this.entry), this.propertyId)
      ) {
        this.setEdit();
      }
    },
    // Monitors focus switching outside of the current cell.
    // We switch back to view mode whenever focus of the current cell is lost.
    // eslint-disable-next-line max-statements
    async onDisplayerBlur(evt) {
      if (!this.isView) {
        const editBlock = this.$refs["editBlock"];

        // The edit block is not rendered while the editor is still loading (e.g. while an edit confirmation modal is
        // displayed), so there is nothing to switch away from yet and clicks (such as on the modal buttons) must be
        // ignored.
        // Focus moved to another element of this cell: keep editing.
        if (
          !editBlock ||
          (evt.relatedTarget && this.$el.contains(evt.relatedTarget))
        ) {
          return;
        }

        // In some cases, a focusout without a related target can be caused by
        // a focus bounce. So we wait for the focus to settle and we skip the
        // event if it actually landed back inside this cell.
        if (!evt.relatedTarget) {
          await new Promise((resolve) => requestAnimationFrame(resolve));
          if (
            editBlock !== this.$refs["editBlock"] ||
            this.$el.contains(document.activeElement)
          ) {
            return;
          }
        }

        await this.applyEdit();
      } else {
        const displayerElement = this.$refs["displayerRoot"];

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
      await this.applyEdit();
      if (this.entry?._new) {
        await this.logic.saveNewEntry();
        if (addNewEntry) {
          this.logic.addEntry();
        }
      }
    },
  },
  mounted() {
    // We need to listen on the edit bus event because isEditable is not reactive.
    this.logic.getEditBus().onAnyEvent(() => {
      this.duringEditing = !this.logic.getEditBus().isEditable();
    });
    // This cell might have been re-created by a refresh following a save,
    // so we try to resume any edit that could have been requested on it.
    this.resumeRequestedEdit();
  },
  watch: {
    // The disable prop behaves weirdly, so we handle that manually instead.
    isEditMode(newValue) {
      if (newValue) {
        this.$refs.tippy.tippy.disable();
      } else {
        this.$refs.tippy.tippy.enable();
      }
    },
    // This cell might have been re-used as-is by a refresh following a save,
    // so we try to resume any edit that could have been requested on it.
    entry() {
      this.resumeRequestedEdit();
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
