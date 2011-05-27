var XWiki = (function (XWiki) {
  
  /**
   * The search suggest hooks itself on the search input and provide live results as the user types.
   * Search form validation is not affected (user can still type enter and get to the regular search result page)
   */
  XWiki.SearchSuggest = Class.create({
    
     /** 
      * Constructor. Prepares a light modal container on the same model as the modalPopup 
      * and registers event listerners.
      */
    initialize: function(searchInput, sources){
        
      this.sources = sources;
        
      this.searchInput = $(searchInput);
      if (!this.searchInput) {
        return;
      }

      this.searchInput.observe("keyup", this.onKeyUp.bindAsEventListener(this));

      document.observe("xwiki:suggest:clearSuggestions", this.onClearSuggestions.bindAsEventListener(this));
      document.observe("xwiki:suggest:containerCreated", this.onSuggestContainerCreated.bindAsEventListener(this)); 
      document.observe("xwiki:suggest:selected", this.onSuggestionSelected.bindAsEventListener(this));
    
      this.createSuggest();
    },
      
    /**
     * Callback triggered when the original suggest clears its suggestions.
     */
    onClearSuggestions: function(event){
      if (event.memo.suggest == this.suggest) {
        // Restore bottom border style
        this.searchInput.setStyle({'borderBottomStyle' : this.searchInputBorderBottomSavedStyle});
      }
    },
      
    /**
     * Callback triggered when the original suggest has created its results container.
     */
    onSuggestContainerCreated: function(event){
      if (event.memo.suggest == this.suggest) {
        // Save the style of the bottom border of the input field so that we can restore it later on
        this.searchInputBorderBottomSavedStyle = this.searchInput.getStyle('borderBottomStyle');
        // Hide bottom border of input field to not double the container border just under the field
        this.searchInput.setStyle({'borderBottomStyle' : 'none'});
      }
    },

    /**
     * Callback triggered when a suggestion is selected.
     * Submits the form or go to a selected page according to selection.
     */
    onSuggestionSelected: function(event) {
      if (event.memo.suggest == this.suggest) {
        event.stop();
        if (!event.memo.id) {
          // Submit form
          this.searchInput.up('form').submit();
        }
        else {
          // Go to page
          window.location = event.memo.id;;
        }
      }
    },

    /**
     * Creates the underlaying suggest widget.
     */
    createSuggest: function() {
      // Create dummy suggestion node to hold the "Show all results" option
      var valueNode = new Element('div')
            .insert(new Element('span', {'class':'suggestId'}))
            .insert(new Element('span', {'class':'suggestValue'}))
            .insert(new Element('span', {'class':'suggestInfo'}))
      var allResultsNode = new XWiki.widgets.XList([
        new XWiki.widgets.XListItem( "Show all results...", {
          'containerClasses': 'suggestItem',
          'classes': 'showAllResuts',
          'eventCallbackScope' : this,
          'noHighlight' : true,
          'value' : valueNode
        } ),
      ], 
      {
        'classes' : 'suggestList',
        'eventListeners' : {
          'click': function(event){
            this.searchInput.up('form').submit();
          },
          'mouseover':function(event){
            this.suggest.clearHighlight();
            this.suggest.iHighlighted = event.element();
            event.element().addClassName('xhighlight');
          }
        }
      });
      this.suggest = new XWiki.widgets.Suggest( this.searchInput, {
        parentContainer: $('searchSuggest'),
        className: 'searchSuggest horizontalLayout',
        fadeOnClear:false,
        align: "right",
        minchars: 3,    
        sources : this.sources,
        insertBeforeSuggestions : new Element("div", {'class' : 'results'}).update(allResultsNode.getElement()),
        displayValue:true,
        displayValueText: "in ",
        timeout: 0,
        width: 500,
        align: "right"
      });
    },
    
   /**
    * Callback triggered when a key has been typed on the virtual input.
    */
   onKeyUp: function(event){
     var key = event.keyCode;
     switch(key) {
       case Event.KEY_RETURN:
         if (!this.suggest.hasActiveSelection()) {
           event.stop();
           this.searchInput.up('form').submit();
         }
     }
   } 
    
  });   
    
  return XWiki;

})(XWiki);

document.observe("dom:loaded", function(){

  var sources = [
  ## Iterate over the sources defined in the configuration document, and create a source array to be passed to the
  ## search suggest contructor.
  #set($sourceDocument = $xwiki.getDocument("XWiki.SearchSuggestConfig"))
  #foreach($source in $sourceDocument.getObjects('XWiki.SearchSuggestSourceClass'))
    #if($source.getProperty('activated').value == 1)
    {
      name : "$escapetool.javascript($source.display('name','view'))",
      varname : 'input',
      script : "#evaluate($source.getProperty('url').value)&query=$source.getProperty('query').value&nb=$source.getProperty('resultsNumber').value&",
      icon : "#evaluate($source.getProperty('icon').value)",
      highlight: #if($source.getProperty('highlight').value == 1) true #else false #end
    },
    #end
  #end
    null  // Don't handle last coma. This is going to be compated anyway.
  ].compact()

  new XWiki.SearchSuggest($('headerglobalsearchinput'), sources);

});

