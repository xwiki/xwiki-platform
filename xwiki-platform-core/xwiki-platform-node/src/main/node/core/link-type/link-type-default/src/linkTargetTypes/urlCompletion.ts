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

// The scheme of a URL (RFC 3986), followed by its separator.
const SCHEME = /^([a-z][a-z0-9+.-]*):/i;

// A host name or address, with an optional port, followed by the end of the URL or by its path, query string or anchor,
// e.g. "www.example.org", "localhost:8080/xwiki", "192.168.0.1?q=1" or "[::1]:8080".
const HOST =
  /^(?:localhost|[^\s/?#:.]+(?:\.[^\s/?#:.]+)+|\[[0-9a-f:.]+\])(?::\d+)?(?:[/?#]|$)/i;

// The schemes of the URLs that don't designate a host, which could otherwise be taken for a host name followed by a
// port (e.g. "tel:0123456789").
const OPAQUE_SCHEMES = new Set([
  "mailto",
  "tel",
  "sms",
  "callto",
  "urn",
  "data",
]);

/**
 * @param url - a URL, absolute or relative
 * @returns whether the given URL starts with a scheme (e.g. "https:" or "mailto:"), i.e. whether it's an absolute URL.
 *   A host name followed by a port (e.g. "localhost:8080") doesn't count as a scheme.
 *
 * @since 18.10.0RC1
 * @beta
 */
function hasURLScheme(url: string): boolean {
  const scheme = SCHEME.exec(url)?.[1];
  return (
    scheme !== undefined &&
    (url.startsWith(`${scheme}://`) ||
      OPAQUE_SCHEMES.has(scheme.toLowerCase()) ||
      !HOST.test(url))
  );
}

/**
 * Complete a URL typed by the user, the way a browser address bar does: a URL starting with a host name (e.g.
 * "www.example.org/docs") gets the "https://" scheme. The other URLs are left as they are, apart from the surrounding
 * white space: the absolute ones, and the relative ones (e.g. "/xwiki/bin/view/Main/", "../docs" or "#anchor"), which
 * designate a resource relative to the page displaying the link.
 *
 * @param url - the URL typed by the user
 * @returns the completed URL
 *
 * @since 18.10.0RC1
 * @beta
 */
function completeURL(url: string): string {
  const trimmed = url.trim();
  return !hasURLScheme(trimmed) && HOST.test(trimmed)
    ? `https://${trimmed}`
    : trimmed;
}

export { completeURL, hasURLScheme };
