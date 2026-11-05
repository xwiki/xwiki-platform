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
import { AbstractPlainTextLinkTargetType } from "@xwiki/platform-link-type-api";
import { ResourceType } from "@xwiki/platform-rendering-api";
import { injectable } from "inversify";

/**
 * Built-in link target type for links to a path relative to the server (e.g. `/xwiki/bin/view/Main/`).
 *
 * @since 18.10.0RC1
 * @beta
 */
@injectable()
class PathLinkTargetType extends AbstractPlainTextLinkTargetType {
  readonly type = "path";

  readonly order = 300;

  protected readonly resourceType = ResourceType.PATH;

  // An untyped path is parsed back as a document reference.
  protected readonly typedReference = true;

  getLabel(locale: string): Promise<string> {
    return labelFromTranslations("link-type.target-types.path.label")(locale);
  }
}

export { PathLinkTargetType };
