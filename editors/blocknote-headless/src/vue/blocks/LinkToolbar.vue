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
import LinkSuggestion from "./LinkSuggestion.vue";
import { EditorType } from "../../blocknote";
import { LinkEditionContext } from "../../components/linkEditionContext";
import { LinkToolbarProps } from "@blocknote/react";

defineProps<{
  editor: EditorType;
  linkToolbarProps: LinkToolbarProps;
  linkEditionCtx: LinkEditionContext;
}>();
</script>

<template>
  <div style="border: 1px solid black; padding: 5px; background-color: white">
    <p>
      Current link is: <strong>{{ linkToolbarProps.url }}</strong>
    </p>

    <LinkSuggestion
      :link-edition-ctx
      @selected="({ url, title }) => linkToolbarProps.editLink(url, title)"
    />
  </div>
</template>
