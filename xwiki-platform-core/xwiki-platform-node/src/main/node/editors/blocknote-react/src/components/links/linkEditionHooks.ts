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
import type { LinkData } from "./LinkEditor";

/**
 * Hooks to intercept the link creation / edition flow.
 *
 * @since 18.6.0RC1
 * @beta
 */
type LinkEditionHooks = {
  /**
   * Run before the popover is opened to edit an existing link.
   *
   * @param linkData - the current link data
   * @returns a (possibly transformed) link data to pre-fill the popover with, or nothing to keep the
   *   current link data unchanged
   */
  beforeEdit?: (linkData: LinkData) => LinkData | void;

  /**
   * Run right before the link is written into the content, i.e. before BlockNote's `createLink` (for a
   * new link) or `editLink` (for an existing link) is called.
   *
   * @param linkData - the link data about to be written
   * @param previous - the link data before the edition, when editing an existing link; undefined when
   *   creating a new link. This lets the integration tell an edit (which should preserve the metadata
   *   bound to the existing link) apart from a creation.
   * @returns a (possibly transformed) link data to write instead, or nothing to write the provided
   *   link data unchanged. Throwing cancels the write.
   */
  beforeUpdate?: (linkData: LinkData, previous?: LinkData) => LinkData | void;
};

export type { LinkEditionHooks };
