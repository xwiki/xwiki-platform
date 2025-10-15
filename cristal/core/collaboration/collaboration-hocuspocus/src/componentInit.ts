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

import { HocuspocusCollaborationProvider } from "./hocuspocusCollaborationProvider";
import { collaborationManagerName } from "@xwiki/cristal-collaboration-api";
import type { CollaborationManager } from "@xwiki/cristal-collaboration-api";
import type { Container } from "inversify";

/**
 * @since 0.20
 * @beta
 */
class ComponentInit {
  constructor(container: Container) {
    // Register the hocuspocus collaboration provider as the default for legacy reason.
    container
      .bind<CollaborationManager>(collaborationManagerName)
      .to(HocuspocusCollaborationProvider)
      .inSingletonScope()
      .whenDefault();
  }
}

export { ComponentInit };
