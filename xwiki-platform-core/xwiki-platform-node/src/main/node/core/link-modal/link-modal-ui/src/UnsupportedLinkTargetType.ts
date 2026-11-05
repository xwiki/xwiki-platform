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
import UnsupportedLinkTargetForm from "./vue/UnsupportedLinkTargetForm.vue";
import { unsupportedLinkTargetType } from "@xwiki/platform-link-type-api";
import type {
  LinkTargetTypeExtension,
  UnsupportedLinkTargetConfig,
} from "@xwiki/platform-link-type-api";
import type { Component } from "vue";

/**
 * The link target type of the edited link when it points to a resource that no registered link target type supports
 * (see `unsupportedLinkTargetType`). It's not registered in the container: the link modal creates it for the edited
 * link only, so that the link type selector offers it next to the registered types, and the reference of the linked
 * resource is edited as plain text.
 */
class UnsupportedLinkTargetType
  implements LinkTargetTypeExtension<UnsupportedLinkTargetConfig>
{
  readonly type = unsupportedLinkTargetType;

  private readonly originalConfig: UnsupportedLinkTargetConfig;

  /**
   * @param config - the configuration of the edited link, copied (see {@link copyConfig}) so that it can be restored
   *   after the link modal modifies it
   */
  constructor(config: UnsupportedLinkTargetConfig) {
    this.originalConfig = copyConfig(config);
  }

  /**
   * @returns the type of the linked resource, as is: there's no translation for a type that no link target type
   *   supports
   */
  async getLabel(): Promise<string> {
    return this.originalConfig.reference.type;
  }

  /**
   * @returns a copy of the configuration of the edited link, so that selecting this type again in the link type
   *   selector restores the original reference
   */
  createDefaultConfig(): UnsupportedLinkTargetConfig {
    return copyConfig(this.originalConfig);
  }

  component(): Component {
    return UnsupportedLinkTargetForm;
  }

  /**
   * @returns `null`: this type is never used to parse a URL, since it's not registered in the container
   */
  tryParseURL(): null {
    return null;
  }

  /**
   * @returns an empty string: the URL of a resource that no link target type supports can't be computed client-side,
   *   so the callers keep the URL of the edited link
   */
  serializeURL(): string {
    return "";
  }
}

/**
 * Copy a configuration, so that the copy can be modified without affecting the original: the link modal edits the
 * configuration of the link target in place. A JSON round trip is enough because the configuration holds only a
 * resource reference, which is plain JSON data, and unlike `structuredClone()` it also accepts a Vue reactive proxy.
 *
 * @param config - the configuration to copy
 * @returns a deep copy of the configuration
 */
function copyConfig(
  config: UnsupportedLinkTargetConfig,
): UnsupportedLinkTargetConfig {
  return JSON.parse(JSON.stringify(config));
}

export { UnsupportedLinkTargetType };
