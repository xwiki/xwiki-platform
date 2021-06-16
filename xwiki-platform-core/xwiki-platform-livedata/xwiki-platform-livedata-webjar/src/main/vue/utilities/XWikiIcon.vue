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
    v-if="iconSetType === 'IMAGE'"
    :src="iconDescriptor.url"
  />

  <span
    v-else-if="iconSetType === 'FONT'"
    :class="iconDescriptor.cssClass"
  ></span>

  <XWikiIcon
    v-else-if="iconSetType === 'REMOTE' && remoteIconDescriptor"
    :iconDescriptor="remoteIconDescriptor"
  />

  <span
    v-else
    class="icon-placeholder"
  ></span>

</template>


<script>
const iconCache = {};

export default {

  name: "XWikiIcon",

  props: {
    iconDescriptor: Object,
  },

  data () {
    return {
      remoteIconDescriptor: undefined,
    };
  },

  computed: {
    iconSetType () {
      if (this.iconDescriptor?.iconSetType) { return this.iconDescriptor?.iconSetType; }
      else if (this.iconDescriptor?.cssClass) { return "FONT"; }
      else if (this.iconDescriptor?.url) { return "IMAGE"; }
      else if (this.iconDescriptor?.name) { return "REMOTE"; }
      return "";
    },
  },

  methods: {
    async fetchRemoteIconDescriptor (iconName) {
      try {
        const iconURLQuery = `?media=json&name=${iconName}`;
        const iconURL = `/xwiki/rest/wikis/${XWiki.currentWiki}/iconThemes/icons${iconURLQuery}`;
        const response = await fetch(iconURL);
        const jsonResponse = await response.json();
        iconCache[iconName] = jsonResponse.icons[0];
        return jsonResponse.icons[0];
      } catch (err) {
        console.error(err);
      }
    },
  },

  watch: {
    icon: {
      async handler () {
        const iconName = this.iconDescriptor.name;
        // Only fetch remote icon descriptor if we have to
        if (this.iconSetType !== "REMOTE") return;
        if (!iconName) return;
        // If the icon was not already fetched, fetch it!
        if (!iconCache[iconName]) {
          // We set the iconCache value to the promise object, so that every other request
          // of the same icon whether they occur during the firts request or after,
          // will use the same promise and will not create another request
          iconCache[iconName] = this.fetchRemoteIconDescriptor(iconName);
        }
        // Set the icon to the resolved value of the promise
        this.remoteIconDescriptor = await iconCache[iconName];
      },
      immediate: true,
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
