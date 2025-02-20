<script setup lang="ts">
import messages from "../../translations";
import { computed } from "vue";
import { useI18n } from "vue-i18n";
import { VTextField } from "vuetify/components/VTextField";
import type { TextFieldProps } from "@xwiki/cristal-dsapi";

const { t } = useI18n({
  messages,
});
const props = defineProps<TextFieldProps>();
const input = defineModel<string>();

const rules = computed(() => {
  const rulesList = [];
  if (props.required) {
    rulesList.push((value: unknown) => {
      if (value) {
        return true;
      } else {
        return t("vuetify.text.input.mandatory");
      }
    });
  }
  return rulesList;
});
</script>

<template>
  <v-text-field
    v-model="input"
    :label="label"
    :name="name"
    :autofocus="autofocus"
    :hint="help"
    :persistent-hint="help !== undefined"
    :rules="rules"
  ></v-text-field>
</template>

<style scoped></style>
