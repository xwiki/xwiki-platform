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
import { CristalApp } from "@xwiki/cristal-api";
import { NextcloudAuthenticationState } from "@xwiki/cristal-authentication-nextcloud-state";
import { inject, ref, watch } from "vue";
import { useI18n } from "vue-i18n";
import type { Ref } from "vue";

const { t } = useI18n({
  messages,
});

const cristal = inject<CristalApp>("cristal")!;

const authenticationState = cristal
  .getContainer()
  .get<NextcloudAuthenticationState>(NextcloudAuthenticationState);
const modalOpened: Ref<boolean> = authenticationState.modalOpened;
const username: Ref<string> = authenticationState.username;
const password: Ref<string> = authenticationState.password;
const errorStatus: Ref<number | undefined> = ref(undefined);

async function callback() {
  const response = await authenticationState.callback();
  if (!response.success) {
    errorStatus.value = response.status;
  }
}

watch(
  authenticationState.modalOpened,
  async (newValue: boolean, oldValue: boolean) => {
    if (newValue && !oldValue) {
      authenticationState.username.value = "";
      authenticationState.password.value = "";
      errorStatus.value = undefined;
    }
  },
);
</script>

<template>
  <x-dialog
    v-model="modalOpened"
    width="auto"
    :title="t('authentication.nextcloud.modal.title')"
  >
    <template #default>
      <!-- We need 2 different divs to implement the following behavior:
             - Use max available width from the parent
             - Do not resize the parent if the content is larger -->
      <div class="alerts-wrapper">
        <div class="alerts">
          <x-alert v-if="errorStatus === 401" type="error">
            {{ t("authentication.nextcloud.modal.error.invalid") }}
          </x-alert>
          <x-alert v-else-if="errorStatus !== undefined" type="error">
            {{
              t("authentication.nextcloud.modal.error.generic", {
                status: errorStatus,
              })
            }}
          </x-alert>
        </div>
      </div>
      <x-form id="nextcloud-login-form" @submit="callback">
        <x-text-field
          v-model="username"
          :label="t('authentication.nextcloud.modal.username.label')"
          :help="t('authentication.nextcloud.modal.username.help')"
          autofocus
          required
        ></x-text-field>
        <x-text-field
          v-model="password"
          type="password"
          :label="t('authentication.nextcloud.modal.password.label')"
          :help="t('authentication.nextcloud.modal.password.help')"
        ></x-text-field>
      </x-form>
    </template>
    <template #footer>
      <x-btn variant="primary" type="submit" form="nextcloud-login-form">{{
        t("authentication.nextcloud.modal.submit.label")
      }}</x-btn>
    </template>
  </x-dialog>
</template>

<style scoped>
.alerts-wrapper {
  display: flex;
}
.alerts {
  flex-grow: 1;
  width: 0;
}
</style>
