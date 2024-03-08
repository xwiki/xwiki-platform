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
  The LivedataLayout component is used to display formatted data to the user.

  There are several layout, which are defined by their unique id.
  They should follow the path and naming convention:
  ./<layoutid>/Layout<layoutid>.vue

  The LivedataLayout component directly handle for us the choice of
  which layout to use, based on the Logic `currentLayoutId` property,
  and dynamically import this layout component and mount it at runtime.
  If the layout can't be found, or there was an loading error,
  then it falls back to the default one
  (specified by the `defaultLayout` property in the Livedata configuration).
-->
<template>
  <div class="livedata-layout">
    <p class="livedata-layout-description" :id="descriptionId" v-if="hasDescription">
      {{ description }}
    </p>

      <!--
        We are using the <keep-alive> tag in order to keep the layout mounted
        even when it is not displayed on the screen.
        This is a sort of caching, that avoid re-rendering the whole layout
        each time we switch back on it.
      -->
      <keep-alive>
        <!-- This is where the specific filter component gets injected -->
        <component
          v-if="layoutComponent"
          :is="layoutComponent"
          :aria-describedby="descriptionId"
        ></component>
      </keep-alive>

  </div>
</template>


<script>
// We import explicitly the most used layout to avoid having to load it dynamically during the component rendering.
import LayoutTable from "./table/LayoutTable.vue";
export default {

  name: "LivedataLayout",
  
  components: { LayoutTable },

  inject: ["logic"],

  props: {
    // The id of the layout to load
    layoutId: String,
  },

  data () {
    return {
      // The layout component
      // It is set to `undefined before it is resolved
      layoutComponent: undefined,
    };
  },

  computed: {
    data () { return this.logic.data; },
    description() {
      return this.data?.meta?.description;
    },
    hasDescription() {
      return this.description && this.description !== '';
    },
    descriptionId() {
      return `${this.logic.data.id}-description`;
    }
  },


  // On mounted and when the `layoutId` prop change,
  // try to load the layout corresponding to the layoutId
  // or the default one as fallback
  watch: {
    layoutId: {
      immediate: true,
      handler (layoutId, previousLayoutId) {
        // Try to load layout
        this.loadLayout(this.layoutId)
        .then(layoutComponent => {
          // dispatch events
          this.logic.triggerEvent("layoutLoaded", {
            layoutId,
            previousLayoutId,
            component: layoutComponent,
          });
        })
        .catch(err => {
          // If the layout was not the default one, try to load default layout
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
    // Capitalize the given string
    capitalize (string) {
      string ??= "";
      return string[0].toUpperCase() + string.slice(1);
    },

    // Load the layout component corresponding to the given layoutId
    // On success, set `this.layoutComponent` to the retreived component,
    // which automatically insert the component in the html
    loadLayout (layoutId) {
      return new Promise ((resolve, reject) => {

        layoutId ??= this.layoutId;

        // Load success callback
        const loadLayoutSuccess = layoutComponent => {
          this.layoutComponent = layoutComponent;
          resolve(layoutComponent);
        };

        // Load error callback
        const loadLayoutFailure = err => {
          reject(err);
        };

        // Load layout based on it's id
        import("./" + layoutId + "/Layout" + this.capitalize(layoutId) + ".vue")
          // We *have to* destructure the return value as `{ default: component }`,
          // because it's how Webpack is handling dynamic imports
          .then(({ default: component }) => loadLayoutSuccess(component))
          .catch(err => void loadLayoutFailure(err));
      });

    },
  },

};
</script>

