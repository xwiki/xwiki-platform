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
import { translations } from "../translations";

/**
 * Resolve the label of a built-in link target type from this package's own bundled translations, for its
 * `LinkTargetType.getLabel` implementation: link target types are plain objects and can't rely on the Vue composition
 * API (`useI18n()`) to resolve their label.
 *
 * @param key - the translation key of the label
 * @param locale - the locale to resolve the label for
 * @returns the translated label, falling back to English, then to the key itself; it resolves immediately, since the
 *   translations are bundled
 */
async function translateLabel(key: string, locale: string): Promise<string> {
  return translations[locale]?.[key] ?? translations.en[key] ?? key;
}

export { translateLabel };
