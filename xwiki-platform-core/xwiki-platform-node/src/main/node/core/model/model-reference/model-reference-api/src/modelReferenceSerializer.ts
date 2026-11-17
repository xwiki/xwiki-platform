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

import type { EntityReference } from "@xwiki/platform-model-api";

/**
 * @since 18.0.0RC1
 * @beta
 */
interface ModelReferenceSerializer {
  /**
   * @param reference - the reference to serialize
   * @param base - (since 18.10.0RC1) the reference to serialize the given reference relative to, omitting the leading
   *   entities they have in common (e.g. only the page name for a page of the same space as the base document), so
   *   that parsing the result against the same base gives back the given reference; when not specified, or when the
   *   implementation doesn't support relative references, the given reference is fully serialized
   * @returns the serialized reference, or `undefined` when no reference is given
   */
  serialize(
    reference?: EntityReference,
    base?: EntityReference,
  ): string | undefined;
}

export type { ModelReferenceSerializer };
