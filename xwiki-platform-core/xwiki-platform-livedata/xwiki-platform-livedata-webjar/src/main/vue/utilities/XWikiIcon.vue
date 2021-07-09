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
  <img v-if="isImage" :src="url" :class="additionalClasses"/>
  <span v-else-if="isFont" :class="[cssClass, ...additionalClasses]"></span>
  <span v-else class="icon-placeholder"></span>
</template>


<script>

import $ from "jquery";

// This cache stores the metadata of the already resolved icons as well as the Promises for the icons currently being 
// asynchronously resolved.
// The goal of this cache is to only request for the resolution of an icon once per live data rendering.
const iconCache = {};

export default {

  name: "XWikiIcon",

  props: {
    iconDescriptor: Object,
    additionalClasses: {
      default: () => ([])
    }
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
    fetchRemoteIconDescriptor(iconName) {
      return new Promise((resolve, reject) => {
        const iconURL = `/xwiki/rest/wikis/${XWiki.currentWiki}/iconThemes/icons`;
        $.getJSON(iconURL, {name: iconName})
          .done((data) => {
            if (!data || (data.missingIcons || []).length > 0 || (data.icons || []).length < 1) {
              console.error("Malformed icon response", data);
              reject();
            } else {
              resolve(data.icons[0]);
            }
          })
          .fail((jqxhr, textStatus, error) => {
            console.error(textStatus, error)
            reject();
          })
      });
    },
  },

  watch: {
    iconDescriptor: {
      handler(iconDescriptor) {
        const iconName = iconDescriptor?.name;
        if (!iconName) {
          return;
        }
        // If the icon was not already fetched, fetch it!
        if (!iconCache[iconName]) {
          // We set the iconCache value to the promise object, so that every other request
          // of the same icon whether they occur during the firts request or after,
          // will use the same promise and will not create another request
          iconCache[iconName] = this.fetchRemoteIconDescriptor(iconName);
        }
        // Set the icon to the resolved value of the promise.
        iconCache[iconName].then(value => this.remoteIconDescriptor = value)
      },
      immediate: true
    }
  }
};
</script>


<style>
.icon-placeholder {
  width: 1em;
  height: 1em;
}
</style>
