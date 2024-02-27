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
  DisplayerActions is a special custom displayer that displays actions
  concerning the entry.
  Actions are links to specific pages, whose url is a property of the entry.
-->
<template>
  <!--
    Uses the BaseDisplayer as root element, as it handles for us
    all the displayer default behavior
  -->
  <BaseDisplayer
    class="displayer-actions"
    view-only
    :property-id="propertyId"
    :entry="entry"
    :is-empty="false"
    :intercept-touch="false"
  >

    <!-- Provide the Action Viewer widget to the `viewer` slot -->
    <template #viewer>
      <div class="actions-container">
        <a
          v-for="action in actions"
          :key="action.id"
          :class="'action action_' + action.id"
          :title="action.description"
          :href="sanitizeUrl(entry[action.urlProperty]) || '#'"
          @click="handleClick($event, action)"
        >
          <XWikiIcon :iconDescriptor="action.icon" /><span class="action-name">{{ action.name }}</span>
        </a>
      </div>
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

export default {

  name: "displayer-actions",

  components: {
    BaseDisplayer,
    XWikiIcon,
  },

  // Add the displayerMixin to get access to all the displayers methods and computed properties inside this component
  mixins: [displayerMixin],

  computed: {
    actions () {
      // The list of actions can be overwritten from the displayer configuration.
      return (this.config.actions || this.data.meta.actions)
        // Show only the actions that are allowed for the current live data entry.
        .filter(action => this.logic.isActionAllowed(action, this.entry))
        .map(action => this.logic.getActionDescriptor(action));
    },
  },
  methods: {
    async handleClick(event, action) {
      const {async} = action;
      if (async) {
        event.preventDefault();
        const confirmed = await new Promise((resolve) => {
          if (async.confirmationMessage) {
            new XWiki.widgets.ConfirmationBox({
              onYes: () => resolve(true),
              onNo: () => resolve(false)
            }, {
              confirmationText: async.confirmationMessage
            })
          } else {
            resolve(true)
          }
        })
        if (confirmed) {
          const notif = new XWiki.widgets.Notification(async.loadingMessage, 'inprogress');
          const resource = this.sanitizeUrl(this.entry[action.urlProperty]);

          const options = {
            "method": async.method
          };
          if (async.body) {
            options.body = async.body;

          }

          const response = await fetch(resource, options)
          if (response.ok) {
            notif.replace(new XWiki.widgets.Notification(async.successMessage, 'done'));
            this.logic.updateEntries();
          } else {
            notif.replace(new XWiki.widgets.Notification(async.failureMessage, 'error'));
          }
        }
      }
    }
  }
};
</script>


<style>

.displayer-actions .action {
  color: @text-muted;
  white-space: nowrap;
}

.displayer-actions .action + .action {
  margin-left: .5em;
}

.displayer-actions .action-name {
  margin-left: .25em;
}

</style>
