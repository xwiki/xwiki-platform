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
  FilterNumber is a custom filter that allow to filter numbers
-->
<template>
  <!--
    Simple input that apply filter on change event
    Should be type="number", but current style.css apply custom input style
    for type="text" or type="password", but not type="number"
  -->
  <input
    class="filter-number"
    type="text"
    size="1"
    :aria-label="$t('livedata.filter.number.label')"
    :value="filterEntry.value"
    @input="applyFilterWithDelay($event.target.value)"
  />
</template>


<script>
import filterMixin from "./filterMixin.js";

export default {

  name: "filter-number",

  // Add the filterMixin to get access to all the filters methods and computed properties inside this component
  mixins: [filterMixin],

};
</script>


<style>

.livedata-filter .filter-number {
  border: none;
  text-align: right;
}

</style>
