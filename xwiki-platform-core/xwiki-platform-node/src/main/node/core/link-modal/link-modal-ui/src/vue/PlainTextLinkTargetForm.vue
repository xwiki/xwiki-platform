<!--
  See the NOTICE file distributed with this work for additional
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
<script setup lang="ts">
import LinkForm from "./LinkForm.vue";
import type { TextFieldProps } from "@xwiki/platform-dsapi";
import type {
  LinkData,
  PlainTextLinkTargetConfig,
} from "@xwiki/platform-link-type-api";

defineProps<{
  linkData: LinkData;

  /**
   * The (translated) label of the location field.
   */
  fieldLabel: string;

  /**
   * The value of the `data-test` attribute of the location field.
   */
  dataTest: string;

  /**
   * The type of the location field (e.g. "url" to validate it as a URL).
   */
  inputType?: TextFieldProps["type"];
}>();

const model = defineModel<PlainTextLinkTargetConfig>({
  required: true,
});
</script>

<template>
  <!-- The form of the link target types designated by a plain text location (e.g. URL, path, UNC path), and of the
       link target types registered without a form. -->
  <LinkForm :link-data>
    <template #config>
      <x-text-field
        v-bind="{ 'data-test': dataTest }"
        :label="fieldLabel"
        :type="inputType ?? 'text'"
        v-model="model.location"
        required
      />
    </template>
  </LinkForm>
</template>
