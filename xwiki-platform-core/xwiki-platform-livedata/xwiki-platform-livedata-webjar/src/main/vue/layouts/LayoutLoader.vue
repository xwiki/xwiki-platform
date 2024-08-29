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
  <div
    :class="['layout-loader', { waiting: loading, visible: visible }]"
  >
    <div class="loader-fill"></div>
  </div>
</template>


<script>
export default {

  name: "layout-loader",

  inject: ["logic"],

  data () {
    return {
      loadingData: false,
      loadingLayout: false,
      visible: false,
      delay: 200,
    };
  },

  computed: {
    loading () {
      return this.loadingData || this.logic.firstEntriesLoading || this.loadingLayout;
    }
  },

  methods: {
    watchLoading (propertyName, eventBefore, eventAfter) {
      let timeoutId;

      this.logic.onEvent(eventBefore, () => {
        clearTimeout(timeoutId);
        this[propertyName] = true;
        // Make the loading animation visible with a small delay in order to reduce the perceived loading time.
        timeoutId = setTimeout(() => {
          this.visible = true;
        }, this.delay);
      });

      this.logic.onEvent(eventAfter, () => {
        clearTimeout(timeoutId);
        this[propertyName] = false;
        this.visible = this.loading;
      });

    }
  },

  mounted () {
    this.watchLoading("loadingData", "beforeEntryFetch", "afterEntryFetch");
    this.watchLoading("loadingLayout", "layoutChange", "layoutLoaded");
  }

};
</script>


<style>

@keyframes waiting {
  0% {
    left: 0; right: unset;
    width: 0%;
  }
  50% {
    left: 0; right: unset;
    width: 100%;
  }
  51% {
    left: unset; right: 0;
    width: 100%;
  }
  100% {
    left: unset; right: 0;
    width: 0%;
  }
}

.layout-loader {
  position: relative;
  width: 100%;
  height: 0.4rem;
  visibility: hidden;
}

.layout-loader .loader-fill {
  position: absolute;
  top: 0;
  left: 0;
  width: 100%;
  height: 100%;
  background-color: @btn-primary-bg;
}

.layout-loader.waiting.visible {
  visibility: visible;
}

.layout-loader.waiting.visible .loader-fill {
  visibility: visible;
  -webkit-animation: waiting 2s linear infinite;
  animation: waiting 2s linear infinite;
}

</style>
