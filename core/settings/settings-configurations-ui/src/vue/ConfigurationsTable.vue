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
import ConfigurationEdit from "./ConfigurationEdit.vue";
import messages from "../translations";
import { CIcon } from "@xwiki/cristal-icons";
import { ConfigurationsSettings } from "@xwiki/cristal-settings-configurations";
import { inject, ref } from "vue";
import { useI18n } from "vue-i18n";
import type { AlertsService } from "@xwiki/cristal-alerts-api";
import type { CristalApp, WikiConfig } from "@xwiki/cristal-api";
import type {
  SettingsManager,
  SettingsStorage,
} from "@xwiki/cristal-settings-api";
import type { WikiConfigProxy } from "@xwiki/cristal-wiki-config-api";
import type { Ref } from "vue";

const props = defineProps<{
  configurations: ConfigurationsSettings;
}>();

const cristal = inject<CristalApp>("cristal")!;
const alertsService = cristal
  .getContainer()
  .get<AlertsService>("AlertsService")!;
const wikiConfigProxy = cristal
  .getContainer()
  .get<WikiConfigProxy>("WikiConfigProxy")!;
const settingsManager = cristal
  .getContainer()
  .get<SettingsManager>("SettingsManager")!;
const settingsStorage = cristal
  .getContainer()
  .get<SettingsStorage>("SettingsStorage")!;
const configTypes = cristal
  .getContainer()
  .getAll<WikiConfig>("WikiConfig")
  .map((c) => c.getType());

const configs: Ref<Map<string, WikiConfig>> =
  wikiConfigProxy.getAvailableConfigurations();
const newConfigurationForm = ref();
const currentConfig = configs.value.get(cristal.getWikiConfig().name);

const reactiveConfigurations: Ref<ConfigurationsSettings> = ref(
  props.configurations,
);
const newDialogOpen: Ref<boolean> = ref(false);
const newName: Ref<string> = ref("");
const newType: Ref<string> = ref("");
const editDialogOpen: Ref<boolean> = ref(false);
const editName: Ref<string> = ref("");
const deleteDialogOpen: Ref<boolean> = ref(false);
const deleteName: Ref<string> = ref("");

const { t } = useI18n({
  messages,
});

async function submit() {
  if (cristal.getAvailableConfigurations().has(newName.value)) {
    alertsService.error(
      t("settings.configurations.table.new.error", {
        configName: newName.value,
      }),
    );
  } else {
    const newConfig = { configType: newType.value, name: newName.value };
    reactiveConfigurations.value.content.set(newName.value, newConfig);
    settingsManager.set(
      new ConfigurationsSettings(reactiveConfigurations.value.content),
    );
    await settingsStorage.save(settingsManager);
    wikiConfigProxy.setAvailableConfigurations({ [newName.value]: newConfig });
    preEditConfig(newName.value);
    await newConfigurationForm.value?.reset();
    newDialogOpen.value = false;
  }
}

function preEditConfig(name: string) {
  editName.value = name;
  editDialogOpen.value = true;
}

function preDeleteConfig(name: string) {
  deleteName.value = name;
  deleteDialogOpen.value = true;
}

async function deleteConfig() {
  reactiveConfigurations.value.content.delete(deleteName.value);
  settingsManager.set(
    new ConfigurationsSettings(reactiveConfigurations.value.content),
  );
  await settingsStorage.save(settingsManager);
  wikiConfigProxy.deleteAvailableConfiguration(deleteName.value);
  deleteDialogOpen.value = false;
}

function isEditable(configName: string): boolean {
  return (
    (configName != currentConfig?.name &&
      settingsManager.get(ConfigurationsSettings)?.content?.has(configName)) ??
    false
  );
}
</script>

