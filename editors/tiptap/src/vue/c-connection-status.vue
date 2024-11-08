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
import { User } from "../extensions/collaboration";
import {
  HocuspocusProvider,
  WebSocketStatus,
  // eslint-disable-next-line import/named
  onAwarenessChangeParameters,
  // eslint-disable-next-line import/named
  onStatusParameters,
} from "@hocuspocus/provider";
import { CIcon, Size } from "@xwiki/cristal-icons";
import { Avvvatars } from "avvvatars-vue";
import { ref } from "vue";
import type { Ref } from "vue";

const props = withDefaults(
  defineProps<{
    provider: HocuspocusProvider;
  }>(),
  {
    provider: undefined,
  },
);
const provider = props.provider;

const status: Ref<WebSocketStatus> = ref(WebSocketStatus.Disconnected);
provider.on("status", (event: onStatusParameters) => {
  status.value = event.status;
});

const users: Ref<User[]> = ref([]);
provider.on("awarenessChange", (event: onAwarenessChangeParameters) => {
  users.value = Array.from(event.states.values()).map((state) => state.user);
});
</script>

<template>
  <span class="connection-status">
    <span
      v-if="status === WebSocketStatus.Disconnected"
      class="connection-status-offline"
    >
      <c-icon
        name="wifi-off"
        :size="Size.Small"
        class="connection-status-icon"
      ></c-icon>
      <span class="connection-status-label">Offline</span>
    </span>
    <span
      v-if="status === WebSocketStatus.Connecting"
      class="connection-status-connecting"
    >
      <c-icon
        name="arrow-clockwise"
        :size="Size.Small"
        class="connection-status-icon"
      ></c-icon>
      <span class="connection-status-label">Connecting</span>
    </span>
    <span
      v-if="status === WebSocketStatus.Connected"
      class="connection-status-users"
    >
      <Avvvatars
        v-for="user in users"
        :key="user.name"
        :value="user.name"
        variant="shape"
        :border="true"
        :size="28"
        :title="user.name"
      />
    </span>
  </span>
</template>

<style scoped>
.connection-status-icon + .connection-status-label {
  margin-left: 0.2em;
}

.connection-status-label {
  font-size: smaller;
}

.connection-status-connecting .connection-status-icon:before {
  animation: 1.2s linear infinite cr-spin;
}
@keyframes cr-spin {
  0% {
    transform: rotate(0deg);
  }
  25% {
    transform: rotate(90deg);
  }
  50% {
    transform: rotate(180deg);
  }
  75% {
    transform: rotate(270deg);
  }
  100% {
    transform: rotate(360deg);
  }
}

.connection-status-users {
  display: flex;
}
.connection-status-users > div + div {
  margin-left: -6px;
}
</style>
