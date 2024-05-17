<!--
 * See the LICENSE file distributed with this work for additional
 * information regarding copyright ownership.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 *
 * This file is part of the Cristal Wiki software prototype
 * @copyright  Copyright (c) 2023 XWiki SAS
 * @license    http://opensource.org/licenses/AGPL-3.0 AGPL-3.0
 *
 **/
-->
<script setup lang="ts">
import "bootstrap-icons/font/bootstrap-icons.css";
import { computed } from "vue";
import { Size } from "../size";

// TODO: make sure that we have the good parameters available for accessibility
const props = withDefaults(
  defineProps<{
    name: string;
    size?: Size;
  }>(),
  {
    size: Size.Normal,
  },
);

function sizeToClass(size: Size): string | undefined {
  let clazz = undefined;
  if (size === Size.Big) {
    clazz = "big";
  } else if (size === Size.Small) {
    clazz = "small";
  }
  return clazz;
}

const classes = computed(() => {
  const ret = ["cr-icon", `bi-${props.name}`];
  const sizeClass = sizeToClass(props.size);
  if (sizeClass) {
    ret.push(sizeClass);
  }
  return ret;
});
// TODO: currently names are bound to bootstrap class names. We'll need to add
// an indirection as soon as we want to support several icon sets.
</script>

<template>
  <span :class="classes"></span>
</template>

<style scoped>
span {
  font-size: 1.3rem;
}

span.big {
  font-size: 1.6rem;
}

span.small {
  font-size: 1rem;
}
</style>
