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
import { BlockNoteForTest } from "./BlockNote.story";
import { FULL_SYNTAX } from "./syntax.mock";
import { useState } from "react";
import type { BlockType } from "../../blocknote";
import type { LinkEditionData } from "../../components/links/linkEdition";

/**
 * Renders the editor with link edition hooks and a link editor defined natively (i.e. running in the
 * browser). They are declared here rather than passed as test props because Playwright component test
 * function props are proxied to Node, which breaks the synchronous return value the hooks rely on.
 *
 * The link editor is scripted: opening it immediately submits {@link submit} (or, when it is not set,
 * the link data it was pre-filled with).
 */
export const BlockNoteWithLinkEditionHooks: React.FC<{
  content: BlockType[];
  // When set, beforeEdit returns the current link data with this title, to pre-fill the link editor.
  beforeEditTitle?: string;
  // The link data submitted by the scripted link editor.
  submit?: LinkEditionData;
  // When set, beforeUpdate returns the submitted link data with this url, to change what is persisted.
  beforeUpdateUrl?: string;
}> = ({ content, beforeEditTitle, submit, beforeUpdateUrl }) => {
  // The link data the scripted link editor was opened with, and the arguments beforeUpdate received,
  // rendered so that the tests can assert on them.
  const [editorInput, setEditorInput] = useState<string>();
  const [updateInput, setUpdateInput] = useState<string>();

  return (
    <>
      {editorInput !== undefined && (
        <div data-test="linkEditorInput">{editorInput}</div>
      )}
      {updateInput !== undefined && (
        <div data-test="beforeUpdateInput">{updateInput}</div>
      )}
      <BlockNoteForTest
        content={content}
        macros={false}
        syntax={FULL_SYNTAX}
        linkEditionHandler={({ current, onSubmit }) => {
          setEditorInput(JSON.stringify(current));
          onSubmit(submit ?? current);
        }}
        overrides={{
          linkEdition: {
            beforeEdit:
              beforeEditTitle === undefined
                ? undefined
                : (linkData) => ({ ...linkData, title: beforeEditTitle }),
            beforeUpdate: (linkData, previous) => {
              setUpdateInput(JSON.stringify({ linkData, previous }));
              return beforeUpdateUrl === undefined
                ? linkData
                : { ...linkData, url: beforeUpdateUrl };
            },
          },
        }}
      />
    </>
  );
};
