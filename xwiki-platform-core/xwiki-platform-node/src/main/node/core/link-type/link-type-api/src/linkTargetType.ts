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
import type { ResourceReference } from "@xwiki/platform-rendering-api";

/**
 * Inversify role used to register {@link LinkTargetType} implementations. Multiple, independent packages can each
 * bind their own implementation to this role, without a name or a tag (no `.whenNamed()`/`.whenDefault()`): the
 * implementations are told apart by their {@link LinkTargetType."type"} field. Use the {@link LinkTargetTypeManager}
 * to access the registered implementations.
 *
 * @since 18.10.0RC1
 * @beta
 */
const linkTargetTypeRole = "LinkTargetType";

/**
 * Contract implemented by a link target type, i.e., a kind of link that can be created and edited from the link
 * modal (e.g., a link to a page, to an attachment, to an URL, to an e-mail address, or any other kind contributed
 * by an extension).
 *
 * Implementations are components, instantiated by the container they are registered in: they get the services they
 * need through constructor injection. Services that depend on the current wiki (e.g. the model reference parser)
 * should be resolved through their provider (e.g. `ModelReferenceParserProvider`) each time they are used.
 *
 * This contract doesn't depend on any UI framework: the form used to edit a link of this type in the link modal is
 * registered separately (see `LinkTargetTypeForm` in `@xwiki/platform-link-modal-ui`). A link target type registered
 * without a form is edited with a plain text field, so its configuration must then be a
 * {@link PlainTextLinkTargetConfig} (see {@link AbstractPlainTextLinkTargetType}).
 *
 * @typeParam TConfig - the shape of this link target type's configuration
 *
 * @since 18.10.0RC1
 * @beta
 */
interface LinkTargetType<TConfig = unknown> {
  /**
   * Stable identifier of this link target type, used as the corresponding {@link LinkTarget}'s `type` field.
   * Since bindings to {@link linkTargetTypeRole} are unconstrained, this is the only thing
   * disambiguating one registered implementation from another.
   */
  readonly type: string;

  /**
   * Ordering hint, used both to sort the link type selector and to decide in which order
   * {@link LinkTargetType.fromURL} is attempted against a raw URL (lower runs/shows first). Defaults
   * to `1000` when unset. A permissive catch-all implementation (e.g., the built-in `"url"` type) should use a
   * very high value so it is only ever picked once every other type has failed to match.
   */
  readonly order?: number;

  /**
   * Each link target type resolves its own label, since there is no mechanism for an extension to register its
   * translations where the link modal could find them. Implementations cannot rely on the Vue composition API
   * (`useI18n()`) since they are plain objects, not Vue component instances: they can resolve the label from
   * translations bundled with the extension, or fetch it from the server (hence the promise). The label is only
   * displayed: the link type selector identifies the selected link target type by its {@link LinkTargetType."type"},
   * so labels don't have to be unique.
   *
   * @param locale - the locale to resolve the label for (e.g., `"en"`)
   * @returns a promise resolving to a localized, human-readable label for this link target type, displayed in the
   *   link type selector
   */
  getLabel(locale: string): Promise<string>;

  /**
   * @returns a fresh default configuration, used when the user switches the link type selector to this type.
   */
  createDefaultConfig(): TConfig;

  /**
   * Convert a URL into the configuration of a link of this type.
   *
   * @param url - a raw URL, as found in a link
   * @returns the configuration of the link if `url` belongs to this link target type, `null` otherwise
   */
  fromURL(url: string): TConfig | null;

  /**
   * Convert the configuration of a link of this type into a URL. The reverse of {@link LinkTargetType.fromURL}.
   *
   * @param config - the configuration of the link
   * @returns the URL of the link
   */
  toURL(config: TConfig): string;

  /**
   * Compute the label of a link of this type, used when the user leaves its display text empty.
   *
   * Optional: the link modal uses the URL of the link (see {@link LinkTargetType.toURL}) for the link target types that
   * don't implement it.
   *
   * @param config - the configuration of the link
   * @returns a short, human-readable name of the link target (e.g. the title of a page, the name of a file, an e-mail
   *   address), or an empty string when the configuration doesn't designate a target yet
   */
  getDefaultLabel?(config: TConfig): string;

  /**
   * Convert the reference of a linked resource into the configuration of a link of this type. Unlike
   * {@link LinkTargetType.fromURL}, implementations must not act as a catch-all here: they must only accept the
   * resource types they represent, so that the reference of a resource that no link target type supports is
   * preserved (see {@link unsupportedLinkTargetType}).
   *
   * Optional: the link target types that don't implement it are skipped when converting a reference.
   *
   * @param reference - the reference of a linked resource
   * @returns the configuration of the link if `reference` belongs to this link target type, `null` otherwise
   */
  fromReference?(reference: ResourceReference): TConfig | null;

  /**
   * Convert the configuration of a link of this type into the reference of the linked resource. The reverse of
   * {@link LinkTargetType.fromReference}, optional for the same reason.
   *
   * @param config - the configuration of the link
   * @returns the reference of the linked resource, or `undefined` (not a placeholder reference) when the
   *   configuration doesn't designate a resource yet (e.g. no page selected, empty URL)
   */
  toReference?(config: TConfig): ResourceReference | undefined;
}

export { linkTargetTypeRole };
export type { LinkTargetType };
