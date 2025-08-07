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
import { DefaultSettingsManager } from "../defaultSettingsManager";
import { DefaultSettingsParser } from "../defaultSettingsParser";
import { describe, expect, it } from "vitest";
import { mock } from "vitest-mock-extended";
import type { CristalApp } from "@xwiki/cristal-api";
import type {
  Settings,
  SettingsParserProvider,
} from "@xwiki/cristal-settings-api";
import type { Container } from "inversify";

class QuestionSettings implements Settings {
  key = "question";
  content: {
    question: string;
    answer: unknown;
    custom: boolean;
  };

  constructor(content?: {
    question: string;
    answer: unknown;
    custom: boolean;
  }) {
    this.content = content ?? {
      question: "",
      answer: undefined,
      custom: false,
    };
  }

  toJSON() {
    return {
      question: this.content.question,
      answer: this.content.answer,
    };
  }
}

function initializeMocks() {
  const cristalMock = mock<CristalApp>();
  const containerMock = mock<Container>();
  const settingsParserProviderMock = mock<SettingsParserProvider>();
  settingsParserProviderMock.get
    .calledWith("settings1")
    .mockReturnValue(new DefaultSettingsParser());
  settingsParserProviderMock.get
    .calledWith("settings2")
    .mockReturnValue(new DefaultSettingsParser());
  settingsParserProviderMock.get.calledWith("question").mockReturnValue({
    parse(serializedSettings): Settings {
      return new QuestionSettings({
        ...JSON.parse(serializedSettings),
        custom: true,
      });
    },
  });
  containerMock.get
    .calledWith("SettingsParserProvider")
    .mockReturnValue(settingsParserProviderMock);
  cristalMock.getContainer.mockReturnValue(containerMock);
  return new DefaultSettingsManager(settingsParserProviderMock);
}

describe("DefaultSettingsManager", () => {
  it("parse and serialize with default parser", () => {
    const settingsManager = initializeMocks();
    const serializedSettings = `{
  "settings1": "Hello World",
  "settings2": {
    "question": "The answer is?",
    "answer": 42
  }
}`;
    settingsManager.fromJSON(serializedSettings);
    expect(
      settingsManager.get(
        class {
          key = "settings1";
          content: unknown;
        },
      ),
    ).toEqual({
      key: "settings1",
      content: "Hello World",
    });
    expect(
      settingsManager.get(
        class {
          key = "settings2";
          content: unknown;
        },
      ),
    ).toEqual({
      key: "settings2",
      content: {
        question: "The answer is?",
        answer: 42,
      },
    });
    expect(settingsManager.toJSON()).toEqual(serializedSettings);
  });

  it("parse and serialize with custom parser", () => {
    const settingsManager = initializeMocks();
    const serializedSettings = `{
  "settings1": "Hello World",
  "question": {
    "question": "The answer is?",
    "answer": 42
  }
}`;
    settingsManager.fromJSON(serializedSettings);
    expect(
      settingsManager.get(
        class {
          key = "settings1";
          content: unknown;
        },
      ),
    ).toEqual({
      key: "settings1",
      content: "Hello World",
    });
    expect(settingsManager.get(QuestionSettings)).toEqual({
      key: "question",
      content: {
        question: "The answer is?",
        answer: 42,
        custom: true,
      },
    });
    expect(settingsManager.toJSON()).toEqual(serializedSettings);
  });
});
