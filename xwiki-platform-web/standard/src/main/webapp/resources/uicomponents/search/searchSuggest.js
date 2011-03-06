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
        
      this.realSearchInput = $(searchInput);
        
      this.modalContainer = new Element('div', {'class': 'xdialog-modal-container'}).hide();
      var screen = new Element('div', {'class': 'xdialog-screen'}).setStyle({
         opacity : 0.25,
         backgroundColor : "#000000"
      });
      this.modalContainer.insert(screen);
      this.modalContainer.insert(new Element('div', {'id' : 'searchSuggest'}));
      document.body.appendChild(this.modalContainer);
    
      document.observe("xwiki:suggest:clearSuggestions", this.onClearSuggestions.bindAsEventListener(this));
      document.observe("xwiki:suggest:containerCreated", this.onSuggestContainerCreated.bindAsEventListener(this)); 
      document.observe("xwiki:suggest:containerPrepared", this.onSuggestContainerPrepared.bindAsEventListener(this));
      document.observe("xwiki:suggest:selected", this.onSuggestionSelected.bindAsEventListener(this));
    
      this.createSuggest();
    },
    
    /**
     * Callback triggered when the original suggest clears its suggestions.
     */
    onClearSuggestions: function(event){
      if (event.memo.suggest == this.suggest) {
        this.modalContainer.hide();
        this.devirtualizeSearchInput();
      }
    },
    
    /**
     * Callback triggered when the original suggest has prepared its results container.
     */
    onSuggestContainerPrepared: function(event){
      if (event.memo.suggest == this.suggest && event.memo.suggest == this.suggest) {
        this.modalContainer.show();
        this.virtualizeSearchInput();
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
          this.devirtualizeSearchInput();
          this.realSearchInput.up('form').submit();
        }
        else {
          // Go to page
          this.modalContainer.hide();
          this.virtualSearchInput.value = event.memo.value;     
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
            this.devirtualizeSearchInput();
            this.realSearchInput.up('form').submit();
          },
          'mouseover':function(event){
            this.suggest.clearHighlight();
            this.suggest.iHighlighted = event.element();
            event.element().addClassName('xhighlight');
          }
        }
      });
      this.suggest = new XWiki.widgets.Suggest( this.realSearchInput, {
        parentContainer: $('searchSuggest'),
        className: 'ajaxsuggest searchSuggest',
        fadeOnClear:false,
        align: "right",
        minchars: 3,    
        sources : this.sources,
        insertBeforeSuggestions : new Element("div", {'class' : 'results'}).update(allResultsNode.getElement()),
        displayValue:true,
        displayValueText: "in ",
        timeout: 0
      });
    },

    /**
     * Add a "virtual" search input on top of the original one, so that it appears on top of the modal container.
     */ 
    virtualizeSearchInput: function(){
      if (!this.isVirtual) {
        this.isVirtual = true;
        this.virtualSearchInput.value = this.realSearchInput.value;
        this.virtualSearchInput.show();
        this.virtualSearchInput.focus();
        this.suggest.setInputField(this.virtualSearchInput);
      }
    },

    /**
     * Hides the virtual search input
     */
    devirtualizeSearchInput: function(){
      this.realSearchInput.value = this.virtualSearchInput.value;
      this.realSearchInput.focus();
      this.suggest.setInputField(this.realSearchInput);
      this.isVirtual = false;
    },
    
    /**
     * Callback triggered when the original suggest has created its results container.
     */
    onSuggestContainerCreated: function(event){
      if (event.memo.suggest == this.suggest) {
        var container = event.memo.container;
        this.modalContainer.show();
        this.virtualSearchInput = this.realSearchInput.clone();
        this.virtualSearchInput.id = this.realSearchInput.id + '-virtual';
        this.virtualSearchInput.clonePosition(container, { setWidth : false });
        $('searchSuggest').insert(this.virtualSearchInput);
        this.virtualSearchInput.setStyle({position : 'absolute'});
        this.virtualSearchInput.setStyle({
          top : (this.virtualSearchInput.cumulativeOffset().top - this.realSearchInput.getHeight()) + 'px',
          position:'absolute'
        });

        if (Prototype.Browser.IE || Prototype.Browser.WebKit) {
          this.virtualSearchInput.observe("keydown", this.onKeyPress.bindAsEventListener(this));
        } else {
          this.virtualSearchInput.observe("keypress", this.onKeyPress.bindAsEventListener(this));
        }
      }
    },

   /**
    * Callback triggered when a key has been typed on the virtual input.
    */
   onKeyPress: function(event){
     var key = event.keyCode;
     switch(key) {
      // Ignore special keys, which are treated in onKeyPress
       case Event.KEY_RETURN:
         if (!this.suggest.hasActiveSelection()) {
           this.devirtualizeSearchInput();
           event.stop();
           this.realSearchInput.up('form').submit();
         }
       default:
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

