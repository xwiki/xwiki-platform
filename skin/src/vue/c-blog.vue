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
<script lang="ts" setup>
import Field from "./c-field.vue";
import { inject, ref } from "vue";
import type { CristalApp, Document, Logger } from "@xwiki/cristal-api";

type Props = { document: Document; mode: string };
const props = defineProps<Props>();

let logger: Logger;

const cristal = inject<CristalApp>("cristal");
if (cristal != undefined) {
  logger = cristal.getLogger("skin.vue.blog");
  logger?.debug("wiki30 content ref set");
}

const document = ref(props.document);
const mode = ref(props.mode);
</script>
<template>
  <div>
    <h1>
      <Field :document="document" name="headline" :mode="mode" />
    </h1>
    <Field :document="document" name="html" :mode="mode" type="html" />
    <x-divider />
    Genre:
    <Field :document="document" name="genre" :mode="mode" />
    Created by
    <Field :document="document" name="creator" :mode="mode" />
    on
    <Field :document="document" name="dateCreated" :mode="mode" />
  </div>
</template>
