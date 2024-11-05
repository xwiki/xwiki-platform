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
  <div>
    <h1>
      <Field :document="document" name="headline" :mode="mode" />
    </h1>
    <ul>
      <li>
        Storyline:
        <Field :document="document" name="text" :mode="mode" type="html" />
      </li>
      <li>
        Poster:
        <Field :document="document" name="thumbnail" :mode="mode" type="html" />
      </li>
      <li>Genre: <Field :document="document" name="genre" :mode="mode" /></li>
      <li>Placeholder</li>
      <li>
        Director: <Field :document="document" name="director" :mode="mode" />
      </li>
      <li>Seen: <Field :document="document" name="seen" :mode="mode" /></li>
      <li>
        Runtime: <Field :document="document" name="runtime" :mode="mode" />
      </li>
      <li>
        Publish date:
        <Field :document="document" name="datePublished" :mode="mode" />
      </li>
      <li>
        Wyisywyg:
        <Field :document="document" name="wysiwyg" :mode="mode" type="html" />
      </li>
    </ul>
  </div>
</template>
<script lang="ts">
import Field from "./c-field.vue";
import { inject } from "vue";
import type { CristalApp, Document, Logger } from "@xwiki/cristal-api";
import type { PropType } from "vue/dist/vue";

let logger: Logger;

export default {
  components: {
    Field,
  },
  props: {
    document: { type: Object as PropType<Document>, required: true },
    mode: { type: String, required: true },
  },
  setup() {
    const cristal = inject<CristalApp>("cristal");
    if (cristal != null) {
      logger = cristal.getLogger("skin.vue.movie");
      logger?.debug("cristal object content ref set");
    }
    return { link: "/" + cristal?.getCurrentPage() + "/edit" };
  },
};
</script>
