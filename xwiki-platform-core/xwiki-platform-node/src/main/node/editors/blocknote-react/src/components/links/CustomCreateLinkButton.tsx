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
import { insertLink, useLinkEditing } from "./linkEditor";
import { EditorInstanceContext } from "../../contexts";
import { selectionHasInlineContent } from "../../selection";
import { formatKeyboardShortcut } from "@blocknote/core";
import { useComponentsContext, useDictionary } from "@blocknote/react";
import { useContext } from "react";
import { RiLink } from "react-icons/ri";

export const CustomCreateLinkButton: React.FC = () => {
  const instance = useContext(EditorInstanceContext)!;
  const linkEditing = useLinkEditing();
  const Components = useComponentsContext()!;
  const dict = useDictionary();

  // Links are inline content, so they can only wrap editable inline content. Hide the button when the
  // selection has no such content (e.g. a block-level macro or other content-less block is selected),
  // mirroring BlockNote's default Create Link button. Hidden also when there's no link editor.
  if (!linkEditing || !selectionHasInlineContent(instance.editor)) {
    return null;
  }

  return (
    <Components.FormattingToolbar.Button
      className={"bn-button"}
      data-test="createLink"
      label={dict.formatting_toolbar.link.tooltip}
      mainTooltip={dict.formatting_toolbar.link.tooltip}
      secondaryTooltip={formatKeyboardShortcut(
        dict.formatting_toolbar.link.secondary_tooltip,
        dict.generic.ctrl_shortcut,
      )}
      icon={<RiLink />}
      onClick={async () => {
        // The display text is pre-filled with the text selected when the link editor is opened, which also tells, on
        // submit, whether the user changed it. When the link label differs from the selected text, the selection is
        // replaced with the new label, keeping the formatting shared by the whole selection, like typing over it does.
        const selectedText = instance.editor.getSelectedText();
        await linkEditing.insert({ title: selectedText }, (link) =>
          insertLink(instance, link, selectedText),
        );
        instance.editor.focus();
      }}
    />
  );
};
