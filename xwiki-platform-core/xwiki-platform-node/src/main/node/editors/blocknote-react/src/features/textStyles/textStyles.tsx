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
import { BooleanStyleButton } from "./BooleanStyleButton";
import { SubscriptStyle, SuperscriptStyle } from "./textStyleSpecs";
import { RiSubscript, RiSuperscript } from "react-icons/ri";
import type {
  EditorFeature,
  FeatureContext,
  FeatureContribution,
} from "../editorFeature";

/**
 * The subscript and superscript text styles, with their formatting toolbar buttons, each contributed only when the
 * syntax of the edited content supports it.
 *
 * @since 18.10.0RC1
 * @beta
 */
const textStylesFeature: EditorFeature = {
  id: "textStyles",
  contribute({ syntax, t }: FeatureContext): FeatureContribution | undefined {
    const { subscript, superscript } =
      syntax.features.inlineContents.textStyles;
    if (!subscript && !superscript) {
      return undefined;
    }
    return {
      schema: {
        styleSpecs: {
          ...(subscript ? { subscript: SubscriptStyle } : {}),
          ...(superscript ? { superscript: SuperscriptStyle } : {}),
        },
      },
      formattingToolbar: {
        items: [
          ...(subscript
            ? [
                <BooleanStyleButton
                  style="subscript"
                  icon={<RiSubscript />}
                  label={t("blocknote.toolbar.subscript")}
                  key="subscriptStyleButton"
                />,
              ]
            : []),
          ...(superscript
            ? [
                <BooleanStyleButton
                  style="superscript"
                  icon={<RiSuperscript />}
                  label={t("blocknote.toolbar.superscript")}
                  key="superscriptStyleButton"
                />,
              ]
            : []),
        ],
      },
    };
  },
};

export { textStylesFeature };
