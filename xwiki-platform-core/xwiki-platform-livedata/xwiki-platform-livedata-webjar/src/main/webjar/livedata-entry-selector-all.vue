<template>
  <div class="livedata-entry-selector-all dropdown">

    <button
      class="btn dropdown-toggle"
      type="button"
      data-toggle="dropdown" aria-haspopup="true" aria-expanded="true"
      >
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
        <span class="caret"></span>
    </button>

    <ul class="dropdown-menu">
      <li>
        <a href="#" @click.prevent="logic.setEntrySelectGlobal(true)">
          Select in all pages
        </a>
      </li>
    </ul>

  </div>
</template>


<script>
/*
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
 */
define([
  "Vue",
], function (
  Vue
) {

  Vue.component("livedata-entry-selector-all", {

    name: "livedata-entry-selector-all",

    template: template,

    props: {
      logic: Object,
    },


    computed: {
      data: function () { return this.logic.data; },
      entrySelection: function () { return this.logic.entrySelection; },

      checked: function () {
          var self = this;
          var allPageEntriesSeleted = this.data.data.entries.every(function (entry) {
            return self.logic.isEntrySelected(entry);
          });
          var allEntriesSelected = this.entrySelection.isGlobal && this.entrySelection.deselected.length === 0;
          return allPageEntriesSeleted || allEntriesSelected;
      },

      indeterminate: function () {
        var selectedCount = this.logic.getSelectedEntriesCount();
          return selectedCount > 0 && !this.checked;
      },

    },

    watch: {
      "indeterminate": function () {
        this.$refs.checkbox.indeterminate = this.indeterminate;
      },
    },


    methods: {
      toggle: function () {
        if (this.entrySelection.isGlobal) {
          this.logic.setEntrySelectGlobal(false);
        } else {
          this.logic.toggleSelectEntries(this.data.data.entries, !this.checked);
        }
      },
    },

  });
});
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
