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
import { MACRO_NAME_PREFIX } from "../../blocknote/utils";
import { useEditor } from "../../hooks";
import { useComponentsContext } from "@blocknote/react";
import { useMemo } from "react";
import { useTranslation } from "react-i18next";
import { RiPencilFill } from "react-icons/ri";
import type { ContextForMacros } from "../../blocknote/utils";
import type { MacroWithUnknownParamsType } from "@xwiki/platform-macros-api";

export type CustomMacroEditButtonProps = {
  macrosList: MacroWithUnknownParamsType[];
  ctxForMacros: ContextForMacros;
};

export const CustomMacroEditButton: React.FC<CustomMacroEditButtonProps> = ({
  macrosList,
  ctxForMacros,
}) => {
  const Components = useComponentsContext()!;
  const { t } = useTranslation();

  const editor = useEditor();

  const selection = editor.getSelection();

  const openMacroEditor = useMemo(() => {
    // Hide the edit action when no params editor is available.
    const openParamsEditor = ctxForMacros.openParamsEditor;

    if (
      !openParamsEditor ||
      !selection ||
      selection.blocks.length !== 1 ||
      !selection.blocks[0].type.startsWith(MACRO_NAME_PREFIX)
    ) {
      return null;
    }

    const macroBlock = selection.blocks[0];

    const macro = macrosList.find(
      (macro) =>
        macro.infos.id === macroBlock.type.replace(MACRO_NAME_PREFIX, ""),
    );

    if (!macro) {
      throw new Error(
        "Internal error: macro not found for block: " + macroBlock.type,
      );
    }

    return () => {
      openParamsEditor(macro, macroBlock.props, (newProps) =>
        editor.updateBlock(macroBlock.id, { props: newProps }),
      );
    };
  }, [selection, macrosList, ctxForMacros, editor]);

  return (
    openMacroEditor && (
      <Components.FormattingToolbar.Button
        className={"bn-button"}
        data-test="insertMacroButton"
        label={t("blocknote.linkToolbar.macros.edit.label")}
        mainTooltip={t("blocknote.linkToolbar.macros.edit.tooltip")}
        icon={<RiPencilFill />}
        onClick={openMacroEditor}
      />
    )
  );
};
