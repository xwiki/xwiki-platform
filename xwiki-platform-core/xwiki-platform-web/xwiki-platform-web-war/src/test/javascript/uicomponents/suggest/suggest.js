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
    'xlist': 'uicomponents/widgets/list/xlist.min',
    'suggest': 'uicomponents/suggest/suggest.min'
  },
  shim: {
    'xlist': ['prototype'],
    'suggest': ['xlist']
  }
});

define(['suggest'], function() {

  describe("Suggest", function() {
    describe("Emphasis Matcher", function() {

      it("emphasizes a single match", function() {
        var result = XWiki.widgets.Suggest.prototype.emphasizeMatches("Typed", "Value with Typed word");
        expect("Value with <em>Typed</em> word").toEqual(result);
      });

      it("emphasizes several matches", function() {
        var result = XWiki.widgets.Suggest.prototype.emphasizeMatches("Words Typed", "A Selection Of Words Been Typed");
        expect("A Selection Of <em>Words</em> Been <em>Typed</em>").toEqual(result);
      });

      it("emphasizes repeated matches", function() {
        var result = XWiki.widgets.Suggest.prototype.emphasizeMatches("To Be", "To Be, Or Not To Be");
        expect("<em>To</em> <em>Be</em>, Or Not <em>To</em> <em>Be</em>").toEqual(result);
      });

      it("preserves original case", function() {
        var result = XWiki.widgets.Suggest.prototype.emphasizeMatches("wOrDs TypEd By An eMo kID", "Words Typed By John Doe");
        expect("<em>Words</em> <em>Typed</em> <em>By</em> John Doe").toEqual(result);
      });

      it("is neutral when there are no match", function() {
        var result = XWiki.widgets.Suggest.prototype.emphasizeMatches("Rock'n'roll", "Bring me A bowl of coffee before I turn into a goat");
        expect("Bring me A bowl of coffee before I turn into a goat").toEqual(result);
      });

    });
  });

});
