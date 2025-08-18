<!--
  See the LICENSE file distributed with this work for additional
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

<script lang="ts" setup>
import messages from "../translations";
import { CIcon, Size } from "@xwiki/cristal-icons";
import { ConfigurationsSettings } from "@xwiki/cristal-settings-configurations";
import { inject, ref, watch } from "vue";
import { useI18n } from "vue-i18n";
import type { CristalApp } from "@xwiki/cristal-api";
import type { Configuration } from "@xwiki/cristal-configuration-api";
import type {
  SettingsManager,
  SettingsStorage,
} from "@xwiki/cristal-settings-api";
import type { WikiConfigProxy } from "@xwiki/cristal-wiki-config-api";
import type { Ref } from "vue";

const props = defineProps<{
  configurations: Map<string, Configuration>;
  configurationName: string;
}>();

const open = defineModel<boolean>();

const cristal = inject<CristalApp>("cristal")!;
const wikiConfigProxy = cristal
  .getContainer()
  .get<WikiConfigProxy>("WikiConfigProxy")!;
const settingsManager = cristal
  .getContainer()
  .get<SettingsManager>("SettingsManager")!;
const settingsStorage = cristal
  .getContainer()
  .get<SettingsStorage>("SettingsStorage")!;

// TODO: find a way to list available design systems automatically.
// https://jira.xwiki.org/browse/CRISTAL-541
const designSystems = ["shoelace", "vuetify"];

// TODO: find a way to list available editors automatically.
// https://jira.xwiki.org/browse/CRISTAL-541
const editors = ["tiptap", "blocknote"];

const configuration: Ref<Configuration | undefined> = ref(undefined);

const baseUrl: Ref<string> = ref("");
const baseRestUrl: Ref<string> = ref("");
const homePage: Ref<string> = ref("");
const designSystem: Ref<string> = ref("");
const storageRoot: Ref<string> = ref("");
const realtimeUrl: Ref<string> = ref("");
const authenticationBaseUrl: Ref<string> = ref("");
const editor: Ref<string> = ref("");

const { t } = useI18n({
  messages,
});

watch(
  () => props.configurationName,
  () => {
    if (
      props.configurationName &&
      props.configurations.has(props.configurationName)
    ) {
      configuration.value = props.configurations.get(props.configurationName)!;
      baseUrl.value = (configuration.value.baseURL ?? "") as string;
      baseRestUrl.value = (configuration.value.baseRestURL ?? "") as string;
      homePage.value = (configuration.value.homePage ?? "") as string;
      designSystem.value = (configuration.value.designSystem ?? "") as string;
      storageRoot.value = (configuration.value.storageRoot ?? "") as string;
      realtimeUrl.value = (configuration.value.realtimeURL ?? "") as string;
      authenticationBaseUrl.value = (configuration.value
        .authenticationBaseURL ?? "") as string;
      editor.value = (configuration.value.editor ?? "") as string;
    }
  },
);

async function submit() {
  if (configuration.value) {
    // We want to only reassign the values managed by the current form.
    Object.assign(configuration.value, {
      baseURL: baseUrl.value,
      baseRestURL: baseRestUrl.value,
      homePage: homePage.value,
      designSystem: designSystem.value,
      storageRoot: storageRoot.value,
      realtimeURL: realtimeUrl.value,
      authenticationBaseURL: authenticationBaseUrl.value,
      editor: editor.value,
    });

    // Clean up all empty and null properties.
    Object.keys(configuration.value).forEach(
      (k) =>
        (configuration.value![k] =
          configuration.value![k] || configuration.value![k] === false
            ? configuration.value![k]
            : undefined),
    );

    settingsManager
      .get(ConfigurationsSettings)!
      .content.set(props.configurationName, configuration.value);
    await settingsStorage.save(settingsManager);
    await settingsStorage.load(settingsManager);
    wikiConfigProxy.setAvailableConfigurations({
      [props.configurationName]: configuration.value,
    });
    open.value = false;
  }
}
</script>

<template>
  <x-dialog
    v-model="open"
    width="auto"
    :title="
      t('settings.configurations.edit.title', {
        config: `${configurationName} (${configuration?.configType})`,
      })
    "
  >
    <template #default>
      <x-form @form-submit="submit">
        <x-text-field
          v-model="baseUrl"
          :label="t('settings.configurations.edit.baseurl.label')"
          :help="t('settings.configurations.edit.baseurl.help')"
        ></x-text-field>
        <x-text-field
          v-model="baseRestUrl"
          :label="t('settings.configurations.edit.baseresturl.label')"
          :help="t('settings.configurations.edit.baseresturl.help')"
        ></x-text-field>
        <x-text-field
          v-model="homePage"
          :label="t('settings.configurations.edit.homepage.label')"
          :help="t('settings.configurations.edit.homepage.help')"
        ></x-text-field>
        <x-select
          v-model="designSystem"
          :label="t('settings.configurations.edit.designsystem.label')"
          :help="t('settings.configurations.edit.designsystem.help')"
          :items="designSystems"
          required
        ></x-select>
        <x-text-field
          v-model="storageRoot"
          :label="t('settings.configurations.edit.storageroot.label')"
          :help="t('settings.configurations.edit.storageroot.help')"
        ></x-text-field>
        <x-text-field
          v-model="realtimeUrl"
          :label="t('settings.configurations.edit.realtimeurl.label')"
          :help="t('settings.configurations.edit.realtimeurl.help')"
        ></x-text-field>
        <x-text-field
          v-model="authenticationBaseUrl"
          :label="t('settings.configurations.edit.authenticationbaseurl.label')"
          :help="t('settings.configurations.edit.authenticationbaseurl.help')"
        ></x-text-field>
        <x-select
          v-model="editor"
          :label="t('settings.configurations.edit.editor.label')"
          :help="t('settings.configurations.edit.editor.help')"
          :items="editors"
        ></x-select>
        <x-btn type="submit" variant="primary">
          <c-icon name="floppy" :size="Size.Small"></c-icon>
          {{ t("settings.configurations.edit.submit") }}</x-btn
        >
      </x-form>
    </template>
  </x-dialog>
</template>
