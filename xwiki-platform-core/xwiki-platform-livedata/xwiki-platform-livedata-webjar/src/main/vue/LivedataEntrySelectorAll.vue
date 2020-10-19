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
  LivedataEntrySelectorAll component allow the user to select
  all the entry of the Livedata.
  By default, it only select all the entries on the current page.
  It comes with a dropdown that allow to select entries more globally:
  - select entries matching current configuration
  - TODO: select all entries of the Livedata

  When toggled, it modifies the `Logic.selection` object.
  When it select all the entries of the Livedata (with or without config),
  it sets the `Logic.selection.isGlobal` property to `ŧrue`

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
        <a href="#" @click.prevent="logic.selection.setGlobal(true)">
          Select in all pages
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
    selection () { return this.logic.selection; },

    // Whether the button is checked or not
    // It is checked if all the page entries are selected,
    // or if global mode is on
    checked () {
         const allPageEntriesSeleted = this.logic.config.data.entries.every(entry => this.logic.selection.isSelected(entry));
         const allEntriesSelected = this.selection.isGlobal && this.selection.deselected.length === 0;
        return allPageEntriesSeleted || allEntriesSelected;
    },

    // Whether the button is indeterminate
    // It is in indeterminate state if at least one but not all
    // the page entries are selected
    indeterminate () {
       const selectedCount = this.logic.selection.getCount();
        return selectedCount > 0 && !this.checked;
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
      // if Global mode is on, uncheck everything
      if (this.selection.isGlobal) {
        this.logic.selection.setGlobal(false);
      // else, toggle `this.checked` state
      } else {
        this.logic.selection.toggleSelect(this.logic.config.data.entries, !this.checked);
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
