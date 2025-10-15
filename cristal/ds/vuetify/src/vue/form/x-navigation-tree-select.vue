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
import messages from "../../translations";
import XBreadcrumb from "../x-breadcrumb.vue";
import { navigationTreeSelectPropsDefaults } from "@xwiki/cristal-dsapi";
import { EntityType, SpaceReference } from "@xwiki/cristal-model-api";
import { inject, onMounted, ref } from "vue";
import { useI18n } from "vue-i18n";
import { VTextField } from "vuetify/components/VTextField";
import type { CristalApp } from "@xwiki/cristal-api";
import type { NavigationTreeSelectProps } from "@xwiki/cristal-dsapi";
import type {
  PageHierarchyItem,
  PageHierarchyResolver,
  PageHierarchyResolverProvider,
} from "@xwiki/cristal-hierarchy-api";
import type { DocumentReference } from "@xwiki/cristal-model-api";
import type {
  ModelReferenceHandler,
  ModelReferenceHandlerProvider,
} from "@xwiki/cristal-model-reference-api";
import type { NavigationTreeNode } from "@xwiki/cristal-navigation-tree-api";
import type { Ref } from "vue";

const cristal: CristalApp = inject<CristalApp>("cristal")!;
const hierarchyResolver: PageHierarchyResolver = cristal
  .getContainer()
  .get<PageHierarchyResolverProvider>("PageHierarchyResolverProvider")
  .get()!;
const referenceHandler: ModelReferenceHandler = cristal
  .getContainer()
  .get<ModelReferenceHandlerProvider>("ModelReferenceHandlerProvider")
  .get()!;

const { t } = useI18n({
  messages,
});
const props = withDefaults(
  defineProps<NavigationTreeSelectProps>(),
  navigationTreeSelectPropsDefaults,
);
const model = defineModel<SpaceReference | DocumentReference>();

const openedLocationDialog: Ref<boolean> = ref(false);
const hierarchy: Ref<Array<PageHierarchyItem>> = ref([]);
let selectedPage: DocumentReference | undefined;

onMounted(async () => {
  selectedPage = props.currentPageReference;
  model.value = props.currentPageReference!.space;
  hierarchy.value = await hierarchyResolver.getPageHierarchy(
    props.currentPageReference!.space!,
    false,
  );
});

async function treeNodeClickAction(node: NavigationTreeNode) {
  if (node.location.type == EntityType.SPACE) {
    if (node.location.names.length > 0) {
      selectedPage = referenceHandler.createDocumentReference(
        node.location.names[node.location.names.length - 1],
        new SpaceReference(
          node.location.wiki,
          ...node.location.names.slice(0, -1),
        ),
      );
      hierarchy.value = await hierarchyResolver.getPageHierarchy(
        selectedPage,
        false,
      );
    } else {
      selectedPage = undefined;
      hierarchy.value = [{ label: node.label, pageId: "", url: node.url }];
    }
  } else {
    selectedPage = node.location;
    hierarchy.value = await hierarchyResolver.getPageHierarchy(
      selectedPage,
      false,
    );
  }
  model.value = node.location;
}
</script>

<template>
  <v-text-field
    :label="label"
    :hint="help"
    :persistent-hint="help !== undefined"
    :active="hierarchy.length > 0"
    readonly
  >
    <template #default>
      <XBreadcrumb :items="hierarchy"></XBreadcrumb>
    </template>
  </v-text-field>
  <XDialog
    v-model="openedLocationDialog"
    width="auto"
    :title="t('vuetify.tree.select.location.label')"
  >
    <template #activator>
      <x-btn id="change-page-location-button" size="small" color="secondary">
        {{ t("vuetify.tree.select.location.label") }}
      </x-btn>
    </template>
    <template #default>
      <XNavigationTree
        :current-page-reference="selectedPage"
        :click-action="treeNodeClickAction"
        :include-terminals="includeTerminals"
        show-root-node
      ></XNavigationTree>
    </template>
    <template #footer>
      <XBtn variant="primary" @click="openedLocationDialog = false">
        {{ t("vuetify.tree.select.location.select") }}
      </XBtn>
    </template>
  </XDialog>
</template>

<style scoped>
.v-treeview {
  overflow: auto;
}
</style>
