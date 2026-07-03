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
define('xwiki-realtime-textCursor', function() {
  'use strict';

  function transformCursor(cursor, op) {
    if (!op) {
      return cursor;
    }

    const pos = op.offset;
    const remove = op.toRemove;
    const insert = op.toInsert.length;
    if (typeof cursor === 'undefined') {
      return;
    }
    if (typeof remove === 'number' && pos < cursor) {
      cursor -= Math.min(remove, cursor - pos);
    }
    if (typeof insert === 'number' && pos < cursor) {
      cursor += insert;
    }
    return cursor;
  }

  return {
    transformCursor: (cursor, ops) => {
      if (!Array.isArray(ops)) {
        ops = [ops];
      }
      for (let i = ops.length - 1; i >= 0; i--) {
        cursor = transformCursor(cursor, ops[i]);
      }
      return cursor;
    }
  };
});
