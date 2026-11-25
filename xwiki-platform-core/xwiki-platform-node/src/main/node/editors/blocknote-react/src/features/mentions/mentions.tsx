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
import { MentionSuggestionController } from "./MentionSuggestionController";
import { openMentionMenu } from "./mentionMenu";
import { getUserSuggester } from "./userSuggester";
import { mentionFactoryName } from "../../services/mentions";
import { createElement } from "react";
import { RiAtLine } from "react-icons/ri";
import type { EditorType } from "../../blocknote";
import type {
  EditorFeature,
  FeatureContext,
  FeatureContribution,
  SlashMenuItem,
} from "../editorFeature";

/**
 * @param editor - the editor where the mention is inserted
 * @param t - translates the given key, in the language of the editor UI
 * @returns the "Mention" item of the slash menu, which opens the mention menu: a way in for a user who doesn't know
 *   the mention trigger
 */
function mentionSlashMenuItem(
  editor: EditorType,
  t: (key: string) => string,
): SlashMenuItem {
  return {
    title: t("blocknote.slashMenu.mention.title"),
    subtext: t("blocknote.slashMenu.mention.subtext"),
    aliases: ["mention", "user"],
    group: t("blocknote.slashMenu.mention.group"),
    icon: createElement(RiAtLine),
    onItemClick: () => openMentionMenu(editor),
  };
}

/**
 * Mentions a user in the edited content. The mention trigger and the "Mention" item of the slash menu open a menu
 * that suggests the users returned by the `UserSuggester` of the backend. Picking one hands it to the
 * `MentionFactory` bound in the service container, and the inline content it returns is inserted at the selection.
 * Looking a user up and representing a mention are each backend specific, so this feature contributes nothing unless
 * the host application provides both and the syntax of the edited content can save a mention.
 *
 * @since 19.0.0RC1
 * @beta
 */
const mentionsFeature: EditorFeature = {
  id: "mentions",
  contribute({
    serviceContainer,
    syntax,
    t,
    getInstance,
  }: FeatureContext): FeatureContribution | undefined {
    if (
      !syntax.features.inlineContents.mentions ||
      !getUserSuggester(serviceContainer) ||
      !serviceContainer.isBound(mentionFactoryName)
    ) {
      return undefined;
    }
    return {
      slashMenuItems: () => [mentionSlashMenuItem(getInstance().editor, t)],
      components: <MentionSuggestionController />,
    };
  },
};

export { mentionsFeature };
