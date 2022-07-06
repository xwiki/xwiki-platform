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
define(['ckeditor', 'xwiki-resource'], function(CKEDITOR) {
  describe('XWiki Resource Plugin for CKEditor', function() {
    var serializeAndParseResourceReference = function(expectedResourceReference) {
      var serializedResourceReference = CKEDITOR.plugins.xwikiResource
        .serializeResourceReference(expectedResourceReference);
      var actualResourceReference = CKEDITOR.plugins.xwikiResource.parseResourceReference(serializedResourceReference);
      expect(actualResourceReference.typed).toBe(expectedResourceReference.typed);
      expect(actualResourceReference.type).toBe(expectedResourceReference.type);
      expect(actualResourceReference.reference).toBe(expectedResourceReference.reference);
      for (var key in expectedResourceReference.parameters) {
        if (expectedResourceReference.parameters.hasOwnProperty(key)) {
          expect(actualResourceReference.parameters[key]).toBe(expectedResourceReference.parameters[key]);
        }
      }
    };

    it('serializes and parses resource references', function() {
      serializeAndParseResourceReference({
        typed: false,
        type: 'doc',
        reference: 'A.B',
        parameters: {queryString: 'a=b&x=y', anchor: 'foo'}
      });

      serializeAndParseResourceReference({
        typed: true,
        type: 'attach',
        reference: 'wiki:Space.Page@image.png'
      });

      serializeAndParseResourceReference({
        typed: true,
        type: 'space',
        reference: 'O"n\'e.T=w o',
        parameters: {foo: '1|-|2', anchor: 'h" ea="de" \'r'}
      });

      serializeAndParseResourceReference({
        typed: true,
        type: 'mailto',
        reference: 'user@example.org',
        parameters: {subject: 'a=b+1&c=d e%20', body: '?1=2%203#x y+z'}
      });
    });
  });
});
