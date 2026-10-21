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
import { useEnvironment } from "./context";
import { computed } from "vue";
import type { ToolbarUser } from "./types";

const props = withDefaults(
  defineProps<{
    user: Partial<ToolbarUser> & Pick<ToolbarUser, "reference">;
    /**
     * Whether to hide the user name (the name is still available as avatar hint).
     */
    compact?: boolean;
    /**
     * Whether the user element is a link to the user profile page.
     */
    link?: boolean;
    /**
     * Called when the user is clicked, instead of following the link to the user profile page. Declared as a prop
     * (rather than an emitted event) so that we know whether there is a listener: without one, clicking follows the
     * link.
     */
    onSelect?: (user: Partial<ToolbarUser>) => void;
  }>(),
  { compact: false, link: true, onSelect: undefined },
);

/**
 * @param name - a user name
 * @returns the abbreviation displayed over the user avatar
 */
function getUserNameAbbreviation(name: string): string {
  const names = name.trim().split(/\s+/);
  if (names.length === 1) {
    return (
      names[0].substring(0, 1).toUpperCase() +
      names[0].substring(1, 2).toLowerCase()
    );
  } else {
    // We assume that the first and the last names are the most important.
    return names[0][0].toUpperCase() + names.at(-1)![0].toUpperCase();
  }
}

const environment = useEnvironment();

const name = computed(() => props.user.name ?? props.user.reference);
const abbreviation = computed(() => getUserNameAbbreviation(name.value));

function onClick(event: MouseEvent) {
  if (props.onSelect) {
    event.preventDefault();
    props.onSelect(props.user);
  }
}
</script>

<template>
  <component
    :is="link ? 'a' : 'span'"
    :href="link ? environment.getUserURL(user.reference) : undefined"
    class="realtime-user"
    :class="{ 'realtime-user-compact': compact }"
    :data-id="user.id"
    :data-reference="user.reference"
    @click="onClick"
  >
    <span class="realtime-user-avatar-wrapper" :data-abbr="abbreviation">
      <img
        class="realtime-user-avatar"
        :src="user.avatar || environment.defaultAvatarURL"
        :alt="name"
        :title="name"
      />
    </span>
    <span class="realtime-user-name">{{ name }}</span>
  </component>
</template>

<style>
.realtime-user {
  display: inline-block;
}

.realtime-user-avatar {
  max-width: 24px;
  max-height: 24px;
  border-radius: var(--border-radius-base);
  margin: 4px;
}

.realtime-user-avatar-wrapper {
  position: relative;
  display: inline-block;
}

.realtime-user-avatar-wrapper::after {
  content: attr(data-abbr);
  position: absolute;
  bottom: 0;
  right: 0;
  padding: 2px;
  line-height: 12px;
  border-radius: var(--border-radius-small);
  font-size: 10px;
  font-weight: bold;
  font-family: monospace;
  background-color: var(--btn-primary-bg);
  color: var(--btn-primary-color);
}

/* Hide the user name in compact mode. */
.realtime-user-compact .realtime-user-name {
  display: none;
}

.realtime-user-name {
  vertical-align: middle;
  margin-right: 4px;
}
</style>
