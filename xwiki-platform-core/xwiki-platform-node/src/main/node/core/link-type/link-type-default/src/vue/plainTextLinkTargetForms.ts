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
import PlainTextLinkTargetForm from "./PlainTextLinkTargetForm.vue";
import { defineComponent, h } from "vue";
import type { PlainTextLinkTargetConfig } from "../data/linkType";
import type { TextFieldProps } from "@xwiki/platform-dsapi";
import type { LinkData } from "@xwiki/platform-link-type-api";
import type { Component, PropType } from "vue";

/**
 * Create the form of a link target type designated by a plain text location. The link modal passes only the link data
 * and the configuration (`v-model`) to the form of a link target type, so the created form passes them on to the
 * shared form, along with what differs between these link target types.
 *
 * @param fieldLabelKey - the translation key of the label of the location field
 * @param dataTest - the value of the `data-test` attribute of the location field
 * @param inputType - the type of the location field
 * @returns the form component
 */
function plainTextLinkTargetForm(
  fieldLabelKey: string,
  dataTest: string,
  inputType?: TextFieldProps["type"],
): Component {
  return defineComponent({
    props: {
      linkData: { type: Object as PropType<LinkData>, required: true },
      modelValue: {
        type: Object as PropType<PlainTextLinkTargetConfig>,
        required: true,
      },
    },
    emits: ["update:modelValue"],
    setup(props, { emit }) {
      return () =>
        h(PlainTextLinkTargetForm, {
          linkData: props.linkData,
          modelValue: props.modelValue,
          "onUpdate:modelValue": (config: PlainTextLinkTargetConfig) =>
            emit("update:modelValue", config),
          fieldLabelKey,
          dataTest,
          inputType,
        });
    },
  });
}

const URLLinkTargetForm = plainTextLinkTargetForm(
  "link-type.target-types.url.url",
  "linkUrl",
  "url",
);

const PathLinkTargetForm = plainTextLinkTargetForm(
  "link-type.target-types.path.path",
  "linkPath",
);

const UNCLinkTargetForm = plainTextLinkTargetForm(
  "link-type.target-types.unc.path",
  "linkUNCPath",
);

export { PathLinkTargetForm, UNCLinkTargetForm, URLLinkTargetForm };
