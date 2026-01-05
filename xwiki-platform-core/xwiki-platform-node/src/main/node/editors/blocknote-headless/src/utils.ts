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

/**
 * Convert a string to a color. * Similar to a hash, changing the input string slightly will result in a totally
 * different color. Colors should be expected to be nice-looking and different enough from one another.
 *
 * @param str - An input string (e.g., username, document title, etc.)
 * @param prc - Optional lightness/darkness variation number
 *
 * @returns The generated color, in hexadecimal (e.g. `#789ABC`)
 *
 * @since 18.0.0RC1
 * @beta
 */
function stringToColor(str: string, prc?: number): string {
  // Check for optional lightness/darkness
  prc = prc ?? -10;

  let hash = 0;

  for (let i = 0; i < str.length; i++) {
    hash = str.charCodeAt(i) + ((hash << 5) - hash);
  }

  const rgba =
    ((hash >> 24) & 0xff).toString(16) +
    ((hash >> 16) & 0xff).toString(16) +
    ((hash >> 8) & 0xff).toString(16) +
    (hash & 0xff).toString(16);

  const num = parseInt(rgba, 16),
    amt = Math.round(2.55 * prc),
    R = (num >> 16) + amt,
    G = ((num >> 8) & 0x00ff) + amt,
    B = (num & 0x0000ff) + amt;

  const comp =
    0x1000000 +
    (R < 255 ? (R < 1 ? 0 : R) : 255) * 0x10000 +
    (G < 255 ? (G < 1 ? 0 : G) : 255) * 0x100 +
    (B < 255 ? (B < 1 ? 0 : B) : 255);

  return `#${comp.toString(16).slice(1)}`;
}

export { stringToColor };
