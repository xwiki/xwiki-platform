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
import { dsComponentStubs } from "./dsStubs";
import LinkForm from "../LinkForm.vue";
import { flushPromises, mount } from "@vue/test-utils";
import { afterEach, describe, expect, test } from "vitest";
import { nextTick } from "vue";
import { createI18n } from "vue-i18n";
import type { LinkData, LinkTargetType } from "@xwiki/platform-link-type-api";

// LinkForm.vue only uses the registered link target types to fill the type selector and to reset the link target
// when another type is selected; the parsing/serializing part of the contract is never exercised here.
function targetTypeStub(
  type: string,
  getLabel: LinkTargetType["getLabel"],
  defaultConfig: unknown = {},
): LinkTargetType {
  return {
    type,
    getLabel,
    createDefaultConfig: () => defaultConfig,
    fromURL: () => null,
    toURL: () => "",
  };
}

const urlTargetTypeStub = targetTypeStub("url", async () => "URL", { url: "" });

function buildLinkData(): LinkData {
  return {
    displayText: "",
    target: { type: "url", config: { url: "" } },
  };
}

/** Waits past the double requestAnimationFrame used by LinkForm.vue's onMounted focus logic. */
async function settle(): Promise<void> {
  await nextTick();
  await new Promise<void>((resolve) =>
    requestAnimationFrame(() => requestAnimationFrame(() => resolve())),
  );
}

function mountLinkForm(
  targetTypes: LinkTargetType[] = [urlTargetTypeStub],
  linkData: LinkData = buildLinkData(),
  defaultDisplayText?: string,
) {
  // LinkForm.vue calls useI18n() with its own local `messages`, but vue-i18n still requires a global instance to
  // be installed for that local scope to be created against.
  const i18n = createI18n({ legacy: false, locale: "en", messages: {} });

  return mount(LinkForm, {
    props: { linkData, linkTargetTypes: targetTypes, defaultDisplayText },
    // The target field is provided by the selected link target type, which the link modal asks to mark its main input
    // with autofocus.
    slots: {
      target: `<div><input /><input data-test="target" autofocus /></div>`,
    },
    attachTo: document.body,
    global: {
      plugins: [i18n],
      components: dsComponentStubs,
    },
  });
}

describe("LinkForm.vue", () => {
  afterEach(() => {
    document.body.innerHTML = "";
  });

  test("focuses the input of the target field marked with autofocus once mounted", async () => {
    const wrapper = mountLinkForm();

    await settle();

    const input = wrapper.get('[data-test="target"]').element;
    expect(document.activeElement).toBe(input);
  });

  test("offers the display text as an option, which hints at the text the link gets when it's left empty", () => {
    const wrapper = mountLinkForm(undefined, undefined, "Some page");

    const displayText = wrapper.get('[data-test="linkDisplayText"]');
    expect(displayText.attributes("placeholder")).toBe("Some page");
    expect(displayText.attributes("required")).toBeUndefined();
    expect(displayText.element.closest("details.options")).not.toBeNull();
  });

  test("doesn't mark the link type as required, since it always has a value, but tells assistive technologies", () => {
    const wrapper = mountLinkForm();

    const select = wrapper.get('[data-test="linkTargetType"]');
    // The select stub doesn't declare a required prop: the attribute would fall through if the prop was passed.
    expect(select.attributes("required")).toBeUndefined();
    expect(select.attributes("aria-required")).toBe("true");
  });

  test("steals focus back from an element focused just before it settles", async () => {
    // Simulates the modal opening while a rich text editor still holds focus (e.g. after picking the link quick
    // action from a slash menu) and that editor synchronously reclaiming focus right after.
    const editorStandIn = document.createElement("div");
    editorStandIn.tabIndex = 0;
    document.body.appendChild(editorStandIn);

    const wrapper = mountLinkForm();
    editorStandIn.focus();
    expect(document.activeElement).toBe(editorStandIn);

    await settle();

    const input = wrapper.get('[data-test="target"]').element;
    expect(document.activeElement).toBe(input);
  });

  test("identifies the link target types by their type, even when they have the same label", async () => {
    const wrapper = mountLinkForm([
      urlTargetTypeStub,
      targetTypeStub("first", async () => "Same label"),
      targetTypeStub("second", async () => "Same label"),
    ]);

    await flushPromises();

    const options = wrapper
      .get('[data-test="linkTargetType"]')
      .findAll("option")
      .map((option) => [option.attributes("value"), option.text()]);
    expect(options).toEqual([
      ["url", "URL"],
      ["first", "Same label"],
      ["second", "Same label"],
    ]);
  });

  test("displays the type of a link target type until its label is resolved, or when it fails to resolve", async () => {
    let resolveLabel: (label: string) => void = () => {};
    const wrapper = mountLinkForm([
      targetTypeStub(
        "url",
        () => new Promise((resolve) => (resolveLabel = resolve)),
      ),
      targetTypeStub("broken", async () => {
        throw new Error("Failed to load the translation");
      }),
    ]);
    const optionTexts = () =>
      wrapper
        .get('[data-test="linkTargetType"]')
        .findAll("option")
        .map((option) => option.text());

    await flushPromises();
    expect(optionTexts()).toEqual(["url", "broken"]);

    resolveLabel("URL");
    await flushPromises();
    expect(optionTexts()).toEqual(["URL", "broken"]);
  });

  test("resets the link target to the default configuration of the selected type", async () => {
    const linkData: LinkData = {
      displayText: "",
      target: { type: "url", config: { url: "https://www.xwiki.org" } },
    };
    const wrapper = mountLinkForm(
      [
        urlTargetTypeStub,
        targetTypeStub("first", async () => "Same label", { first: true }),
        targetTypeStub("second", async () => "Same label", { second: true }),
      ],
      linkData,
    );
    await flushPromises();

    await wrapper.get('[data-test="linkTargetType"]').setValue("second");

    expect(linkData.target).toEqual({
      type: "second",
      config: { second: true },
    });
  });
});
