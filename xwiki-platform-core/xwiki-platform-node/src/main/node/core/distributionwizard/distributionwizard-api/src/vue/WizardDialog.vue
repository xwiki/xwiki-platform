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
import WizardBreadcrumb from "./WizardBreadcrumb.vue";
import WizardStep from "./WizardStep.vue";
import {
  computed,
  onMounted,
  onUpdated,
  ref,
  useTemplateRef,
  watch,
} from "vue";
import type { StepsResolverFunction } from "../StepsResolver";
import type { WizardStepProps } from "../WizardStepProps";

const emit = defineEmits(["closed"]);
const props = defineProps<{
  stepResolver: StepsResolverFunction;
  wizardTitle: string;
}>();

const steps = ref<WizardStepProps[]>([]);
let activeStep = ref<WizardStepProps | undefined>();
const stepComponent = ref();

const htmlDialog = useTemplateRef("wizardDialog");

const stepIndex = computed(() => {
  return activeStep.value ? activeStep.value.index : 0;
});
const hasNextStep = computed(() => stepIndex.value < steps.value.length - 1);
const hasPreviousStep = computed(() => stepIndex.value > 0);
const stepNames = computed(() => {
  let names = [];
  for (let step of steps.value) {
    names.push(step.title);
  }
  return names;
});
const isFinished = true;

onMounted(async () => {
  steps.value = await props.stepResolver();
  if (steps.value.length > 1) {
    activeStep.value = steps.value[0];
  }
});
onUpdated(() => {
  if (activeStep.value) {
    htmlDialog.value?.showModal();
  }
});
watch(activeStep, async (newStep, oldStep) => {
  if (newStep && newStep.uiComponent) {
    console.log("Loading", newStep.uiComponent);
    stepComponent.value = (await import(newStep.uiComponent.module))[
      newStep.uiComponent.component
    ];
  } else {
    stepComponent.value = null;
  }
});

function nextStep() {
  if (hasNextStep.value) {
    activeStep.value = steps.value[stepIndex.value + 1];
  }
}

function previousStep() {
  if (hasPreviousStep.value) {
    activeStep.value = steps.value[stepIndex.value - 1];
  }
}
function finishWizard() {
  htmlDialog.value?.requestClose();
  emit("closed");
}
</script>
<script></script>

<template>
  <!-- Should use closedby="none" -->
  <dialog
    class="wizard-dialog"
    ref="wizardDialog"
    aria-labelledby="wizard-title"
    v-if="activeStep"
  >
    <div class="wizard-header">
      <h2 class="wizard-title">{{ wizardTitle }} - {{ activeStep.title }}</h2>
      <WizardBreadcrumb :steps="stepNames" :activeStep="activeStep.index" />
    </div>
    <main class="wizard-content">
      <WizardStep :step="activeStep" :component="stepComponent"></WizardStep>
    </main>
    <div class="wizard-footer">
      <div class="footer-left">
        <button class="button" v-if="hasPreviousStep" @click="previousStep">
          Previous
        </button>
      </div>
      <div class="footer-right">
        <button class="button primary" v-if="hasNextStep" @click="nextStep">
          Next
        </button>
        <button
          class="button primary"
          v-if="isFinished && !hasNextStep"
          @click="finishWizard"
        >
          Finish
        </button>
      </div>
    </div>
  </dialog>
</template>

<style scoped>
.wizard-dialog {
  width: min(720px, 90vw);
  border: none;
  border-radius: 8px;
  padding: 0;
  overflow: hidden;
  box-shadow: 0 3px 9px rgba(0, 0, 0, 0.5);
}
.wizard-header {
  padding: 24px;
  border-bottom: 1px solid #e5e7eb;
  background: #fafafa;
}
.wizard-header h2 {
  margin: 0 0 16px;
}
.wizard-content {
  padding: 28px;
  min-height: 200px;
}
.wizard-footer {
  padding: 16px 24px;
  border-top: 1px solid #e5e7eb;
  display: flex;
  gap: 8px;
  justify-content: flex-end;
  background: #fafafa;
}
.footer-left,
.footer-right {
  flex-basis: 100%;
}
.footer-right {
  display: flex;
  justify-content: flex-end;
}
.button {
  padding: 10px 18px;
  border-radius: 6px;
  border: 1px solid #ddd;
  background: white;
  cursor: pointer;
  color: black;
  font-weight: 500;
}
.primary {
  color: white;
  background: #3f79bd;
  border: 1px solid #3f79bd;
}
.primary:hover {
  background: #326095;
  border: 1px solid #326095;
}
</style>
