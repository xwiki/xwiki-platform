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
  <div :class="{view: isView, edit: !isView, editing: isView && isEditing}" ref="displayerRoot">
    <!--
      The base displayer contains three slots: `viewer`, `editor`, and `loading`.
      It displays `viewer` or `loading` according to its current state: `this.isView` when `this.isLoading` is false,
      and `loading` otherwise.
    -->

    <!-- The slot containing the displayer Viewer widget -->
    <div @dblclick="setEdit"
         @keypress.self.enter="setEdit"
         tabindex="0"
         v-if="isView && !isLoading && !isEditing">
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
         v-if="(!isView && !isLoading) || isEditing"
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
    isEditable() {
      const editable = this.logic.isEditable({
        entry: this.entry,
        propertyId: this.propertyId,
      });
      // Checks that no other cell is already editing.
      const noOtherEditing = this.editBus.isEditable()
      return editable && noOtherEditing;
    },
    // The base value uses the value provided in the props the initial value of the form input.
    // Once the form is edited, `this.editedValue` is defined and is used instead. 
    baseValue: {
      get() {
        return this.editedValue || this.value;
      },
      set(value) {
        this.editedValue = value;
      }
    }
  },

  data() {
    return {
      editedValue: undefined,
    }
  },

  // The following methods are only used by the BaseDisplayer component
  // The methods for specific displayers can be found in the displayerMixin
  methods: {
    setView() {
      this.$emit('update:isView', true);
    },

    setEdit() {
      if (this.isEditable) {
        this.$emit('update:isView', false);
        this.editBus.start(this.entry, this.propertyId)
      }
    },

    // Trigger View mode (switch from Editor widget to Viewer widget)
    // This should rarely be used directly as it does not validate modified data
    // Used the `applyEdit` method instead (found in the displayerMixin)
    // which call this view function after validating data
    view () {
      if (this.isView) { return; }
      this.$el.focus();
    },

    // This method should be used to apply edit and go back to view mode.
    // The validation of the edited property is done once the whole entry is done editing.
    applyEdit() {
      // Skip the event if the new focused element is contained by the edit block.

      // When edit slot is redefined by the parent component, the edited value is always undefined and 
      // can is ignored by the parent, which has access the the value of its own edit slot.
      this.$emit('saveEdit', this.editedValue);
      // Go back to view mode
      this.setView();

    },
    // This method should be used to cancel edit and go back to view mode.
    // This is like applyEdit but it does not save the entered value
    cancelEdit() {
      // Notifies the edit bus that the entry is canceled.
      this.editBus.cancel(this.entry, this.propertyId)

      // Switches to view mode.
      this.setView();
    }
  },
  watch: {
    /** Focus on a cell when it passes to edit mode. */
    isView: function(newIsView) {
      // Focuses in the current cell.
      if (newIsView) this.view();
    }
  },
  mounted() {
    // Monitors clicks outside of the current cell. We switch back to edit mode whenever a click is done outside of 
    // the current cell.
    document.addEventListener("click", (evt) => {
      if (!this.isView) {
        const editBlock = this.$refs['editBlock'];
        var targetElement = evt.target;

        do {
          if (targetElement === editBlock) {
            return;
          }

          targetElement = targetElement.parentNode;
        } while (targetElement);

        // Wait a little before switch back to view mode, otherwise the change case cause a column width change 
        // and make the user click on the wrong column, for instance when trying to edit the next column.
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
