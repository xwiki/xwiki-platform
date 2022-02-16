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
   * The found flavors
   */
  var flavors = [];

  /**
   * Send an event when the selection change or when the results are refreshed
   */
  var sendRefreshEvent = function (picker) {
    picker.trigger('xwiki:flavorpicker:updated', {'elements': picker[0]});
  }

  var updatePicker = function () {
    updateFlavors();
    updateProgress();
  }

  var updateProgress = function () {
    // Get base URL
    var url = "$escapetool.javascript($services.flavor.getSearchValidFlavorsStatusURL(''))";

    // Add namespace to the URL if needed
    var picker = $('.xwiki-flavor-picker');
    url += encodeURIComponent(picker.attr('data-namespace'));

    $.getJSON(url).then(data => {
      // Update progress
      var jobState = data.state;

      var flavorProgressBackground = $('#xwiki-flavor-picker-progress-background');
      if (flavorProgressBackground.length) {
        var flavorProgressBar = $('#xwiki-flavor-picker-progress-bar');

        // Update state in the dom
        flavorProgressBackground.find('input').attr('value', jobState);

        if (jobState == 'RUNNING') {
          // Make sure progress is visible
          flavorProgressBackground.removeClass('hidden');

          var jobProgressOffset = data.progress.offset;
          flavorProgressBar.css('width', (jobProgressOffset * 100) + '%');
        } else {
          // We don't need the progress anymore
          flavorProgressBackground.addClass('hidden');
        }

        // Progress callback (if needed)
        maybeNextStatus();
      }
    }).catch(() => {
      new XWiki.widgets.Notification("$escapetool.javascript($services.localization.render('flavor.picker.ajaxError'))", 'error');
    });
  }

  var updateFlavors = function () {
    // Get base URL
    var url = "$escapetool.javascript($doc.getURL('get', 'xpage=flavor/picker_flavors'))";

    // Add namespace to the URL if needed
    var picker = $('.xwiki-flavor-picker');
    url += '&namespace=' + encodeURIComponent(picker.attr('data-namespace'));

    $.getJSON(url).then(data => {
      // Update the list of flavors
      flavors = data;

      // Update the list of flavors
      $.each( data, function( key, extension ) {
        updateFlavor(key, extension);
      });

      // Update event listeners
      initPickerResults($('.xwiki-flavor-picker'));
    }).catch(() => {
      new XWiki.widgets.Notification("$escapetool.javascript($services.localization.render('flavor.picker.ajaxError'))", 'error');
    });
  }

  var updateFlavor = function (key, flavor) {
    var picker = $('.xwiki-flavor-picker');
    var fieldName = picker.find('input[name="fieldName"]').attr('value');
    var results = picker.find('ul');

    var flavorValue = flavor.id.id + ':::' + flavor.id.version.value;

    var flavorElement = results.find('input[value="' + flavorValue + '"]');
    if (flavorElement.length == 0) {
      // Add new flavor
      var li = $('<li class="xwiki-flavor-picker-option"></li>');
      li.append($('<input type="radio"/>').attr('name', fieldName).attr('value', flavorValue).attr('id', fieldName + '_' + key));
      li.append("<span class=\"xwiki-flavor-picker-option-icon\">$escapetool.javascript($services.icon.renderHTML('wiki'))</span>");
      var div = $('<div></div>');
      var label = $('<div></div>');
      label.attr('for', fieldName + '_' + key);

      // Name
      var text;
      if (flavor.name != null && flavor.name != '') {
        text = flavor.name;
      } else {
        text = flavor.id.id;
      }
      if (flavor.website != null && flavor.website != '') {
        label.append($('<a class="popup"></a>').attr('href', flavor.website).text(text));
      } else {
        label.text(text);
      }

      label.append(' ');

      // Version
      label.append($('<small></small>').text(flavor.id.version.value));

      // Rating
      var star = "$escapetool.javascript($services.icon.renderHTML('star'))";
      if (flavor.rating != null && flavor.rating > 0) {
        label.append(Array(Math.round(flavor.rating)).join(star));
      }

      div.append(label);

      // Authors
      if (flavor.authors.length > 0) {
        var authors = $('<p class="authors"></p>');
        var authorsBy = $('<small></small>');
        authorsBy.text("$escapetool.javascript($services.localization.render('flavor.picker.authorsBy')) ");

        $.each(flavor.authors, function(key, author) {
          if (key > 0) {
            authorsBy.append(', ');
          }
          if (author.url != null && author.url != '') {
            authorsBy.append($('<a></a>').attr('href', author.url).text(author.name));
          } else {
            authorsBy.append(author.name);
          }
        });
        authors.append(authorsBy);
        div.append(authors);
      }

      // Summary
      if (flavor != null && flavor.summary != '') {
        div.append($('<p class="xHint"></p>').text(flavor.summary));
      }

      li.append($('<input type="hidden" name="match"/>').attr('value', JSON.stringify(flavor)));

      li.append(div);
      results.append(li);
    }
  }

  var maybeNextStatus = function () {
    // Set a timeout if the job isnot finished
    var flavorProgressBackground = $('#xwiki-flavor-picker-progress-background');
    if (flavorProgressBackground.length) {
      var state = flavorProgressBackground.find('input').attr('value');

      if (state != 'FINISHED') {
         setTimeout(updatePicker, 100)
      }
    }
  }

  var filterFlavor = function(flavor, filterString) {
    var match = flavor.find('input[name="match"]').attr('value');
    if (match.toLowerCase().indexOf(filterString) > -1) {
      flavor.removeClass('hidden');
    } else {
      flavor.addClass('hidden');
    }
  }

  var filterFlavors = function(filterString) {
    var picker = $('.xwiki-flavor-picker');
    if (typeof filterString !== 'string' || filterString.trim() === '') {
      picker.find('li').removeClass('hidden');
    } else {
      picker.find('li').each(function(i) {
        filterFlavor($(this), filterString);
      });
    }
  }

  /**
   * Initializer called each time the picker's result container is refreshed
   */
  var initPickerResults = function(picker) {

    // Called when an option is clicked
    picker.find('.xwiki-flavor-picker-option').on('click', function(event) {
      var thisOption = $(this);
      var picker = thisOption.parents('.xwiki-flavor-picker');
      picker.find('.xwiki-flavor-picker-option-selected').removeClass('xwiki-flavor-picker-option-selected');
      thisOption.find('input').prop('checked', true);
      thisOption.addClass('xwiki-flavor-picker-option-selected');
      sendRefreshEvent(picker);
    });

    // Called when a flavor's link is clicked
    picker.find('.xwiki-flavor-picker-option a.popup').on('click', function(event) {
      // TODO: replace this by a modal box with an iframe
      window.open(this.href, 'flavor-popup', config='height=600, width=700, toolbar=no, menubar=no, scrollbars=yes, resizable=yes, location=no, directories=no, status=yes');
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
    $('input.xwiki-flavor-picker-filter').on('keyup', function() {
      var filter = $(this);
      var filterValue = filter.val();

      filterFlavors(filterValue);
    });

    // Called when the "no flavor" option is clicked
    $('.xwiki-flavor-picker-noflavor').on('click', function(event) {
      var picker = $(this).parents('.xwiki-flavor-picker');
      picker.find('.xwiki-flavor-picker-option-selected').removeClass('xwiki-flavor-picker-option-selected');
      sendRefreshEvent(picker);
    });

    // Start progress
    maybeNextStatus();
  }

  $(init);

});

