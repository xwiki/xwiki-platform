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
export {};

declare global {
  const XWiki: {
    contextPath: string;
    currentWiki: string;
  };

  /**
   * The suggest widget's jQuery plugin, declared by `uicomponents/suggest/xwiki.selectize.js`, which the displayer
   * template imports. It ships no type definitions of its own.
   */
  interface JQuery {
    xwikiSelectize(settings: unknown): JQuery;
  }

  /**
   * The Tom Select instance the suggest widget stores on the element it enhances; absent on a plain field.
   */
  interface Element {
    selectize?: {
      clear: (silent?: boolean) => void;
      clearOptions: () => void;
      setValue: (value: string, silent?: boolean) => void;
      /**
       * The element the widget is rendered in, which replaces the enhanced field on screen.
       */
      wrapper: HTMLElement;
    };
  }
}
