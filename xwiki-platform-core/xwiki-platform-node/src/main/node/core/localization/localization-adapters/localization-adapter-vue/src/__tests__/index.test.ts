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
import { useI18nAdapter } from "../index";
import { flushPromises, mount } from "@vue/test-utils";
import { beforeEach, describe, expect, it, vi } from "vitest";
import { defineComponent, nextTick } from "vue";
import { createI18n } from "vue-i18n";
import type {
  Query,
  Resolver,
  TranslationsWithMissed,
} from "@xwiki/platform-localization-api";

// ---------------------------------------------------------------------------
// Helpers
// ---------------------------------------------------------------------------

function buildI18n(locale = "en"): ReturnType<typeof createI18n> {
  return createI18n({ legacy: false, locale, messages: {} });
}

function buildResolver(translations: Record<string, string> = {}): Resolver {
  return {
    resolve: vi.fn().mockResolvedValue({ translations }),
  };
}

function mountWithComposable(
  resolver: Resolver,
  query: Query,
  i18n: ReturnType<typeof createI18n>,
) {
  let exposed: ReturnType<typeof useI18nAdapter> | undefined;

  const TestComponent = defineComponent({
    setup() {
      exposed = useI18nAdapter(resolver, query);
      return exposed;
    },
    template: "<div />",
  });

  mount(TestComponent, { global: { plugins: [i18n] } });

  return exposed!;
}

// ---------------------------------------------------------------------------
// Tests
// ---------------------------------------------------------------------------

describe("useI18nAdapter", () => {
  beforeEach(() => {
    vi.clearAllMocks();
  });

  it("loads translations immediately on mount", async () => {
    const resolver = buildResolver({ "hello.world": "Hello World" });
    const i18n: ReturnType<typeof createI18n> = buildI18n("en");

    const query = ["key1", "key2"];
    mountWithComposable(resolver, query, i18n);

    await flushPromises();

    expect(resolver.resolve).toHaveBeenCalledOnce();
    expect(resolver.resolve).toHaveBeenCalledWith(query);
  });

  it("merges translations into the current locale", async () => {
    const resolver = buildResolver({ greeting: "Hello" });
    const i18n = buildI18n("en");

    const { t } = mountWithComposable(resolver, ["key1", "key2"], i18n);

    await flushPromises();

    expect(t("greeting")).toBe("Hello");
  });

  it("sets isLoading to true while resolving, then false after", async () => {
    let resolvePromise!: (v: TranslationsWithMissed) => void;
    const resolver: Resolver = {
      resolve: () =>
        new Promise((resolve) => {
          resolvePromise = resolve;
        }),
    };
    const i18n = buildI18n("en");

    const { isLoading } = mountWithComposable(resolver, ["key1", "key2"], i18n);

    await nextTick(); // load() has been called, but not yet awaited

    expect(isLoading.value).toBe(true);

    resolvePromise({ translations: {} });
    await flushPromises();

    expect(isLoading.value).toBe(false);
  });

  it("sets isLoading to false even when resolver rejects", async () => {
    const resolver: Resolver = {
      resolve: vi.fn().mockRejectedValue(new Error("network error")),
    };
    const i18n = buildI18n("en");
    vi.spyOn(console, "error").mockImplementation(() => {});

    const { isLoading } = mountWithComposable(resolver, ["key1", "key2"], i18n);

    await flushPromises();

    expect(isLoading.value).toBe(false);
  });

  it("logs the error to console.error when resolver rejects", async () => {
    const error = new Error("network error");
    const resolver: Resolver = {
      resolve: vi.fn().mockRejectedValue(error),
    };
    const consoleSpy = vi.spyOn(console, "error").mockImplementation(() => {});
    const i18n = buildI18n("en");

    mountWithComposable(resolver, ["key1", "key2"], i18n);

    await flushPromises();

    expect(consoleSpy).toHaveBeenCalledWith(error);
  });

  it("reloads translations when the locale changes", async () => {
    const resolver = buildResolver({ greeting: "Bonjour" });
    const i18n = buildI18n("en");

    mountWithComposable(resolver, ["key1", "key2"], i18n);

    await flushPromises();

    // Change locale
    i18n.global.locale.value = "fr";

    await flushPromises();

    expect(resolver.resolve).toHaveBeenCalledTimes(2);
  });
});
