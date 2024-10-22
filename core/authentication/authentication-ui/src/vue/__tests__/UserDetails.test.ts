/*
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

import { afterAll, beforeAll, describe, expect, it, vi } from "vitest";
import { config, flushPromises, mount } from "@vue/test-utils";
import UserDetails from "../UserDetails.vue";
import {
  makeInjectable,
  mockI18n,
  wrapInSuspense,
} from "@xwiki/cristal-dev-test-utils";
import { Container } from "inversify";
import "reflect-metadata";

function mountUserDetails(container) {
  return mount(wrapInSuspense(UserDetails, {}), {
    shallow: true,
    global: {
      stubs: {
        UserDetails: false,
        Suspense: false,
        XBtn: {
          template: "<button><slot></slot></button>",
        },
      },
    },
    provide: {
      cristal: {
        getContainer() {
          return container;
        },
      },
    },
  });
}

describe("UserDetails", () => {
  let container;
  const authenticationManagerMock = vi.fn();
  beforeAll(() => {
    vi.mock("vue-i18n");
    mockI18n();

    // Allow default slot of stubbed components tO have their default slot in
    // their content.
    config.global.renderStubDefaultSlot = true;

    container = new Container();
    container
      .bind("AuthenticationManagerProvider")
      .to(
        makeInjectable(
          class {
            get() {
              return new authenticationManagerMock();
            }
          },
        ),
      )
      .inSingletonScope();

    container
      .bind("BrowserApi")
      .to(makeInjectable(class {}))
      .inSingletonScope();
  });

  afterAll(() => {
    config.global.renderStubDefaultSlot = false;
  });

  it("display the user profile", async () => {
    authenticationManagerMock.prototype.getUserDetails = vi.fn(async () => {
      return {
        profile: "http://localhost/user1",
        name: "U1",
      };
    });
    const userDetails = mountUserDetails(container);
    // Wait for all the asynchronous operations to be terminated before starting
    // to assert the rendered content.
    await flushPromises();
    const link = userDetails.find("a");
    expect(link.attributes("href")).eq("http://localhost/user1");
    expect(link.text()).eq("U1");
    expect(userDetails.find("button").text()).eq("logout");
  });

  it("report network issue", async () => {
    authenticationManagerMock.prototype.getUserDetails = vi.fn(async () => {
      return Promise.reject();
    });
    const userDetails = mountUserDetails(container);
    // Wait for all the asynchronous operations to be terminated before starting
    // to assert the rendered content.
    await flushPromises();

    expect(userDetails.text()).eq("userDetails.error");
  });
});
