<script setup lang="ts">
import type { TextFieldModel, TextFieldProps } from "@xwiki/cristal-dsapi";
import { computed } from "vue";
import { VFileInput } from "vuetify/components/VFileInput";
import messages from "../../translations";
import { useI18n } from "vue-i18n";

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
