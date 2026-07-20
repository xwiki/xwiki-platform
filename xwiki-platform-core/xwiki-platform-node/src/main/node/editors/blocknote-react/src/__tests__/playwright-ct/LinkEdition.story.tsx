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

/**
 * Renders the editor with link edition hooks defined natively (i.e. running in the browser). The hooks
 * are declared here rather than passed as test props because Playwright component test function props
 * are proxied to Node, which breaks the synchronous return value the hooks rely on.
 */
export const BlockNoteWithLinkEditionHooks: React.FC<{
  content: BlockType[];
  // When set, beforeEdit returns the current link data with this title, to pre-fill the popover.
  beforeEditTitle?: string;
  // When set, beforeUpdate returns the current link data with this url, to change what is persisted.
  beforeUpdateUrl?: string;
  // When true, beforeUpdate renders the received resource reference so tests can assert on it.
  captureReference?: boolean;
}> = ({ content, beforeEditTitle, beforeUpdateUrl, captureReference }) => {
  const [capturedReference, setCapturedReference] = useState<string>();

  return (
    <>
      {capturedReference !== undefined && (
        <div data-test="capturedReference">{capturedReference}</div>
      )}
      <BlockNoteForTest
        content={content}
        macros={false}
        syntax={FULL_SYNTAX}
        overrides={{
          linkEdition: {
            beforeEdit:
              beforeEditTitle === undefined
                ? undefined
                : (linkData) => ({ ...linkData, title: beforeEditTitle }),
            beforeUpdate: (linkData) => {
              if (captureReference) {
                setCapturedReference(JSON.stringify(linkData.reference));
              }
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
