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
import { editorFeatureName } from "./features/editorFeature";
import { fileTransferFeature } from "./features/fileTransfer/fileTransfer";
import type { EditorFeature } from "./features/editorFeature";
import type { Container } from "inversify";

/**
 * Binds the generic editing features in the given service container. The editor has no feature of its own, so an
 * integration that doesn't call this gets an editor without them (e.g. without the handling of the pasted and dropped
 * files), unless it binds its own. The features are bound in the order they contribute in (e.g. to the formatting
 * toolbar). An integration needing only some of them, another order, or its own version of one of them binds the
 * exported features itself (e.g. `fileTransferFeature`) instead of calling this.
 *
 * @since 18.10.0RC1
 * @beta
 */
export class ComponentInit {
  constructor(container: Container) {
    for (const feature of [fileTransferFeature]) {
      container.bind<EditorFeature>(editorFeatureName).toConstantValue(feature);
    }
  }
}
