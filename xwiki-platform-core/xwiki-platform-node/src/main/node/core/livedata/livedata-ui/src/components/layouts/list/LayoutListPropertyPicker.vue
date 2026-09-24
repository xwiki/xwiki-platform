<!--
  See the NOTICE file distributed with this work for additional
  information regarding copyright ownership.

  This is free software; you can redistribute it and/or modify it
  under the terms of the GNU Lesser General Public License as
  published by the Free Software Foundation; either version 2.1 of
  the License, or (at your option) any later version.

  This software is distributed in the hope that it will be useful,
  but WITHOUT ANY WARRANTY; without even the implied warranty of
  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
  Lesser General Public License for more details.

  You should have received a copy of the GNU Lesser General Public
  License along with this software; if not, write to the Free
  Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
  02110-1301 USA, or see the FSF site: http://www.fsf.org.
-->

<!--
  LayoutListPropertyPicker lets the user choose the property displayed by the List layout.
  It is only displayed when there is more than one property to choose from.
-->
<template>
  <span v-if="properties.length > 1" class="layout-list-property-picker">
    <label :for="selectId">{{ $t("livedata.layout.list.property") }}</label>
    <select
      :id="selectId"
      :title="$t('livedata.layout.list.property.hint')"
      @change="changeProperty"
    >
      <option
        v-for="property in properties"
        :key="property.id"
        :value="property.id"
        :selected="property.id === propertyId"
      >
        {{ property.name }}
      </option>
    </select>
  </span>
</template>

<script>
import { getListPropertyId, setListPropertyId } from "./listLayout.js";

export default {
  name: "LayoutListPropertyPicker",

  inject: ["logic"],

  computed: {
    // The properties that can be displayed
    properties() {
      return this.logic
        .getPropertyDescriptors()
        .filter((property) => property !== undefined);
    },
    // The id of the currently displayed property
    propertyId() {
      return getListPropertyId(this.logic);
    },
    selectId() {
      return `${this.logic.data.id}-list-property`;
    },
  },

  methods: {
    changeProperty(event) {
      setListPropertyId(this.logic, event.target.value);
    },
  },
};
</script>

<style>
.layout-list-property-picker {
  display: inline-flex;
  align-items: center;
  gap: 0.25em;
}

.layout-list-property-picker label {
  margin: 0;
  font-weight: var(--font-weight-regular);
}

.layout-list-property-picker select {
  padding: 2px 4px;
}
</style>
