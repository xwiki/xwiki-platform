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
import { parse } from "yaml";
import type { PageReader } from "@xwiki/cristal-page-api";

/**
 * @since 0.20
 * @beta
 */
class DefaultPageReader implements PageReader {
  readPage(pageContent: string): { content: string; [key: string]: unknown } {
    const lines = pageContent.split("\n");
    if (lines[0] == "---") {
      // Remove the first element
      lines.shift();
      const endFrontMatterIndex = lines.findIndex((line) => line == "---");
      if (endFrontMatterIndex >= 0) {
        // We only try to read the front matter if we can find two '---' lines
        const frontMatterContent = lines
          .slice(0, endFrontMatterIndex)
          .join("\n");
        const parsedYaml = parse(frontMatterContent);
        return {
          ...parsedYaml,
          content: lines.slice(endFrontMatterIndex + 1).join("\n"),
        };
      } else {
        return {
          content: pageContent,
        };
      }
    } else {
      // When the page does not start with '---' it's content is returned as-is
      return {
        content: pageContent,
      };
    }
  }
}

export { DefaultPageReader };
