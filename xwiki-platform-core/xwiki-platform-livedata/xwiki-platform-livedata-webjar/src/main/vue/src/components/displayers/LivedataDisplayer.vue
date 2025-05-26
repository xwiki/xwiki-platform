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
  The LivedataDisplayer component is used to display any data of the Livedata.
  Each LivedataDisplayer instance is defined by two props:
  - an entry
  - the property of the entry to display the value

  It comes with two modes: View mode (default) and Edit mode,
  that are implemented throught the Viewer and Editor widgets.
  When the user double-click on the Viewer widget, it changes to the Editor one
  Then when the user validates the new value for the entry property,
  it change back to the Viewer mode.

  There are differents type of displayers: Text, Number, Link, Html, ...
  Those are specified in every propertyDescriptors.
  Each specific displayer comes with its own implementation
  of Viewer and Editor widgets.

  The LivedataDisplayer component directly handle for us the choice of
  which custom Displayer implementation to use, based on the props passed to it,
  and dynamically import this Displayer component and mount it at runtime.
  If this custom implementation can't be found, or there was an loading error,
  then it falls back to the default one
  (specified by the `defaultDisplayer` property in the Livedata configuration).
-->
<template>
  <!--
    This is where the specific displayer component gets injected.
    The timestamp props if an optional and do not have to be declared in the injected component.
    It can be watched in order to force the upgrade of the component when the content is not passed using the props.
    For instance, it is used by the DisplayerXObjectProperty component in order to fetch an updated view content when
    the table is refreshed.
  -->
  <component
    class="livedata-displayer"
    :data-livedata-property-id="propertyId"
    :is="displayerComponent"
    v-if="displayerComponent"
    :property-id="propertyId"
    :entry="entry"
  ></component>
</template>


<script>
import { markRaw } from "vue";
import { componentStore } from "@/components/store.js";

export default {

  name: "LivedataDisplayer",

  inject: ["logic"],

  data() {
    return {
      displayerComponent: null,
    };
  },

  // The two props defining the Displayer
  props: {
    propertyId: String,
    entry: Object,
  },
  computed: {
    data() {
      return this.logic.data;
    },
    // The displayer id of the Displayer component to load,
    // corresponding to the property id
    displayerId() {
      return this.logic.getDisplayerDescriptor(this.propertyId).id;
    },
  },

  methods: {
    // Capitalize the given string
    capitalize(string) {
      string ??= "";
      return string[0].toUpperCase() + string.slice(1);
    },
  },
  // On mounted, try to load the Filter corresponding to the passed props,
  // or the default one as fallback
  async mounted() {
    const value = await componentStore.load("displayer", this.displayerId);
    this.displayerComponent = markRaw(value);
  },
};
</script>
