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
  LayoutTableNewRow is a component for the Table layout that can be used
  to add new entries to the Livedata
-->
<template>
  <!--
    The component is formatted like a normal row
    but has contains one cell that span on the entire row
  -->
  <tr class="layout-table-new-row">
    <!--
      We need to create an empty cell for the entry selector
      so that it align well with the entries selectors of the rows
      and the select-all entries in the header
    -->
    <td class="entry-selector"></td>

    <!--
      The cell to add a new entry
      Span the whole row width
    -->
    <td :colspan="colspan">
      <a
        href="#"
        @click.prevent="logic.addEntry()"
      >
        <XWikiIcon :icon-descriptor="{name: 'add'}"/>
        {{ $t('livedata.action.addEntry') }}
      </a>
    </td>

  </tr>
</template>


<script>
import XWikiIcon from "../../utilities/XWikiIcon";

export default {

  name: "LayoutTableNewRow",

  components: {XWikiIcon},

  inject: ["logic"],

  props: {
    entry: Object,
  },

  computed: {
    data () { return this.logic.data; },

    // The colspan value to specify to the create-new-entry cell
    // It is set to the number of properties that can be displayed
    // as it is the max number of column that could be displayed at once
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
