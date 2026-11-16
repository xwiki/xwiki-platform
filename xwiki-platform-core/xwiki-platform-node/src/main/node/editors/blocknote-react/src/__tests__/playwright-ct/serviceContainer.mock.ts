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

import { FULL_SYNTAX } from "./syntax.mock";
import {
  AttachmentReference,
  DocumentReference,
  EntityType,
  SpaceReference,
} from "@xwiki/platform-model-api";
import { DefaultResourceReferenceParser } from "@xwiki/platform-rendering-api";
import { SYNTAX_CONFIG_COMPONENT_GROUP_NAME } from "@xwiki/platform-syntaxes-config";
import { Container } from "inversify";
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
import type { SyntaxConfig } from "@xwiki/platform-syntaxes-config";

/**
 * @param syntaxes - the syntax configurations registered in the container, the full syntax when not specified
 * @returns a container providing mocks of the services the editor needs (a real container, because the editor creates
 *   a child container for each editor instance)
 */
// eslint-disable-next-line max-statements
export function serviceContainerMock(
  syntaxes: SyntaxConfig[] = [FULL_SYNTAX],
): Container {
  const container = new Container();

  for (const syntax of syntaxes) {
    container
      .bind<SyntaxConfig>(SYNTAX_CONFIG_COMPONENT_GROUP_NAME)
      .toConstantValue(syntax);
  }

  container.bind("RemoteURLParserProvider").toConstantValue({
    get: () => ({
      parse() {
        throw new Error("Unreachable");
      },
    }),
  } satisfies RemoteURLParserProvider);

  container.bind("RemoteURLSerializerProvider").toConstantValue({
    get: () => ({
      serialize(reference) {
        if (reference?.type === EntityType.ATTACHMENT) {
          return "some attachment url";
        }

        throw new Error("Unreachable");
      },
    }),
  } satisfies RemoteURLSerializerProvider);

  container.bind("ModelReferenceParserProvider").toConstantValue({
    get: () => ({
      parse(reference, options) {
        return parseModelReference(reference, options);
      },

      async parseAsync(reference, options) {
        return parseModelReference(reference, options);
      },
    }),
  } satisfies ModelReferenceParserProvider);

  container.bind("ModelReferenceHandlerProvider").toConstantValue({
    get: () => ({
      createDocumentReference() {
        throw new Error("Unreachable");
      },
      getTitle() {
        throw new Error("Unreachable");
      },
      getParentDocumentReference() {
        throw new Error("Unreachable");
      },
      getParentSpaceReference() {
        throw new Error("Unreachable");
      },
    }),
  } satisfies ModelReferenceHandlerProvider);

  container.bind("ModelReferenceSerializerProvider").toConstantValue({
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

  container.bind("LinkSuggestServiceProvider").toConstantValue({
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

  container
    .bind("ResourceReferenceParser")
    .toConstantValue(new DefaultResourceReferenceParser());

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
    // Like an actual entity reference, it doesn't hold the type of the entity.
    opts?.type === EntityType.ATTACHMENT
  ) {
    return new AttachmentReference(
      "some attachment",
      new DocumentReference("some attachment", new SpaceReference()),
    );
  }

  throw new Error("Invalid reference provided");
};
