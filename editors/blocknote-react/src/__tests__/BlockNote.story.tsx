/**
 * See the LICENSE file distributed with this work for additional
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
import { DEFAULT_MACROS } from "..";
import { BlockType } from "../blocknote";
import { BlockNoteViewWrapper } from "../components/BlockNoteViewWrapper";
import { LinkEditionContext } from "../misc/linkSuggest";
import {
  AttachmentReference,
  DocumentReference,
  EntityType,
  SpaceReference,
} from "@xwiki/cristal-model-api";
import { useMemo } from "react";

export type BlockNoteForTestProps = {
  content: BlockType[];
};

export const BlockNoteForTest: React.FC<BlockNoteForTestProps> = ({
  content,
}) => {
  const linkEditionCtx = useMemo(linkEditionCtxMock, []);

  return (
    <BlockNoteViewWrapper
      content={content}
      lang="en"
      linkEditionCtx={linkEditionCtx}
      macros={{
        buildable: Object.values(DEFAULT_MACROS),
        openMacroParamsEditor(macro, id, params) {
          throw new Error("Macros params editor is not supported");
        },
      }}
    />
  );
};

function linkEditionCtxMock(): LinkEditionContext {
  return {
    // eslint-disable-next-line @typescript-eslint/no-explicit-any
    attachmentsService: null as any,
    // eslint-disable-next-line @typescript-eslint/no-explicit-any
    documentService: null as any,
    linkSuggestService: {
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
    },
    // eslint-disable-next-line @typescript-eslint/no-explicit-any
    modelReferenceHandler: null as any,
    modelReferenceParser: {
      parse(reference, type) {
        if (
          reference === "some page reference" &&
          (!type || type === EntityType.DOCUMENT)
        ) {
          return new DocumentReference("some page", new SpaceReference());
        }

        if (
          reference === "some attachment reference" &&
          (!type || type === EntityType.ATTACHMENT)
        ) {
          return new AttachmentReference(
            "some attachment",
            new DocumentReference("some attachment", new SpaceReference()),
          );
        }

        throw new Error("Invalid reference provided");
      },
    },
    modelReferenceSerializer: {
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
    },
    // eslint-disable-next-line @typescript-eslint/no-explicit-any
    remoteURLParser: null as any,
    // eslint-disable-next-line @typescript-eslint/no-explicit-any
    remoteURLSerializer: null as any,
  };
}
