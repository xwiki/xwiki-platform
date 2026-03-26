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
import type { MarkdownToUniAstConverter } from "./markdown/markdown-to-uni-ast-converter";
import type { UniAstToMarkdownConverter } from "./markdown/uni-ast-to-markdown-converter";
import type { Container, Factory } from "inversify";

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
}

export {
  ComponentInit,
  markdownToUniAstConverterName,
  uniAstToMarkdownConverterName,
};
