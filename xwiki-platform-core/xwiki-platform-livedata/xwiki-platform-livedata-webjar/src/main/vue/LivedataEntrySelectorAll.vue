<!--
 * See the NOTICE file distributed with this work for additional
 * information regarding copyright ownership.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
-->

<!--
  the LivedataEntrySelectorAll component allows the user to select
  all the entries of the Livedata.
  By default, it only selects all the entries on the current page.
  It comes with a dropdown that allows selecting entries more globally:
  - select entries matching current configuration
  - TODO: select all entries of the Livedata

  When toggled, it modifies the `Logic.entrySelection` object.
  When it select all the entries of the Livedata (with or without config),
  it sets the `Logic.entrySelection.isGlobal` property to `ŧrue`

  When the LivedataSelectorAll is in global mode, unchecking the checkbox
  takes you out of this global mode.
  Leaving the global mode uncheck all the entries.
-->
<template>
  <!--
    The LivedataEntrySelectorAll is actually a dropdown
    with the checkbox as button, and an arrow on its right
    to expand the dropdown
    It uses the Bootstrap 3 dropdown syntax.
  -->
  <div class="livedata-entry-selector-all dropdown">

    <button
      class="btn dropdown-toggle"
      type="button"
      data-toggle="dropdown" aria-haspopup="true" aria-expanded="true"
      >
        <!--
          Property selection checkbox
          Checkbox is surrounded by a div with padding in order to
          facilitate the user click
        -->
        <span
          class="livedata-entry-selector-all-checkbox"
          @click.stop.self="$refs.checkbox.click()"
        >
          <input
            ref="checkbox"
            type="checkbox"
            :checked="checked"
            @click.stop
            @change="toggle"
          />
        </span>
        <!--
          The caret used to expand the dropdown
          (Actually the dropdown is extended when the user clicks
          on anything but the checkbox)
        -->
        <span class="caret"></span>
    </button>

    <!-- Entry seleSelector All options -->
    <ul class="dropdown-menu">
      <!-- Select all entries matching current config -->
      <li>
        <a href="#" @click.prevent="logic.setEntrySelectGlobal(true)">
          {{ $t('livedata.selection.selectInAllPages') }}
        </a>
      </li>
    </ul>

  </div>
</template>


<script>
export default {

  name: "LivedataEntrySelectorAll",

  inject: ["logic"],

  computed: {
    data () { return this.logic.data; },
    entrySelection () { return this.logic.entrySelection; },

    // Whether the button is checked or not
    // It is checked if all the page entries are selected,
    // or if global mode is on
    checked () {
      const selectablePageEntries = this.data.data.entries
        .filter(entry => this.logic.isSelectionEnabled({ entry }));
      const allPageEntriesSeleted = selectablePageEntries
        .every(entry => this.logic.entrySelection.selected.includes(this.logic.getEntryId(entry)));
      const allEntriesSelected = this.entrySelection.isGlobal && this.entrySelection.deselected.length === 0;
      return allPageEntriesSeleted || allEntriesSelected;
    },

    // Whether the button is indeterminate
    // It is in indeterminate state if at least one but not all
    // the page entries are selected
    indeterminate () {
      const someDeselected = this.logic.entrySelection.deselected.length > 0;
      const someSelected = this.logic.entrySelection.selected.length > 0;
      return someDeselected || (someSelected && !this.checked);
    },

  },

  watch: {
    // As the indeterminate state of a checkbox has to be trigger
    // by javascript (cannot be set in html),
    // we need to watch when the inderterminate state changes
    // to update the checkbox state
    indeterminate () {
      this.$refs.checkbox.indeterminate = this.indeterminate;
    },
  },


  methods: {
    // Event handler for when the user click on the checkbox
    toggle () {
      // if Global mode is on
      if (this.entrySelection.isGlobal) {
        if (this.checked) {
          // Remove global mode if everything is checked
          this.logic.setEntrySelectGlobal(false);
        } else {
          // Reinforce global mode if state is indeterminate
          this.logic.setEntrySelectGlobal(true);
        }
      // else, normal mode
      } else {
        if (this.checked) {
          // Deselect all selected entries
          this.logic.deselectEntries(this.logic.entrySelection.selected);
        } else {
          // Selecte all entries in current page
          this.logic.selectEntries(this.data.data.entries);
        }
      }
    },
  },

};
</script>


<style>

.livedata-entry-selector-all .btn {
  display: flex;
  align-items: center;
  background-color: unset;
}

.livedata-entry-selector-all-checkbox {
  display: flex;
  justify-content: center;
  align-items: center;
  height: 100%;
  width: 100%;
}

.livedata-entry-selector-all-checkbox input {
  bottom: 0;
}

.livedata-entry-selector-all .caret{
  margin-left: 5px;
}


</style>
