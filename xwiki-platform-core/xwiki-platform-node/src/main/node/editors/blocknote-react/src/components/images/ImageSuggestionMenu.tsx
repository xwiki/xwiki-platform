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
import { useEditor } from "../../hooks";
import { insertOrUpdateBlockForSlashMenu } from "@blocknote/core";
import { Button } from "@mantine/core";
import { useCallback } from "react";
import { useTranslation } from "react-i18next";
import type {
  DefaultReactSuggestionItem,
  SuggestionMenuProps,
} from "@blocknote/react";

import "./ImageSuggestionMenu.css";

// HACK: the first item is actually a placeholder for the upload button, in order to make it selectable with the keyboard
const IMAGE_SUGGESTION_UPLOAD_BTN_TITLE_PLACEHOLDER = "#imageSuggestionHack";

// This component renders the image suggestions menu
//
// The first item is a placeholder for the upload button ; it needs to be
// provided by the suggestions function as BlockNote will use it for keyboard
// navigation.
// The first item *must* use the IMAGE_SUGGESTION_UPLOAD_BTN_TITLE_PLACEHOLDER
// constant for its `subtext` property.
//
// In order to access all items' URL to render them, they *must* be provided
// through the items' `subtext` property. Items with no `subtext` (e.g. a "search
// isn't supported" message) are rendered as plain, non-clickable text instead.

// eslint-disable-next-line max-statements
function ImageSuggestionMenu(
  props: SuggestionMenuProps<DefaultReactSuggestionItem>,
) {
  const editor = useEditor();

  const { t } = useTranslation();

  const onSelected = useCallback(
    (url: string) => {
      insertOrUpdateBlockForSlashMenu(editor, {
        type: "image",
        props: { url },
      });
    },
    [editor],
  );

  // NOTE: required as the menu initially opens with no content
  if (props.items.length === 0) {
    return null;
  }

  if (props.items[0].title !== IMAGE_SUGGESTION_UPLOAD_BTN_TITLE_PLACEHOLDER) {
    throw new Error(
      "Expected first item to be the placeholder for upload button",
    );
  }

  const [uploadItem, ...suggestionItems] = props.items;

  if (
    suggestionItems.find(
      (item) => item.subtext !== undefined && !item.subtext.startsWith("http"),
    )
  ) {
    throw new Error(
      "Expected image suggestion items to either have no subtext (a message item) or a URL as their subtext",
    );
  }

  return (
    <div className="slash-menu">
      <div
        className={`slash-menu-item ${props.selectedIndex === 0 ? "selected" : ""}`}
      >
        <Button variant="default" onClick={() => uploadItem.onItemClick()}>
          {t("blocknote.imageSelector.uploadButton")}
        </Button>
      </div>

      {suggestionItems.map((item, index) => (
        <div
          key={item.title}
          className={`slash-menu-item ${
            props.selectedIndex === index + 1 ? "selected" : ""
          }`}
          onClick={() => {
            if (item.subtext) {
              onSelected(item.subtext);
            }
          }}
        >
          {item.subtext && <img src={item.subtext} />}
          <span>{item.title}</span>
        </div>
      ))}
    </div>
  );
}

export { IMAGE_SUGGESTION_UPLOAD_BTN_TITLE_PLACEHOLDER, ImageSuggestionMenu };
