<!--
 * See the LICENSE file distributed with this work for additional
 * information regarding copyright ownership.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 *
 * This file is part of the Cristal Wiki software prototype
 * @copyright  Copyright (c) 2023 XWiki SAS
 * @license    http://opensource.org/licenses/AGPL-3.0 AGPL-3.0
 *
-->
<script lang="ts">
import { inject, Component, markRaw } from "vue";
import { Logger, CristalApp } from "@cristal/api";

let comps: Array<Component>;
let logger: Logger;

export default {
  setup() {
    let cristal = inject<CristalApp>("cristal");
    if (cristal) {
      comps = cristal.getUIXTemplates("editor");
      logger = cristal.getLogger("skin.vue.editor");
    }
  },
  data() {
    logger?.debug("Editor UIX components are ", comps);
    if (!comps || comps.length == 0) return {};
    else {
      let editComponent = null;
      logger?.debug("Using first editor UIX component ", comps);
      if (comps != null) {
        for (let i = 0; i < comps.length; i++) {
          logger?.debug("Route name ", this.$route.name);
          logger?.debug("Component name ", comps[i].editorname);
          if ("edit" + comps[i].editorname == this.$route.name) {
            editComponent = comps[i];
          }
        }
      }
      if (editComponent == null) {
        editComponent = comps[0];
      }
      logger?.debug("Final component ", editComponent);
      return {
        component: markRaw(editComponent),
      };
    }
  },
};
</script>
<template>
  Here in edit
  <article id="edit" ref="root">
    <UIX uixname="edit.before" />
    <component :is="component" />
    <UIX uixname="edit.after" />
  </article>
</template>
<style>
.pagemenu {
  float: right;
}
</style>
