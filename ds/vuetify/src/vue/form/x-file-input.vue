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
<script setup lang="ts">
import messages from "../../translations";
import { computed, defineModel } from "vue";
import { useI18n } from "vue-i18n";
import { VFileInput } from "vuetify/components/VFileInput";
import type { FileInputModel, TextFieldProps } from "@xwiki/cristal-dsapi";

const { t } = useI18n({
  messages,
});

const model = defineModel<FileInputModel>();
const props = defineProps<TextFieldProps>();

const rules = computed(() => {
  const rulesList = [];
  if (props.required) {
    rulesList.push((value: unknown) => {
      if (value) {
        return true;
      } else {
        return t("vuetify.file.input.mandatory");
      }
    });
  }
  return rulesList;
});
</script>

<template>
  <v-file-input
    v-model="model"
    :label="label"
    :name="name"
    :rules="rules"
  ></v-file-input>
</template>

<style scoped></style>
