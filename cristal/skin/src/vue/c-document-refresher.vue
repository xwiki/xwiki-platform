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
import { DocumentReference } from "@xwiki/cristal-model-api";
import { inject, onMounted, shallowRef } from "vue";
import type { CristalApp } from "@xwiki/cristal-api";
import type { DocumentService } from "@xwiki/cristal-document-api";
import type { ModelReferenceSerializerProvider } from "@xwiki/cristal-model-reference-api";

defineSlots<{
  default: [];
}>();

const cristal = inject<CristalApp>("cristal")!;

const container = cristal.getContainer();

const documentService = container.get<DocumentService>("DocumentService")!;

const modelReferenceSerializer = container
  .get<ModelReferenceSerializerProvider>("ModelReferenceSerializerProvider")
  .get()!;

function computeDocumentRef(documentRef: DocumentReference): string {
  return modelReferenceSerializer.serialize(documentRef)!;
}

const initialDoc = documentService.getCurrentDocumentReference();

const documentId = shallowRef<string>(
  initialDoc.value ? computeDocumentRef(initialDoc.value) : "",
);

onMounted(() => {
  documentService.registerDocumentChangeListener("update", async (ref) => {
    documentId.value = computeDocumentRef(ref);
  });
});
</script>

<template>
  <slot :key="documentId" />
</template>
