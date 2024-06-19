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
  <template v-if="pageStatus.withSheet">
    <c-template
      :name="pageStatus.sheet"
      :document="pageStatus.document"
      mode="edit"
    />
  </template>
  <template v-else>
    <iframe
      id="editui"
      :src="link"
      width="100%"
      height="90%"
      border="0"
      frameborder="0"
    />
  </template>
</template>
<script lang="ts">
import { Logger, CristalApp } from "@xwiki/cristal-api";
import { inject } from "vue";
import { ref } from "vue";
import { CTemplate } from "@xwiki/cristal-skin";

const pageStatus = ref({
  currentContent: "Initial content",
  css: [],
  js: [],
  html: "",
  document: null,
  withSheet: false,
  sheet: "",
});
let logger: Logger;

export default {
  components: { CTemplate },
  setup() {
    const cristal = inject<CristalApp>("cristal");
    if (cristal) {
      logger = cristal.getLogger("skin.vue.edit");
      cristal.setContentRef(pageStatus);
      let link =
        cristal?.getWikiConfig().baseURL +
        "/bin/view/" +
        cristal?.getCurrentPage().replaceAll(".", "/") +
        "?xpage=plain&htmlHeaderAndFooter=true&minify=false&outputTitle=true#edit";
      return { link: link, pageStatus: pageStatus };
    } else {
      return { link: "", pageStatus: pageStatus };
    }
  },
  mounted() {
    const cristal = inject<CristalApp>("cristal");
    window.addEventListener(
      "message",
      (event) => {
        // if (event.origin !== "http://example.org:8080") return;
        logger?.debug("received message ", event.data);
        if (
          event.data &&
          event.data.type &&
          (event.data.type == "cancel" || event.data.type == "save")
        ) {
          logger?.debug("Go finished edit");
          let path = "/" + cristal?.getCurrentPage() + "/view";
          logger?.debug("pushing path in router", path);
          cristal?.getRouter().push({ path: path });
          cristal?.loadPage().then(function () {});
        }
      },
      false,
    );
  },
};
</script>
