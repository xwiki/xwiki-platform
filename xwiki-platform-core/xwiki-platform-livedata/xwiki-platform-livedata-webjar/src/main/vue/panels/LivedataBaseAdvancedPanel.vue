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
  LivedataBaseAdvancedPanel is a component that provide for advanced panels
  an interface that already handles the panel's base behavior:
  open from dropdown menu, close, and also the panel display style.
  It is not meant to be used directly, but instead to be used inside a
  specific advanced panel that pass to it a `title` and `body` slot.
  In that way, Advanced panels only care about implementing
  the content of the panel, rather the reimplemting the whole panel logic
  each time.
-->
<template>
  <!--
    The Livedata Advanced panel base
    Uses the Bootstrap 3 panel syntax.
  -->
  <div
    class="livedata-advanced-panel panel panel-default"
    v-show="panelOpened"
  >
    <!--
      Panel Header
      Contains the panel title on the left
      and buttons to close panel on the right
    -->
    <div class="panel-heading">
      <!--
        The slot containing the panel header
        The header should contains an icon and a title
      -->
      <span class="title">
        <slot name="header"></slot>
      </span>

      <!-- Panel header buttons -->
      <div class="actions">
        <!-- Collapse panel button -->
        <span
          class="action collapse-button"
          @click="collapsed = !collapsed"
        >
          <XWikiIcon v-if="!collapsed" :icon-descriptor="{name: 'arrow-in'}"/>
          <XWikiIcon v-else :icon-descriptor="{name: 'arrow_out'}"/>
        </span>
        <!-- Close panel button -->
        <span
          class="action close-button"
          @click="logic.uniqueArrayRemove(logic.openedPanels, panelId)"
        >
          <XWikiIcon :icon-descriptor="{name: 'cross'}" />
        </span>

      </div>
    </div>

    <!--
      Panel Body
    -->
    <div
      class="panel-body"
      v-if="!collapsed"
    >
      <!-- The slot containing the panel body content -->
      <slot name="body"></slot>
    </div>

  </div>
</template>


<script>
import XWikiIcon from "../utilities/XWikiIcon";
export default {

  name: "LivedataBaseAdvancedPanel",

  components: {XWikiIcon},

  inject: ["logic"],

  props: {
    // The panel id is used to open the panel in the dropdown menu
    // and is used as item in the `Logic.openedPanels` array
    panelId: String,
  },

  data () {
    return {
      // whether the panel is collapsed (panel body is hidden) or not
      // A panel will always be expanded when opened.
      collapsed: false,
    };
  },

  computed: {
    data () { return this.logic.data; },

    // returns whether the panel is opened based on the `Logic.openedPanels` array
    panelOpened () {
      return this.logic.uniqueArrayHas(this.logic.openedPanels, this.panelId);
    },
  },

  watch: {
    // whenever panel is opened, ensure it is not collapsed
    panelOpened () {
      if (this.panelOpened) {
        this.collapsed = false;
      }
    },
  },

};
</script>


<style>

.livedata-advanced-panel .panel-heading {
  position: relative;
  display: flex;
  flex-direction: row;
  justify-content: space-between;
  align-items: stretch;
  padding: 0;
}
.livedata-advanced-panel .panel-heading .title {
  padding: 10px 15px;
}

.livedata-advanced-panel .actions {
  display: flex;
  flex-direction: row;
  justify-content: flex-end;
  align-items: stretch;
}
.livedata-advanced-panel .actions .action {
  display: flex;
  justify-content: center;
  align-items: center;
  padding: 0 15px;
  cursor: pointer;
}

</style>
