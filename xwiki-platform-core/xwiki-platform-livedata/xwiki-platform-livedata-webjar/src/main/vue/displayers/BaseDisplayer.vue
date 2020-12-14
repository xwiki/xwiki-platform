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
  instead of reimplemting the whole displayer logic each time.
-->
<template>
  <div
    :class="isView ? 'view' : 'edit'"
    @dblclick="edit"
    @keypress.self.enter="edit"
    :tabindex="isEditable ? 0 : ''"
  >
    <!--
      The base displayer contains two slots: `viewer` and `editor`.
      It displays the one according to its current state: `this.isView`.
    -->

    <!-- The slot containing the displayer Viewer widget -->
    <slot
      name="viewer"
      v-if="isView"
    >
      <!--
        Default Viewer widget
        Normally this should rarely be used, as the default displayer
        should provide a default viewer if no displayer is specified
        However, this is usefull if a custom displayer only implement
        its Editor widget, as a default Viewer widget would still be provided
      -->
      <div>{{ value }}</div>
    </slot>

    <!-- The slot containing the displayer Editor widget -->
    <slot
      name="editor"
      v-if="!isView"
    >
      <!--
        Default Editor widget
        Normally this should rarely be used, as the default displayer
        should provide a default editor if no displayer is specified
        However, this is usefull if a custom displayer only implement
        its Viewer widget, as a default Editor widget would still be provided
      -->
      <input
        class="default-input"
        type="text"
        size="1"
        v-autofocus
        :value="value"
        @focusout="applyEdit($event.target.value)"
        @keypress.enter="applyEdit($event.target.value)"
        @keydown.esc="cancelEdit"
      />
    </slot>

  </div>
</template>


<script>
import displayerMixin from "./displayerMixin.js";

export default {

  name: "BaseDisplayer",

  // Add the displayerMixin to get access to all the displayers methods and computed properties inside this component
  mixins: [displayerMixin],

  data () {
    return {
      // Whether the displayer is in view or edit mode
      isView: true,
    };
  },

  computed: {
    isEditable () {
      return this.logic.isEditable({
        entry: this.entry,
        propertyId: this.propertyId,
      });
    }
  },

  // The following methods are only used by the BaseDisplayer component
  // The methods for specific displayers can be found in the displayerMixin
  methods: {

    // Trigger View mode (switch from Editor widget to Viewer widget)
    // This should rarely be used directly as it does not validate modified data
    // Used the `applyEdit` method instead (found in the displayerMixin)
    // which call this view function after validating data
    view () {
      if (this.isView) { return; }
      this.isView = true;
      this.$el.focus();
    },

    // Trigger Edit mode (switch from Editor widget to Viewer widget)
    // This function is only used for the baseDisplayer logic
    // and should not be used inside a specific displayer
    edit () {
      if (!this.isEditable) { return; }
      if (!this.isView) { return; }
      this.isView = false;
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

</style>
