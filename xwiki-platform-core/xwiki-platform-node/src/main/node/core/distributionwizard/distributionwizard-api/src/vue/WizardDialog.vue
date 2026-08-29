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
import { DialogState, StepState } from "../WizardStepProps";
import {
  computed,
  onMounted,
  onUpdated,
  ref,
  shallowRef,
  useTemplateRef,
} from "vue";
import type { DistributionWizardResolverFunctions } from "../StepsResolver";
import type {
  WizardDialogProps,
  WizardStepProps,
  WizardStepSummary,
} from "../WizardStepProps";

const props = defineProps<{
  stepResolverFunctions: DistributionWizardResolverFunctions;
}>();

const wizardProps = ref<WizardDialogProps>({
  wizardTitle: "Distribution Wizard",
  state: DialogState.INITIALIZING,
  steps: [],
  stepIndex: -1,
});
const activeStep = ref<WizardStepProps | undefined>();
const stepComponent = shallowRef();
const stepDialogRef = useTemplateRef<typeof WizardStep>("stepDialogRef");

const htmlDialog = useTemplateRef("wizardDialog");
const hasNextStep = computed(
  () => wizardProps.value.stepIndex < wizardProps.value.steps.length - 1,
);
const getInitFunctionName = (stepId: string) => stepId.toLowerCase() + "Init";
const getCallbackFunctionName = (stepId: string) =>
  stepId.toLowerCase() + "Callback";

onMounted(async () => {
  wizardProps.value = await props.stepResolverFunctions.stepsResolverFunction();
  wizardProps.value.state = DialogState.STEPS_LOADED;
  if (wizardProps.value.steps.length > 1) {
    await loadStep(wizardProps.value.steps[0]);
    wizardProps.value.stepIndex = 0;
  }
});
async function loadStep(step: WizardStepSummary) {
  // eslint-disable-next-line @typescript-eslint/no-explicit-any
  async function initStep(stepId: string, asyncModule: any) {
    let init = asyncModule[getInitFunctionName(stepId)];
    if (typeof init === "function") {
      await init();
    }
  }
  async function loadModule(loadedStep: WizardStepProps) {
    if (loadedStep.uiComponent.module) {
      return import(loadedStep.uiComponent.module);
    } else {
      return null;
    }
  }
  async function handledLoadedStep(loadedStep: WizardStepProps) {
    // eslint-disable-next-line @typescript-eslint/no-explicit-any
    let asyncModule: any = await loadModule(loadedStep);
    if (asyncModule) {
      if (loadedStep.uiComponent.component) {
        stepComponent.value = asyncModule[loadedStep.uiComponent.component];
      } else {
        stepComponent.value = null;
        await initStep(step.id, asyncModule);
      }
    }
    const stepState = loadedStep.skippable
      ? StepState.VALIDATED
      : StepState.DISPLAYED;
    activeStep.value = { ...loadedStep, state: stepState };
  }
  wizardProps.value.state = DialogState.LOADING_STEP;
  // This needs to be done before loading the step info, as the html of the step might depend on it.
  if (step.startsOnDisplay) {
    await props.stepResolverFunctions.startStepFunction(step.id);
  }
  const loadedStep = await props.stepResolverFunctions.stepResolverFunction(
    step.id,
  );
  await handledLoadedStep(loadedStep);
  wizardProps.value.state = DialogState.STEP_DISPLAYED;
}
/**
 * We can only show the modal once the DOM has been initialized, so after the app has been mounted, that's why we call
 * showModal in this onUpdated.
 */
onUpdated(() => {
  if (activeStep.value) {
    htmlDialog.value?.showModal();
  }
});
async function nextStep() {
  async function getCallback(step: WizardStepProps) {
    if (step.uiComponent.html && step.uiComponent.module) {
      const asyncModule = await import(step.uiComponent.module);
      return asyncModule[getCallbackFunctionName(step.id)];
    } else {
      return stepDialogRef.value?.wizardStepCallback;
    }
  }
  async function handleCallback(
    step: WizardStepProps,
    callback: () => Promise<boolean>,
  ): Promise<boolean> {
    let loadNextStep = false;
    step.state = StepState.PROCESSING;
    const processed = await callback();
    if (processed) {
      step.state = StepState.PROCESSED;
      loadNextStep = true;
    } else {
      step.state = StepState.PROCESS_ERROR;
    }
    return loadNextStep;
  }
  async function processStep(step: WizardStepProps) {
    let loadNextStep = true;

    if (step.needsInput) {
      const callback = await getCallback(step);
      if (callback) {
        loadNextStep = await handleCallback(step, callback);
      }
    }
    return loadNextStep;
  }
  if (hasNextStep.value && activeStep.value) {
    const loadNextStep = await processStep(activeStep.value);
    if (loadNextStep) {
      wizardProps.value.stepIndex++;
      await loadStep(wizardProps.value.steps[wizardProps.value.stepIndex]);
    }
  }
}

function finishWizard() {
  htmlDialog.value?.requestClose();
  window.location.reload();
}
function validateStep() {
  if (activeStep.value) {
    activeStep.value.state = StepState.VALIDATED;
  }
}
function invalidateStep() {
  if (activeStep.value) {
    activeStep.value.state = StepState.DISPLAYED;
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
      <h2>{{ wizardProps.wizardTitle }} - {{ activeStep.title }}</h2>
      <WizardBreadcrumb
        :steps="wizardProps.steps"
        :activeStep="wizardProps.stepIndex"
      />
    </div>
    <main :class="$style.wizardContent">
      <!-- FIXME: handle dialog step loading knowing that it could break the watcher in WizardStep -->
      <WizardStep
        :step="activeStep"
        :component="stepComponent"
        ref="stepDialogRef"
        @validateStep="validateStep"
        @invalidateStep="invalidateStep"
      />
    </main>
    <div :class="$style.wizardFooter">
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
  height: 50vh;
  overflow-y: auto;
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
