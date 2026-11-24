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
// @vitest-environment jsdom

import {
  createBlockNoteSchema,
  querySuggestionsMenuItems,
} from "../../blocknote";
import { insertMention } from "../../features/mentions/mentionInsertion";
import {
  shouldOpenMentionMenu,
  toSuggestions,
} from "../../features/mentions/mentionMenu";
import { mentionsFeature } from "../../features/mentions/mentions";
import {
  mentionRendererName,
  userSuggesterName,
} from "../../services/mentions";
import { FULL_SYNTAX } from "../playwright-ct/syntax.mock";
import { BlockNoteEditor } from "@blocknote/core";
import { Container } from "inversify";
import { beforeAll, describe, expect, test, vi } from "vitest";
import type { EditorInstance } from "../../editorInstance";
import type { FeatureContext } from "../../features/editorFeature";
import type { InlineMacroInvocation } from "../../services/macroEditor";
import type {
  MentionRenderer,
  MentionableUser,
  UserSuggester,
} from "../../services/mentions";

const user: MentionableUser = {
  reference: "XWiki.Admin",
  label: "Administrator",
  hint: "admin@example.com",
};

const userSuggester: UserSuggester = {
  suggestUsers: async () => [user],
};

const invocation: InlineMacroInvocation = {
  kind: "inline",
  id: "mention",
  params: { reference: user.reference },
  body: { type: "none" },
};

// eslint-disable-next-line @typescript-eslint/no-explicit-any
type AnyEditor = BlockNoteEditor<any, any, any>;

let editor: AnyEditor;

beforeAll(() => {
  editor = BlockNoteEditor.create({
    schema: createBlockNoteSchema(),
  }) as AnyEditor;
});

/**
 * @param suggester - whether a user suggester is bound, as a host application that can look users up does
 * @param renderer - whether a mention renderer is bound, as a host application that can represent mentions does
 * @param mentions - whether the syntax of the edited content can save a mention
 * @param macros - whether the syntax of the edited content can save an inline macro
 * @returns the context the mentions feature contributes to
 */
function context({
  suggester = true,
  renderer = true,
  mentions = true,
  macros = true,
}: {
  suggester?: boolean;
  renderer?: boolean;
  mentions?: boolean;
  macros?: boolean;
} = {}) {
  const serviceContainer = new Container();
  if (suggester) {
    serviceContainer
      .bind<UserSuggester>(userSuggesterName)
      .toConstantValue(userSuggester);
  }
  if (renderer) {
    serviceContainer
      .bind<MentionRenderer>(mentionRendererName)
      .toConstantValue({ render: () => invocation });
  }
  const syntax = structuredClone(FULL_SYNTAX);
  syntax.features.inlineContents.mentions = mentions;
  syntax.features.inlineContents.macros = macros;
  return {
    serviceContainer,
    options: { features: {} },
    syntax,
    // Identity stub: the keys are what the assertions below are tied to.
    t: (key: string) => key,
    getInstance: () => ({ editor }) as EditorInstance,
  } as FeatureContext;
}

/**
 * @param query - what the user typed after the slash
 * @returns the "Mention" item of the slash menu, when the given query keeps it
 */
function findMentionItem(query = "") {
  return querySuggestionsMenuItems({
    editor,
    query,
    syntax: FULL_SYNTAX,
    featureItems: mentionsFeature.contribute(context())!.slashMenuItems!(),
  }).find((item) => item.title === "blocknote.slashMenu.mention.title");
}

/**
 * @param textBefore - the text the mention trigger is about to be typed after
 * @returns a transaction whose selection sits right after the given text
 */
function transactionAfter(textBefore: string) {
  return {
    selection: {
      $from: {
        parent: {
          textBetween: (from: number, to: number) => textBefore.slice(from, to),
        },
        parentOffset: textBefore.length,
      },
    },
    // eslint-disable-next-line @typescript-eslint/no-explicit-any
  } as any;
}

