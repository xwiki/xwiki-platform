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
  >

    <!-- Provide the Action Viewer widget to the `viewer` slot -->
    <template #viewer>
      <div class="actions-container">
        <a
          v-for="actionId in visibleActions"
          :key="actionId"
          :title="actions[actionId].name"
          :href="entry[actions[actionId].propertyUrl] || '#'"
        >
          <span :class="`fa fa-${actions[actionId].faIcon}`"></span>
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

export default {

  name: "displayer-actions",

  components: {
    BaseDisplayer,
  },

  props: {
    visibleActions: {
      type: Array,
      default: () => ["view", "edit", "delete"],
    },
  },

  // Add the displayerMixin to get access to all the displayers methods and computed properties inside this component
  mixins: [displayerMixin],

  computed: {
    actions () {
      return {
        view: {
          name: "View",
          propertyUrl: "doc_url",
          faIcon: "eye",
        },
        edit: {
          name: "Edit",
          propertyUrl: "doc_edit_url",
          faIcon: "pencil",
        },
        rights: {
          name: "Rights",
          propertyUrl: "doc_rights_url",
          faIcon: "lock",
        },
        "delete": {
          name: "Delete",
          propertyUrl: "doc_delete_url",
          faIcon: "trash",
        },
      };
    },
  },

};
</script>


<style>

.displayer-actions .actions-container {
  display: flex;
  flex-direction: row;
  flex-wrap: wrap;
  justify-content: flex-start;
  align-content: center;
  align-items: center;
  width: 100%;
  height: 100%;
}

.displayer-actions .actions-container a {
  display: block;
  padding: 0 0.8rem;
  white-space: nowrap;
}

</style>
