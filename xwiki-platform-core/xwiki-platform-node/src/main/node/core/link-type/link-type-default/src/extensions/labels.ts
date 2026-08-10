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
 * Build a {@link LinkTargetTypeExtension.getLabel} implementation resolving a label from this package's own
 * bundled translations, given `LinkTargetTypeExtension` implementations are plain objects and cannot rely on the
 * Vue composition API (`useI18n()`) to resolve their own label.
 *
 * @param key - the translation key to resolve
 * @returns a function resolving the translated label for a given locale, falling back to English
 */
function labelFromTranslations(key: string): (locale: string) => string {
  return (locale: string): string =>
    translations[locale]?.[key] ?? translations.en[key] ?? key;
}

export { labelFromTranslations };
