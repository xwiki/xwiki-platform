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
import { BlockNoteViewWrapper } from "../components/BlockNoteViewWrapper";
import {
  AttachmentReference,
  DocumentReference,
  EntityType,
  SpaceReference,
} from "@xwiki/platform-model-api";
import { useMemo } from "react";
import { mock } from "vitest-mock-extended";
import type { BlockNoteViewWrapperProps } from "../components/BlockNoteViewWrapper";
import type { LinkSuggestService } from "@xwiki/platform-link-suggest-api";
import type {
  ModelReferenceParser,
  ModelReferenceParserProvider,
  ModelReferenceSerializerProvider,
} from "@xwiki/platform-model-reference-api";
import type {
  RemoteURLParserProvider,
  RemoteURLSerializerProvider,
} from "@xwiki/platform-model-remote-url-api";
import type { SyntaxConfig } from "@xwiki/platform-syntaxes-config";
import type { Container } from "inversify";

export type BlockNoteForTestProps = Omit<
  BlockNoteViewWrapperProps,
  "lang" | "syntax" | "label" | "depsContainer"
> & { syntax?: SyntaxConfig };

export const BlockNoteForTest: React.FC<BlockNoteForTestProps> = ({
  syntax,
  ...props
}) => {
  const depsContainer = useMemo(depsContainerMock, []);

  return (
    <BlockNoteViewWrapper
      lang="en"
      label="Some Label"
      syntax={syntax ?? FULL_SYNTAX}
      depsContainer={depsContainer}
      {...props}
    />
  );
};

function depsContainerMock(): Container {
  const container = mock<Container>();

  container.get.calledWith("RemoteURLParserProvider").mockReturnValue({
    get: () => ({
      parse(url, type) {
        throw new Error("TODO");
      },
    }),
  } satisfies RemoteURLParserProvider);

  container.get.calledWith("RemoteURLSerializerProvider").mockReturnValue({
    get: () => ({
      serialize(reference) {
        throw new Error("TODO");
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

  container.get.calledWith("LinkSuggestService").mockReturnValue({
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
  } satisfies LinkSuggestService);

  container.get.calledWith("AttachmentsService").mockReturnValue(null);
  container.get.calledWith("DocumentsService").mockReturnValue(null);

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

const FULL_SYNTAX: SyntaxConfig = {
  id: "default-syntax",
  features: {
    blocks: {
      headings: {
        levels1To3: true,
        levels4To6: true,
      },
      images: {
        basicImages: true,
        altText: true,
        caption: true,
        customBorder: true,
        customDimensions: true,
        insideLinks: true,
      },
      lists: {
        bulletLists: true,
        blockInListItems: true,
        checkableLists: true,
        contiguousNumberedLists: true,
        contiguousNumberedListsAnyStartIndex: true,
        mixableCheckableListItems: true,
        multipleBlocksInListItems: true,
        unorderedNumberedLists: true,
        listsNesting: true,
      },
      quotes: true,
      code: {
        basicCodeBlocks: true,
        language: true,
      },
      dividers: true,
      macros: true,
      nesting: true,
      styling: {
        justifyAlignment: true,
        lcrAlignment: true,
      },
      tables: {
        basicTables: true,
        blockInTableCells: true,
        colRows: true,
        colSpan: true,
        headerColumns: true,
        multipleBlocksInTableCells: true,
        multipleFooterRows: true,
        multipleHeaderRows: true,
        noHeaderRowTable: true,
        singleFooterRow: true,
        singleHeaderRow: true,
      },
    },
    inlineContents: {
      images: true,
      links: {
        basicLinks: true,
        customText: true,
        customTextStyling: true,
        descriptiveTooltip: true,
        metadata: true,
      },
      code: {
        basicInlineCode: true,
        language: true,
      },
      macros: true,
      rawHtml: true,
      textStyles: {
        bold: true,
        italic: true,
        strikethrough: true,
        underline: true,
        nesting: true,
        fontFamily: true,
        fontSize: true,
        subscript: true,
        superscript: true,
      },
    },
  },
};
