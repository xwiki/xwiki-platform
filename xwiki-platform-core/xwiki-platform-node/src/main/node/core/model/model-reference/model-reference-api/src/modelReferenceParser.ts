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

import type { ModelReferenceParserOptions } from "./modelReferenceParserOptions";
import type { EntityReference } from "@xwiki/platform-model-api";

/**
 * @since 18.0.0RC1
 * @beta
 */
interface ModelReferenceParser {
  /**
   * @param reference - an entity reference
   * @param options - (since 0.22) an optional configuration object
   */
  parse(
    reference: string,
    options?: ModelReferenceParserOptions,
  ): EntityReference;

  /**
   * Parse a reference with additional analysis that can only be performed asynchronously
   * @param reference - an entity reference
   * @param options - an optional configuration object
   * @since 18.0.0RC1
   * @beta
   */
  parseAsync(
    reference: string,
    options?: ModelReferenceParserOptions,
  ): Promise<EntityReference>;
}
export type { ModelReferenceParser };
