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
import { createInlineMacro } from "../../misc/macroInvocation";
import { mentionFactoryName } from "../../services/mentionFactory";
import { userSuggesterProviderName } from "@xwiki/platform-user-suggest-api";
import { useMemo, useState } from "react";
import type { EditorType } from "../../blocknote";
import type { MentionFactory } from "../../services/mentionFactory";
import type {
  SuggestedUser,
  UserSuggesterProvider,
} from "@xwiki/platform-user-suggest-api";

export type MentionSuggestionForTestProps = {
  // The users the user suggester returns, whatever the query (none when not set).
  users?: SuggestedUser[];
  // When set, the backend has no user suggester, as with one that can't look up users.
  noUserSuggester?: boolean;
  // When set, no mention factory is bound, as with a backend that has no way to represent a mention.
  noMentionFactory?: boolean;
  // When set, the syntax of the edited content can't save a mention.
  syntaxWithoutMentions?: boolean;
  // When set, looking the users up fails, as when the backend can't be reached.
  failingSuggestions?: boolean;
};

/**
 * Renders the editor with a scripted user suggester feeding the mention menu and a scripted mention factory standing
 * for the host application, and reports the content the editor holds, so that the tests can check what a picked
 * suggestion inserted. The factory builds a call to a `mention` macro taking the mentioned reference: what an XWiki
 * mention looks like is the integration's business, tested there.
 *
 * This is a dedicated story (rather than a `BlockNoteForTest` prop) because the services have to be built here, where
 * the code actually runs in the browser: a function passed as a test prop is proxied back to Node when called.
 */
export const MentionSuggestionForTest: React.FC<
  MentionSuggestionForTestProps
> = ({
  users,
  noUserSuggester,
  noMentionFactory,
  syntaxWithoutMentions,
  failingSuggestions,
}) => {
  // The edited content, rendered so that the tests can assert on it.
  const [content, setContent] = useState<string>();

  const serviceContainer = useMemo(() => {
    const syntax = structuredClone(FULL_SYNTAX);
    syntax.features.inlineContents.mentions = !syntaxWithoutMentions;
    const container = serviceContainerMock([syntax]);
    container
      .bind<UserSuggesterProvider>(userSuggesterProviderName)
      .toConstantValue({
        get: () =>
          noUserSuggester
            ? undefined
            : {
                async suggestUsers(query) {
                  if (failingSuggestions) {
                    throw new Error("Failed to reach the backend");
                  }
                  // Matched on the user name, like the user suggestion template of XWiki does.
                  return (users ?? []).filter((user) =>
                    user.label.toLowerCase().includes(query.toLowerCase()),
                  );
                },
              },
      });
    if (!noMentionFactory) {
      container.bind<MentionFactory>(mentionFactoryName).toConstantValue({
        createMention: (user) =>
          createInlineMacro({
            kind: "inline",
            id: "mention",
            params: { reference: user.reference },
            body: { type: "none" },
          }),
      });
    }
    return container;
    // The services are registered once, when the container is created.
  }, []);

  return (
    <>
      <BlockNoteViewWrapper
        options={{
          lang: "en",
          label: "Some Label",
          serviceContainer,
          syntax: FULL_SYNTAX.id,
        }}
        content={[]}
        onChange={(editor: EditorType) =>
          setContent(JSON.stringify(editor.document))
        }
      />
      <output data-testid="content">{content}</output>
    </>
  );
};
