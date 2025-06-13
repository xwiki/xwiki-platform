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
import { reactive } from "vue";

/**
 * A service providing footnotes related operations.
 */
export class FootnotesService {
  constructor() {
    this._footnotes = reactive([]);
  }

  /**
   * Register a new footnote. If a footnote with the same translationKey is already registered this
   * method has no effect on the list of registered footnotes.
   * @param symbol the symbol to identify the entries related to the footnote
   * @param translationKey the translation key of the footnote text
   */
  put(symbol, translationKey) {
    if (!this._footnotes.some(footnote => footnote.translationKey === translationKey)) {
      this._footnotes.push({ symbol, translationKey });
    }
  }

  reset() {
    this._footnotes.splice(0);
  }

  list() {
    return this._footnotes;
  }
}
