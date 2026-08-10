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
  ModelReferenceParser,
  ModelReferenceSerializer,
} from "@xwiki/platform-model-reference-api";
import type {
  RemoteURLParser,
  RemoteURLSerializer,
} from "@xwiki/platform-model-remote-url-api";
import type { ResourceReference } from "@xwiki/platform-rendering-api";
import type { Component } from "vue";

/**
 * Component manager role identifier used to register {@link LinkTargetTypeExtension} implementations with
 * `@xwiki/platform-component-manager-default`'s shared `manager`. The extension's own `type` field is used as the
 * registration name (hint).
 *
 * @since 18.7.0RC1
 * @beta
 */
const linkTargetTypeExtensionRole: unique symbol = Symbol(
  "LinkTargetTypeExtension",
);

/**
 * Services required to parse a plain URL into a link target's configuration, and to serialize it back into one.
 * Passed explicitly at call time, rather than through constructor injection, because
 * {@link LinkTargetTypeExtension} implementations are resolved through the shared client-side component
 * manager's own, isolated container, which has no access to the hosting application's domain-service container
 * (e.g., the `depsContainer` used elsewhere to resolve `LinkSuggestService`).
 *
 * @since 18.7.0RC1
 * @beta
 */
type LinkTargetUrlContext = {
  remoteURLParser: RemoteURLParser;
  remoteURLSerializer: RemoteURLSerializer;
};

/**
 * Services required to convert a link target's configuration into the reference of the resource it
 * designates, and to convert such a reference back into a configuration. Kept separate from
 * {@link LinkTargetUrlContext} since it is a distinct, optional concern (resource-reference
 * round-tripping) needing different services, so that implementations/callers only depend on the
 * subset of services their supported operations actually need.
 *
 * @since 18.7.0RC1
 * @beta
 */
type LinkTargetReferenceContext = {
  modelReferenceParser: ModelReferenceParser;
  modelReferenceSerializer: ModelReferenceSerializer;
};

/**
 * Contract implemented by a link target type, i.e., a kind of link that can be created and edited from the link
 * modal (e.g., a link to a page, to an attachment, to an URL, to an e-mail address, or any other kind contributed
 * by an extension).
 *
 * Implementations are registered with `@xwiki/platform-component-manager-default`'s shared `manager`, using
 * {@link linkTargetTypeExtensionRole} as the role and the extension's own `type` field as the registration name
 * (hint):
 *
 * ```ts
 * manager.registerComponent(linkTargetTypeExtensionRole, async () => MyLinkTargetType, { name: "my-type" });
 * ```
 *
 * Registering a new implementation under the *same* name as an existing one, with a lower `priority` (a
 * `registerComponent` option, defaulting to `1000`, lower wins) replaces it. There is no built-in way to hide a
 * type without providing a full replacement this way — use {@link isEnabled} instead.
 *
 * Implementations must be usable with a no-argument constructor: they are instantiated through the component
 * manager's own, isolated Inversify container, which is not connected to any other dependency injection container
 * used by the hosting application. Anything an implementation needs beyond its own logic must be received as an
 * explicit argument (see {@link LinkTargetUrlContext}), not through `@inject()`.
 *
 * @typeParam TConfig - the shape of this link target type's configuration
 *
 * @since 18.7.0RC1
 * @beta
 */
interface LinkTargetTypeExtension<TConfig = unknown> {
  /**
   * Stable identifier of this link target type. Used as the corresponding {@link LinkTarget}'s `type` field, and
   * as the registration name (hint) with the component manager.
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
   * @returns the Vue component rendering this type's fields in the link modal. Loaded lazily (e.g., through a
   *   dynamic `import()`) so that link target types the user never selects do not increase the initial bundle
   *   size.
   */
  component(): Promise<Component>;

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

  /**
   * @param reference - the reference of a linked resource
   * @param ctx - services needed to inspect the reference
   * @returns the parsed configuration if `reference` belongs to this link target type, `null` otherwise.
   *   Unlike {@link tryParseUrl}, implementations MUST NOT act as a catch-all here: they must only claim
   *   the specific resource type(s) they actually represent, so that `resourceReferenceToLinkTarget` can
   *   return `undefined` for resource types with no matching link target and let the caller fall back to
   *   {@link parseLinkTarget}.
   *
   * Optional: extensions that don't implement it are simply skipped by `resourceReferenceToLinkTarget`.
   */
  tryParseReference?(
    reference: ResourceReference,
    ctx: LinkTargetReferenceContext,
  ): TConfig | null;

  /**
   * The reverse of {@link tryParseReference}. Returns `undefined`, not a placeholder reference, when the
   * configuration doesn't designate a resource yet (e.g. no page selected, empty URL). Optional, for the
   * same reason as {@link tryParseReference}.
   *
   * @param config - this type's configuration
   * @param ctx - services needed to build the reference
   */
  configToReference?(
    config: TConfig,
    ctx: LinkTargetReferenceContext,
  ): ResourceReference | undefined;

  /**
   * @returns whether this link target type is currently offered to the user for creating/switching to a new
   *   link. Defaults to always enabled when unset. This only affects the type selector, not the ability to
   *   correctly display and re-submit a link that already uses this type ({@link tryParseUrl}/
   *   {@link serializeUrl} are always available regardless of this flag) — this is the supported way to
   *   remove/hide a link target type (including a built-in one) without providing a full replacement
   *   implementation.
   */
  isEnabled?(): boolean | Promise<boolean>;
}

export { linkTargetTypeExtensionRole };
export type {
  LinkTargetReferenceContext,
  LinkTargetTypeExtension,
  LinkTargetUrlContext,
};
