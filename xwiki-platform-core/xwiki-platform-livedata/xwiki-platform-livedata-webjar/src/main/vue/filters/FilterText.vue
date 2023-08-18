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
  FilterText is a custom filter that allow to filter strings
-->
<template>
  <!--
    Simple input of type text that apply filter on change event
  -->
  <input
    class="filter-text"
    type="text"
    size="1"
    :aria-label="$t('livedata.filter.text.label')"
    :value="filterEntry.value"
    @input="applyFilterWithDelay($event.target.value)"
  />
</template>


<script>
import filterMixin from "./filterMixin.js";

export default {

  name: "filter-text",

  // Add the filterMixin to get access to all the filters methods and computed properties inside this component
  mixins: [filterMixin],

};
</script>


<style>

.livedata-filter .filter-text {
  width: 100%;
}

</style>
