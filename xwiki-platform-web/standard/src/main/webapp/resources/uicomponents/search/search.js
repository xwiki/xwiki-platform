document.observe("xwiki:dom:loaded", function() {
  $$("input.searchQuery.defaultText").each(function(item) {
    item.observe("focus", function() {
      if (this.value == this.defaultValue) {
        this.value = "";
      }
    }.bind(item));
  });
  $$("input.searchQuery").each(function(item) {
    item.observe("focus", function() {
      this.select();
    }.bind(item));
    item.observe("blur", function() {
      if (this.value == "") {
        this.value = this.defaultValue;
      }
    }.bind(item));
  });
});