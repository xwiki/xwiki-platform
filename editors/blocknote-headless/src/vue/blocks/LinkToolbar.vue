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
import LinkEditor from "./LinkEditor.vue";
import ToolbarButtonSet from "./ToolbarButtonSet.vue";
import { EditorType } from "../../blocknote";
import { LinkEditionContext } from "../../components/linkEditionContext";
import { LinkToolbarProps } from "@blocknote/react";
import { tryFallible } from "@xwiki/cristal-fn-utils";
import { ref } from "vue";

const { linkToolbarProps } = defineProps<{
  editor: EditorType;
  linkToolbarProps: LinkToolbarProps;
  linkEditionCtx: LinkEditionContext;
}>();

const editingLink = ref(false);

function openTarget() {
  if (linkToolbarProps.url) {
    window.open(linkToolbarProps.url);
  }
}
</script>

<template>
  <ToolbarButtonSet
    :buttons="[
      {
        icon: 'pencil',
        title: 'Edit',
        onClick() {
          editingLink = !editingLink;
        },
      },
      {
        icon: 'box-arrow-up-right',
        title: 'Open',
        onClick() {
          openTarget();
        },
      },
      {
        icon: 'trash',
        title: 'Delete',
        onClick() {
          linkToolbarProps.deleteLink();
        },
      },
    ]"
  />

  <div v-if="editingLink" class="link-editor">
    <LinkEditor
      :link-edition-ctx
      :current="{
        url: linkToolbarProps.url,
        reference: tryFallible(
          () =>
            linkEditionCtx.remoteURLParser.parse(linkToolbarProps.url) ?? null,
        ),
        title: linkToolbarProps.text,
      }"
      @update="({ url, title }) => linkToolbarProps.editLink(url, title)"
    />
  </div>
</template>

<style scoped>
/*
  NOTE: Popover is implemented manually here due to positioning problems with Tippy + considerations to switch to Floating UI now that Tippy is deprecated
  This is temporary and will be replaced with a "proper" solution using a dedicated library in the near future
*/
.link-editor {
  position: absolute;
  left: 0;
  /*
    Here we are positioning the popoverin an absolute fashion below the above element (the toolbar)
    We're assuming it here it has a fixed height of 2.5 rem (+ padding), which should stay true no matter what your screen resolution is
    (Yes, this is dirty)
  */
  top: 2.5rem;
  background: white;
  box-shadow: 0px 4px 12px #cfcfcf;
  border-radius: 6px;
}
</style>
