<script setup lang="ts">
import messages from "../../translations";
import { computed, defineModel } from "vue";
import { useI18n } from "vue-i18n";
import { VFileInput } from "vuetify/components/VFileInput";
import type { TextFieldModel, TextFieldProps } from "@xwiki/cristal-dsapi";

const { t } = useI18n({
  messages,
});

const model = defineModel<TextFieldModel>();
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
