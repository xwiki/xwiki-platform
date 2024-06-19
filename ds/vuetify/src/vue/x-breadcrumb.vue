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
import { BreadcrumbProps } from "@xwiki/cristal-dsapi";

defineProps<BreadcrumbProps>();
</script>

<template>
  <v-breadcrumbs
    :items="
      items.map((hItem) => {
        var bItem: { title: string; href: string } = {
          title: hItem.label,
          href: hItem.url,
        };
        return bItem;
      })
    "
  >
  </v-breadcrumbs>
</template>

<style scoped></style>
