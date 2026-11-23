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

import { MINIMAL_SYNTAX_NAME } from "@xwiki/platform-minimal-syntax-config";
import { SYNTAX_CONFIG_COMPONENT_GROUP_NAME } from "@xwiki/platform-syntaxes-config";
import type { SyntaxConfig } from "@xwiki/platform-syntaxes-config";
import type { Container } from "inversify";

/**
 * Looks up the configuration of the given syntax among the syntax configurations registered in the container. Falls
 * back on the minimal syntax, which disables most editor features, when the syntax has no configuration.
 *
 * @param serviceContainer - the container holding the syntax configurations
 * @param syntaxId - the id of the syntax of the edited content (e.g. "xwiki/2.1")
 * @returns the configuration of the given syntax, or of the minimal syntax
 */
function getSyntaxConfig(
  serviceContainer: Container,
  syntaxId: string,
): SyntaxConfig {
  const syntaxes = serviceContainer.getAll<SyntaxConfig>(
    SYNTAX_CONFIG_COMPONENT_GROUP_NAME,
    { optional: true },
  );

  const syntax = syntaxes.find((config) => config.id === syntaxId);
  if (syntax) {
    return syntax;
  }

  // Falling back to the minimal syntax disables most editor features (headings, tables, images, lists, macros, ...).
  // Warn so that a missing / mismatched syntax configuration doesn't silently cripple the editor.
  console.warn(
    `No syntax configuration registered for [${syntaxId}], falling back to the minimal syntax: most editor features ` +
      "will be disabled.",
  );
  const minimalSyntax = syntaxes.find(
    (config) => config.id === MINIMAL_SYNTAX_NAME,
  );
  if (!minimalSyntax) {
    throw new Error(
      `The syntax [${syntaxId}] is not supported, and the minimal syntax is not available.`,
    );
  }
  return minimalSyntax;
}

export { getSyntaxConfig };
