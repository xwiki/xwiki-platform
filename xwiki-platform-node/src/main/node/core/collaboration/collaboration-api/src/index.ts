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

import { collaborationManagerName } from "./collaborationManagerName";
import { collaborationManagerProviderName } from "./collaborationManagerProviderName";
import { ComponentInit } from "./componentInit";
import { Status } from "./status";
import type { CollaborationInitializer } from "./collaborationInitializer";
import type { CollaborationManager } from "./collaborationManager";
import type { CollaborationManagerProvider } from "./collaborationManagerProvider";
import type { User } from "./user";

export {
  ComponentInit,
  Status,
  collaborationManagerName,
  collaborationManagerProviderName,
};

export type {
  CollaborationInitializer,
  CollaborationManager,
  CollaborationManagerProvider,
  User,
};
