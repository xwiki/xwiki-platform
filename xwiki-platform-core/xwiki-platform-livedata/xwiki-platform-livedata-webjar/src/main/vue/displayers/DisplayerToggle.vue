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
  DisplayerToggle is a special custom displayer that displays actions
  concerning the entry.
  Actions are links to specific pages, whose url is a property of the entry.
-->
<template>
  <!--
    Uses the BaseDisplayer as root element, as it handles for us
    all the displayer default behavior
  -->
  <BaseDisplayer
      class="displayer-toggle"
      view-only
      :property-id="propertyId"
      :entry="entry"
      :is-empty="false"
      :intercept-touch="false"
  >

    <!-- Provide the Action Viewer widget to the `viewer` slot -->
    <template #viewer>
      <input
          type='checkbox'
          class='toggleableFilterPreferenceCheckbox'
          :checked="checked"
          :disabled="disabled"
          ref="input"
      />
      <!-- We keep this section hidden as it is only there to be copied when initializing the toggle -->
      <span v-show="false">
        <XWikiIcon
            :icon-descriptor="{name: iconName}"
            ref="icon"
            @ready="iconReady = true"
        />
      </span>

    </template>

    <!--
      The Action displayer does not have an Editor widget
      So we leave the editor template empty
      Moreover, we add the `view-only` property on the BaseDisplayer component
      so that user can't possibly switch to the Editor widget.
    -->
    <template #editor></template>


  </BaseDisplayer>
</template>

<script>
import displayerMixin from "./displayerMixin.js";
import BaseDisplayer from "./BaseDisplayer.vue";
import XWikiIcon from "../utilities/XWikiIcon.vue";
import $ from "jquery";

export default {
  name: "displayer-toggle",
  components: {BaseDisplayer, XWikiIcon},
  // Add the displayerMixin to get access to all the displayers methods and computed properties inside this component
  mixins: [displayerMixin],
  props: {
    iconName: {
      type: String,
      default: 'bell'
    }
  },
  data() {
    return {
      iconReady: false
    }
  },
  computed: {
    checked() {
      return this.entry[`${this.propertyId}_checked`]
    },
    disabled() {
      return this.entry[`${this.propertyId}_disabled`]
    },
    toggleData() {
      return this.entry[`${this.propertyId}_data`]
    }
  },
  watch: {
    iconReady: function (val) {
      if (val) {
        // Wait for the icon component to be fully rendered before copying its content.
        this.$nextTick(() => {
          $(this.$refs.input).bootstrapSwitch({
            size: 'mini',
            labelText: this.$refs.icon.$el.outerHTML,
            onSwitchChange() {
              this.logic.triggerEvent("toggle", {
                data: this.toggleData,
                checked: this.checked,
                disabled: this.disabled,
                callback: function(newData, newChecked, newDisabled) {
                  // TODO: add callback handling on XWiki.Notifications.Code.NotificationsSystemFiltersPreferencesMacro
                  this.data = newData;
                  this.checked = newChecked;
                  this.disabled = newDisabled;
                }
              })
            }
          })
        })
      }
    }
  }
}
</script>

<style scoped>

</style>