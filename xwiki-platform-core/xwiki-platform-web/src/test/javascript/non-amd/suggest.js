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
