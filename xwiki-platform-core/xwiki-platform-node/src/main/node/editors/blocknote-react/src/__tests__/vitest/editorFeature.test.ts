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
import {
  contributeFeatures,
  editorFeatureName,
} from "../../features/editorFeature";
import { afterEach, describe, expect, test, vi } from "vitest";
import { mock } from "vitest-mock-extended";
import type {
  EditorFeature,
  FeatureContext,
  FeatureContribution,
} from "../../features/editorFeature";
import type { Container } from "inversify";

/**
 * @param id - the id of the feature
 * @param contribution - what the feature contributes
 * @returns a feature contributing the given contribution
 */
function feature(
  id: string,
  contribution?: FeatureContribution,
): EditorFeature {
  return { id, contribute: vi.fn(() => contribution) };
}

/**
 * @param boundFeatures - the features bound in the service container
 * @param features - the options of the features
 * @returns the context of an editor instance whose service container holds the given features
 */
function context(
  boundFeatures: EditorFeature[] = [],
  features?: Record<string, unknown>,
): FeatureContext {
  const serviceContainer = mock<Container>();
  serviceContainer.isBound.mockImplementation(
    (name) => name === editorFeatureName && boundFeatures.length > 0,
  );
  serviceContainer.getAll.mockImplementation((name) =>
    name === editorFeatureName ? boundFeatures : [],
  );
  return {
    options: { features },
    serviceContainer,
    syntax: {},
    t: (key: string) => key,
    getInstance: vi.fn(),
  } as unknown as FeatureContext;
}

describe("contributeFeatures", () => {
  afterEach(() => {
    vi.restoreAllMocks();
  });

  test("merges the contributions of the bound features, in the order they are found", () => {
    const merged = contributeFeatures(
      context([
        feature("first", {
          slashMenuItems: () => [{ title: "first", onItemClick: () => {} }],
          components: "first component",
        }),
        feature("second", {
          slashMenuItems: () => [{ title: "second", onItemClick: () => {} }],
          formattingToolbar: { blockTypeSelectItems: [] },
          components: "second component",
        }),
      ]),
    );

    expect(merged.slashMenuItems().map(({ title }) => title)).toEqual([
      "first",
      "second",
    ]);
    expect(merged.components).toEqual(["first component", "second component"]);
  });

  test("contributes nothing when no feature is bound", () => {
    const merged = contributeFeatures(context());

    expect(merged.slashMenuItems()).toEqual([]);
    expect(merged.components).toEqual([]);
  });

  test("passes the context of the editor instance to the features", () => {
    const bound = feature("bound");
    const editorContext = context([bound]);

    contributeFeatures(editorContext);

    expect(bound.contribute).toHaveBeenCalledWith(editorContext);
  });

  test("doesn't ask a disabled feature for its contribution", () => {
    const disabled = feature("disabled", { components: "disabled" });
    const enabled = feature("enabled", { components: "enabled" });

    const merged = contributeFeatures(
      context([disabled, enabled], { disabled: false, enabled: {} }),
    );

    expect(disabled.contribute).not.toHaveBeenCalled();
    expect(merged.components).toEqual(["enabled"]);
  });

  test("keeps the other features when one of them fails", () => {
    const consoleError = vi
      .spyOn(console, "error")
      .mockImplementation(() => {});
    const failing: EditorFeature = {
      id: "failing",
      contribute: () => {
        throw new Error("Feature failed");
      },
    };

    const merged = contributeFeatures(
      context([failing, feature("other", { components: "other" })]),
    );

    expect(merged.components).toEqual(["other"]);
    expect(consoleError).toHaveBeenCalledWith(
      "The editor feature [failing] failed.",
      expect.objectContaining({ message: "Feature failed" }),
    );
  });

  test("keeps the first of the BlockNote options and the block toolbars set by several features, and reports it", () => {
    const consoleError = vi
      .spyOn(console, "error")
      .mockImplementation(() => {});
    const firstUpload = vi.fn();
    const firstToolbar = vi.fn();

    const merged = contributeFeatures(
      context([
        feature("first", {
          blockNoteOptions: { uploadFile: firstUpload },
          formattingToolbar: { blockToolbars: { image: firstToolbar } },
        }),
        feature("second", {
          blockNoteOptions: { uploadFile: vi.fn() },
          formattingToolbar: { blockToolbars: { image: vi.fn() } },
        }),
      ]),
    );

    expect(merged.blockNoteOptions.uploadFile).toBe(firstUpload);
    expect(merged.formattingToolbar.blockToolbars.image).toBe(firstToolbar);
    expect(consoleError).toHaveBeenCalledWith(
      "The BlockNote option [uploadFile] is set by several editor features. Only the first one is taken into account.",
    );
    expect(consoleError).toHaveBeenCalledWith(
      "The toolbar of the block type [image] is set by several editor features. Only the first one is taken into account.",
    );
  });

  test("merges nothing when the features contribute nothing", () => {
    const merged = contributeFeatures(context([feature("nothing")]));

    expect(merged.blockNoteExtensions).toEqual([]);
    expect(merged.blockNoteOptions).toEqual({});
    expect(merged.slashMenuItems()).toEqual([]);
    expect(merged.formattingToolbar).toEqual({
      items: [],
      blockTypeSelectItems: [],
      blockToolbars: {},
    });
    expect(merged.components).toEqual([undefined]);
  });
});
