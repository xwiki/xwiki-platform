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
import type {
  RemoteURLParser,
  RemoteURLSerializer,
} from "@xwiki/platform-model-remote-url-api";
import type { Component } from "vue";

/**
 * Inversify role used to register {@link LinkTargetTypeExtension} implementations against the shared
 * `depsContainer` (the same container already used to resolve other domain services, e.g.
 * `LinkSuggestService`). Multiple, independent packages can each bind their own implementation to this same
 * role — see {@link LinkTargetTypeExtension} for the registration example — and every registered implementation
 * is returned by `container.getAll(linkTargetTypeExtensionRole)`. This mirrors the existing `"SyntaxConfig"` role
 * (see `@xwiki/platform-syntaxes-config`): an unconstrained (no `.whenNamed()`/`.whenDefault()`) multi-binding,
 * disambiguated by a plain `type` field on each resolved object rather than by an Inversify name/tag.
 *
 * @since 18.7.0RC1
 * @beta
 */
const linkTargetTypeExtensionRole = "LinkTargetTypeExtension";

/**
 * Services required to parse a plain URL into a link target's configuration, and to serialize it back into one.
 * Passed explicitly at call time, rather than through constructor injection, so that implementations stay
 * decoupled from exactly which services the caller happens to have on hand.
 *
 * @since 18.7.0RC1
 * @beta
 */
type LinkTargetUrlContext = {
  remoteURLParser: RemoteURLParser;
  remoteURLSerializer: RemoteURLSerializer;
};

/**
 * Contract implemented by a link target type, i.e., a kind of link that can be created and edited from the link
 * modal (e.g., a link to a page, to an attachment, to an URL, to an e-mail address, or any other kind contributed
 * by an extension).
 *
 * @typeParam TConfig - the shape of this link target type's configuration
 *
 * @since 18.7.0RC1
 * @beta
 */
interface LinkTargetTypeExtension<TConfig = unknown> {
  /**
   * Stable identifier of this link target type, used as the corresponding {@link LinkTarget}'s `type` field.
   * Since bindings to {@link linkTargetTypeExtensionRole} are unconstrained, this is the only thing
   * disambiguating one registered implementation from another.
   */
  readonly type: string;

  /**
   * Ordering hint, used both to sort the link type selector and to decide in which order
   * {@link LinkTargetTypeExtension.tryParseUrl} is attempted against a raw URL (lower runs/shows first). Defaults
   * to `1000` when unset. A permissive catch-all implementation (e.g., the built-in `"url"` type) should use a
   * very high value so it is only ever picked once every other type has failed to match.
   */
  readonly order?: number;

  /**
   * @param locale - the locale to resolve the label for (e.g., `"en"`)
   * @returns a localized, human-readable label for this link target type, displayed in the link type selector.
   *   Implementations cannot rely on the Vue composition API (`useI18n()`) since they are plain objects, not Vue
   *   component instances — resolve the label from the extension's own bundled translations instead.
   */
  getLabel(locale: string): string;

  /**
   * @returns a fresh default configuration, used when the user switches the link type selector to this type.
   */
  createDefaultConfig(): TConfig;

  /**
   * @returns the Vue component rendering this type's fields in the link modal.
   */
  component(): Component;

  /**
   * @param url - a raw URL, as found in a link
   * @param ctx - services needed to inspect the URL
   * @returns the parsed configuration if `url` belongs to this link target type, `null` otherwise
   */
  tryParseUrl(url: string, ctx: LinkTargetUrlContext): TConfig | null;

  /**
   * The reverse operation of {@link tryParseUrl}: turns a configuration of this type back into a raw URL.
   *
   * @param config - this type's configuration
   * @param ctx - services needed to build the URL
   * @returns the serialized URL
   */
  serializeUrl(config: TConfig, ctx: LinkTargetUrlContext): string;
}

export { linkTargetTypeExtensionRole };
export type { LinkTargetTypeExtension, LinkTargetUrlContext };
