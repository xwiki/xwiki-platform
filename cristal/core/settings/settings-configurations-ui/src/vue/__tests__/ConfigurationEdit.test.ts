/**
 * See the LICENSE file distributed with this work for additional
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

import ConfigurationEdit from "../ConfigurationEdit.vue";
import { flushPromises, mount } from "@vue/test-utils";
import { mockI18n } from "@xwiki/cristal-dev-test-utils";
import { ConfigurationsSettings } from "@xwiki/cristal-settings-configurations";
import { Container } from "inversify";
import { describe, expect, it, vi } from "vitest";
import { mock } from "vitest-mock-extended";
import type { Configuration } from "@xwiki/cristal-configuration-api";
import type {
  SettingsManager,
  SettingsStorage,
} from "@xwiki/cristal-settings-api";
import type { WikiConfigProxy } from "@xwiki/cristal-wiki-config-api";
import type { MockProxy } from "vitest-mock-extended";

let settingsManagerConfigurations: Map<string, Configuration>;
let mockWikiConfigProxy: MockProxy<WikiConfigProxy> & WikiConfigProxy;
let mockSettingsManager: MockProxy<SettingsManager> & SettingsManager;
let mockSettingsStorage: MockProxy<SettingsStorage> & SettingsStorage;

// eslint-disable-next-line max-statements
function mountConfigurationEdit(params: {
  configurations: Map<string, Configuration>;
  configurationName: string;
}) {
  const containerMock = mock<Container>();

  settingsManagerConfigurations = params.configurations;

  mockWikiConfigProxy = mock<WikiConfigProxy>();
  mockSettingsManager = mock<SettingsManager>();
  mockSettingsStorage = mock<SettingsStorage>();

  mockSettingsManager.get.mockReturnValue(
    new ConfigurationsSettings(settingsManagerConfigurations),
  );

  containerMock.get
    .calledWith("WikiConfigProxy")
    .mockReturnValue(mockWikiConfigProxy);
  containerMock.get
    .calledWith("SettingsManager")
    .mockReturnValue(mockSettingsManager);
  containerMock.get
    .calledWith("SettingsStorage")
    .mockReturnValue(mockSettingsStorage);

  vi.mock("vue-i18n");
  mockI18n();

  return mount(ConfigurationEdit, {
    props: params,
    global: {
      provide: {
        cristal: {
          getContainer() {
            return containerMock;
          },
        },
      },
      stubs: {
        ConfigurationEdit: false,
        XBtn: {
          props: {
            type: String,
            variant: String,
          },
          template: "<input :type='type'><slot></slot></input>",
        },
        XDialog: {
          template: "<div><slot></slot></div>",
        },
        XForm: {
          emits: ["formSubmit"],
          template: `<form @submit.prevent="$emit('formSubmit')"><slot></slot></form>`,
        },
        XSelect: {
          props: {
            label: String,
            modelValue: String,
            items: Array<string>,
          },
          emits: ["update:modelValue"],
          template: `<div>
    <select :id="label" :value="modelValue" @input="$emit('update:modelValue', $event.target.value)">
      <option v-for="item in items" :value="item">{{ item }}</option>
    </select>
  </div>`,
        },
        XTextField: {
          props: {
            label: String,
            modelValue: String,
          },
          emits: ["update:modelValue"],
          template: `<div>
    <input :id="label" :value="modelValue" @input="$emit('update:modelValue', $event.target.value)" />
  </div>`,
        },
      },
    },
  });
}

describe("ConfigurationEdit", () => {
  // eslint-disable-next-line max-statements
  it("Checks stored values from form", async () => {
    const configurationEdit = mountConfigurationEdit({
      configurations: new Map<string, Configuration>([
        [
          "TestConfiguration",
          {
            name: "TestConfiguration",
            configType: "XWiki",
            homePage: "Hello World",
            designSystem: "shoelace",
          },
        ],
      ]),
      configurationName: "",
    });

    // Wait for all the asynchronous operations to be terminated before starting
    // to assert the rendered content.
    await flushPromises();

    await configurationEdit.setProps({
      configurationName: "TestConfiguration",
    });

    await configurationEdit
      .find("input[id=settings.configurations.edit.homepage.label]")
      .setValue("Hello World 2");
    await configurationEdit
      .find("input[id=settings.configurations.edit.storageroot.label]")
      .setValue("/");
    await configurationEdit
      .find("select[id=settings.configurations.edit.designsystem.label]")
      .setValue("vuetify");
    await configurationEdit.find("form").trigger("submit");
    await flushPromises();

    const expectedTestConfiguration = {
      name: "TestConfiguration",
      configType: "XWiki",
      homePage: "Hello World 2",
      designSystem: "vuetify",
      storageRoot: "/",
    };

    expect(settingsManagerConfigurations).toStrictEqual(
      new Map<string, Configuration>([
        ["TestConfiguration", expectedTestConfiguration],
      ]),
    );
    expect(mockSettingsStorage.save).toHaveBeenCalledWith(mockSettingsManager);
    expect(mockWikiConfigProxy.setAvailableConfigurations).toHaveBeenCalledWith(
      {
        TestConfiguration: expectedTestConfiguration,
      },
    );
  });
});
