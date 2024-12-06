/*
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

import { DocumentReference } from "@xwiki/cristal-model-api";
import { injectable } from "inversify";
import type { SpaceReference } from "@xwiki/cristal-model-api";
import type { ModelReferenceHandler } from "@xwiki/cristal-model-reference-api";

/**
 * Implementation of {@link ModelReferenceHandler} for the XWiki backend.
 *
 * @since 0.13
 */
@injectable()
export class XWikiModelReferenceHandler implements ModelReferenceHandler {
  createDocumentReference(
    name: string,
    space: SpaceReference,
  ): DocumentReference {
    space.names.push(name);
    return new DocumentReference("WebHome", space);
  }
}
