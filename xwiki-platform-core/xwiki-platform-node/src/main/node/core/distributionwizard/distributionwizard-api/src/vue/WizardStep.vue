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
// @ts-expect-error this is a JavaScript file, it is expected to not have types.
import { loadById } from "../services/require.js";
import { computed, nextTick, useTemplateRef, watch } from "vue";
import type { WizardStepProps } from "../WizardStepProps";
const emit = defineEmits(["validateStep", "invalidateStep"]);
// eslint-disable-next-line @typescript-eslint/no-explicit-any
const stepRef = useTemplateRef<any>("stepRefId");
const props = defineProps<{
  step: WizardStepProps;
  component: unknown;
}>();
function validateStep() {
  emit("validateStep");
}
function invalidateStep() {
  emit("invalidateStep");
}
const wizardStepCallback = computed(() => {
  if (stepRef.value) {
    return stepRef.value.stepAnswerCallback;
  } else {
    return null;
  }
});

watch(props, async () => {
  if (props.step.uiComponent.html) {
    await nextTick();
    const jQuery = await loadById("jquery");
    jQuery(document).trigger("xwiki:dom:updated", {
      elements: jQuery("#wizard-step-html").toArray(),
    });
    jQuery(document).on("xwiki:distributionWizard:validateStep", validateStep);
    jQuery(document).on(
      "xwiki:distributionWizard:invalidateStep",
      invalidateStep,
    );
  }
});
defineExpose({
  wizardStepCallback,
});
</script>

<template>
  <component
    v-if="component"
    :is="component"
    @validateStep="validateStep"
    @invalidateStep="invalidateStep"
    ref="stepRefId"
  />
  <div
    v-else-if="step.uiComponent.html"
    v-html="step.uiComponent.html"
    id="wizard-step-html"
  ></div>
  <span v-else>The step is missing proper content.</span>
</template>

<style scoped></style>
