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
</script>

<template>
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
  <h2>XBreadcrumb</h2>
  <XBreadcrumb :items="breadcrumbItems"></XBreadcrumb>
  <h2>XCard</h2>
  <XCard />
  <h2>XCheckbox</h2>
  <XCheckbox />
  <h2>XDialog</h2>
  <XDialog />
  <h2>XDivider</h2>
  <XDivider />
  <h2>XFileInput</h2>
  <XFileInput />
  <h2>XForm</h2>
  <XForm />
  <h2>XImg</h2>
  <XImg />
  <h2>XLoad</h2>
  <XLoad />
  <h2>XMenu</h2>
  <XMenu />
  <h2>XMenuItem</h2>
  <XMenuItem />
  <h2>XMenuLabel</h2>
  <XMenuLabel />
  <h2>XSelect</h2>
  <XSelect />
  <h2>XTab</h2>
  <XTab />
  <h2>XTabGroup</h2>
  <XTabGroup />
  <h2>XTabPanel</h2>
  <XTabPanel />
  <h2>XTextField</h2>
  <XTextField />
  <h2>XTree</h2>
  <XTree />
</template>

<style scoped></style>
