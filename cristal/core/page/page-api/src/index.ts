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

/**
 * Provide the operations to read a page content
 * @since 0.20
 * @beta
 */
interface PageReader {
  /**
   * Parse the provided page content, and returns a json object containing the actual page content plus the
   * available metadata. The metadata are expected to be provided as a yaml object wrapped between two lines
   * containing exactly '---'
   * @param pageContent - the provided page content
   */
  readPage(pageContent: string): { [key: string]: unknown };
}

/**
 * @since 0.20
 * @beta
 */
interface PageWriter {
  writePage(pageContent: { [key: string]: unknown }): string;
}

export type { PageReader, PageWriter };
