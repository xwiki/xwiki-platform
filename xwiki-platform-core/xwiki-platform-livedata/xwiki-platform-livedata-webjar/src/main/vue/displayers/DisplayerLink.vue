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
  It uses the `propertyHref` property of the displayer descriptor,
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
    :is-view.sync="isView"
    :is-empty="false"
    @saveEdit="genericSave"
  >

    <!-- Provide the Link Viewer widget to the `viewer` slot -->
    <template #viewer>
      <!--
        If there is no value but still a link, the user should still be able to click the link,
        so we create an explicit "no value" message in that case
      -->
      <a v-if="linkContent && hasViewRight"
        :href="sanitizeUrl(href)"
        :class="{'explicit-empty-value': !html && !htmlValue}"
        v-html="sanitizeHtml(linkContent)"
      ></a>
      <span v-else v-html="sanitizeHtml(linkContent)"></span>
    </template>


    <!-- Keep the default Editor widget -->
    <template #editor></template>

  </BaseDisplayer>
</template>


<script>
import displayerMixin from "./displayerMixin.js";
import displayerLinkMixin from "./displayerLinkMixin.js";
import displayerStatesMixin from "./displayerStatesMixin";
import BaseDisplayer from "./BaseDisplayer.vue";

export default {

  name: "displayer-link",

  components: {
    BaseDisplayer,
  },

  // Add the displayerMixin to get access to all the displayers methods and computed properties inside this component
  mixins: [displayerMixin, displayerLinkMixin, displayerStatesMixin],
  
  props: {
    // An optional html to use instead of the computed htmlValue when displaying the inner html of the link.
    html: {
      optional: true
    }
  },

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
    linkContent() {
      if (!this.hasViewRight) {
        return this.$t('livedata.displayer.emptyValue');
      } else if (this.html) {
        return this.html;
      } else {
        return this.htmlValue || this.$t('livedata.displayer.link.noValue')
      }
    },
    hasViewRight() {
      return this.logic.isActionAllowed('view', this.entry)
    }
  },
};
</script>


<style>

.livedata-displayer.displayer-link .explicit-empty-value {
  font-style: italic;
  color: grey;
}

</style>
