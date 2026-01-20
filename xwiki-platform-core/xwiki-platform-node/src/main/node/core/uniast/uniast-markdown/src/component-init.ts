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
import { DefaultMarkdownToUniAstConverter } from "./markdown/default-markdown-to-uni-ast-converter";
import { DefaultUniAstToMarkdownConverter } from "./markdown/default-uni-ast-to-markdown-converter";
import { ParserConfigurationResolver } from "./markdown/internal-links/parser/parser-configuration-resolver";
import { InternalLinksSerializerResolver } from "./markdown/internal-links/serializer/internal-links-serializer-resolver";
import type { MarkdownParserConfiguration } from "./markdown/internal-links/parser/markdown-parser-configuration";
import type { InternalLinksSerializer } from "./markdown/internal-links/serializer/internal-links-serializer";
import type { MarkdownToUniAstConverter } from "./markdown/markdown-to-uni-ast-converter";
import type { UniAstToMarkdownConverter } from "./markdown/uni-ast-to-markdown-converter";
import type { Container, Factory, Newable, ResolutionContext } from "inversify";

/**
 * @since 18.0.0RC1
 * @beta
 */
const markdownToUniAstConverterName = "MarkdownToUniAstConverter";

/**
 * @since 18.0.0RC1
 * @beta
 */
const uniAstToMarkdownConverterName = "UniAstToMarkdownConverter";

/**
 * @since 18.0.0RC1
 * @beta
 */
class ComponentInit {
  constructor(container: Container) {
    container
      .bind<MarkdownToUniAstConverter>(markdownToUniAstConverterName)
      .to(DefaultMarkdownToUniAstConverter)
      .whenDefault();

    container
      .bind<UniAstToMarkdownConverter>(uniAstToMarkdownConverterName)
      .to(DefaultUniAstToMarkdownConverter)
      .whenDefault();

    // New components loading pattern attempt (as of Cristal 0.22).
    // A resolver is declared. Its job is to resolve the right factory based
    // on the current configuration type.
    // Then, the factory is loaded asynchronously with a dynamic import so that
    // only the code corresponding to the current configuration is loaded.
    container
      .bind<InternalLinksSerializerResolver>("InternalLinksSerializerResolver")
      .to(InternalLinksSerializerResolver);
    // Factories for the officially supported backends are registered statically
    // But nothing prevents factories for other backends to be registered at
    // initialization time.
    // The main side effect of regisering those factories is a few kb of
    // initialization code being bundled for nothing (i.e., for the backends
    // that are not going to be used).
    this.initXWikiFactory(container);
    this.initNextcloudFactory(container);
    this.initGitHubFactory(container);
    this.initFileSystemFactory(container);

    container
      .bind<ParserConfigurationResolver>("ParserConfigurationResolver")
      .to(ParserConfigurationResolver)
      .whenDefault();

    // Overrides the configuration for the XWiki backend
    container
      .bind<Factory<MarkdownParserConfiguration>>(
        "Factory<MarkdownParserConfiguration>",
      )
      .toFactory(() => {
        return () => {
          return {
            supportFlexmarkInternalLinks: true,
          };
        };
      })
      .whenNamed("XWiki");
  }

  private initXWikiFactory(container: Container) {
    const name = "XWiki";
    container
      .bind<Factory<Promise<InternalLinksSerializer>>>(
        "Factory<InternalLinksSerializer>",
      )
      .toFactory((context) => {
        return async () => {
          const component = (
            await import(
              "./markdown/internal-links/serializer/xwiki-internal-link-serializer"
            )
          ).XWikiInternalLinkSerializer;
          return this.bindAndLoad(container, name, component, context);
        };
      })
      .whenNamed(name);
  }
  private initNextcloudFactory(container: Container) {
    const name = "Nextcloud";
    container
      .bind<Factory<Promise<InternalLinksSerializer>>>(
        "Factory<InternalLinksSerializer>",
      )
      .toFactory((context) => {
        return async () => {
          const component = (
            await import(
              "./markdown/internal-links/serializer/nextcloud-internal-link-serializer"
            )
          ).NextcloudInternalLinkSerializer;
          return this.bindAndLoad(container, name, component, context);
        };
      })
      .whenNamed(name);
  }
  private initGitHubFactory(container: Container) {
    const name = "GitHub";
    container
      .bind<Factory<Promise<InternalLinksSerializer>>>(
        "Factory<InternalLinksSerializer>",
      )
      .toFactory((context) => {
        return async () => {
          const component = (
            await import(
              "./markdown/internal-links/serializer/github-internal-link-serializer"
            )
          ).GitHubInternalLinkSerializer;
          return this.bindAndLoad(container, name, component, context);
        };
      })
      .whenNamed(name);
  }
  private initFileSystemFactory(container: Container) {
    const name = "FileSystem";
    container
      .bind<Factory<Promise<InternalLinksSerializer>>>(
        "Factory<InternalLinksSerializer>",
      )
      .toFactory((context) => {
        return async () => {
          const component = (
            await import(
              "./markdown/internal-links/serializer/filesystem-internal-link-serializer"
            )
          ).FilesystemInternalLinkSerializer;
          return this.bindAndLoad(container, name, component, context);
        };
      })
      .whenNamed(name);
  }

  /**
   * Registed the component in the container on demand.
   *
   * @param container - the container
   * @param name - the name of the component interface
   * @param component - the actual component to register
   * @param context - the context
   */
  private bindAndLoad<T extends InternalLinksSerializer>(
    container: Container,
    name: string,
    component: Newable<T>,
    context: ResolutionContext,
  ) {
    if (!container.isBound("InternalLinksSerializer", { name: name })) {
      container
        .bind<InternalLinksSerializer>("InternalLinksSerializer")
        .to(component)
        .whenNamed(name);
    }
    return context.get<InternalLinksSerializer>("InternalLinksSerializer", {
      name: name,
    });
  }
}

export {
  ComponentInit,
  markdownToUniAstConverterName,
  uniAstToMarkdownConverterName,
};
