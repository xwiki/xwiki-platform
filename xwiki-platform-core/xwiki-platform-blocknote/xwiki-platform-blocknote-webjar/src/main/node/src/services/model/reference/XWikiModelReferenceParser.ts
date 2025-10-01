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
import { EntityReference, EntityType } from "@xwiki/cristal-model-api";
import { ModelReferenceParser } from "@xwiki/cristal-model-reference-api";
import { Container, injectable } from "inversify";
import { toCristalEntityReference } from "./XWikiEntityReference";

type ResourceReference = {
  type: string;
  reference: string;
};

@injectable("Singleton")
export class XWikiModelReferenceParser implements ModelReferenceParser {
  // See ResourceType in xwiki-rendering-api.
  public static readonly RESOURCE_TYPES: string[] = [
    "unknown",
    "doc",
    "page",
    "space",
    "url",
    "interwiki",
    "path",
    "mailto",
    "attach",
    "pageAttach",
    "icon",
    "unc",
    "user",
    "data",
  ];

  public static bind(container: Container): void {
    container.bind("ModelReferenceParser").to(XWikiModelReferenceParser).inSingletonScope().whenNamed("XWiki");
  }

  public parse(reference: string, type?: EntityType): EntityReference {
    const defaultType = type === EntityType.ATTACHMENT ? "attach" : "doc";
    const resourceReference = this.parseResourceReference(reference, defaultType);
    const entityType = this.getEntityType(resourceReference);
    if (entityType) {
      return this.parseEntityReference(resourceReference.reference, entityType);
    } else {
      throw new Error(`[${reference}] is not an entity reference.`);
    }
  }

  private parseResourceReference(reference: string, defaultType: string): ResourceReference {
    const parts = reference.split(":");
    if (parts.length > 1) {
      const type = parts[0];
      if (XWikiModelReferenceParser.RESOURCE_TYPES.includes(type)) {
        return {
          type,
          reference: parts.slice(1).join(":"),
        };
      } else if (parts[1].startsWith("//")) {
        return {
          type: "url",
          reference,
        };
      }
    }

    return {
      type: defaultType,
      reference,
    };
  }

  private getEntityType(resourceReference: ResourceReference): number | undefined {
    switch (resourceReference.type) {
      case "space":
        return XWiki.EntityType.SPACE;
      case "doc":
        return XWiki.EntityType.DOCUMENT;
      case "attach":
        return XWiki.EntityType.ATTACHMENT;
      default:
        return undefined;
    }
  }

  private parseEntityReference(reference: string, type: number): EntityReference {
    return toCristalEntityReference(XWiki.Model.resolve(reference, type))!;
  }
}
