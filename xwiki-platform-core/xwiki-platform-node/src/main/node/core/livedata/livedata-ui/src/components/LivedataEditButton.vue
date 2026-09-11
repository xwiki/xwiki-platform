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
  LivedataEditButton is used to toggle edit mode on and off, for the sources that support it.
  It should be placed in the actions area.
-->
<template>
  <button
    type="button"
    class="btn btn-default livedata-edit-button"
    :class="{ active: isEditMode }"
    :title="$t('livedata.action.editMode')"
    :aria-label="$t('livedata.action.editMode')"
    :aria-pressed="isEditMode"
    @click="toggleEditMode"
  >
    <XWikiIcon :icon-descriptor="{ name: 'pencil' }" />
  </button>
</template>

<script>
import XWikiIcon from "./utilities/XWikiIcon.vue";

export default {
  name: "LivedataEditButton",

  components: {
    XWikiIcon,
  },

  inject: ["logic"],

  // We disable edit mode on unmount to reset properties to view mode.
  async beforeUnmount() {
    this.logic.disableEditMode();
    await this.logic.updateEntries();
  },

  computed: {
    isEditMode() {
      return this.logic.isEditMode();
    },
  },

  methods: {
    async toggleEditMode() {
      if (this.logic.isEditMode()) {
        this.logic.disableEditMode();
      } else {
        this.logic.enableEditMode();
      }
      // The entries are re-fetched because the edit mode changes the set of displayed properties.
      await this.logic.updateEntries();
    },
  },
};
</script>

<style>
/*
 * The edit mode is a state the user stays in, so the button has to look pressed when it's on.
 */
.livedata-edit-button.btn-default.active {
  background-color: var(--btn-primary-bg);
  border-color: var(--btn-primary-bg);
  color: var(--btn-primary-color);
}
</style>
