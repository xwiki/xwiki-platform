/*
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
import { config, mount } from "@vue/test-utils";
import { describe, expect, it } from "vitest";
import BaseAction from "./BaseAction.vue";
import sinon from "sinon";
import _ from "lodash-es";

config.global.mocks = {
  $t: tKey => tKey,
};

function initWrapper(options) {
  return mount(BaseAction, _.merge({
    props: {
      titleTranslationKey: "title.translation.key",
      closePopover: () => {
      },
    },
    global: {
      mocks: {
        $t: (key) => key,
      },
    },
  }, options));
}

describe("BaseAction.vue", () => {
  it("Renders with no label and icon", () => {
    const wrapper = initWrapper();

    expect(wrapper.attributes("title")).toBe("title.translation.key");
    expect(wrapper.text()).toBe("");
  });

  it("Renders with a label and no icon", () => {
    const wrapper = initWrapper({
      props: {
        labelTranslationKey: "label.translation.key",
      },
    });

    expect(wrapper.attributes("title")).toBe("title.translation.key");
    expect(wrapper.text()).toBe("label.translation.key");
  });

  it("Renders with an icon and no label", () => {
    const wrapper = initWrapper({
      props: {
        iconDescriptor: {
          cssClass: "fa-add",
        },
      },
    });

    expect(wrapper.attributes("title")).toBe("title.translation.key");
    expect(wrapper.find("span span").attributes("class")).toBe("fa-add");
    expect(wrapper.text()).toBe("");
  });

  it("Handler is called on click", async () => {
    const mockFn = sinon.fake();

    const wrapper = initWrapper({
      props: {
        handler: mockFn,
      },
    });

    await wrapper.trigger("click");

    expect(mockFn.callCount).to.be.gte(1);
  });
});
