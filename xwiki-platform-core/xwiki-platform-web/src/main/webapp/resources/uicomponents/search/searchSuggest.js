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
    initialize: function(searchInput, sources) {

      this.sources = sources;

      this.searchInput = $(searchInput);
      if (!this.searchInput) {
        return;
      }

      document.observe("xwiki:suggest:clearSuggestions", this.onClearSuggestions.bindAsEventListener(this));
      document.observe("xwiki:suggest:containerCreated", this.onSuggestContainerCreated.bindAsEventListener(this));
      document.observe("xwiki:suggest:containerPrepared", this.onSuggestContainerPrepared.bindAsEventListener(this));
      document.observe("xwiki:suggest:updated", this.onSuggestUpdated.bindAsEventListener(this));
      document.observe("xwiki:suggest:selected", this.onSuggestionSelected.bindAsEventListener(this));

      this.createSuggest();
    },

    /**
     * Callback triggered when the original suggest clears its suggestions.
     */
    onClearSuggestions: function(event) {
      if (event.memo.suggest == this.suggest) {
        // Restore bottom border style
        this.searchInput.setStyle({'borderBottomStyle' : this.searchInputBorderBottomSavedStyle});
      }
    },

    /**
     * Callback triggered when the original suggest has created its results container.
     */
    onSuggestContainerCreated: function(event) {
      if (event.memo.suggest == this.suggest) {
        // Save the style of the bottom border of the input field so that we can restore it later on
        this.searchInputBorderBottomSavedStyle = this.searchInput.getStyle('borderBottomStyle');
        // Hide bottom border of input field to not double the container border just under the field
        this.searchInput.setStyle({'borderBottomStyle' : 'none'});
      }
    },

    /**
     * Callback triggered just before the suggest sends a new set of requests to fetch the suggestions from all the
     * configured sources. At this point the list of suggestions is empty and all sources are marked as loading.
     */
    onSuggestContainerPrepared: function(event) {
      // Hide the "No results!" message.
      this.noResultsMessage.addClassName('hidden');
    },

    /**
     * Callback triggered after the suggest receives the list of suggestions from all configured sources (even if the
     * list of suggestions from one source is empty).
     */
    onSuggestUpdated: function(event) {
      // Check if there are any suggestions, taking into account that there is at least one suggestion used to link the
      // search page.
      if (event.memo.container.select('.suggestItem').length == 1) {
        // Show the "No results!" message.
        this.noResultsMessage.removeClassName('hidden').setStyle({'float': 'left'});
      }
    },

    /**
     * Callback triggered when a suggestion is selected.
     * Submits the form or go to a selected page according to selection.
     */
    onSuggestionSelected: function(event) {
      if (event.memo.suggest == this.suggest) {
        event.stop();
        // Also stop the browser event that triggered the custom "xwiki:suggest:selected" event.
        if (event.memo.originalEvent) {
          Event.stop(event.memo.originalEvent);
        }
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
      // Create dummy suggestion node to hold the "Go to search page..." option.
      var valueNode = new Element('div')
            .insert(new Element('span', {'class':'suggestId'}))
            .insert(new Element('span', {'class':'suggestValue'}))
            .insert(new Element('span', {'class':'suggestInfo'}));
      this.noResultsMessage = new Element('div', {'class': 'hidden'})
        .update("$escapetool.javascript($services.localization.render('core.widgets.suggest.noResults'))".escapeHTML());
      var gotoSearchPageMessage = new Element('div')
        .update("$escapetool.javascript($services.localization.render('core.widgets.suggest.showResults'))"
        .escapeHTML());
      var content = new Element('div').insert(this.noResultsMessage).insert(gotoSearchPageMessage)
        .insert(new Element('div', {'class': 'clearfloats'}));
      var allResultsNode = new XWiki.widgets.XList([
        new XWiki.widgets.XListItem( content, {
          'containerClasses': 'suggestItem',
          'classes': 'showAllResults',
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
      var allResults = allResultsNode.getElement();
      this.suggest = new XWiki.widgets.Suggest( this.searchInput, {
        parentContainer: $('searchSuggest'),
        className: 'searchSuggest horizontalLayout',
        fadeOnClear: false,
        align: "auto",
        minchars: 3,
        sources : this.sources,
        insertBeforeSuggestions : new Element("div", {'class' : 'results'}).update( allResults ),
        displayValue: true,
        displayValueText: "$escapetool.javascript($services.localization.render('platform.search.suggestResultLocatedIn'))",
        timeout: 0,
        width: 500,
        unifiedLoader: true,
        loaderNode: allResults.down("li"),
        shownoresults: false,
        propagateEventKeyCodes : [ Event.KEY_RETURN ]
      });
    }

  });

  var init = function() {
    /*
    ## Iterate over the sources defined in the configuration document, and create a source array to be passed to the
    ## search suggest contructor.
    #set ($sources = [])
    #set ($searchSuggestConfig = $xwiki.getDocument('XWiki.SearchSuggestConfig'))
    #foreach ($source in $searchSuggestConfig.getObjects('XWiki.SearchSuggestSourceClass'))
      #if ($source.getProperty('activated').value == 1)
        #set ($engine = $source.getProperty('engine').value)
        #if ("$!engine" == '')
          ## For backward compatibility we consider the search engine to be Lucene when it's no specified.
          #set ($engine = 'lucene')
        #end
        #set ($discard = $xwiki.getDocument('XWiki.SearchCode').getRenderedContent())
        #if ($engine == $searchEngine)
          #set ($name = $source.getProperty('name').value)
          #if ($services.localization.get($name))
            #set ($name = $services.localization.render($name))
          #else
            ## Evaluate the Velocity code for backward compatibility.
            #set ($name = "#evaluate($name)")
          #end
          #set ($icon = $source.getProperty('icon').value)
          #if ($icon.startsWith('icon:'))
            #set ($icon = $xwiki.getSkinFile("icons/silk/${icon.substring(5)}.png"))
          #else
            ## Evaluate the Velocity code for backward compatibility.
            #set ($icon = "#evaluate($icon)")
          #end
          #set ($service = $source.getProperty('url').value)
          #set ($parameters = {
            'query': $source.getProperty('query').value,
            'nb': $source.getProperty('resultsNumber').value
          })
          #if ($xwiki.exists($service))
            #set ($discard = $parameters.put('outputSyntax', 'plain'))
            #set ($service = $xwiki.getURL($service, 'get', $escapetool.url($parameters)))
          #else
            ## Evaluate the Velocity code for backward compatibility.
            #set ($service = "#evaluate($service)")
            #set ($service = "$service#if ($service.contains('?'))&#else?#end$escapetool.url($parameters)")
          #end
          #set ($highlight = $source.getProperty('highlight').value == 1)
          #set ($discard = $sources.add({
            'name': $name,
            'varname': 'input',
            'script': $service,
            'icon': $icon,
            'highlight': $highlight
          }))
        #end
      #end
    #end
    */
    var sources = $jsontool.serialize($sources);
    new XWiki.SearchSuggest($('headerglobalsearchinput'), sources);
    return true;
  };

  // When the document is loaded, install search suggestions
  (XWiki.isInitialized && init())
  || document.observe('xwiki:dom:loading', init);

  return XWiki;

})(XWiki);