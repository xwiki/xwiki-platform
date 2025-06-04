/*
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
 * Trigger the loading of CSS resources (by injecting link tags in the page HTML head) without waiting for them to be
 * successfully loaded.
 *
 * Usage example:
 *
 * require(['css!module'], () => {
 *   // Code that doesn't depend on the CSS being loaded.
 * });
 *
 * @since 17.5.0RC1
 */
define("css", [], function () {
  return {
    load: function (name, parentRequire, onLoad, config) {
      const link = document.createElement("link");
      link.type = "text/css";
      link.rel = "stylesheet";
      link.href = parentRequire.toUrl(name + "-css");
      document.getElementsByTagName("head")[0]?.appendChild(link);
      onLoad();
    },
  };
});
