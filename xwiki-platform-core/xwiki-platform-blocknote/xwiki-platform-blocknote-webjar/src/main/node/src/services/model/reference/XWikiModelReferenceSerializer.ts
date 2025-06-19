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

import { toXWikiEntityReference } from "@/services/model/reference/XWikiEntityReference";
import { EntityReference, EntityType } from "@xwiki/cristal-model-api";
import { ModelReferenceSerializer } from "@xwiki/cristal-model-reference-api";
import { Container, injectable } from "inversify";

@injectable("Singleton")
export class XWikiModelReferenceSerializer implements ModelReferenceSerializer {
  public static bind(container: Container): void {
    container.bind("ModelReferenceSerializer").to(XWikiModelReferenceSerializer).inSingletonScope().whenNamed("XWiki");
  }

  public serialize(reference?: EntityReference): string | undefined {
    if (!reference) {
      return undefined;
    }
    return this.getPrefix(reference.type) + XWiki.Model.serialize(toXWikiEntityReference(reference));
  }

  private getPrefix(type: EntityType): string {
    switch (type) {
      case EntityType.SPACE:
        return "space:";
      case EntityType.DOCUMENT:
        return "doc:";
      case EntityType.ATTACHMENT:
        return "attach:";
      default:
        return "";
    }
  }
}
