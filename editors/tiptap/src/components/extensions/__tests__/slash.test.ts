/**
 * See the LICENSE file distributed with this work for additional
 * information regarding copyright ownership.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 *
 * This file is part of the Cristal Wiki software prototype
 * @copyright  Copyright (c) 2023 XWiki SAS
 * @license    http://opensource.org/licenses/AGPL-3.0 AGPL-3.0
 *
 **/

import { ActionCategoryDescriptor, filterActionsByQuery } from "../slash";

import { describe, expect, it } from "vitest";

describe("filterActionsByQuery", () => {
  it("do nothing on empty", () => {
    expect(filterActionsByQuery("", [])).toStrictEqual([]);
  });
  it("removes empty categories", () => {
    const category2: ActionCategoryDescriptor = {
      title: "category2",
      actions: [],
    };
    const category1 = {
      title: "category1",
      actions: [],
    };
    expect(filterActionsByQuery("", [category2, category1])).toStrictEqual([]);
  });
  it("be sorted", () => {
    const actionA = {
      title: "actionA",
      command() {},
      hint: "Action A",
      icon: "action-a",
    };
    const actionB = {
      title: "actionB",
      command() {},
      hint: "Action B",
      icon: "action-b",
    };
    const category2: ActionCategoryDescriptor = {
      title: "category2",
      actions: [actionA],
    };
    const category1 = {
      title: "category1",
      actions: [actionB],
    };
    expect(filterActionsByQuery("", [category2, category1])).toStrictEqual([
      category1,
      category2,
    ]);
  });
  it("be sorted by sortField when defined", () => {
    const actionA = {
      title: "actionA1",
      command() {},
      hint: "Action A",
      icon: "action-a",
    };
    const actionB = {
      title: "actionB",
      command() {},
      hint: "Action B",
      icon: "action-b",
      sortField: "actionA0",
    };
    const category1 = {
      title: "category1",
      actions: [actionA, actionB],
    };
    expect(filterActionsByQuery("", [category1])).toStrictEqual([
      {
        ...category1,
        actions: [actionB, actionA],
      },
    ]);
  });
  it("filter", () => {
    const actionX1 = {
      title: "actionX1",
      command() {},
      hint: "Action X 1",
      icon: "action-X-1",
    };
    const actionX2 = {
      title: "actionX2",
      command() {},
      hint: "Action X 2",
      icon: "action-X-2",
    };
    const actionB = {
      title: "actionB",
      command() {},
      hint: "Action B",
      icon: "action-b",
    };
    const actionC = {
      title: "actionC",
      command() {},
      hint: "Action C",
      icon: "action-c",
    };
    const category0: ActionCategoryDescriptor = {
      title: "category2",
      actions: [actionX2, actionX1, actionC],
    };
    const category1 = {
      title: "category1",
      actions: [actionB],
    };
    expect(filterActionsByQuery("X", [category0, category1])).toStrictEqual([
      {
        ...category0,
        actions: [actionX1, actionX2],
      },
    ]);
  });
});
