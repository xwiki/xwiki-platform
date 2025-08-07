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
import { CTemplate } from "@xwiki/cristal-skin";
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

const newConfigurationForm = ref();

const reactiveConfigurations: Ref<ConfigurationsSettings> = ref(
  props.configurations,
);
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
</script>

<template>
  <!-- TODO: Implement https://jira.xwiki.org/browse/CRISTAL-539 -->
  <CTemplate name="config" @edit="preEditConfig" @delete="preDeleteConfig" />
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
.actions {
  display: flex;
  gap: var(--cr-spacing-x-small);
}
</style>
