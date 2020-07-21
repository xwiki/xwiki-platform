<template>
  <div class="livedata-topbar">

    <div class="livedata-topbar-left">
      <slot name="left"></slot>
    </div>

    <div class="livedata-topbar-right">
      <slot name="right"></slot>
    </div>

  </div>
</template>


<script>
/*
  * See the NOTICE file distributed with this work for additional
  * information regarding copyright ownership.
  *
  * This is free software; you can redistribute it and/or modify it
  * under the terms of the GNU Lesser General Public License as
  * published by the Free Software Foundation; either version 2.1 of
  * the License, or (at your option) any later version.
  *
  * This software is distributed in the hope that it will be useful,
  * but WITHOUT ANY WARRANTY; without even the implied warranty of
  * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
  * Lesser General Public License for more details.
  *
  * You should have received a copy of the GNU Lesser General Public
  * License along with this software; if not, write to the Free
  * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
  * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
  */
define([
  "Vue",
], function (
  Vue,
) {
  Vue.component("livedata-topbar", {

    name: "livedata-topbar",

    template: template,

    props: {
      logic: Object,
    },

  });
});
</script>


<style>

.livedata-topbar {
  display: flex;
  flex-direction: row;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 1rem;
}

.livedata-topbar-left,
.livedata-topbar-right {
  display: flex;
  flex-direction: row;
  justify-content: flex-start;
  align-items: center;
}

.livedata-dropdown-menu {
  margin-right: 1rem;
}


</style>
