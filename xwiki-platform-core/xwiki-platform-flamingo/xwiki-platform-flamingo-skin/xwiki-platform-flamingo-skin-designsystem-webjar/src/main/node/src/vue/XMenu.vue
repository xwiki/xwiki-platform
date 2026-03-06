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
import { useId } from "vue";

const rootId = useId();
</script>

<template>
  <div class="dropdown">
    <button
      :id="rootId"
      type="button"
      data-toggle="dropdown"
      aria-haspopup="true"
      aria-expanded="false"
    >
      <slot name="activator" />
    </button>
    <ul class="dropdown-menu" :aria-labelledby="rootId">
      ...
    </ul>
  </div>
  <sl-dropdown hoist stay-open-on-select>
    <!-- @vue-expect-error the slot attribute is shoelace specific and is not know by the typechecker.
    Disabling it for now as I did not find an elegant solution to declare this property. -->
    <!--eslint-disable-next-line vue/no-deprecated-slot-attribute -->
    <span slot="trigger">
      <slot name="activator" />
    </span>
    <sl-menu>
      <slot name="default" />
    </sl-menu>
  </sl-dropdown>
</template>

<style scoped></style>