<template>
  <div v-if="currentConfig !== undefined" class="current-config">
    <h2 class="nomargin">{{ t("settings.configurations.current.title") }}</h2>
    <h3 class="nomargin">{{ currentConfig.name }}</h3>
    {{ currentConfig.baseURL }}
    <h4 class="nomargin">{{ t("settings.configurations.table.header.ds") }}</h4>
    {{ currentConfig.designSystem }}
  </div>

  <h2>{{ t("settings.configurations.available.title") }}</h2>
  <x-dialog
    v-model="newDialogOpen"
    width="auto"
    :title="t('settings.configurations.new')"
  >
    <template #activator>
      <x-btn id="new-configuration-button" variant="primary">
        <c-icon name="plus"></c-icon>
        {{ t("settings.configurations.new") }}</x-btn
      >
    </template>
    <template #default>
      <x-form ref="newConfigurationForm" @form-submit="submit">
        <x-text-field
          v-model="newName"
          :label="t('settings.configurations.table.new.name.label')"
          :help="t('settings.configurations.table.new.name.help')"
          required
        ></x-text-field>
        <x-select
          v-model="newType"
          :label="t('settings.configurations.table.new.type.label')"
          :help="t('settings.configurations.table.new.type.help')"
          :items="configTypes"
          required
        ></x-select>
        <x-btn type="submit" variant="primary">
          <c-icon name="plus"></c-icon>
          {{ t("settings.configurations.table.new.submit") }}</x-btn
        >
      </x-form>
    </template>
  </x-dialog>

  <table class="mobile-transform">
    <thead>
      <tr>
        <th>{{ t("settings.configurations.table.header.name") }}</th>
        <th>{{ t("settings.configurations.table.header.type") }}</th>
        <th>{{ t("settings.configurations.table.header.ds") }}</th>
        <th></th>
        <th></th>
        <th></th>
      </tr>
    </thead>
    <tbody>
      <tr
        v-for="[key, wikiConfig] in configs"
        :key="key"
        :class="
          isEditable(wikiConfig.name) && wikiConfig.name != currentConfig?.name
            ? 'editable-configuration'
            : 'non-editable-configuration'
        "
        @click="
          isEditable(wikiConfig.name) && wikiConfig.name != currentConfig?.name
            ? preEditConfig(wikiConfig.name)
            : undefined
        "
      >
        <td class="text-cell">{{ wikiConfig.name }}</td>
        <td class="text-cell">{{ wikiConfig.getType() }}</td>
        <td class="text-cell">{{ wikiConfig.designSystem }}</td>
        <td class="fixed-size-cell">
          <div v-if="wikiConfig.name == currentConfig?.name">
            {{ t("settings.configurations.in-use") }}
          </div>
          <x-btn v-else @click="cristal?.switchConfig(wikiConfig.name)">
            {{ t("settings.configurations.select") }}
          </x-btn>
        </td>
        <td class="fixed-size-cell">
          <c-icon
            name="pencil"
            class="action-icon"
            v-if="isEditable(wikiConfig.name)"
            @click.stop="preEditConfig(wikiConfig.name)"
          ></c-icon>
        </td>
        <td class="fixed-size-cell">
          <c-icon
            name="trash"
            class="action-icon"
            v-if="isEditable(wikiConfig.name)"
            @click.stop="preDeleteConfig(wikiConfig.name)"
          ></c-icon>
        </td>
      </tr>
    </tbody>
  </table>
  <ConfigurationEdit
    v-model="editDialogOpen"
    :configuration-name="editName"
    :configurations="reactiveConfigurations.content"
  >
  </ConfigurationEdit>
  <x-dialog
    v-model="deleteDialogOpen"
    width="auto"
    :title="t('settings.configurations.table.delete.title')"
  >
    <template #default>
      <p>
        {{
          t("settings.configurations.table.delete.confirm", {
            configName: deleteName,
          })
        }}
      </p>
    </template>
    <template #footer>
      <x-btn @click.stop="deleteDialogOpen = false">
        {{ t("settings.configurations.table.delete.cancel") }}
      </x-btn>
      <x-btn variant="primary" @click.stop="deleteConfig">
        {{ t("settings.configurations.table.delete.title") }}
      </x-btn>
    </template>
  </x-dialog>
</template>

<style scoped>
table {
  width: 100%;
  margin-top: var(--cr-spacing-medium);
}

.fixed-size-cell {
  width: 0;
}

.current-config {
  background-color: var(--cr-color-neutral-50);
  border-radius: var(--cr-border-radius-large);
  padding: var(--cr-spacing-small);
}

.nomargin {
  margin: 0;
}

.non-editable-configuration .text-cell {
  color: var(--cr-mute-text-color);
}

.editable-configuration:hover,
.action-icon:hover {
  background-color: var(--cr-color-neutral-200);
  cursor: pointer;
}
</style>
