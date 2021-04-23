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

define(['xwiki-json-merge'], (merge) => {
  describe('jsonMerge.js', () => {
    it('merges two undefined', () => {
      expect(merge(undefined, undefined)).toBe(null)
    })

    it('merges two null', () => {
      expect(merge(null, null)).toBe(null)
    })

    it('merges two empty objects', () => {
      expect(merge({}, {})).toEqual({})
    })

    it('merges an object and an empty object', () => {
      expect(merge({}, {a: 1})).toEqual({a: 1})
      expect(merge({a: 1}, {})).toEqual({a: 1})
    })

    it('merge objects with simmilar ids', () => {
      expect(merge({id: 1, a: 1}, {id: 1, a: 2})).toEqual({id: 1, a: 2})
    })
    
    it('does not merge objects with different ids', () => {
      expect(merge({id: 1, a: 1}, {id: 2, a: 2})).toEqual({id: 2, a: 2})
    })

    it('overrides a field', () => {
      expect(merge({a: 0, b: 1}, {a: 1})).toEqual({a: 1, b: 1})
      expect(merge({a: 0}, {a: 1, b: 1})).toEqual({a: 1, b: 1})
    })

    it('merges arrays', () => {
      expect(merge([{id: 1}, {id: 2}], [{id: 3}, {id: 4}])).toEqual([{id: 3}, {id: 4}, {id: 1}, {id: 2}]);
    })
  })
});