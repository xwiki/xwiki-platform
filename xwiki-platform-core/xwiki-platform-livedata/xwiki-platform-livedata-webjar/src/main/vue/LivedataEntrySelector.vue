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
  LivedataEntrySelector component allow the user to select
  an entry of the Livedata.
  One LivedataEntrySelector component is associated with one entry,
  passed as its prop.

  When toggled, it modifies the `Logic.entrySelection` object.
-->
<template>
  <!--
    Property selection checkbox
    Checkbox is surrounded by a div with padding to facilitate the user click
  -->
  <div
    class="livedata-entry-selector"
    @click.self="$refs.checkbox.click()"
  >
    <input
      ref="checkbox"
      type="checkbox"
      :checked="selected"
      @change="logic.toggleSelectEntries(entry)"
    />
  </div>
</template>


<script>
export default {

  name: "LivedataEntrySelector",

  inject: ["logic"],

  props: {
    entry: Object,
  },

  computed: {
    data () { return this.logic.data; },

    // Whether the selector property is currently selected
    selected () {
      return this.logic.isEntrySelected(this.entry);
    },

  },

};
</script>


<style>

.livedata-entry-selector {
  display: flex;
  justify-content: center;
  align-items: center;
  height: 100%;
  width: 100%;
}

.livedata-entry-selector input {
  bottom: 0;
}

</style>
