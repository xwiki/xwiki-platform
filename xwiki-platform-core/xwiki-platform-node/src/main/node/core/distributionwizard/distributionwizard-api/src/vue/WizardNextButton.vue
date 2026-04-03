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
import { StepState } from "../WizardStepProps";
import { computed } from "vue";

const props = defineProps<{
  stepState: StepState;
  hasNextStep: boolean;
}>();
defineEmits(["nextStep", "finishWizard"]);

const displayNext = computed(() => {
  const isStateOK = props.stepState !== StepState.PROCESSING;
  return props.hasNextStep && isStateOK;
});
const isDisabled = computed(() => {
  return props.stepState === StepState.DISPLAYED;
});
</script>

<template>
  <button
    class="button primary"
    v-if="displayNext"
    @click="$emit('nextStep')"
    :disabled="isDisabled"
  >
    Next
  </button>
  <div v-else-if="props.stepState === StepState.PROCESSING">PROCESSING</div>
</template>

<style scoped>
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
.primary[disabled] {
  cursor: not-allowed;
  background: #3870af;
  color: #e38d8d;
}
</style>
