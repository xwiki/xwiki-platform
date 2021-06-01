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
  <div :class="{view: isView, edit: !isView}" ref="displayerRoot" @dblclick="setEdit" @keypress.self.enter="setEdit">
    <!--
      The base displayer contains three slots: `viewer`, `editor`, and `loading`.
      It displays `viewer` or `loading` according to its current state: `this.isView` when `this.isLoading` is false,
      and `loading` otherwise.
    -->

    <!-- The slot containing the displayer Viewer widget -->
    <div tabindex="0" v-if="isView && !isLoading">
      <slot name="viewer">
        <!--
          Default Viewer widget
          Normally this should rarely be used, as the default displayer
          should provide a default viewer if no displayer is specified
          However, this is useful if a custom displayer only implement
          its Editor widget, as a default Viewer widget would still be provided
        -->
        {{ value }}
      </slot>
    </div>

    <!-- The slot containing the displayer Editor widget -->
    <div @keypress.enter="applyEdit"
         @keydown.esc="cancelEdit"
         v-if="!isView && !isLoading"
         tabindex="0"
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
</template>


<script>
import displayerMixin from "./displayerMixin.js";
import XWikiLoader from "../utilities/XWikiLoader.vue";

export default {

  name: "BaseDisplayer",

  // Add the displayerMixin to get access to all the displayers methods and computed properties inside this component
  mixins: [displayerMixin],

  components: {
    XWikiLoader,
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
      default: false
    }
  },

  computed: {
    // Checks if the property value is allowed to be edited and if the livedata is in a state where the displayer can
    // be edited.
    isEditable() {
      const editable = this.logic.isEditable({
        entry: this.entry,
        propertyId: this.propertyId,
      });
      // Checks that no other property is currently being edited.
      const noOtherEditing = this.logic.getEditBus().isEditable()
      return editable && noOtherEditing;
    },
    
  },

  // The following methods are only used by the BaseDisplayer component
  // The methods for specific displayers can be found in the displayerMixin
  methods: {
    // Switches the displayer to edit mode.
    setEdit() {
      if (this.isEditable && this.isView) {
        this.$emit('update:isView', false);
        this.logic.getEditBus().start(this.entry, this.propertyId)
      }
    },

    // This method should be used to apply edit and go back to view mode.
    // The validation of the edited property is done once the whole entry is done editing.
    applyEdit() {
      // When the #edit slot is not the default, the editValue field is overloaded and is always undefined.
      // When receiving a saveEdit event, the passed value can then be ignored since the overloaded slot has it own 
      // field.
      this.$emit('saveEdit', this.editedValue);
      // Go back to view mode
      this.$emit('update:isView', true);
    },

    // This method should be used to cancel edit and go back to view mode.
    // This is like applyEdit but it does not save the entered value
    cancelEdit() {
      // Notifies the edit bus that the entry edition is canceled (consequently, the edited value must not be persisted,
      // and should not be considered as edited anymore).
      this.logic.getEditBus().cancel(this.entry, this.propertyId)

      // Switches to view mode.
      this.$emit('update:isView', true);
    }
  },
  mounted() {
    // Monitors clicks outside of the current cell. We switch back to view mode whenever a click is done outside of 
    // the current cell.
    document.addEventListener("click", (evt) => {
      if (!this.isView) {
        const editBlock = this.$refs['editBlock'];

        if (editBlock.contains(evt.target)) {
          return;
        }

        // Wait a little before switching back to view mode, otherwise the change case cause a column width change 
        // and make the user click on the wrong column, for instance when trying to edit the next column by double 
        // clicking on it.
        setTimeout(() => this.applyEdit(), 200);
      }
    })
  }
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

</style>
