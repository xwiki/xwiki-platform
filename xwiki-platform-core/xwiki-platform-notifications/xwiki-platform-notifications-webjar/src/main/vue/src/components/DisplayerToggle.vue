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
  DisplayerToggle is a special custom displayer that displays a toggle element supported by bootstrap-switch.
  It is sending a "xwiki:livedata:toggle" even on state change, to be listened for by external scripts.
-->
<template>
  <!--
    Uses the BaseDisplayer as root element, as it handles for us all the displayer default behavior.
  -->
  <BaseDisplayer
      class="displayer-toggle"
      view-only
      :property-id="propertyId"
      :entry="entry"
      :is-empty="false"
      :intercept-touch="false"
  >
    <template #viewer>
      <input
          type='checkbox'
          class='toggleableFilterPreferenceCheckbox'
          ref="input"
      />
      <!-- We keep this section hidden as it is only there to be copied when initializing the toggle. -->
      <span v-show="false">
        <XWikiIcon
            :icon-descriptor="{name: iconName}"
            ref="icon"
            @ready="iconReady = true"
        />
      </span>
    </template>

    <!--
      The Toggle displayer does not have an Editor widget. Therefore, we leave the editor template empty.
      Moreover, we add the `view-only` property on the BaseDisplayer component so that user can't possibly switch 
      to the Editor widget.
    -->
    <template #editor></template>
  </BaseDisplayer>
</template>

<script>
import {displayerMixin, BaseDisplayer, XWikiIcon} from "xwiki-livedata-vue";
import $ from "jquery";

export default {
  name: "displayer-toggle",
  components: {BaseDisplayer, XWikiIcon},
  // Add the displayerMixin to get access to all the displayers methods and computed properties inside this component.
  mixins: [displayerMixin],
  props: {
    iconName: {
      type: String,
      default: 'bell'
    }
  },
  data() {
    return {
      // When this value changes to true following the ready from XWikiIcon, the toggle is initialized.
      iconReady: false,
      innerChecked: this.entry[`${this.propertyId}_checked`],
      innerDisabled: this.entry[`${this.propertyId}_disabled`],
      innerData: {
        ... this.entry[`${this.propertyId}_data`]
      }
    }
  },
  watch: {
    iconReady: function (val) {
      if (val) {
        const component = this;
        // Wait for the icon component to be fully rendered before copying its content.
        this.$nextTick(() => {
          $(this.$refs.input).bootstrapSwitch({
            size: 'mini',
            state: component.innerChecked,
            disabled: component.innerDisabled,
            labelText: this.$refs.icon.$el.outerHTML,
            /*
             Send a xwiki:livedata:toggle event with the following event data:
             - the data of the toggle
             - the new state of the toggle
             - the disabled status of the toggle
             - a callback. When the callback is called, the toggle component is updated with the provided data, state, 
                and disabled status. 
            */
            onSwitchChange(event, state) {
              const toggleData = component.innerData;
              const disabledVal = component.innerDisabled;
              component.logic.triggerEvent("toggle", {
                data: toggleData,
                checked: state,
                disabled: disabledVal,
                callback: function ({
                  data = toggleData,
                  checked = state,
                  disabled = disabledVal
                }) {
                  component.innerData = data;
                  component.innerChecked = checked;
                  component.innerDisabled = disabled;
                  // The last parameter is skip, preventing to call onSwitchChange again.
                  $(component.$refs.input).bootstrapSwitch('state', checked, true);
                  $(component.$refs.input).bootstrapSwitch('disabled', disabled);
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