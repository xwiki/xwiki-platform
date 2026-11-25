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
import {
  MENTION_TRIGGER,
  shouldOpenMentionMenu,
  toSuggestions,
} from "./mentionMenu";
import { getUserSuggester } from "./userSuggester";
import { EditorInstanceContext } from "../../contexts";
import { mentionFactoryName } from "../../services/mentions";
import { SuggestionMenuController } from "@blocknote/react";
import {
  debounceAsync,
  tryFalliblePromiseOrError,
} from "@xwiki/platform-fn-utils";
import { useContext, useMemo } from "react";
import { useTranslation } from "react-i18next";
import type { MentionFactory } from "../../services/mentions";

/**
 * Suggests the users to mention, as the user types after the mention trigger, and hands the picked one to the mention
 * factory of the host application.
 */
export function MentionSuggestionController() {
  const instance = useContext(EditorInstanceContext)!;
  // Resolved by the mentions feature before it mounts this controller.
  const userSuggester = getUserSuggester(instance.serviceContainer)!;
  const mentionFactory =
    instance.serviceContainer.get<MentionFactory>(mentionFactoryName);

  const { t } = useTranslation();

  // Memoized and debounced because the suggestion menu re-queries its items whenever this function changes, and
  // looking the users up is a round-trip to the host application.
  const suggestUsers = useMemo(
    () =>
      debounceAsync(async (query: string) => {
        const users = await tryFalliblePromiseOrError(() =>
          userSuggester.suggestUsers(query),
        );
        if (users instanceof Error) {
          console.warn(
            `Failed to get the user suggestions for [${query}].`,
            users,
          );
          return [];
        }
        return toSuggestions(
          instance,
          mentionFactory,
          users,
          t("blocknote.mentionMenu.group"),
        );
      }),
    [instance, userSuggester, mentionFactory, t],
  );

  return (
    <SuggestionMenuController
      triggerCharacter={MENTION_TRIGGER}
      shouldOpen={shouldOpenMentionMenu}
      getItems={suggestUsers}
    />
  );
}
