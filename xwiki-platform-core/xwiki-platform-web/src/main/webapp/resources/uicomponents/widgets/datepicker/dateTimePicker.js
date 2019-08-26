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
var XWiki = (function (XWiki) {
// Start XWiki augmentation.

/**
 * Enhances a date-time field with the ability to pick the date and time from a Gregorian calendar widget.
 */
XWiki.DateTimePicker = Class.create({
  timeStampClassNamePattern : /^t\d+$/,
  initialize : function(input, format) {
    this.input = $(input);
    this.input.writeAttribute('autocomplete', 'off');
    var trigger = this._trigger.bindAsEventListener(this);
    this.input.observe('focus', trigger).observe('click', trigger);

    this.formatter = new Externals.SimpleDateFormat(format);
    this.timePickerEnabled = this._isTimePickerEnabled();
  },
  _isTimePickerEnabled : function() {
    var formattedDate = this.formatter.format(new Date(1876, 1, 1, 23, 59, 0, 0));
    return formattedDate.indexOf('23') >= 0 && formattedDate.indexOf('59') >= 0;
  },
  _getDate : function() {
    if (!this.input._timestamp) {
      this.input._timestamp = this.input.readAttribute('data-timestamp') || new Date().getTime();
      var classNames = $w(this.input.className);
      for(var i = 0; i < classNames.length; i++) {
        if (this.timeStampClassNamePattern.test(classNames[i])) {
          this.input._timestamp = parseInt(classNames[i].substring(1));
        }
      }
    }
    return new Date(this.input._timestamp);
  },
  _trigger : function(event) {
    if (!this.input.calendar_date_select && !this.input.calendar_date_select_closing) {
      new Externals.CalendarDateSelect(this.input, {
        time: this.timePickerEnabled,
        date: this._getDate(),
        clear_button: false,
        formatter: this,
        before_close: function() {
          this.calendar_date_select_closing = true;
          if (this.calendar_date_select.selection_made) {
            // Store the timestamp of the selected date so that we don't have to parse the input value when the picker is redisplayed.
            event.element()._timestamp = this.calendar_date_select.selected_date.getTime();
          }
        },
        after_close: function() {
          this.calendar_date_select_closing = undefined;
        }
      });
    }
  },
  format : function(date) {
    return this.formatter.format(date);
  },
  parse : function(string) {
    return new Date(string);
  }
});

// -------------------------------------------- //
// AppWithinMinutes code that needs to be moved //
// -------------------------------------------- //
function enhanceEmptyIsToday(field) {
  var emptyIsToday = $(field.getPropertyId('emptyIsToday'));
  if (emptyIsToday.type == 'text') {
    emptyIsToday.type = 'checkbox';
    emptyIsToday.checked = emptyIsToday.value == '1';
    emptyIsToday.value = '1';
    var dd = emptyIsToday.up('dd');
    dd.previous('dt').down('label').insert({top: emptyIsToday});
    dd.remove();
    emptyIsToday.insert({after: new Element('input', {type: 'hidden', value: '0', name: emptyIsToday.name})});
  }
}
function enhanceDateFormat(field) {
  var dateFormatInput = $(field.getPropertyId('dateFormat'));
  if (dateFormatInput.type == 'text') {
    // TODO
  }
}
function enhanceDefaultValue(field) {
  field.getViewer().select('input[type=text]').each(function(input) {
    if (input.name.endsWith('_0_' + field.getName())) {
      new XWiki.DateTimePicker(input, $(field.getPropertyId('dateFormat')).value);
    }
  });
}
function isDateField(field) {
  return $('type-' + field.getName()).value == 'Date';
}
function maybeEnhanceField(field) {
  if (isDateField(field)) {
    enhanceEmptyIsToday(field);
    enhanceDateFormat(field);
    enhanceDefaultValue(field);
  }
}
// -------------------------------------------- //

var initDateTimePickers = function(event) {
  var containers = (event && event.memo.elements) || [$('body')];
  containers.each(function(container) {
    $(container).select('input.datetime').each(function(dateTimeInput) {
      if (!dateTimeInput.hasClassName('initialized')) {
        // The input title holds the date format.
        var dateFormat = dateTimeInput.readAttribute('data-format') || dateTimeInput.title;
        new XWiki.DateTimePicker(dateTimeInput, dateFormat);
        dateTimeInput.addClassName('initialized');
      }
    });
  });
};

var init = function(event, tryCount) {
  // If there's no event then the code was probably lazy loaded. Make sure dependencies are present.
  if (!event && (typeof Externals == 'undefined' || !Externals.SimpleDateFormat || !Externals.CalendarDateSelect)) {
    tryCount = tryCount || 0;
    if (tryCount < 3) {
      typeof console != 'undefined' && console.warn
        && console.warn('Cannot initialize DateTimePicker due to missing dependencies. Waiting a bit before trying again.');
      setTimeout(init.bind(this, event, tryCount + 1), 100);
    } else {
      typeof console != 'undefined' && console.error
        && console.error('Failed to initialize DateTimePicker due to missing dependencies: SimpleDateFormat and CalendarDateSelect.');
    }
    return;
  }

  // AppWithinMinutes code //
  document.observe('xwiki:class:displayField', function(event) {
    maybeEnhanceField(event.memo.field);
  });
  document.observe('xwiki:class:previewField', function(event) {
    var field = event.memo.field;
    if (isDateField(field)) {
      enhanceDefaultValue(field);
    }
  });
  // This is needed in case this script is loaded asynchronously.
  var fields = $('fields');
  if (fields) {
    // AppWithinMinutes Class Editor.
    fields.childElements().each(function(item) {
      var field = new XWiki.FormField(item);
      if (field.getConfig()) {
        maybeEnhanceField(field);
      }
    });
  } else {
    initDateTimePickers(event);
  }

  return true;
};
(XWiki.domIsLoaded && init()) || document.observe('xwiki:dom:loaded', init);
document.observe('xwiki:dom:updated', initDateTimePickers);

// End XWiki augmentation.
return XWiki;
}(XWiki || {}));
