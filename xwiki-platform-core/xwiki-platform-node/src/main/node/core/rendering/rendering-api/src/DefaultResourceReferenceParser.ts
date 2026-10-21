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
import { ResourceType } from "./ResourceReference";
import { Container, injectable } from "inversify";
import type { ResourceReference } from "./ResourceReference";
import type {
  ResourceReferenceParser,
  ResourceReferenceParserOptions,
} from "./ResourceReferenceParser";

/**
 * Default `ResourceReferenceParser` implementation.
 *
 * @since 18.6.0RC1
 * @beta
 */
@injectable("Singleton")
export class DefaultResourceReferenceParser implements ResourceReferenceParser {
  public static bind(container: Container): void {
    container
      .bind("ResourceReferenceParser")
      .to(DefaultResourceReferenceParser)
      .inSingletonScope();
  }

  public parse(
    serializedReference: string,
    options?: ResourceReferenceParserOptions,
  ): ResourceReference {
    const parts = serializedReference.split(":");
    if (parts.length > 1) {
      const type = parts[0];
      if (Object.values(ResourceType).includes(type)) {
        return {
          type,
          typed: true,
          reference: parts.slice(1).join(":"),
          parameters: {},
        };
      } else if (parts[1].startsWith("//")) {
        return {
          type: "url",
          typed: false,
          reference: serializedReference,
          parameters: {},
        };
      }
    }

    return {
      type: options?.type ?? ResourceType.UNKNOWN,
      typed: false,
      reference: serializedReference,
      parameters: {},
    };
  }
}
