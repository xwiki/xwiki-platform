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
import { ImageUploadButton } from "./ImageUploadButton";
import { useEditor } from "../../hooks";
import { insertOrUpdateBlockForSlashMenu } from "@blocknote/core";
import { useCallback } from "react";
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
// through the items' `subtext` property.
//
// The items' `onItemClick` property is also ignored.

function ImageSuggestionMenu(
  props: SuggestionMenuProps<DefaultReactSuggestionItem>,
) {
  const editor = useEditor();

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

  if (
    props.items
      .slice(1)
      .find((item) => !item.subtext || !item.subtext.startsWith("http"))
  ) {
    throw new Error(
      "Expected all image suggestion items to have URLs as subtexts",
    );
  }

  return (
    <div className="slash-menu">
      <div className="slash-menu-upload">
        <ImageUploadButton onUploaded={onSelected} />
      </div>

      {props.items.slice(1, 5).map((item, index) => (
        <div
          key={item.title}
          className={`slash-menu-item ${
            props.selectedIndex === index ? "selected" : ""
          }`}
          onClick={() => {
            onSelected(item.subtext!);
          }}
        >
          <img src={item.subtext} />
          <span>{item.title}</span>
        </div>
      ))}
    </div>
  );
}

export { IMAGE_SUGGESTION_UPLOAD_BTN_TITLE_PLACEHOLDER, ImageSuggestionMenu };
