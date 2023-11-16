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
<template>
  <CTemplate name="view" />
</template>
<script lang="ts">
import { CTemplate } from "@cristal/skin";
import { defineComponent } from "vue";

export default defineComponent({
  setup() {},
  components: {
    CTemplate,
  },
});
</script>
<style scoped>
:global(h1) {
  color: red;
}

:global(.template) {
  border: 1px solid;
}

:global(header) {
  grid-area: header;
  background-color: #30bdfe8a;
  padding: 10px;
}

:global(article) {
  grid-area: content;
  background-color: white;
  padding: 20px;
}

:global(aside) {
  grid-area: sidebar;
  background-color: #a3d0f0;
  padding: 20px;
}

:global(footer) {
  padding: 10px;
  grid-area: footer;
  background-color: #005ed8;
}

:global(.wrapper) {
  display: grid;
  grid-gap: 2px;
  grid-template-columns: 1fr 5fr;
  grid-template-areas:
    "header  header"
    "sidebar content"
    "footer  footer";
}

@media (max-width: 500px) {
  :global(.wrapper) {
    grid-template-columns: 4fr;
    grid-template-areas:
      "header"
      "content"
      "sidebar"
      "footer";
  }
}
</style>
