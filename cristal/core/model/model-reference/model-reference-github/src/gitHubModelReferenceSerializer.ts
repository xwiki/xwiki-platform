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

import { EntityType } from "@xwiki/cristal-model-api";
import { injectable } from "inversify";
import type { EntityReference } from "@xwiki/cristal-model-api";
import type { ModelReferenceSerializer } from "@xwiki/cristal-model-reference-api";

@injectable()
export class GitHubModelReferenceSerializer
  implements ModelReferenceSerializer
{
  // TODO: reduce the number of statements in the following method and reactivate the disabled eslint rule.
  // eslint-disable-next-line max-statements
  serialize(reference?: EntityReference): string | undefined {
    if (!reference) {
      return undefined;
    }
    const type = reference.type;
    const { SPACE, ATTACHMENT, DOCUMENT, WIKI } = EntityType;
    switch (type) {
      case WIKI:
        throw new Error("Wiki currently not supported from GitHub");
      case SPACE: {
        return reference.names.join("/");
      }
      case DOCUMENT: {
        const spaces = this.serialize(reference.space);
        const name = reference.name;
        if (spaces === undefined || spaces == "") {
          return name;
        } else {
          return `${spaces}/${name}`;
        }
      }
      case ATTACHMENT: {
        return this.serialize(reference.document) + "/" + reference.name;
      }
      default:
        throw new Error(`Unknown reference type [${type}]`);
    }
  }
}
