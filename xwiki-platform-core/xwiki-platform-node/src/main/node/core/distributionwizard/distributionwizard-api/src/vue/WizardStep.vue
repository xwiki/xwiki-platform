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
import { computed, onUpdated, useTemplateRef } from "vue";
import type { WizardStepProps } from "../WizardStepProps";
const emit = defineEmits(["validateStep", "invalidateStep"]);
const stepRef = useTemplateRef<any>("stepRefId");
defineProps<{ step: WizardStepProps; component: unknown }>();
function validateStep() {
  emit("validateStep");
}
function invalidateStep() {
  emit("invalidateStep");
}
const wizardStepCallback = computed(() => {
  return stepRef.value.stepAnswerCallback;
});

onUpdated(() => {
  const skinExtensions = document.getElementById("skinExtensions");
  if (skinExtensions && skinExtensions.hasChildNodes()) {
    while (skinExtensions.hasChildNodes() && skinExtensions.firstChild) {
      skinExtensions.firstElementChild?.setAttribute("async", "false");
      document.head.appendChild(skinExtensions.firstChild);
    }
  }
});
defineExpose({
  wizardStepCallback,
});
</script>

<template>
  <div
    id="skinExtensions"
    v-if="step.uiComponent.requiredSkinExtensions"
    v-html="step.uiComponent.requiredSkinExtensions"
  ></div>
  <component
    v-if="component"
    :is="component"
    @validateStep="validateStep"
    @invalidateStep="invalidateStep"
    ref="stepRefId"
  />
  <div v-else-if="step.uiComponent.html" v-html="step.uiComponent.html"></div>
  <span v-else>The step is missing proper content.</span>
</template>

<style scoped></style>
