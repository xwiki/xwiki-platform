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
import { queryMentionMenuItems } from "./mentionMenu";
import { useEditor } from "../../hooks";
import { SuggestionMenuController } from "@blocknote/react";
import { debounceAsync } from "@xwiki/platform-fn-utils";
import { useMemo } from "react";
import { useTranslation } from "react-i18next";
import type { UserSuggestFn } from "./mentionMenu";

export type MentionSuggestionControllerProps = {
  /** Source of the user suggestions. */
  userSuggest: UserSuggestFn;
};

/**
 * Suggests the users to mention, as the user types after the mention trigger.
 */
export function MentionSuggestionController({
  userSuggest,
}: MentionSuggestionControllerProps) {
  const editor = useEditor();

  const { t } = useTranslation();

  // Memoized and debounced because the suggestion menu re-queries its items whenever this function changes, and
  // looking the users up is a round-trip to the host application.
  const suggestUsers = useMemo(
    () =>
      debounceAsync((query: string) =>
        queryMentionMenuItems({ query, editor, userSuggest, t }),
      ),
    [editor, userSuggest, t],
  );

  return (
    <SuggestionMenuController triggerCharacter={"@"} getItems={suggestUsers} />
  );
}
