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
import type {
  Translations,
  Translator,
} from "@xwiki/platform-localization-api";

/**
 * @param id - the id of the hidden element containing the json of the translations
 * @since 18.3.0RC1
 * @beta
 */
export function translatorFactory(id: string): Translator {
  // The element is only loaded once and is not expected to be modified.
  const content = document.getElementById(id);

  if (!content) {
    console.error(
      `Unable to find element with id ${id}. This translator will no be used to resolve translation keys.`,
    );
  }

  const json = content ? JSON.parse(content.textContent) : undefined;

  return {
    async resolve(query): Promise<Translations> {
      if (!json) {
        return {};
      }
      const args = Array.isArray(query)
        ? query
        : query.keys.map((key) => (query.prefix ?? "") + key);
      return args.reduce((prev, key) => {
        const translation = json[key];
        if (translation) {
          return { ...prev, [key]: translation };
        } else {
          return prev;
        }
      }, {});
    },
  };
}
