import { describe, expect, it } from "vitest";
import { shallowMount } from "@vue/test-utils";
import XAlert from "../x-alert.vue";

describe("x-alert", () => {
  it("has a description", () => {
    const xAlert = shallowMount(XAlert, {
      props: {
        title: "My Title",
        type: "warning",
        description: "My description",
      },
    });
    expect(xAlert.attributes("title")).eq("My Title");
    expect(xAlert.text()).eq("My description");
  });
});
