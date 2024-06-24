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
  <img
    v-if="isImage"
    :src="url"
    alt=""
  />
  <span
    v-else-if="isFont"
    :class="cssClass"
  ></span>
  <span
    v-else
    class="icon-placeholder"
  ></span>
</template>


<script>
// This cache stores the metadata of the already resolved icons as well as the Promises for the icons currently being 
// asynchronously resolved.
// The goal of this cache is to only request for the resolution of an icon once per live data rendering.
const iconCache = {};

export default {

  name: "XWikiIcon",

  props: {
    iconDescriptor: Object
  },

  data: () => ({
    remoteIconDescriptor: undefined
  }),

  computed: {
    descriptor() {
      return this.remoteIconDescriptor || this.iconDescriptor
    },
    iconSetType() {
      let type = "";
      if (this.descriptor?.iconSetType) {
        type = this.descriptor?.iconSetType;
      } else if (this.descriptor?.cssClass) {
        type = "FONT";
      } else if (this.descriptor?.url) {
        type = "IMAGE";
      }
      return type;
    },
    isImage() {
      return this.iconSetType === 'IMAGE'
    },
    isFont() {
      return this.iconSetType === 'FONT'
    },
    url() {
      return this.descriptor?.url
    },
    cssClass() {
      return this.descriptor?.cssClass
    }
  },

  methods: {
    async fetchRemoteIconDescriptor(iconName) {
      try {
        const parameters = `name=${encodeURIComponent(iconName)}`;
        const iconURL = `${XWiki.contextPath}/rest/wikis/${XWiki.currentWiki}/iconThemes/icons?${parameters}`;
        const response = await window.fetch(iconURL, {
          headers: {
            'Accept': 'application/json'
          }
        });
        const jsonResponse = await response.json();
        iconCache[iconName] = jsonResponse?.icons[0];
        return jsonResponse?.icons[0];
      } catch (err) {
        console.error(err);
      }
    },
  },

  watch: {
    iconDescriptor: {
      async handler(iconDescriptor) {
        // If the new icon descriptor already has an icon type, we consider it is not needed to fetch its metadata
        // remotely. The remote icon descriptor is set to undefined and the iconDescriptor prop value are directly used
        // to render the icon.
        const iconSetType = iconDescriptor?.iconSetType
        if (iconSetType === 'IMAGE' || iconSetType === 'FONT') {
          this.remoteIconDescriptor = undefined
          return;
        }
        const iconName = iconDescriptor?.name;
        if (!iconName) {
          this.remoteIconDescriptor = undefined
          return;
        }
        // If the icon was not already fetched, fetch it!
        if (!iconCache[iconName]) {
          // We set the iconCache value to the promise object, so that every other request of the same icon whether they
          // occur during the first request or after, will use the same promise and will not create another request.
          iconCache[iconName] = this.fetchRemoteIconDescriptor(iconName);
        }
        // Set the icon to the resolved value of the promise.
        this.remoteIconDescriptor = await iconCache[iconName];
        // Send a ready even once initialize so that parent containers can know when this component is in a stable 
        // state. 
        this.$emit('ready');
      },
      immediate: true
    },
  },
};
</script>


<style>
.icon-placeholder {
  width: 1em;
  height: 1em;
}
</style>
