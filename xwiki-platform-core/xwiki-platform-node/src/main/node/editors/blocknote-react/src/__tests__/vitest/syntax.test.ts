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

import { getSyntaxConfig } from "../../misc/syntax";
import { FULL_SYNTAX } from "../playwright-ct/syntax.mock";
import {
  ComponentInit as MinimalSyntaxComponentInit,
  MINIMAL_SYNTAX_NAME,
} from "@xwiki/platform-minimal-syntax-config";
import { SYNTAX_CONFIG_COMPONENT_GROUP_NAME } from "@xwiki/platform-syntaxes-config";
import { Container } from "inversify";
import { afterEach, describe, expect, test, vi } from "vitest";

/**
 * @param withMinimalSyntax - whether to register the configuration of the minimal syntax
 * @returns a container holding the configuration of the full syntax and, optionally, of the minimal syntax
 */
function createContainer(withMinimalSyntax: boolean): Container {
  const container = new Container();
  container
    .bind(SYNTAX_CONFIG_COMPONENT_GROUP_NAME)
    .toConstantValue(FULL_SYNTAX);
  if (withMinimalSyntax) {
    new MinimalSyntaxComponentInit(container);
  }
  return container;
}

describe("getSyntaxConfig", () => {
  afterEach(() => {
    vi.restoreAllMocks();
  });

  test("returns the configuration of the given syntax", () => {
    expect(getSyntaxConfig(createContainer(true), FULL_SYNTAX.id)).toBe(
      FULL_SYNTAX,
    );
  });

  test("falls back on the minimal syntax, with a warning, when the syntax has no configuration", () => {
    const warn = vi.spyOn(console, "warn").mockImplementation(() => {});

    expect(getSyntaxConfig(createContainer(true), "unknown/1.0").id).toBe(
      MINIMAL_SYNTAX_NAME,
    );
    expect(warn).toHaveBeenCalledWith(expect.stringContaining("[unknown/1.0]"));
  });

  test("fails when neither the syntax nor the minimal syntax have a configuration", () => {
    vi.spyOn(console, "warn").mockImplementation(() => {});

    expect(() =>
      getSyntaxConfig(createContainer(false), "unknown/1.0"),
    ).toThrow("The syntax [unknown/1.0] is not supported");
  });

  test("fails when no syntax configuration is registered at all", () => {
    vi.spyOn(console, "warn").mockImplementation(() => {});

    expect(() => getSyntaxConfig(new Container(), FULL_SYNTAX.id)).toThrow(
      "is not supported",
    );
  });
});
