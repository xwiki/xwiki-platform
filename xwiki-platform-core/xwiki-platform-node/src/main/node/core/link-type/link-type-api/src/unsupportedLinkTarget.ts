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
import type { ResourceReference } from "@xwiki/platform-rendering-api";

/**
 * The type of the link targets pointing to a resource that no registered {@link LinkTargetType} supports
 * (e.g. an interwiki link written in the wiki syntax), so that such links can still be edited, and saved without
 * changing the reference of the linked resource. This type is reserved: no link target type should use it.
 *
 * @since 18.10.0RC1
 * @beta
 */
const unsupportedLinkTargetType = "unsupported";

/**
 * The configuration of a link target of the {@link unsupportedLinkTargetType} type.
 *
 * @since 18.10.0RC1
 * @beta
 */
type UnsupportedLinkTargetConfig = {
  /**
   * The reference of the linked resource, as written in the content. Only its `reference` field is edited, the
   * resource type, whether the reference is typed and its parameters are kept.
   */
  reference: ResourceReference;
};

export { unsupportedLinkTargetType };
export type { UnsupportedLinkTargetConfig };
