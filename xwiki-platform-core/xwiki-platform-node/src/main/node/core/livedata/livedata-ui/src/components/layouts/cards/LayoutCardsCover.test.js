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
import LayoutCardsCover from "./LayoutCardsCover.vue";
import LivedataDisplayer from "../../displayers/LivedataDisplayer.vue";
import { shallowMount } from "@vue/test-utils";
import { describe, expect, it } from "vitest";
import { nextTick } from "vue";

/**
 * @param props - the props of the cover
 * @returns a shallow wrapper for the LayoutCardsCover component
 */
function initWrapper(props) {
  return shallowMount(LayoutCardsCover, {
    props: { entry: { id: "idA", title: "Title A" }, ...props },
    global: {
      provide: {
        logic: { isEditable: () => true, getEntryId: (e) => e.id },
      },
    },
  });
}

describe("LayoutCardsCover.vue", () => {
  it("Displays the image", async () => {
    const wrapper = initWrapper({ image: { src: "/a.png", alt: "A" } });
    // The image is displayed once the cover is measured.
    await nextTick();
    const image = wrapper.find("img");
    expect(image.attributes("src")).toBe("/a.png");
    expect(image.attributes("alt")).toBe("A");
    expect(wrapper.classes()).not.toContain("card-cover-text");
    expect(wrapper.findComponent(LivedataDisplayer).exists()).toBe(false);
  });

  it("Notifies when the image fails to load", async () => {
    const wrapper = initWrapper({ image: { src: "/a.png", alt: "" } });
    await nextTick();
    await wrapper.find("img").trigger("error");
    expect(wrapper.emitted("imageError")).toHaveLength(1);
  });

  it("Displays the text of a property when there is no image", () => {
    const wrapper = initWrapper({ textPropertyId: "title" });
    expect(wrapper.find("img").exists()).toBe(false);
    expect(wrapper.classes()).toContain("card-cover-text");
    expect(wrapper.findComponent(LivedataDisplayer).props()).toMatchObject({
      propertyId: "title",
      entry: { id: "idA", title: "Title A" },
    });
  });

  it("Provides a read-only logic to the displayer", () => {
    const wrapper = initWrapper({ textPropertyId: "title" });
    const providedLogic = wrapper.vm.$.provides.logic;
    expect(providedLogic.isEditable({ propertyId: "title" })).toBe(false);
    expect(providedLogic.getEntryId({ id: "idB" })).toBe("idB");
  });
});
