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
  <component
    v-if="component"
    :is="component"
    :property-id="propertyId"
    :entry="entry"
  ></component>

  <XWikiLoader
    v-else
  ></XWikiLoader>
</template>


<script>
import XWikiLoader from "../utilities/XWikiLoader.vue";

export default {

  name: "LivedataDisplayer",

  components: {
    XWikiLoader,
  },

  inject: ["logic"],

  props: {
    propertyId: String,
    entry: Object,
  },

  data () {
    return {
      component: undefined,
    };
  },

  computed: {
    data () { return this.logic.data; },
    displayerId () {
      return this.logic.getDisplayerDescriptor(this.propertyId).id;
    },
  },

  methods: {
    capitalize (string) {
      string ??= "";
      return string[0].toUpperCase() + string.slice(1);
    },

    loadDisplayer (displayerId) {
      return new Promise ((resolve, reject) => {

        displayerId ??= this.displayerId;

        // load success callback
        const loadDisplayerSuccess = component => {
          this.component = component;
          resolve(component);
        };

        // load error callback
        const loadDisplayerFailure = err => {
          reject(err);
        };

        // load displayer based on it's id
        import("./Displayer" + this.capitalize(displayerId) + ".vue")
          .then(({ default: component }) => loadDisplayerSuccess(component))
          .catch(err => void loadDisplayerFailure(err));
      });

    },
  },

  mounted () {
    // load displayer
    this.loadDisplayer(this.displayerId).catch(err => {
      console.warn(err);
      this.loadDisplayer(this.data.meta.defaultDisplayer).catch(err => {
        console.error(err);
      });
    });
  },

};
</script>


<style>

</style>
