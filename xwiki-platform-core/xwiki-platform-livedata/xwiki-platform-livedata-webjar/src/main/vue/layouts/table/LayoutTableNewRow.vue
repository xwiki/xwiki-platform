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

<template>
  <tr class="layout-table-new-row">

    <!-- Entry Select -->
    <td class="entry-selector"></td>

    <!-- Entry cells -->
    <td :colspan="colspan">
      <a
        href="#"
        @click.prevent="logic.addEntry()"
      >
        <span class="fa fa-plus-circle"></span>
        Add entry
      </a>
    </td>

  </tr>
</template>


<script>
export default {

  name: "LayoutTableNewRow",

  inject: ["logic"],

  props: {
    entry: Object,
  },

  computed: {
    data () { return this.logic.data; },
    colspan () {
      return this.logic.getPropertyDescriptors().length;
    },
  },

};
</script>


<style>

.layout-table-new-row a {
  display: inline-block;
  width: 100%;
}

.layout-table-new-row a .fa {
  display: inline-block;
  margin-right: 0.5rem;
}

</style>
