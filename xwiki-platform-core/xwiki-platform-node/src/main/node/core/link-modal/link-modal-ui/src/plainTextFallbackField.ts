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
import { useLinkTargetTypeLabels } from "./linkTargetTypeLabels";
import PlainTextLinkTargetField from "./vue/PlainTextLinkTargetField.vue";
import { defineComponent, h } from "vue";
import { useI18n } from "vue-i18n";
import type {
  LinkData,
  LinkTargetType,
  PlainTextLinkTargetConfig,
} from "@xwiki/platform-link-type-api";
import type { Component, PropType } from "vue";

/**
 * Create the target field of a link target type registered without a form: its configuration is a plain text location
 * (see `PlainTextLinkTargetConfig`), edited with a plain text field labelled with the label of the link target type.
 *
 * @param targetType - the link target type registered without a form
 * @returns the target field component
 */
function plainTextFallbackField(targetType: LinkTargetType): Component {
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
      const { locale } = useI18n();
      const labels = useLinkTargetTypeLabels([targetType], locale);
      return () =>
        h(PlainTextLinkTargetField, {
          linkData: props.linkData,
          modelValue: props.modelValue,
          "onUpdate:modelValue": (config: PlainTextLinkTargetConfig) =>
            emit("update:modelValue", config),
          fieldLabel: labels.value[targetType.type],
          dataTest: "linkLocation",
        });
    },
  });
}

export { plainTextFallbackField };
