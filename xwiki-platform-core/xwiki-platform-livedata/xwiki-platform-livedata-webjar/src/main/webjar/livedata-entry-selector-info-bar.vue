<template>
  <div
    class="livedata-entry-selector-info-bar"
    v-if="selectedCount > 0"
  >
  {{ selectedCount }} / {{ data.data.count }} entries selected
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

  Vue.component("livedata-entry-selector-info-bar", {

    name: "livedata-entry-selector-info-bar",

    template: template,

    props: {
      logic: Object,
    },

    computed: {
      data: function () { return this.logic.data; },

      selectedCount: function () {
        return this.logic.getSelectedEntriesCount();
      },
    },

  });
});
</script>


<style>

.livedata-entry-selector-info-bar {
  margin-bottom: 1rem;
  padding: 12px;
  border-radius: 5px;
  background-color: #ddd4;
}

</style>
