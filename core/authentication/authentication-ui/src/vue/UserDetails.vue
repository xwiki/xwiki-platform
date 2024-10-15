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
<script setup lang="ts">
import { inject } from "vue";
import { CristalApp } from "@xwiki/cristal-api";
import type { AuthenticationManagerProvider } from "@xwiki/cristal-authentication-api";
import { useI18n, I18nT } from "vue-i18n";
import { name as browserApiName, BrowserApi } from "@xwiki/cristal-browser-api";
import messages from "../translations";

const { t } = useI18n({
  messages,
});

const cristal = inject<CristalApp>("cristal")!;

const authenticationManager = cristal
  .getContainer()
  .get<AuthenticationManagerProvider>("AuthenticationManagerProvider")
  .get()!;

const browserApi = cristal.getContainer().get<BrowserApi>(browserApiName);

const { profile, name } = await authenticationManager.getUserDetails();

function logout() {
  authenticationManager.logout().then(() => browserApi.reload());
}
</script>

<template>
  <i18n-t keypath="userDescription" tag="span">
    <a :href="profile">{{ name }}</a>
  </i18n-t>
  <br />
  <br />
  <x-btn variant="danger" @click.prevent="logout">{{ t("logout") }}</x-btn>
</template>
