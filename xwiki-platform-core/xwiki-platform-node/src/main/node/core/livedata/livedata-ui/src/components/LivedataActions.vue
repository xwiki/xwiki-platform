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
  LivedataActions groups the actions of the Live Data at the end of its top bar. They are laid out
  as a list of buttons sharing a flat look, the same way the skin displays the actions of a page.
-->
<template>
  <ul class="livedata-actions flat-buttons">
    <li v-if="logic.hasEditMode()"><LivedataEditButton /></li>
    <li><LivedataDropdownMenu /></li>
  </ul>
</template>

<script>
import LivedataDropdownMenu from "./LivedataDropdownMenu.vue";
import LivedataEditButton from "./LivedataEditButton.vue";

export default {
  name: "LivedataActions",

  components: {
    LivedataDropdownMenu,
    LivedataEditButton,
  },

  inject: ["logic"],
};
</script>

<style>
.livedata-actions {
  display: flex;
  gap: var(--button-spacing);
  list-style: none;
  margin-bottom: 0;
  margin-left: var(--grid-gutter-width);
  padding-left: 0;
}

.livedata-actions .btn span {
  vertical-align: middle;
}
</style>
