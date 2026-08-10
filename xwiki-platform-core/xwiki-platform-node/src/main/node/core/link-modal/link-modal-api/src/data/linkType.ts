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

/**
 * Target of a link: its type and its type-specific configuration.
 *
 * The set of valid `type`/`config` combinations is open-ended: it is defined by whichever
 * {@link LinkTargetTypeExtension} implementations are registered (the built-in ones, provided by
 * `@xwiki/platform-link-modal-default`, contribute `"page"`, `"attachment"`, `"url"` and `"email"`), rather than
 * being a fixed, closed set of types.
 *
 * @since 18.7.0RC1
 * @beta
 */
type LinkTarget = {
  type: string;
  config: unknown;
};

/**
 * Parameters for a link
 *
 * @since 18.7.0RC1
 * @beta
 */
type LinkData = {
  displayText: string;
  newTab?: boolean;
  target: LinkTarget;
};

export type { LinkData, LinkTarget };
