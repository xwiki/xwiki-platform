import { describe, expect, it } from "vitest";
import { shallowMount } from "@vue/test-utils";
import CIcon from "../c-icon.vue";
import { Size } from "../../size";

describe("c-icon", () => {
  it("has a name", () => {
    const icon = shallowMount(CIcon, {
      props: {
        name: "test",
      },
    });
    expect(icon.classes()).toMatchObject(["cr-icon", "bi-test"]);
  });

  it("is small", () => {
    const icon = shallowMount(CIcon, {
      props: {
        name: "test",
        size: Size.Small,
      },
    });
    expect(icon.classes()).toMatchObject(["cr-icon", "bi-test", "small"]);
  });
});
