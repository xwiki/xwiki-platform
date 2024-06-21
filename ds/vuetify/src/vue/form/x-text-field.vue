<script setup lang="ts">
import { type TextFieldProps } from "@xwiki/cristal-dsapi";
import { computed } from "vue";
import { VTextField } from "vuetify/components/VTextField";
import messages from "../../translations";
import { useI18n } from "vue-i18n";

const { t } = useI18n({
  messages,
});
const props = defineProps<TextFieldProps>();

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
  <v-text-field :label="label" :name="name" :rules="rules"></v-text-field>
</template>

<style scoped></style>
