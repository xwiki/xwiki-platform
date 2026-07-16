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

import {
  AttachmentReference,
  DocumentReference,
  EntityType,
  SpaceReference,
} from "@xwiki/platform-model-api";
import { mock } from "vitest-mock-extended";
import type { LinkSuggestServiceProvider } from "@xwiki/platform-link-suggest-api";
import type {
  ModelReferenceHandlerProvider,
  ModelReferenceParser,
  ModelReferenceParserProvider,
  ModelReferenceSerializerProvider,
} from "@xwiki/platform-model-reference-api";
import type {
  RemoteURLParserProvider,
  RemoteURLSerializerProvider,
} from "@xwiki/platform-model-remote-url-api";
import type { Container } from "inversify";

export function depsContainerMock(): Container {
  const container = mock<Container>();

  container.get.calledWith("RemoteURLParserProvider").mockReturnValue({
    get: () => ({
      parse() {
        throw new Error("Unreachable");
      },
    }),
  } satisfies RemoteURLParserProvider);

  container.get.calledWith("RemoteURLSerializerProvider").mockReturnValue({
    get: () => ({
      serialize() {
        throw new Error("Unreachable");
      },
    }),
  } satisfies RemoteURLSerializerProvider);

  container.get.calledWith("ModelReferenceParserProvider").mockReturnValue({
    get: () => ({
      parse(reference, options) {
        return parseModelReference(reference, options);
      },

      async parseAsync(reference, options) {
        return parseModelReference(reference, options);
      },
    }),
  } satisfies ModelReferenceParserProvider);

  container.get.calledWith("ModelReferenceSerializerProvider").mockReturnValue({
    get: () => ({
      serialize(reference) {
        if (!reference) {
          throw new Error("Please provide a reference to serialize");
        }

        if (reference.type === EntityType.DOCUMENT) {
          return "some page reference";
        }

        if (reference.type === EntityType.ATTACHMENT) {
          return "some attachment reference";
        }

        throw new Error("Invalid reference provided");
      },
    }),
  } satisfies ModelReferenceSerializerProvider);

  container.get.calledWith("ModelReferenceHandlerProvider").mockReturnValue({
    get: () => ({
      createDocumentReference(name, space) {
        return new DocumentReference(name, space);
      },

      getTitle(reference) {
        return reference.name;
      },

      getParentDocumentReference() {
        return undefined;
      },

      getParentSpaceReference() {
        return undefined;
      },
    }),
  } satisfies ModelReferenceHandlerProvider);

  container.get.calledWith("LinkSuggestServiceProvider").mockReturnValue({
    get: () => ({
      async getLinks() {
        return [
          {
            id: "some page id",
            hint: "some page hint",
            label: "some page label",
            reference: "some page reference",
            type: 0,
            url: "some page url",
          },
          {
            id: "some attachment id",
            hint: "some attachment hint",
            label: "some attachment label",
            reference: "some attachment reference",
            type: 1,
            url: "some attachment url",
          },
        ];
      },
    }),
  } satisfies LinkSuggestServiceProvider);

  container.get.calledWith("AttachmentsService").mockReturnValue(null);
  container.get.calledWith("DocumentService").mockReturnValue(null);

  return container;
}

const parseModelReference: ModelReferenceParser["parse"] = (
  reference,
  opts,
) => {
  if (
    reference === "some page reference" &&
    (!opts?.type || opts?.type === EntityType.DOCUMENT)
  ) {
    return new DocumentReference("some page", new SpaceReference());
  }

  if (
    reference === "some attachment reference" &&
    (!opts?.type || opts?.type === EntityType.ATTACHMENT)
  ) {
    return new AttachmentReference(
      "some attachment",
      new DocumentReference("some attachment", new SpaceReference()),
    );
  }

  throw new Error("Invalid reference provided");
};
