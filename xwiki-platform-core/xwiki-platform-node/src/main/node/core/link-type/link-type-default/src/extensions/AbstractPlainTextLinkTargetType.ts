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
import { labelFromTranslations } from "./labels";
import {
  typedResourceReference,
  untypedResourceReference,
} from "./referenceHelpers";
import type { PlainTextLinkTargetConfig } from "../data/linkType";
import type { LinkTargetTypeExtension } from "@xwiki/platform-link-type-api";
import type { ResourceReference } from "@xwiki/platform-rendering-api";
import type { Component } from "vue";

/**
 * Base class of the link target types designated by a plain text location (e.g. a URL, a path or a UNC path), which
 * differ only by the type of the resource references they handle and how they are presented.
 *
 * @since 18.10.0RC1
 * @beta
 */
abstract class AbstractPlainTextLinkTargetType
  implements LinkTargetTypeExtension<PlainTextLinkTargetConfig>
{
  abstract readonly type: string;

  abstract readonly order: number;

  /**
   * The type of the resource references that this link target type handles: it's the only type it claims, and the
   * type of the references it produces, so that editing a link preserves the type of the linked resource.
   */
  protected abstract readonly resourceType: string;

  /**
   * Whether the references produced by this link target type are typed. They have to be when an untyped reference
   * would be parsed back as another resource type (e.g. an untyped path is parsed as a document reference).
   */
  protected abstract readonly typedReference: boolean;

  /**
   * The translation key of the label of this link target type.
   */
  protected abstract readonly labelKey: string;

  abstract component(): Component;

  getLabel(locale: string): Promise<string> {
    return labelFromTranslations(this.labelKey)(locale);
  }

  createDefaultConfig(): PlainTextLinkTargetConfig {
    return { location: "" };
  }

  /**
   * @returns `null`: a plain text location can't be told apart from a URL, so only the URL link target type (the
   *   catch-all) parses URLs
   */
  // The URL is a parameter so that the URL link target type can override this method.
  // eslint-disable-next-line @typescript-eslint/no-unused-vars
  tryParseURL(url: string): PlainTextLinkTargetConfig | null {
    return null;
  }

  serializeURL({ location }: PlainTextLinkTargetConfig): string {
    return location;
  }

  tryParseReference(
    reference: ResourceReference,
  ): PlainTextLinkTargetConfig | null {
    return reference.type === this.resourceType
      ? { location: reference.reference }
      : null;
  }

  configToReference({
    location,
  }: PlainTextLinkTargetConfig): ResourceReference | undefined {
    if (location.trim() === "") {
      return undefined;
    }
    return this.typedReference
      ? typedResourceReference(this.resourceType, location)
      : untypedResourceReference(this.resourceType, location);
  }
}

export { AbstractPlainTextLinkTargetType };
