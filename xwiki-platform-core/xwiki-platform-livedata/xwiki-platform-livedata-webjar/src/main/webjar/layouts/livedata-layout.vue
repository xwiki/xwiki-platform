<template>
  <div class="livedata-layout">

      <keep-alive>
        <component
          v-if="componentName"
          :is="componentName"
        ></component>
      </keep-alive>

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
  "vue!panels/livedata-advanced-panels",
], function (
  Vue,
) {
  Vue.component("livedata-layout", {

    name: "livedata-layout",

    inject: ["logic"],

    template: template,

    props: {
      layoutId: String,
    },

    data: function () {
      return {
        componentName: undefined,
      };
    },

    computed: {
      data: function () { return this.logic.data; },
    },

    watch: {
      layoutId: {
        immediate: true,
        handler: function () {
          var self = this;
          this.loadLayout(this.layoutId).catch(function (err) {
            if (self.layoutId && self.layoutId !== self.data.meta.defaultLayout) {
              console.warn(err);
              self.logic.changeLayout(self.data.meta.defaultLayout);
            } else {
              console.error(err);
            }
          });
        },
      },
    },

    methods: {
      loadLayout: function (layoutId) {
        var self = this;
        return new Promise (function (resolve, reject) {

          var componentName = "layout-" + layoutId;

          // load success callback
          var loadLayoutSuccess = function () {
            self.componentName = componentName;
            resolve(layoutId);
          };

          // load error callback
          var loadLayoutFailure = function (err) {
            reject(err);
          };

          // load layout based on it's filename
          require(["vue!layouts/" + layoutId + "/" + componentName],
            loadLayoutSuccess,
            loadLayoutFailure
          );

        });

      },
    },

  });
});
</script>


<style>


</style>
