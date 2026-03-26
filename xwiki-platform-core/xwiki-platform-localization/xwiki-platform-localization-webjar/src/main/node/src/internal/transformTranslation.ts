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
/**
 * Takes a translation string with indexed parameters, and replace the parameters with the provided args.
 * @param value - the translation string (e.g. Hello \{0\} \{1\})
 * @param args - the arguments (e.g., "World", "!")
 */
export function transformTranslation(
  value: string | null,
  ...args: unknown[]
): string | null {
  if (typeof value === "string") {
    let transformedKey = value;
    // If there's an argument, ensure to unescape doubled single quotes.
    if (args.length > 0) {
      transformedKey = transformedKey.replaceAll("''", "'");
    }
    // Naive implementation for message parameter substitution that suits our current needs.
    for (let i = 0; i < args.length; i++) {
      transformedKey = transformedKey.replace(
        new RegExp(`\\{${i}\\}`, "g"),
        `${args[i]}`,
      );
    }
    return transformedKey;
  } else {
    return value;
  }
}
