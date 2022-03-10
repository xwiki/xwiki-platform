<!--
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
-->

<template>
  <div class="draggable-item">
    <div class="handle">
      <slot name="handle">
        <XWikiIcon :icon-descriptor="{name: 'menu'}" />
      </slot>
    </div>

    <slot></slot>
  </div>
</template>


<script>
import XWikiIcon from "./XWikiIcon";

export default {

  name: "XWikiDraggableItem",

  components: {XWikiIcon}
};
</script>


<style>


.draggable-item  {
  display: flex;
  flex-direction: row;
  justify-content: flex-start;
  align-items: center;
}

.draggable-item .handle {
  padding: 6px;
  cursor: pointer; /* IE */
  cursor: grab;
  opacity: 0;
}

.draggable-item:hover .handle {
  opacity: 1;
  transition: opacity 0.2s;
}

.draggable-item .handle .fa {
  vertical-align: middle;
}

/* Responsive mode */
@media screen and (max-width: @screen-xs-max) {
  .draggable-item .handle {
    /* Always show the drag handle on small screens because we can't rely on hover. */
    opacity: 1;
  }
}

</style>
