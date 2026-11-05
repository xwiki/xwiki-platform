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
import { translations } from "../translations";
import { useI18n } from "vue-i18n";
import type { UnsupportedLinkTargetConfig } from "@xwiki/platform-link-type-api";
import type { Container } from "inversify";

// The fields of the links pointing to a resource that no link target type supports: the reference of the linked
// resource can only be edited as plain text. The link modal passes the container to every target field (see
// `LinkTargetTypeForm`): it's declared although unused, so that it doesn't fall through as an attribute of the field.
defineProps<{ depsContainer: Container }>();

const model = defineModel<UnsupportedLinkTargetConfig>({
  required: true,
});

const { t } = useI18n({ messages: translations });
</script>

<template>
  <x-text-field
    v-bind="{ 'data-test': 'linkReference' }"
    :label="t('link-modal.unsupported.reference')"
    v-model="model.reference.reference"
    required
  />
</template>
