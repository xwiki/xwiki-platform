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
          v-for="action in actions"
          :key="action.id"
          :title="action.name"
          :href="entry[action.propertyHref] || '#'"
        >
          <XWikiIcon :iconDescriptor="action.icon" />
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

  props: {
    visibleActions: {
      type: Array,
      default: () => ["viewEntry", "editEntry", "editRights", "deleteEntry"],
    },
  },

  computed: {
    actions () {
      return this.data.meta.actions.filter(action => this.visibleActions.includes(action.id));
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


/*
  We use a grid layout instead of flex so that wrapping items continue to be
  positioned in a nice grid pattern.
  However, IE11 does not support a lot grid layouts, but does not support
  the `@supports` at-rule either, so only browser which support this at-rule
  and "display: grid" will use the following styles
*/
@supports (display: grid) {

  .displayer-actions .actions-container {
    display: grid;
    grid-template-columns: repeat(auto-fill, 2.2rem);
    justify-content: start;
    align-content: start;
    justify-items: stretch;
    align-items: stretch;
    grid-row-gap: 0.5rem;
  }

  .displayer-actions .actions-container a {
    padding: 0;
    display: flex;
    justify-content: center;
    align-items: center;
  }

}


</style>
