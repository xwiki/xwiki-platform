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
import XAlert from "./XAlert.vue";

const actions = [
  {
    name: "A1",
    callback: () => {
      console.log("ok");
    },
  },
];
</script>

<template>
  <div>
    <!--    <x-alert type="info">This is an alert</x-alert>-->
    <!--    <x-alert type="info" flat-corners>This is an alert</x-alert>-->
    <!--    <x-alert type="warning" title="This is a title">This is an alert</x-alert>-->
    <!--    <x-alert type="warning" :actions><br />This is an alert</x-alert>-->
    <x-alert type="warning" closable>
      Lorem Ipsum is simply dummy text of the printing and typesetting industry.
      Lorem Ipsum has been the industry's standard dummy text ever since the
      1500s, when an unknown printer took a galley of type and scrambled it to
      make a type specimen book. It has survived not only five centuries, but
      also the leap into electronic typesetting, remaining essentially
      unchanged. It was popularised in the 1960s with the release of Letraset
      sheets containing Lorem Ipsum passages, and more recently with desktop
      publishing software like Aldus PageMaker including versions of Lorem
      Ipsum.
    </x-alert>
    <!--    <div class="box warningmessage">ABCD</div>-->
  </div>
</template>

<style scoped></style>
