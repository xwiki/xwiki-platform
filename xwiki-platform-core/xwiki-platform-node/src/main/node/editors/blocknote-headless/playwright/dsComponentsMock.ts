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

// The design system's components (e.g. <x-text-field>) are provided at runtime by whichever
// application embeds the editor (they are only typed, not implemented, in `@xwiki/platform-dsapi`).
// These minimal stand-ins render real form elements so Playwright component tests can interact
// with them, without pulling in a full design system implementation.

import { defineComponent, h } from "vue";
import type { App, PropType } from "vue";

const XTextField = defineComponent({
  name: "XTextField",
  inheritAttrs: false,
  props: {
    label: { type: String, required: true },
    modelValue: { type: String, default: "" },
    type: { type: String, default: "text" },
    required: { type: Boolean, default: false },
    readonly: { type: Boolean, default: false },
  },
  emits: ["update:modelValue"],
  setup(props, { attrs, emit }) {
    return () =>
      h("input", {
        ...attrs,
        "aria-label": props.label,
        type: props.type,
        required: props.required,
        readonly: props.readonly,
        value: props.modelValue,
        onInput: (event: Event) =>
          emit("update:modelValue", (event.target as HTMLInputElement).value),
      });
  },
});

const XSelect = defineComponent({
  name: "XSelect",
  inheritAttrs: false,
  props: {
    label: { type: String, required: true },
    items: { type: Array as PropType<string[]>, required: true },
    modelValue: { type: String, default: undefined },
    required: { type: Boolean, default: false },
  },
  emits: ["update:modelValue"],
  setup(props, { attrs, emit }) {
    return () =>
      h(
        "select",
        {
          ...attrs,
          "aria-label": props.label,
          required: props.required,
          value: props.modelValue,
          onChange: (event: Event) =>
            emit(
              "update:modelValue",
              (event.target as HTMLSelectElement).value,
            ),
        },
        props.items.map((item) => h("option", { key: item, value: item }, item)),
      );
  },
});

const XBtn = defineComponent({
  name: "XBtn",
  inheritAttrs: false,
  props: {
    variant: { type: String, default: undefined },
  },
  setup(_props, { attrs, slots }) {
    return () => h("button", { ...attrs, type: "button" }, slots.default?.());
  },
});

const XCheckbox = defineComponent({
  name: "XCheckbox",
  inheritAttrs: false,
  props: {
    label: { type: String, required: true },
    modelValue: { type: Boolean, default: false },
  },
  emits: ["update:modelValue"],
  setup(props, { attrs, emit }) {
    return () =>
      h("label", [
        h("input", {
          ...attrs,
          type: "checkbox",
          checked: props.modelValue,
          onChange: (event: Event) =>
            emit(
              "update:modelValue",
              (event.target as HTMLInputElement).checked,
            ),
        }),
        props.label,
      ]);
  },
});

export function registerDsComponentsMock(app: App): void {
  app.component("XTextField", XTextField);
  app.component("XSelect", XSelect);
  app.component("XBtn", XBtn);
  app.component("XCheckbox", XCheckbox);
}
