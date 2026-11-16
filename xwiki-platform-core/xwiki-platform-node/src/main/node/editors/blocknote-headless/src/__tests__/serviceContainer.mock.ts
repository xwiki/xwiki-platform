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

import { FULL_SYNTAX } from "./playwright-ct/syntax.mock";
import { ComponentInit as LinkTypeComponentInit } from "@xwiki/platform-link-type-api";
import { ComponentInit as LinkTypeDefaultComponentInit } from "@xwiki/platform-link-type-default";
import { ComponentInit as LinkTypeUIComponentInit } from "@xwiki/platform-link-type-ui";
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
        // The URL of the page returned by the link suggestion service below, for the tests submitting a page link.
        if (reference?.type === EntityType.DOCUMENT) {
          return "https://example.org/some/page";
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

  container.bind("ModelReferenceSerializerProvider").toConstantValue({
    get: () => ({
      serialize(reference, base) {
        if (!reference) {
          throw new Error("Please provide a reference to serialize");
        }

        if (reference.type === EntityType.DOCUMENT) {
          // Relative to the document holding the link, see parseModelReference.
          return base ? RELATIVE_PAGE_REFERENCE : "some page reference";
        }

        if (reference.type === EntityType.ATTACHMENT) {
          return "some attachment reference";
        }

        throw new Error("Invalid reference provided");
      },
    }),
  } satisfies ModelReferenceSerializerProvider);

  container.bind("ModelReferenceHandlerProvider").toConstantValue({
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

  // The link modal and the editor resolve the link target type manager, and the forms of the link target types, out of
  // the container, so the built-in link target types are registered the same way the BlockNote webjar registers them
  // in production (see its container.ts).
  new LinkTypeComponentInit(container);
  new LinkTypeDefaultComponentInit(container);
  new LinkTypeUIComponentInit(container);

  return container;
}

/**
 * The reference of the "some page" page relative to the document holding the link, which can only be resolved against
 * that document.
 */
const RELATIVE_PAGE_REFERENCE = "some sibling page";

const parseModelReference: ModelReferenceParser["parse"] = (
  reference,
  opts,
) => {
  if (
    (reference === "some page reference" ||
      (reference === RELATIVE_PAGE_REFERENCE && opts?.base)) &&
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
