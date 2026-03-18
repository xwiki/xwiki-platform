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
defineProps<{ steps: string[]; activeStep: number }>();
</script>

<template>
  <nav aria-label="Wizard steps">
    <ol class="wizard-breadcrumb">
      <li
        v-for="(stepName, index) in steps"
        class="step"
        :class="{ active: activeStep === index }"
        :key="index"
      >
        <span class="step-counter">{{ index + 1 }}</span>
        <span class="step-label">{{ stepName }}</span>
      </li>
    </ol>
  </nav>
</template>

<style scoped>
.wizard-breadcrumb {
  display: flex;
  gap: 24px;
  margin: 0;
  padding: 0;
  list-style: none;
}

.step {
  display: flex;
  align-items: center;
  gap: 8px;
  color: #6b7280;
}

.step.active {
  color: #111827;
  font-weight: 600;
}

.step-counter {
  display: inline-flex;
  justify-content: center;
  align-items: center;
  width: 28px;
  height: 28px;
  border-radius: 50%;
  background: #e5e7eb;
  font-size: 0.8rem;
}

.step.active .step-counter {
  background: #3f79bd;
  color: white;
}
</style>
