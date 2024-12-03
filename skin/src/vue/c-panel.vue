<!--
See the LICENSE file distributed with this work for additional
information regarding copyright ownership.

This is free software; you can redistribute it and/or modify it
under the terms of the GNU Lesser General Public License as
published by the Free Software Foundation; either version 2.1 of
the License, or (at your option) any later version.

This software is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
Lesser General Public License for more details.

You should have received a copy of the GNU Lesser General Public
License along with this software; if not, write to the Free
Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
02110-1301 USA, or see the FSF site: http://www.fsf.org.
-->
<template>
  <!-- eslint-disable vue/no-v-html -->
  <div :id="panelidname" v-html="content.currentContent" />
</template>
<script lang="ts">
import { ContentTools } from "./contentTools";
import { inject, ref } from "vue";
import type { CristalApp, Logger, PageData } from "@xwiki/cristal-api";
import type { StorageProvider } from "@xwiki/cristal-backend-api";

let currentContent = "Panel content";
// TODO get rid of any
// eslint-disable-next-line @typescript-eslint/no-explicit-any
let contentMap: Map<string, any> = new Map<string, any>();
let logger: Logger;

export default {
  props: { panelName: { type: String, required: true } },
  setup(props: { panelName: string }) {
    contentMap.set(props.panelName, ref({ currentContent: currentContent }));
    const cristal = inject<CristalApp>("cristal");
    if (cristal != undefined) {
      ContentTools.init(cristal);
      logger = cristal.getLogger("skin.vue.panel");
      let panelName = props.panelName;
      logger?.debug("Ready to load panel ", panelName);
      const storage = cristal
        .getContainer()
        .get<StorageProvider>("StorageProvider")
        .get();
      storage
        .getPanelContent(panelName, "", "html")
        .then(async (panelData: PageData) => {
          if (panelData.html == "") {
            panelData.html = await cristal.renderContent(
              panelData.source,
              panelData.syntax,
              "html",
              cristal.getWikiConfig(),
            );
          }
          logger?.debug("Panel content is ", panelData.html);
          contentMap.get(props.panelName).value.currentContent = panelData.html;
          logger?.debug("Panel css is ", panelData.css);
          ContentTools.loadCSS(panelData.css);
          // ContentTools.loadJS(panelContent.js);
        });
    }
    return {
      content: contentMap.get(props.panelName),
      panelidname: "panelcontent" + props.panelName,
    };
  },
  mounted() {
    const cristal = inject<CristalApp>("cristal")!;
    logger?.debug("in mounted");
    ContentTools.transformImages(cristal, "panelcontent");
    // ContentTools.transformScripts(cristal);
  },
  updated() {
    const cristal = inject<CristalApp>("cristal")!;
    logger?.debug("in updated");
    ContentTools.listenToClicks(this.$el, cristal);
  },
};
</script>
