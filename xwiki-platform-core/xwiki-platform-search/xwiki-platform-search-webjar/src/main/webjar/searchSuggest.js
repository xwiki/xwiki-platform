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
define('search-suggest-messages', {
   keys: [
     'core.widgets.suggest.noResults',
     'core.widgets.suggest.showResults',
     'platform.search.suggestResultLocatedIn'
   ]
});
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
      require(['xwiki-l10n!search-suggest-messages'], (l10n) => {
        this._l10n = l10n;
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
        document.observe("xwiki:suggest:collapsed", this.onSuggestCollapsed.bindAsEventListener(this));

        this.createSuggest();
      });
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
      if (event.memo.container.select('.suggestItem').length === 1) {
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
        if (!event.memo.url) {
          // Submit form
          this.searchInput.up('form').submit();
        }
        else {
          // Go to page
          window.location = event.memo.url;
        }
      }
    },

    /**
     * Callback triggered when the suggest element is collapsed, because of a focusout event for example.
     */
    onSuggestCollapsed: function(event) {
      /* We match the collapse of the input field timing to close the suggest panel.
         As of 16.2.0-RC1, this value is defined in action-menus.less -> #headerglobalsearchinput */
      this.suggest.resetTimeout(300);
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
        .update(this._l10n['core.widgets.suggest.noResults'].escapeHTML());
      var gotoSearchPageMessage = new Element('div').update(this._l10n['core.widgets.suggest.showResults']
        .escapeHTML());
      var content = new Element('div').insert(this.noResultsMessage).insert(gotoSearchPageMessage)
        .insert(new Element('div', {'class': 'clearfloats'}));
      var allResultsNode = new XWiki.widgets.XList([
        new XWiki.widgets.XListItem( content, {
          containerClasses: 'suggestItem',
          classes: 'showAllResults',
          eventCallbackScope: this,
          noHighlight: true,
          value: valueNode,
          containerTagName: 'button'
        } ),
      ],
      {
        'classes' : 'suggestList',
        'eventListeners' : {
          'mouseover':function(event){
            this.suggest.setHighlight(event.currentTarget);
          }
        }
      });
      var allResults = allResultsNode.getElement();
      allResultsNode.items[0].getElement().addEventListener('focusin',
        (event) => this.suggest.setHighlight($(event.currentTarget)));
      this.suggest = new XWiki.widgets.Suggest( this.searchInput, {
        parentContainer: $('globalsearch'),
        className: 'searchSuggest horizontalLayout',
        fadeOnClear: false,
        align: "auto",
        minchars: 3,
        sources : this.sources,
        insertBeforeSuggestions : new Element("div", {'class' : 'results'}).update( allResults ),
        displayValue: true,
        displayValueText: this._l10n['platform.search.suggestResultLocatedIn'],
        resultInfoHTML: true,
        timeout: 0,
        width: 500,
        unifiedLoader: true,
        loaderNode: allResults.down("li"),
        shownoresults: false,
        propagateEventKeyCodes : [ Event.KEY_RETURN ]
      });
    }

  });

  const sourcesUrl = new XWiki.Document(XWiki.Model.resolve('XWiki.SearchSuggestCode', XWiki.EntityType.DOCUMENT))
    .getURL('get');

  var init = async function() {
    var sources = await (await fetch(sourcesUrl)).json();
    new XWiki.SearchSuggest($('headerglobalsearchinput'), sources);
    return true;
  };

  // When the document is loaded, install search suggestions
  var discard = (XWiki.isInitialized && init()) || document.observe('xwiki:dom:loading', init);
  return XWiki;
})(XWiki);
