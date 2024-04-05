import { describe, expect, it } from "vitest";
import { shallowMount } from "@vue/test-utils";
import CIcon from "../c-icon.vue";

describe("c-icon", () => {
  it("has a name", () => {
    const icon = shallowMount(CIcon, {
      props: {
        name: "test",
      },
    });
    expect(icon.classes()).toMatchObject(["bi-test"]);
  });
});
