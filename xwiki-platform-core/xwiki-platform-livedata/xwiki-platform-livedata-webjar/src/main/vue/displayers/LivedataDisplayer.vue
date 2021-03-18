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
    It can be watched in order to force the upgrade the the component when the content is not passed using the props.
    For instance, it is used by the DisplayerXClassProperty component in order to fetch an updated view content when
    the table is refreshed.
  -->
  <component
    class="livedata-displayer"
    v-if="displayerComponent"
    :is="displayerComponent"
    :property-id="propertyId"
    :entry="entry"
    :timestamp="new Date().getTime()"
  ></component>

  <!--
    This loader component is displayed while the displayer is being loaded
  -->
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

  // The two props defining the Displayer
  props: {
    propertyId: String,
    entry: Object,
  },

  data () {
    return {
      // The displayer component used to display the value
      // It is set to `undefined before it is resolved
      displayerComponent: undefined,
    };
  },

  computed: {
    data () { return this.logic.data; },
    // The displayer id of the Displayer component to load,
    // corresponding to the property id
    displayerId () {
      return this.logic.getDisplayerDescriptor(this.propertyId).id;
    },
  },

  methods: {
    // Capitalize the given string
    capitalize (string) {
      string ??= "";
      return string[0].toUpperCase() + string.slice(1);
    },

    // Load the displayer component corresponding to the given displayerId
    // On success, set `this.displayerComponent` to the retrieved component,
    // which automatically insert the component in the html
    loadDisplayer (displayerId) {
      return new Promise ((resolve, reject) => {

        displayerId ??= this.displayerId;

        // Load success callback
        const loadDisplayerSuccess = displayerComponent => {
          this.displayerComponent = displayerComponent;
          resolve(displayerComponent);
        };

        // Load error callback
        const loadDisplayerFailure = err => {
          reject(err);
        };

        // Load displayer based on it's id
        import("./Displayer" + this.capitalize(displayerId) + ".vue")
          // We *have to* destructure the return value as `{ default: component }`,
          // because it's how Webpack is handling dynamic imports
          .then(({ default: component }) => loadDisplayerSuccess(component))
          .catch(err => void loadDisplayerFailure(err));
      });

    },
  },

  // On mounted, try to load the Displayer corresponding to the passed props,
  // or the default one as fallback
  mounted () {
    // Try to load Displayer
    this.loadDisplayer(this.displayerId).catch(err => {
      // Try to load default Displayer
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
