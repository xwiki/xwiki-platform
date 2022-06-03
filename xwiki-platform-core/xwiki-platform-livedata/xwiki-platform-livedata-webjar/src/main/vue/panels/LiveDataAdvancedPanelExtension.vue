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
  The LiveDataAdvancedPanelExtension component is used to provide
  extensions a way to inject a custom panel.
-->
<template>
  <!--
    Uses the LivedataBaseAdvancedPanel as root element, as it handles for us
    all the Advanced Panels default behavior
  -->
  <LivedataBaseAdvancedPanel
      :class="['livedata-advanced-panel-extension', `livedata-advanced-panel-${panel.id}`]"
      :panel-id="panel.id"
      ref="basePanel"
  >

    <!-- Provide the panel title and icon to the `header` slot -->
    <template #header>
      <XWikiIcon :icon-descriptor="{name: panel.icon}"/>
      {{ panel.title }}
    </template>

    <!-- Define panel content inside the `body` slot -->
    <template #body>
      <div ref="bodyContainer" class="extension-body"></div>
    </template>
  </LivedataBaseAdvancedPanel>

</template>

<script>
import LivedataBaseAdvancedPanel from "./LivedataBaseAdvancedPanel.vue";
import XWikiIcon from "../utilities/XWikiIcon";

export default {
  name: "LiveDataAdvancedPanelExtension",

  components: {
    XWikiIcon,
    LivedataBaseAdvancedPanel,
  },

  inject: ["logic"],

  props: {'panel': Object},

  methods: {
    attachContainer() {
      // Make sure nothing happens if this method is called several times or at the wrong time.
      if ('bodyContainer' in this.$refs) {
        const bodyContainer = this.$refs.bodyContainer;
        if (!bodyContainer.hasChildNodes()) {
          bodyContainer.appendChild(this.panel.container);
        } else if (bodyContainer.firstChild !== this.panel.container) {
          bodyContainer.firstChild.replaceWith(this.panel.container);
        }
      }
    }
  },

  watch: {
    'panel.container': function () {
      this.attachContainer();
    }
  },

  mounted() {
    this.attachContainer();
    // Watch the child's collapsed property to re-attach the body element when the body is rendered again.
    this.$watch("$refs.basePanel.collapsed", function (newValue) {
      if (!newValue) {
        // Wait for the next tick such that the change has been applied in the DOM.
        this.$nextTick(this.attachContainer);
      }
    });
  }
}
</script>
