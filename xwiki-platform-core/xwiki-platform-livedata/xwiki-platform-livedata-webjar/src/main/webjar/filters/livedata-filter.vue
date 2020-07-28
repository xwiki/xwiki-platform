<template>
  <div class="livedata-filter">
    <component
      v-if="componentName"
      :is="componentName"
      :property-id="propertyId"
      :index="index"
      :logic="logic"
    ></component>

    <xwiki-loader
      v-else
    ></xwiki-loader>
  </div>
</template>


<script>
/*
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
  */
define([
  "Vue",
  "vue!utilities/xwiki-loader",
], function (
  Vue
) {

  Vue.component("livedata-filter", {

    name: "livedata-filter",

    template: template,

    props: {
      propertyId: String,
      index: Number,
      logic: Object,
    },

    data: function () {
      return {
        componentName: undefined,
      };
    },

    computed: {
      data: function () { return this.logic.data; },
      filterId: function () {
        return this.logic.getFilterDescriptor(this.propertyId).id;
      },
    },

    methods: {
      loadFilter: function (filterId) {
        var self = this;
        return new Promise (function (resolve, reject) {

          var componentName = "filter-" + filterId;

          // load success callback
          var loadFilterSuccess = function () {
            self.componentName = componentName;
            resolve();
          };

          // load error callback
          var loadFilterFailure = function (err) {
            reject(err);
          };

          // load filter based on it's id
          require(["vue!filters/" + componentName],
            loadFilterSuccess,
            loadFilterFailure
          );

        });
      },
    },

    mounted: function () {
      var self = this;
      // load filter
      this.loadFilter(this.filterId).catch(function (err) {
        console.warn(err);
        self.loadFilter(self.data.meta.defaultFilter).catch (function (err) {
          console.error(err);
        });
      });
    },

  });

});
</script>


<style>

@keyframes waiting {
  from { background-position-x: 100%; }
  to { background-position-x: -100%; }
}

.livedata-displayer-container:empty {
  animation: waiting 2s linear infinite;
  --c1: transparent;
  --c2: #ccc4;
  background: linear-gradient(135deg, var(--c1) 25%, var(--c2) 50%, var(--c1) 75%);
  background-repeat: repeat;
  background-size: 200% 100%;
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
