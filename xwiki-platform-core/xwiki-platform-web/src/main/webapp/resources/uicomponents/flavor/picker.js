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
// TODO: use the xwiki select widget: http://jira.xwiki.org/browse/XWIKI-12503
require(['jquery'], function($) {

  /**
   * Count the ajax calls;
   */
  var ajaxCalls = 0;

  /**
   * The last ajax request
   */
  var lastAjaxRequest = false;

  /**
   * Send an event when the selection change or when the results are refreshed
   */
  var sendRefreshEvent = function (picker) {
    picker.trigger('xwiki:flavorpicker:updated', {'elements': picker[0]});
  }
    
  /** 
   * Perform an ajax query and refresh the picker's results container with the results.
   */
  var refreshResults = function(picker, url, parameters) {
    var thisAjaxCall = ++ajaxCalls;
    var resultsContainer = picker.find('.xwiki-flavor-picker-results-container');
    resultsContainer.addClass('loading');
    resultsContainer.find('.xwiki-flavor-picker-results').fadeOut();
    // Kill the previous request
    if (lastAjaxRequest) {
        lastAjaxRequest.abort();
    }
    $.ajax(url, {data: parameters}).done(function(data) {
      // Exit if the ajax call is not the last one (kill old requests)
      // Probably not usefull since we called lastAjaxRequest.abort() just before.
      if (thisAjaxCall != ajaxCalls) {
        return;
      }
      resultsContainer[0].innerHTML = data;
      resultsContainer.removeClass('loading');
      var results = picker.find('.xwiki-flavor-picker-results');
      results.hide();
      results.fadeIn();
      initPickerResults(picker);
    }).fail(function(){
      new XWiki.widgets.Notification("$escapetool.javascript($services.localization.render('flavor.picker.ajaxError'))", 'error');
      resultsContainer.removeClass('loading');
      picker.find('.xwiki-flavor-picker-results').fadeIn();
    });
  }
  
  /**
   * Initializer called each time the picker's result container is refreshed
   */
  var initPickerResults = function(picker) {
  
    // Called when an option is clicked
    picker.find('.xwiki-flavor-picker-option').click(function (event) {
      var thisOption = $(this);
      var picker = thisOption.parents('.xwiki-flavor-picker');
      picker.find('.xwiki-flavor-picker-option-selected').removeClass('xwiki-flavor-picker-option-selected');
      thisOption.find('input').prop('checked', true);
      thisOption.addClass('xwiki-flavor-picker-option-selected');
      sendRefreshEvent(picker);
    });
    
    // Called when a flavor's link is clicked
    picker.find('.xwiki-flavor-picker-option a.popup').click(function (event) {
      // TODO: replace this by a modal box with an iframe
      window.open(this.href, 'flavor-popup', config='height=600, width=700, toolbar=no, menubar=no, scrollbars=yes, resizable=yes, location=no, directories=no, status=yes');
      return false;
    });
    
    // Called when a pagination link is clicked
    picker.find('.paginationFilter a').click(function (event) {
      var picker = $(this).parents('.xwiki-flavor-picker');
      refreshResults(picker, this.href, {});
      return false;
    });
    
    sendRefreshEvent(picker);
  }
  
  /**
   * Initializer called when the DOM is ready
   */
  var init = function() {
    initPickerResults($('.xwiki-flavor-picker'));
    
    // Called when the picker's filter is updated on keyboard pressed
    $('input.xwiki-flavor-picker-filter').keyup(function() {
      var filter = $(this);
      var filterValue = filter.val();
      // To avoid having too much requests when the user is typing, we only do a research 500ms after the content has changed
      setTimeout(function(){
        // We do the request only if the content has not changed since the last event
        if (filter.val() == filterValue) {
          var picker = filter.parents('.xwiki-flavor-picker');
          var url = "$escapetool.javascript($doc.getURL('view', 'xpage=flavor/picker_results'))";
          var fieldName = picker.find("input[type='radio']").attr('name');
          var parameters = {'fieldName': fieldName, 'firstIndex': 0, 'filter': filter.val()};
          refreshResults(picker, url, parameters);
        }
      }, 500);

    });
    
    // Called when the "no flavor" option is clicked
    $('.xwiki-flavor-picker-noflavor').click(function (event) {
      var picker = $(this).parents('.xwiki-flavor-picker');
      picker.find('.xwiki-flavor-picker-option-selected').removeClass('xwiki-flavor-picker-option-selected');
      sendRefreshEvent(picker);
    });
  }

  $(window).ready(init);

});

