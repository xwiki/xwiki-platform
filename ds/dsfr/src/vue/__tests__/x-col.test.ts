import { describe, expect, it } from "vitest";
import { shallowMount } from "@vue/test-utils";
import XCol from "../x-col.vue";

describe("x-col", () => {
  it("contains the provided default slot", () => {
    const slotHtml = "<strong>inner</strong>";
    const xCol = shallowMount(XCol, {
      slots: {
        default: slotHtml,
      },
    });
    expect(xCol.element.children.length).toBe(1);
    expect(xCol.element.children[0].outerHTML).toBe(slotHtml);
  });
});
