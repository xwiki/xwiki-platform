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
import LinkConfig from "../LinkConfig.vue";
import { mount } from "@vue/test-utils";
import { afterEach, describe, expect, test } from "vitest";
import { defineComponent, nextTick } from "vue";
import { createI18n } from "vue-i18n";
import type { LinkData } from "../../data/linkType";

// Minimal stand-ins for the design system components used by LinkConfig.vue. XTextFieldStub mirrors the contract
// the real component (and the one used by the blocknote-headless Playwright fixtures) must honor: a *single* root
// element (inheritAttrs: false, so a stray attribute doesn't land twice) wrapping the actual focusable input -- this
// is exactly the shape LinkConfig.vue's onMounted hook relies on via `$el.querySelector("input, textarea, select")`.
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
      <option v-for="item in items" :key="item" :value="item">{{ item }}</option>
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

function buildLinkData(): LinkData {
  return {
    displayText: "",
    newTab: false,
    target: { type: "url", config: { url: "" } },
  };
}

/** Waits past the double requestAnimationFrame used by LinkConfig.vue's onMounted focus logic. */
async function settle(): Promise<void> {
  await nextTick();
  await new Promise<void>((resolve) =>
    requestAnimationFrame(() => requestAnimationFrame(() => resolve())),
  );
}

function mountLinkConfig() {
  // LinkConfig.vue calls useI18n() with its own local `messages`, but vue-i18n still requires a global instance to
  // be installed for that local scope to be created against.
  const i18n = createI18n({ legacy: false, locale: "en", messages: {} });

  return mount(LinkConfig, {
    props: { linkData: buildLinkData() },
    attachTo: document.body,
    global: {
      plugins: [i18n],
      components: {
        "x-text-field": XTextFieldStub,
        "x-select": XSelectStub,
        "x-checkbox": XCheckboxStub,
      },
    },
  });
}

describe("LinkConfig.vue", () => {
  afterEach(() => {
    document.body.innerHTML = "";
  });

  test("focuses the display text field once mounted", async () => {
    const wrapper = mountLinkConfig();

    await settle();

    const input = wrapper.get('[data-test="linkDisplayText"]').element;
    expect(document.activeElement).toBe(input);
  });

  test("steals focus back from an element focused just before it settles", async () => {
    // Simulates the modal opening while a rich text editor still holds focus (e.g. after picking the link quick
    // action from a slash menu) and that editor synchronously reclaiming focus right after.
    const editorStandIn = document.createElement("div");
    editorStandIn.tabIndex = 0;
    document.body.appendChild(editorStandIn);

    const wrapper = mountLinkConfig();
    editorStandIn.focus();
    expect(document.activeElement).toBe(editorStandIn);

    await settle();

    const input = wrapper.get('[data-test="linkDisplayText"]').element;
    expect(document.activeElement).toBe(input);
  });
});