describe("mentionsFeature", () => {
  test("shows the mention menu when both mention services are bound", () => {
    expect(mentionsFeature.contribute(context())?.components).toBeDefined();
  });

  test("contributes nothing without a user suggester", () => {
    expect(
      mentionsFeature.contribute(context({ suggester: false })),
    ).toBeUndefined();
  });

  test("contributes nothing without a mention renderer", () => {
    expect(
      mentionsFeature.contribute(context({ renderer: false })),
    ).toBeUndefined();
  });

  test("contributes nothing when the syntax can't save a mention", () => {
    expect(
      mentionsFeature.contribute(context({ mentions: false })),
    ).toBeUndefined();
  });

  test("contributes nothing when the syntax can't save the inline macro a mention is inserted as", () => {
    expect(
      mentionsFeature.contribute(context({ macros: false })),
    ).toBeUndefined();
  });

  test("adds a Mention entry to the slash menu", () => {
    const mentionItem = findMentionItem();

    expect(mentionItem).toBeDefined();
    expect(mentionItem!.subtext).toBe("blocknote.slashMenu.mention.subtext");
    expect(mentionItem!.aliases).toEqual(["mention", "user"]);
    expect(mentionItem!.group).toBe("blocknote.slashMenu.mention.group");
  });

  test("the Mention entry survives a query matching its alias", () => {
    expect(findMentionItem("user")).toBeDefined();
  });

  test("the Mention entry is filtered out by a query matching neither its title nor its aliases", () => {
    expect(findMentionItem("table")).toBeUndefined();
  });
});

describe("shouldOpenMentionMenu", () => {
  test.each([
    ["at the start of a block", ""],
    ["after a space", "Hello "],
    ["after an opening bracket", "Hello ("],
  ])("opens %s", (_label, textBefore) => {
    expect(shouldOpenMentionMenu(transactionAfter(textBefore))).toBe(true);
  });

  test.each([
    ["in the middle of a word", "alice"],
    ["in an email address", "john.doe"],
    ["after a digit", "user2"],
    ["after an accented letter", "chloé"],
  ])("doesn't open %s", (_label, textBefore) => {
    expect(shouldOpenMentionMenu(transactionAfter(textBefore))).toBe(false);
  });
});

/**
 * @returns an editor instance whose only ability is to record the inserted inline content
 */
function instanceInsertingInto(insertInlineContent: () => void) {
  return { editor: { insertInlineContent } } as unknown as EditorInstance;
}

describe("toSuggestions", () => {
  const renderer: MentionRenderer = { render: vi.fn(() => invocation) };
  const instance = instanceInsertingInto(vi.fn());

  test("labels the item with the user name and its hint", () => {
    const items = toSuggestions(instance, renderer, [user], "Users");

    expect(items[0].title).toBe("Administrator");
    expect(items[0].subtext).toBe("admin@example.com");
    expect(items[0].group).toBe("Users");
  });

  test("hands the picked user to the mention renderer", () => {
    toSuggestions(instance, renderer, [user], "Users")[0].onItemClick!();

    expect(renderer.render).toHaveBeenCalledWith(user);
  });
});

describe("insertMention", () => {
  const renderer: MentionRenderer = { render: () => invocation };

  test("inserts the rendered macro call at the selection", () => {
    const insertInlineContent = vi.fn();

    insertMention(instanceInsertingInto(insertInlineContent), renderer, user);

    const [content] = insertInlineContent.mock.calls[0];
    expect(content[0].type).toBe("xwikiInlineMacro");
    expect(JSON.parse(content[0].props.call)).toEqual({
      name: "mention",
      parameters: { reference: "XWiki.Admin" },
    });
  });

  test("follows the macro call with a space, so that a lone mention stays inline", () => {
    const insertInlineContent = vi.fn();

    insertMention(instanceInsertingInto(insertInlineContent), renderer, user);

    expect(insertInlineContent.mock.calls[1][0]).toBe(" ");
  });
});
