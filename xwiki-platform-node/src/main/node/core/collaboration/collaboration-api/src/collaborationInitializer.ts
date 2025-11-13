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
import type { Doc } from "yjs";

/**
 * Holds properties of an collaboration. It's provider, the document held by the provider, and a provide resolved on
 * the initialized is ready.
 * @since 0.20
 * @beta
 */
type CollaborationInitializer = {
  /**
   * The provider, can be of arbitrary type.
   */
  // eslint-disable-next-line @typescript-eslint/no-explicit-any
  provider: any;
  /**
   * The yjs document held by the provider.
   */
  doc: Doc;
  /**
   * The promise must be resolved once the provider is connected and ready.
   */
  initialized: Promise<unknown>;
};

export { type CollaborationInitializer };
