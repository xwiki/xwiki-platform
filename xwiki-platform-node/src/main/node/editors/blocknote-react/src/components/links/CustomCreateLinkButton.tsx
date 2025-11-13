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
import { LinkEditor } from "./LinkEditor";
import { useEditor } from "../../hooks";
import { formatKeyboardShortcut } from "@blocknote/core";
import { useComponentsContext, useDictionary } from "@blocknote/react";
import { useCallback, useState } from "react";
import { RiLink } from "react-icons/ri";
import type { LinkEditionContext } from "../../misc/linkSuggest";

export type CustomCreateLinkButtonProps = {
  linkEditionCtx: LinkEditionContext;
};

export const CustomCreateLinkButton: React.FC<CustomCreateLinkButtonProps> = ({
  linkEditionCtx,
}) => {
  const editor = useEditor();
  const Components = useComponentsContext()!;
  const dict = useDictionary();

  const [opened, setOpened] = useState(false);

  const insertLink = useCallback(
    (url: string) => {
      editor.createLink(url);
      editor.focus();
    },
    [editor],
  );

  // TODO: check if we need to update in realtime when the selection change?
  const selected = editor.getSelectedText();

  return (
    <Components.Generic.Popover.Root opened={opened}>
      <Components.Generic.Popover.Trigger>
        {/* TODO: hide tooltip on click
              (note: this comment is from BlockNote's source code but may remain relevant here) */}
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
          onClick={() => setOpened(true)}
        />
      </Components.Generic.Popover.Trigger>
      <Components.Generic.Popover.Content
        className={"bn-popover-content bn-form-popover"}
        variant={"form-popover"}
      >
        <LinkEditor
          creationMode
          linkEditionCtx={linkEditionCtx}
          current={{
            title: selected,
            url: "",
          }}
          updateLink={({ url }) => insertLink(url)}
        />
      </Components.Generic.Popover.Content>
    </Components.Generic.Popover.Root>
  );
};
