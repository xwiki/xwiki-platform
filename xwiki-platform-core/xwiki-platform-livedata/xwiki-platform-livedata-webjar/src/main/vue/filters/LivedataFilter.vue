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
  The LivedataFilter component is used to filter properties of the Livedata
  Each LivedataFilter is defined by two props:
  - a property to filter
  - the index of the property filter array to modify

  It comes with a widget that allows the user to input a value.
  There are differents type of filters: Text, Number, List, ...
  Those are specified in every propertyDescriptors.
  Each specific filter comes with its own implementation of the widget

  The LivedataFilter component directly handle for us the choice of
  which custom Filter implementation to use, based on the props passed to it,
  and dynamically import this Filter component and mount it at runtime.
  If this custom implementation can't be found, or there was an loading error,
  then it falls back to the default one
  (specified by the `defatulFilter` property in the Livedata configuration).
-->
<template>
  <!--
    This is where the specific filter component gets injected
  -->
  <component
    :class="['livedata-filter', isFiltering? 'filtering': '']"
    v-if="filterComponent"
    :is="filterComponent"
    :property-id="propertyId"
    :index="index"
    :is-filtering.sync="isFiltering"
    :is-advanced="isAdvanced"
  ></component>

  <!--
    This loader component is displayed while the filter is being loaded
  -->
  <XWikiLoader
    class="livedata-filter-loader"
    v-else
  ></XWikiLoader>
</template>


<script>
import XWikiLoader from "../utilities/XWikiLoader.vue";
import FilterBoolean from "./FilterBoolean.vue"
import FilterDate from "./FilterDate.vue"
import FilterList from "./FilterList.vue"
import FilterNumber from "./FilterNumber.vue"
import FilterText from "./FilterText.vue"

export default {

  name: "LivedataFilter",

  inject: ["logic"],

  components: {
    XWikiLoader,
    FilterBoolean,
    FilterDate,
    FilterList,
    FilterNumber,
    FilterText,
  },

  // The two props defining the Filter
  props: {
    propertyId: String,
    index: Number,
    isAdvanced: {
      type: Boolean,
      default: false
    }
  },

  data () {
    return {
      // The filter component used to filter the property
      // It is set to `undefined before it is resolved
      filterComponent: undefined,
      isFiltering: false
    };
  },

  computed: {
    data () { return this.logic.data; },
    // The filter id of the Filter component to load
    // corresponding to the property id
    filterId () {
      return this.logic.getFilterDescriptor(this.propertyId).id;
    },
  },

  methods: {
    // Capitalize the given string
    capitalize (string) {
      string ??= "";
      return string[0].toUpperCase() + string.slice(1);
    },

    // Load the filter component corresponding to the given filterId
    // On success, set `this.filterComponent` to the retreived component,
    // which automatically insert the component in the html
    loadFilter (filterId) {
      return new Promise ((resolve, reject) => {

        filterId ??= this.filterId;

        // Load success callback
        const loadFilterSuccess = filterComponent => {
          this.filterComponent = filterComponent;
          resolve(filterComponent);
        };

        // Load error callback
        const loadFilterFailure = err => {
          reject(err);
        };

        // Load filter based on it's id
        import("./Filter" + this.capitalize(filterId) + ".vue")
          // We *have to* destructure the return value as `{ default: component }`,
          // because it's how Webpack is handling dynamic imports
          .then(({ default: component }) => loadFilterSuccess(component))
          .catch(err => void loadFilterFailure(err));
      });
    },
  },

  // On mounted, try to load the Filter corresponding to the passed props,
  // or the default one as fallback
  mounted () {
    // Try to load Filter
    this.loadFilter().catch(err => {
      // Try to load default Filter
      console.warn(err);
      this.loadFilter(this.data.meta.defaultFilter).catch(err => {
        console.error(err);
      });
    });
  },

};
</script>


<style>

.livedata-filter,
.livedata-filter-loader {
  display: inline-block;
  width: 100% !important;
  height: 100% !important;
  min-height: 1em;
}

</style>
