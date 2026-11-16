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
import { FULL_SYNTAX } from "./syntax.mock";
import { BlockNoteViewWrapper } from "../../components/BlockNoteViewWrapper";
import { useMemo } from "react";
import { Awareness } from "y-protocols/awareness";
import { Doc } from "yjs";
import type { Collaboration } from "@xwiki/platform-collaboration-api";

export type CollaborationForTestProps = {
  // The editor instances taking part in the same collaboration session, each with the text of its initial content.
  editors: Array<{ name?: string; text: string }>;
};

/**
 * Renders several editors joining the same collaboration session, as several fields of the same document do.
 *
 * The collaboration session is created here, in the browser, because the Yjs document it holds can't cross the
 * boundary between Playwright and the browser.
 */
export const CollaborationForTest: React.FC<CollaborationForTestProps> = ({
  editors,
}) => {
  const serviceContainer = useMemo(serviceContainerMock, []);

  const collaboration = useMemo(() => {
    const doc = new Doc();
    // Not connected to anything: the editors share the Yjs document directly.
    return {
      doc,
      provider: { awareness: new Awareness(doc) },
      collaborator: { user: { username: "Alice" }, color: "#ff0000" },
    } as unknown as Collaboration;
  }, []);

  return (
    <>
      {editors.map(({ name, text }, index) => (
        <div key={index} data-testid={`editor-${index}`}>
          <BlockNoteViewWrapper
            options={{
              serviceContainer,
              name,
              syntax: FULL_SYNTAX.id,
              lang: "en",
              label: `Editor ${index}`,
              collaboration,
            }}
            content={[
              {
                id: `paragraph-${index}`,
                type: "paragraph",
                props: {
                  backgroundColor: "default",
                  textAlignment: "left",
                  textColor: "default",
                },
                content: [{ type: "text", text, styles: {} }],
                children: [],
              },
            ]}
            linkEditionHandler={() => {
              throw new Error("Link editor should not open for this test");
            }}
          />
        </div>
      ))}
    </>
  );
};
