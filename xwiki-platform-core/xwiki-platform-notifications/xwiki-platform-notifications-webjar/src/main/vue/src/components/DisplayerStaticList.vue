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
  DisplayerStaticList is a special custom displayer that displays a static list of elements.
-->
<template>
  <!--
    Uses the BaseDisplayer as root element, as it handles for us all the displayer default behavior.
  -->
  <BaseDisplayer
      class="displayer-staticList"
      view-only
      :property-id="propertyId"
      :entry="entry"
      :is-empty="false"
      :intercept-touch="false"
  >
    <template #viewer>
      <ul :class="'staticList ' + sanitizeHtml(entry[propertyId].extraClass)">
        <li v-for="item in entry[propertyId].items"
          :key="item"
        >
          {{ sanitizeHtml(item) }}
        </li>
      </ul>
    </template>

    <!--
      The displayer does not have an Editor widget. Therefore, we leave the editor template empty.
      Moreover, we add the `view-only` property on the BaseDisplayer component so that user can't possibly switch 
      to the Editor widget.
    -->
    <template #editor></template>
  </BaseDisplayer>
</template>

<script>
import {displayerMixin, BaseDisplayer} from "xwiki-livedata-vue";

export default {
  name: "displayer-staticList",
  components: {BaseDisplayer},
  // Add the displayerMixin to get access to all the displayers methods and computed properties inside this component.
  mixins: [displayerMixin]
}
</script>