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

import { ModelReferenceHandler } from "./modelReferenceHandler";
import { ModelReferenceHandlerProvider } from "./modelReferenceHandlerProvider";
import { inject, injectable } from "inversify";
import type { CristalApp } from "@xwiki/cristal-api";

/**
 * Default implementation for {@link ModelReferenceHandlerProvider}.
 * Will provide an instance of
 * {@link ./defaultModelReferenceHandler#DefaultModelReferenceHandler} as a
 * fallback if no better fit was registered.
 *
 * @since 0.13
 */
@injectable()
class DefaultModelReferenceHandlerProvider
  implements ModelReferenceHandlerProvider
{
  constructor(
    @inject<CristalApp>("CristalApp") private cristalApp: CristalApp,
  ) {}

  get(type?: string): ModelReferenceHandler | undefined {
    const container = this.cristalApp.getContainer();
    const resolvedType = type || this.cristalApp.getWikiConfig().getType();
    // If there is no specific ModelReferenceHandler registered for the
    // requested type, we return an instance of DefaultModelReferenceHandler
    // (which should be the only unnamed one).
    if (container.isBoundNamed("ModelReferenceHandler", resolvedType)) {
      return container.getNamed("ModelReferenceHandler", resolvedType);
    } else {
      return container.get("ModelReferenceHandler");
    }
  }
}

export { DefaultModelReferenceHandlerProvider };
