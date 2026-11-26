/**
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
import type { EditorInstance } from "../editorInstance";
import type { DocumentService } from "@xwiki/platform-document-api";
import type { DocumentReference } from "@xwiki/platform-model-api";
import type { ModelReferenceSerializerProvider } from "@xwiki/platform-model-reference-api";
import type { Container } from "inversify";

/**
 * @param serviceContainer - the container providing the model reference serializer
 * @param reference - a document reference
 * @returns the given document reference serialized, or undefined when not specified or when it can't be serialized
 */
function serializeDocumentReference(
  serviceContainer: Container,
  reference?: DocumentReference,
): string | undefined {
  return (
    reference &&
    serviceContainer
      .get<ModelReferenceSerializerProvider>("ModelReferenceSerializerProvider")
      .get()
      ?.serialize(reference)
  );
}

/**
 * @param serviceContainer - the container providing the document service
 * @returns the serialized reference of the current page
 */
function getCurrentPage(serviceContainer: Container): string {
  return (
    serviceContainer
      .get<DocumentService>("DocumentService")
      .getCurrentDocumentReferenceString().value ?? ""
  );
}

/**
 * @param instance - the editor instance, or what is known about it before its editor is created
 * @returns the serialized reference of the source document of the edited content, the current page when not specified
 */
function getSourceDocument(
  instance: Pick<EditorInstance, "options" | "serviceContainer">,
): string {
  return (
    serializeDocumentReference(
      instance.serviceContainer,
      instance.options.sourceDocumentReference,
    ) ?? getCurrentPage(instance.serviceContainer)
  );
}

export { getCurrentPage, getSourceDocument, serializeDocumentReference };
