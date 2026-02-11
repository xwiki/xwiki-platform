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
import type { BreadcrumbProps } from "@xwiki/platform-dsapi";

const { items } = defineProps<BreadcrumbProps>();
</script>

<template>
  <ol class="breadcrumb">
    <li v-for="({ url, label }, index) in items" :key="index">
      <a :href="url" v-if="url">{{ label }}</a>
      <template v-else>{{ label }}</template>
    </li>
    <!--    <li class="wiki">-->
    <!--      <a href="/xwiki/bin/view/Main/" title="Home"-->
    <!--        ><span class="fa fa-home" aria-hidden="true"></span-->
    <!--      ></a>-->
    <!--    </li>-->
    <!--    <li class="space"><a href="/xwiki/bin/view/XWiki/">XWiki</a></li>-->
    <!--    <li class="document active">-->
    <!--      <a href="/xwiki/bin/view/XWiki/Admin">Profile of Administrator </a>-->
    <!--    </li>-->
  </ol>
</template>

<style scoped></style>
