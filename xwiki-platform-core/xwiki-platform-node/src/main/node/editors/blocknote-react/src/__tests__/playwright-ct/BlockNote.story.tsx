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
import { serviceContainerMock } from "./serviceContainer.mock";
import { BlockNoteViewWrapper } from "../../components/BlockNoteViewWrapper";
import { useMemo } from "react";
import type { BlockType } from "../../blocknote";
import type { LinkEditionHandler } from "../../components/links/linkEdition";
import type { EditorOptions } from "../../editorOptions";
import type { SyntaxConfig } from "@xwiki/platform-syntaxes-config";

export type BlockNoteForTestProps = {
  content: BlockType[];
  // The options that the tests need to set, the others being the same for all the tests. The configuration of the
  // syntax of the edited content is registered in the container, the editor taking only its id.
  options: Omit<
    EditorOptions,
    "lang" | "label" | "serviceContainer" | "syntax"
  > & {
    syntax: SyntaxConfig;
  };
  linkEditionHandler?: LinkEditionHandler;
};

export const BlockNoteForTest: React.FC<BlockNoteForTestProps> = ({
  content,
  options,
  linkEditionHandler,
}) => {
  const serviceContainer = useMemo(
    () => serviceContainerMock([options.syntax]),
    // The syntax configuration is registered once, when the container is created.
    [],
  );

  return (
    <BlockNoteViewWrapper
      options={{
        lang: "en",
        label: "Some Label",
        serviceContainer,
        ...options,
        syntax: options.syntax.id,
      }}
      content={content}
      linkEditionHandler={
        linkEditionHandler ??
        (() => {
          throw new Error("Link editor should not open for this test");
        })
      }
    />
  );
};
