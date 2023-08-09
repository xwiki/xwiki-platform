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
require.config({
  paths: {
    'xwiki': 'js/xwiki/xwiki.min'
  },
  shim: {
    'xwiki': ['xwiki-entityReference']
  }
});

define(['xwiki'], function() {

  describe("XWiki", function() {
    describe("XWiki.Document.getRestSearchURL", function() {

      it("calls without arguments", function() {
        var url = XWiki.Document.getRestSearchURL();
        expect("${request.contextPath}/rest/wikis/xwiki/search").toEqual(url);
      });

      it("calls with query string", function() {
        var url = XWiki.Document.getRestSearchURL('a=b&x=y');
        expect("${request.contextPath}/rest/wikis/xwiki/search?a=b&x=y").toEqual(url);
      });

      it("calls with space", function() {
        var url = XWiki.Document.getRestSearchURL('', 'foo');
        expect("${request.contextPath}/rest/wikis/xwiki/spaces/foo/search").toEqual(url);
      });

      it("calls with query string and spaces", function() {
        var url = XWiki.Document.getRestSearchURL('a=b', ['foo', 'bar']);
        expect("${request.contextPath}/rest/wikis/xwiki/spaces/foo/spaces/bar/search?a=b").toEqual(url);
      });

      it("calls with empty spaces array", function() {
        var url = XWiki.Document.getRestSearchURL(null, []);
        expect("${request.contextPath}/rest/wikis/xwiki/search").toEqual(url);
      });

      it("calls with spaces and wiki", function() {
        var url = XWiki.Document.getRestSearchURL('', ['foo', 'bar'], 'test');
        expect("${request.contextPath}/rest/wikis/test/spaces/foo/spaces/bar/search").toEqual(url);
      });

      it("calls with empty spaces and wiki", function() {
        var url = XWiki.Document.getRestSearchURL('a=b', [], 'test');
        expect("${request.contextPath}/rest/wikis/test/search?a=b").toEqual(url);
      });

    });

    describe("XWiki.Document.getRestURL", function() {
      it("always escapes spaces and the page name", function () {
        const xwikiDocument = new XWiki.Document(new XWiki.DocumentReference('xwiki', ['F?o', 'Ne?ted'], 'Ba?'));
        const url = xwikiDocument.getRestURL();
        expect("${request.contextPath}/rest/wikis/xwiki/spaces/F%3Fo/spaces/Ne%3Fted/pages/Ba%3F").toEqual(url);
      });
    });
  });

});
