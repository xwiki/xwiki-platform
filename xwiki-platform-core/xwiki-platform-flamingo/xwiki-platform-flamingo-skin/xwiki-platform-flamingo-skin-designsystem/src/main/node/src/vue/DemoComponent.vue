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
import XBtn from "./XBtn.vue";
import XTab from "./XTab.vue";
import XTabPanel from "./XTabPanel.vue";
import { ref } from "vue";

const breadcrumbItems = [
  { label: "Home" },
  { label: "Page" },
  { label: "mleduc", url: "https://mleduc.xyz" },
];

const actions = [
  {
    name: "My Action",
    callback: () => {
      console.log("callback");
    },
  },
];

const preserveChildren = ref(false);
const files = defineModel(undefined);

const selectItems = ["v1", "v2"];
</script>

<template>
  <div>
    <h2>XAlert</h2>
    <!--  <XAlert type="success">Do<strong>ne</strong></XAlert>-->
    <!--  <XAlert type="error" title="with a title">Do<strong>ne</strong></XAlert>-->
    <!--  <XAlert type="info" title="with a title" :closable="false"-->
    <!--    >Never closed</XAlert-->
    <!--  >-->
    <XAlert type="info" title="with a title" :actions="actions">
      With action
    </XAlert>

    <!--  <XAlert-->
    <!--    type="info"-->
    <!--    title="with a title"-->
    <!--    :closable="false"-->
    <!--    :flat-corners="true"-->
    <!--    >Never closed</XAlert-->
    <!--  >-->
    <h2>XAvatar</h2>
    <XAvatar
      name="hello world"
      image="https://mleduc.xyz/_astro/screen-dynamic-tests.5JgFQJUL_M2zxx.webp"
    />
    <br />
    <XAvatar name="hello world" size="200" />
    <h2>XBtn</h2>
    <XBtn @click="console.log('HELLO WORLD')">BUTTON</XBtn>
    <XBtn size="small">SMALL BUTTON</XBtn>
    <XBtn size="small" variant="primary">SMALL BUTTON</XBtn>
    <h2>XBreadcrumb</h2>
    <XBreadcrumb :items="breadcrumbItems"></XBreadcrumb>
    <h2>XCard</h2>
    <XCard title="card title"><strong>Card</strong> content</XCard>
    Preserve children {{ preserveChildren }}
    <h2>XDialog</h2>
    <XDialog v-model="preserveChildren" title="Modal TITLE"
      >MODAL CONTENT
    </XDialog>
    <!--  <XDialog title="Modal TITLE">-->
    <!--    MODAL CONTENT-->
    <!--    <template #footer>THIS IS THE FOOTER!</template>-->
    <!--  </XDialog>-->
    <h2>XDivider</h2>
    <XDivider />
    <h2>XFileInput</h2>
    <XFileInput v-model="files" label="files" /> {{ files }}
    <XFileInput v-model="files" label="files" multiple /> {{ files }}
    <h2>XForm</h2>
    <XForm :on-form-submit="() => console.log('ok')">
      <XCheckbox
        v-model="preserveChildren"
        label="LABEL"
        help="HELP"
        name="testname"
      ></XCheckbox>

      <XTextField
        model-value="42"
        required
        label="hello world"
        help="optional help"
      />

      <input type="submit" />
    </XForm>
    <h2>XImg</h2>
    <XImg
      src="https://mleduc.xyz/_astro/screen-dynamic-tests.5JgFQJUL_M2zxx.webp"
    />
    <h2>XLoad</h2>
    <XLoad />
    <h2>XSelect</h2>
    <XSelect
      label="select me"
      help="select help"
      required
      :items="selectItems"
    />
    <h2>XTab</h2>
    <x-tab-group>
      <template #tabs>
        <x-tab tab-id="t1"> T1 </x-tab>
        <x-tab tab-id="t2"> T2 </x-tab>
      </template>
      <template #panels>
        <x-tab-panel tab-id="t1">T1</x-tab-panel>
        <x-tab-panel tab-id="t2">T2</x-tab-panel>
      </template>
    </x-tab-group>

    <h2>XTree</h2>
    <XTree />
  </div>
</template>

<style scoped></style>
