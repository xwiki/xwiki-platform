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

<template>
  <div class="livedata-filter">
    <component
      v-if="component"
      :is="component"
      :property-id="propertyId"
      :index="index"
    ></component>

    <XWikiLoader
      v-else
    ></XWikiLoader>
  </div>
</template>


<script>
import XWikiLoader from "../utilities/XWikiLoader.vue";

export default {

  name: "LivedataFilter",

  inject: ["logic"],

  components: {
    XWikiLoader,
  },

  props: {
    propertyId: String,
    index: Number,
  },

  data () {
    return {
      component: undefined,
    };
  },

  computed: {
    data () { return this.logic.data; },
    filterId () {
      return this.logic.getFilterDescriptor(this.propertyId).id;
    },
  },

  methods: {
    capitalize (string) {
      string ??= "";
      return string[0].toUpperCase() + string.slice(1);
    },

    loadFilter (filterId) {
      return new Promise ((resolve, reject) => {

        filterId ??= this.filterId;

        // load success callback
        const loadFilterSuccess = component => {
          this.component = component;
          resolve(component);
        };

        // load error callback
        const loadFilterFailure = err => {
          reject(err);
        };

        // load filter based on it's id
        import("./Filter" + this.capitalize(filterId) + ".vue")
          .then(({ default: component }) => loadFilterSuccess(component))
          .catch(err => void loadFilterFailure(err));
      });
    },
  },

  mounted () {
    // load filter
    this.loadFilter().catch(err => {
      console.warn(err);
      this.loadFilter(this.data.meta.defaultFilter).catch(err => {
        console.error(err);
      });
    });
  },

};
</script>


<style>

@keyframes waiting {
  from { background-position-x: 100%; }
  to { background-position-x: -100%; }
}

.livedata-filter {
  display: inline-block;
  width: 100%;
  height: 100%;
  min-height: 1em;
}

.livedata-filter > * {
  width: 100%;
  height: 100%;
}

</style>
