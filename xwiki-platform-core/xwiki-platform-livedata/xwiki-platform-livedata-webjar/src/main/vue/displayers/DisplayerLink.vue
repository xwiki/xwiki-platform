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
  DisplayerLink is a custom displayer that displays the entry value as link.
  It uses the `propertyHref` proprety of the displayer descriptor,
  which refers to the property in the entries holding the displayer href
-->
<template>
  <!--
    Uses the BaseDisplayer as root element, as it handles for us
    all the displayer default behavior
  -->
  <BaseDisplayer
    class="displayer-link"
    :property-id="propertyId"
    :entry="entry"
  >

    <!-- Provide the Link Viewer widget to the `viewer` slot -->
    <template #viewer>
      <!-- If there is no value but still a link the user should still be able to click the link so we create an
        explicit "no value" message in that case. -->
      <a :href="href" :class="{'explicit-empty-value': !htmlValue}" v-html="htmlValue || '(no value)'"></a>
    </template>


    <!-- Keep the default Editor widget -->
    <template #editor></template>

  </BaseDisplayer>
</template>


<script>
import displayerMixin from "./displayerMixin.js";
import BaseDisplayer from "./BaseDisplayer.vue";

export default {

  name: "displayer-link",

  components: {
    BaseDisplayer,
  },

  // Add the displayerMixin to get access to all the displayers methods and computed properties inside this component
  mixins: [displayerMixin],

  computed: {
    // The link href taken from the propertyHref property of the entry
    href () {
      // propertyHref can have multiple values, in which case we use the first that is set on the live data entry.
      let values = this.config.propertyHref;
      if (typeof values === 'string') {
        values = [values];
      } else if (!Array.isArray(values)) {
        values = [];
      }
      return values.map(value => this.entry[value]).find(value => value) || '#';
    },

    htmlValue() {
      const container = document.createElement('div');
      container[this.config.html ? 'innerHTML' : 'textContent'] = this.value;
      // Remove the interactive content because it isn't allowed inside an anchor element.
      // See https://developer.mozilla.org/en-US/docs/Web/HTML/Element/a#properties
      // See https://developer.mozilla.org/en-US/docs/Web/Guide/HTML/Content_categories#interactive_content
      const interactiveContent = 'a, button, details, embed, iframe, keygen, label, select, textarea, audio[controls],'
        + 'img[usemap], input, menu[type=toolbar], object[usemap], video[controls]';
      [...container.querySelectorAll(interactiveContent)].forEach(node => node.parentNode.removeChild(node));
      return container.innerHTML.trim();
    }
  },

};
</script>


<style>

.livedata-displayer .explicit-empty-value {
  font-style: italic;
  color: grey;
}

</style>
