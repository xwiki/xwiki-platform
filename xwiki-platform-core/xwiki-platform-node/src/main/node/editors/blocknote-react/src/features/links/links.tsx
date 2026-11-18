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
import { CustomCreateLinkButton } from "./CustomCreateLinkButton";
import { CustomLinkToolbar } from "./CustomLinkToolbar";
import { LinkSuggestionController } from "./LinkSuggestionController";
import { openLinkSuggestionMenu } from "./linkSuggestionMenu";
import { FormattingToolbar, LinkToolbarController } from "@blocknote/react";
import { createElement } from "react";
import { RiLink } from "react-icons/ri";
import type { EditorType } from "../../blocknote";
import type {
  EditorFeature,
  FeatureContext,
  FeatureContribution,
  SlashMenuItem,
} from "../editorFeature";

/**
 * @param editor - the editor where the link is inserted
 * @param t - translates the given key, in the language of the editor UI
 * @returns the "Link" item of the slash menu, which opens the link suggestion menu: it suggests pages and attachments
 *   to link to, in addition to uploading a file to link to and opening the link editor
 */
function linkSlashMenuItem(
  editor: EditorType,
  t: (key: string) => string,
): SlashMenuItem {
  return {
    title: t("blocknote.slashMenu.link.title"),
    subtext: t("blocknote.slashMenu.link.subtext"),
    aliases: ["link"],
    group: t("blocknote.slashMenu.link.group"),
    icon: createElement(RiLink),
    onItemClick: () => openLinkSuggestionMenu(editor),
  };
}

/**
 * Inserts and edits links: the "Link" item of the slash menu and the link suggestion menu it opens, the button of the
 * formatting toolbar creating a link from the selected text, and the toolbar shown when hovering a link. The links are
 * created and edited with the `LinkEditor` bound in the service container; without one, the actions to create and edit
 * a link are hidden, the link suggestions being still available. Nothing is contributed when the syntax of the edited
 * content doesn't support links.
 *
 * @since 18.10.0RC1
 * @beta
 */
const linksFeature: EditorFeature = {
  id: "links",
  contribute({
    syntax,
    t,
    getInstance,
  }: FeatureContext): FeatureContribution | undefined {
    if (!syntax.features.inlineContents.links.basicLinks) {
      return undefined;
    }
    return {
      slashMenuItems: () => [linkSlashMenuItem(getInstance().editor, t)],
      formattingToolbar: {
        // The same appearance as BlockNote's button creating a link, but opens the link editor (and is hidden when
        // there's none).
        items: [<CustomCreateLinkButton key="createLinkButton" />],
      },
      components: (
        <>
          <LinkSuggestionController
            maxSuggestions={8}
            pageSuggestionsShare={5}
          />
          <LinkToolbarController
            linkToolbar={(props) => (
              <FormattingToolbar>
                <CustomLinkToolbar linkToolbarProps={props} />
              </FormattingToolbar>
            )}
          />
        </>
      ),
    };
  },
};

export { linksFeature };
