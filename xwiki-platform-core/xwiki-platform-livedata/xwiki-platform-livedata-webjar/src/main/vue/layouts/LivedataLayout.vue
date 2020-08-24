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
  <div class="livedata-layout">

      <keep-alive>
        <component
          v-if="component"
          :is="component"
        ></component>
      </keep-alive>

  </div>
</template>


<script>
export default {

  name: "LivedataLayout",

  inject: ["logic"],

  props: {
    layoutId: String,
  },

  data () {
    return {
      component: undefined,
    };
  },

  computed: {
    data () { return this.logic.data; },
  },

  watch: {
    layoutId: {
      immediate: true,
      handler () {
        this.loadLayout(this.layoutId).catch(err => {
          if (this.layoutId && this.layoutId !== this.data.meta.defaultLayout) {
            console.warn(err);
            this.logic.changeLayout(this.data.meta.defaultLayout);
          } else {
            console.error(err);
          }
        });
      },
    },
  },

  methods: {
    capitalize (string) {
      string ??= "";
      return string[0].toUpperCase() + string.slice(1);
    },

    loadLayout (layoutId) {
      return new Promise ((resolve, reject) => {

        layoutId ??= this.layoutId;

        // load success callback
        const loadLayoutSuccess = component => {
          this.component = component;
          resolve(component);
        };

        // load error callback
        const loadLayoutFailure = err => {
          reject(err);
        };

        // load layout based on it's filename
        import("./" + layoutId + "/Layout" + this.capitalize(layoutId) + ".vue")
          .then(({ default: component }) => loadLayoutSuccess(component))
          .catch(err => void loadLayoutFailure(err));
      });

    },
  },

};
</script>


<style>

</style>
