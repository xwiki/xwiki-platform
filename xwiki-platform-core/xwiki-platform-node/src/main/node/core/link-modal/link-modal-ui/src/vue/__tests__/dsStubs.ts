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
import { defineComponent } from "vue";

// Minimal stand-ins for the design system components used by the link modal and the link forms. XTextFieldStub mirrors
// the contract the real component (and the one used by the blocknote-headless Playwright fixtures) must honor: a
// *single* root element (inheritAttrs: false, so a stray attribute doesn't land twice) wrapping the actual focusable
// input -- this is exactly the shape LinkForm.vue's onMounted hook relies on via
// `$el.querySelector("input, textarea, select")`.
const XTextFieldStub = defineComponent({
  inheritAttrs: false,
  props: ["modelValue", "label"],
  emits: ["update:modelValue"],
  template: `
    <div>
      <label>{{ label }}</label>
      <input v-bind="$attrs" :value="modelValue"
        @input="$emit('update:modelValue', $event.target.value)" />
    </div>
  `,
});

const XSelectStub = defineComponent({
  inheritAttrs: false,
  props: ["modelValue", "label", "items"],
  emits: ["update:modelValue"],
  template: `
    <select v-bind="$attrs" :value="modelValue"
      @change="$emit('update:modelValue', $event.target.value)">
      <option v-for="item in items" :key="item.value" :value="item.value">{{ item.label }}</option>
    </select>
  `,
});

const XCheckboxStub = defineComponent({
  inheritAttrs: false,
  props: ["modelValue", "label"],
  emits: ["update:modelValue"],
  template: `
    <input type="checkbox" v-bind="$attrs" :checked="modelValue"
      @change="$emit('update:modelValue', $event.target.checked)" />
  `,
});

// The attributes (e.g. data-test) and the click listener of the button fall through to its root element.
const XBtnStub = defineComponent({
  props: ["variant"],
  template: `<button type="button"><slot /></button>`,
});

/**
 * The design system components used by the link modal and the link forms, to pass as global components when mounting
 * them.
 */
const dsComponentStubs = {
  "x-text-field": XTextFieldStub,
  "x-select": XSelectStub,
  "x-checkbox": XCheckboxStub,
  "x-btn": XBtnStub,
};

export { dsComponentStubs };
