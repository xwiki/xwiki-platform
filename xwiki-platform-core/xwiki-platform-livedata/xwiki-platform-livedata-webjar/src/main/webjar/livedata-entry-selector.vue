<template>
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

  Vue.component("livedata-entry-selector", {

    name: "livedata-entry-selector",

    template: template,

    props: {
      entry: Object,
      logic: Object,
    },

    computed: {
      data: function () { return this.logic.data; },

      selected: function () {
        return this.logic.isEntrySelected(this.entry);
      },

    },

    destroyed: function () {
      this.logic.unselectEntries(this.entry);
    },

  });
});
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
