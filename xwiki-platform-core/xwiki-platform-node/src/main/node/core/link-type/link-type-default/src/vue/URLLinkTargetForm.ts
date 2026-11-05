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
import { translations } from "../translations";
import { PlainTextLinkTargetForm } from "@xwiki/platform-link-modal-ui";
import { defineComponent, h } from "vue";
import { useI18n } from "vue-i18n";
import type {
  LinkData,
  PlainTextLinkTargetConfig,
} from "@xwiki/platform-link-type-api";
import type { PropType } from "vue";

/**
 * The form of the URL link target type: the plain text form of the link modal, with a URL field.
 */
const URLLinkTargetForm = defineComponent({
  props: {
    linkData: { type: Object as PropType<LinkData>, required: true },
    modelValue: {
      type: Object as PropType<PlainTextLinkTargetConfig>,
      required: true,
    },
  },
  emits: ["update:modelValue"],
  setup(props, { emit }) {
    const { t } = useI18n({ messages: translations });
    return () =>
      h(PlainTextLinkTargetForm, {
        linkData: props.linkData,
        modelValue: props.modelValue,
        "onUpdate:modelValue": (config: PlainTextLinkTargetConfig) =>
          emit("update:modelValue", config),
        fieldLabel: t("link-type.target-types.url.url"),
        dataTest: "linkUrl",
        inputType: "url",
      });
  },
});

export { URLLinkTargetForm };
