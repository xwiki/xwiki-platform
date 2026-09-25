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

import { LiveDataLogic } from "./LiveDataLogic";
import { describe, expect, it, vi } from "vitest";
import { computed } from "vue";
import type { LiveDataSource } from "@xwiki/platform-livedata-api";

// The logic resolves its translations through vue-i18n, which requires an active Vue application.
// The state tested here does not depend on the translations, so a pass-through is enough.
vi.mock("vue-i18n", () => ({
  useI18n: () => ({ t: (key: string) => key }),
}));

/**
 * @param sourceParameters - the parameters to set on the source of the query
 * @returns a logic instance operating on a minimal Live Data configuration
 */
function initLogic(sourceParameters: Record<string, string> = {}) {
  const data = {
    id: 0,
    query: {
      source: { id: "liveTable", ...sourceParameters },
    },
    meta: {
      defaultLayout: "table",
      layouts: [{ id: "table" }, { id: "cards" }],
    },
    data: { count: 0, entries: [] },
  };
  return new LiveDataLogic(
    {} as LiveDataSource,
    JSON.stringify(data),
    true,
    async () => ({}),
  );
}

describe("LiveDataLogic.ts", () => {
  describe("maximized state", () => {
    it("Is not maximized by default", () => {
      expect(initLogic().isMaximized()).toBe(false);
    });

    it("Switches back and forth between maximized and normal", () => {
      const logic = initLogic();

      logic.toggleMaximized();
      expect(logic.isMaximized()).toBe(true);

      logic.toggleMaximized();
      expect(logic.isMaximized()).toBe(false);
    });

    it("Exposes the maximized state reactively", () => {
      const logic = initLogic();
      const maximized = computed(() => logic.isMaximized());

      expect(maximized.value).toBe(false);

      logic.toggleMaximized();

      expect(maximized.value).toBe(true);
    });
  });

  describe("edit mode", () => {
    it("Has no edit mode when the source does not declare one", () => {
      expect(initLogic().hasEditMode()).toBe(false);
      expect(initLogic({ hasEditMode: "false" }).hasEditMode()).toBe(false);
    });

    it("Has an edit mode when the source declares one", () => {
      expect(initLogic({ hasEditMode: "true" }).hasEditMode()).toBe(true);
    });

    it("Is not in edit mode by default", () => {
      expect(initLogic({ hasEditMode: "true" }).isEditMode()).toBe(false);
    });

    it("Enables and disables the edit mode", () => {
      const logic = initLogic({ hasEditMode: "true" });

      logic.enableEditMode();
      expect(logic.isEditMode()).toBe(true);

      logic.disableEditMode();
      expect(logic.isEditMode()).toBe(false);
    });

    it("Exposes the edit mode state reactively", () => {
      const logic = initLogic({ hasEditMode: "true" });
      const editMode = computed(() => logic.isEditMode());

      expect(editMode.value).toBe(false);

      logic.enableEditMode();

      expect(editMode.value).toBe(true);
    });
  });
});
