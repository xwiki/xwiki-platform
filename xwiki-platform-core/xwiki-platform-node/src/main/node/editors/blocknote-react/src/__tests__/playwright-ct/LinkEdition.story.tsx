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
import {
  linkEditorName,
  linkMetadataHandlerName,
} from "../../services/linkEditor";
import { useState } from "react";
import type { BlockType } from "../../blocknote";
import type {
  LinkEditionData,
  LinkEditor,
  LinkMetadataHandler,
} from "../../services/linkEditor";

/**
 * Renders the editor with a link metadata handler and a link editor defined natively (i.e. running in the browser).
 * They are declared here rather than passed as test props because Playwright component test function props are proxied
 * to Node, which breaks the synchronous return value the link metadata handler relies on.
 *
 * The link editor is scripted: opening it immediately submits {@link submit} (or, when it is not set, the edited link
 * unchanged, or no link when creating one).
 */
export const BlockNoteWithLinkMetadataHandler: React.FC<{
  content: BlockType[];
  // When set, beforeEdit returns the current link data with this title, to pre-fill the link editor.
  beforeEditTitle?: string;
  // The link data submitted by the scripted link editor.
  submit?: LinkEditionData;
  // When set, beforeUpdate returns the submitted link data with this url, to change what is persisted.
  beforeUpdateUrl?: string;
  // When set, the scripted link editor is canceled instead of submitting.
  cancel?: boolean;
  // When set, no link editor is bound.
  noLinkEditor?: boolean;
}> = ({
  content,
  beforeEditTitle,
  submit,
  beforeUpdateUrl,
  cancel,
  noLinkEditor,
}) => {
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
        bind={(container) => {
          if (!noLinkEditor) {
            container.bind<LinkEditor>(linkEditorName).toConstantValue({
              async insert(_instance, prefill) {
                setEditorInput(JSON.stringify(prefill));
                return cancel ? undefined : submit;
              },
              async edit(_instance, link) {
                setEditorInput(JSON.stringify(link));
                return cancel ? undefined : (submit ?? link);
              },
            });
          }
          container
            .bind<LinkMetadataHandler>(linkMetadataHandlerName)
            .toConstantValue({
              beforeEdit: (_instance, link) =>
                beforeEditTitle === undefined
                  ? link
                  : { ...link, title: beforeEditTitle },
              beforeUpdate: (_instance, link, previous) => {
                setUpdateInput(JSON.stringify({ linkData: link, previous }));
                return beforeUpdateUrl === undefined
                  ? link
                  : { ...link, url: beforeUpdateUrl };
              },
            });
        }}
        options={{ syntax: FULL_SYNTAX }}
      />
    </>
  );
};
