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
import WizardNextButton from "./WizardNextButton.vue";
import WizardStep from "./WizardStep.vue";
import { StepState } from "../WizardStepProps";
import {
  computed,
  onMounted,
  onUpdated,
  ref,
  useTemplateRef,
  watch,
} from "vue";
import type { StepsResolverFunction } from "../StepsResolver";
import type { StepCallback, WizardStepProps } from "../WizardStepProps";

const emit = defineEmits(["closed"]);
const props = defineProps<{
  stepResolver: StepsResolverFunction;
  wizardTitle: string;
}>();

const steps = ref<WizardStepProps[]>([]);
const activeStep = ref<WizardStepProps | undefined>();
const stepComponent = ref();
const stepCallback = ref<StepCallback | null>();

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

onMounted(async () => {
  steps.value = await props.stepResolver();
  if (steps.value.length > 1) {
    activeStep.value = { ...steps.value[0], state: StepState.DISPLAYED };
  }
});
onUpdated(() => {
  if (activeStep.value) {
    htmlDialog.value?.showModal();
  }
});
watch(activeStep, async (newStep) => {
  if (newStep && newStep.uiComponent) {
    const asyncModule = await import(newStep.uiComponent.module);
    stepComponent.value = asyncModule[newStep.uiComponent.component];
    const callbackName = newStep.uiComponent.callback || "callback";
    stepCallback.value = asyncModule[callbackName];
  } else {
    stepComponent.value = null;
  }
});

async function nextStep() {
  async function processStep() {
    let loadNextStep = true;
    if (activeStep.value && stepCallback.value) {
      loadNextStep = false;
      activeStep.value.state = StepState.PROCESSING;
      const processed = await stepCallback.value();
      if (processed) {
        activeStep.value.state = StepState.PROCESSED;
        loadNextStep = true;
      }
    }
    return loadNextStep;
  }
  if (hasNextStep.value) {
    const loadNextStep = await processStep();
    if (loadNextStep) {
      activeStep.value = steps.value[stepIndex.value + 1];
      activeStep.value.state = StepState.DISPLAYED;
    }
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
function validate() {
  if (activeStep.value) {
    activeStep.value.state = StepState.VALIDATED;
  }
}
</script>

<template>
  <!-- Should use closedby="none" -->
  <dialog
    :class="$style.wizardDialog"
    ref="wizardDialog"
    aria-labelledby="wizard-title"
    v-if="activeStep"
  >
    <div :class="$style.wizardHeader">
      <h2>{{ wizardTitle }} - {{ activeStep.title }}</h2>
      <WizardBreadcrumb :steps="stepNames" :activeStep="activeStep.index" />
    </div>
    <main :class="$style.wizardContent">
      <WizardStep
        :step="activeStep"
        :component="stepComponent"
        @validated="validate"
      ></WizardStep>
    </main>
    <div :class="$style.wizardFooter">
      <div :class="$style.footerLeft">
        <button v-if="hasPreviousStep" @click="previousStep">Previous</button>
      </div>
      <div :class="$style.footerRight">
        <WizardNextButton
          :step-state="activeStep.state || StepState.DISPLAYED"
          :has-next-step="hasNextStep"
          @finish-wizard="finishWizard"
          @next-step="nextStep"
        />
      </div>
    </div>
  </dialog>
</template>

<style module>
.wizardDialog {
  width: min(720px, 90vw);
  border: none;
  border-radius: 8px;
  padding: 0;
  overflow: hidden;
  box-shadow: 0 3px 9px rgba(0, 0, 0, 0.5);
}
.wizardHeader {
  padding: 24px;
  border-bottom: 1px solid #e5e7eb;
  background: #fafafa;
}
.wizardHeader h2 {
  margin: 0 0 16px;
}
.wizardContent {
  padding: 28px;
  min-height: 200px;
}
.wizardFooter {
  padding: 16px 24px;
  border-top: 1px solid #e5e7eb;
  display: flex;
  gap: 8px;
  justify-content: flex-end;
  background: #fafafa;
}
.footerLeft,
.footerRight {
  flex-basis: 100%;
}
.footerRight {
  display: flex;
  justify-content: flex-end;
}
</style>
