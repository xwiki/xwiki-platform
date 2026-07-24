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
import { onMounted, ref } from "vue";
import type {
  Resolver,
  TranslationsWithMissed,
} from "@xwiki/platform-localization-api";
const translationModule = "xwiki-platform-localization-webjar";
const translations = ref<TranslationsWithMissed>();
onMounted(async () => {
  const resolver: Resolver = (await import(translationModule)).resolver;
  translations.value = await resolver.resolve({
    prefix: "platform.extension.distributionWizard.",
    keys: ["welcomeStepDescription", "welcomeStepStepsHint"],
  });
  console.log("Obtained translations", translations.value);
});
</script>

<template>
  <div class="welcome-message">
    {{
      translations?.translations[
        "platform.extension.distributionWizard.welcomeStepDescription"
      ]
    }}
  </div>
</template>

<style scoped></style>
