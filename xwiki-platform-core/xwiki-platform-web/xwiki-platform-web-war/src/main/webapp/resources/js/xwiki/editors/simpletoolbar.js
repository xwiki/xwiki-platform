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
require(['jquery'], function ($) {
  class SimpleToolbar {
    constructor()
    {
      let self = this;
      self._insertTagFunction = self._defaultInsertTag;
      $('textarea').each(function() {
          self._initTextarea($(this), self);
      });
      $(document).on('xwiki:dom:updated', function (event, data) {
        $(data.elements).find('textarea').each(function() {
          let syntax = $(this).data('syntax');
          if (typeof syntax === 'string' && syntax.startsWith('xwiki')) {
            self._initTextarea($(this), self);
          }
        });
      })
    }

   /**
    * Allows to override the default function to insert a tag syntax.
    * This method should be used when a specific editor implementation is used on top of the default textarea (e.g.
    * CodeMirror for syntax highlighting).
    * @param insertTagFunction the function used for overriding, which needs to take 4 parameters (see
    * _defaultInsertTag) for the example.
    */
    setInsertTagFunction(insertTagFunction) {
      this._insertTagFunction = insertTagFunction;
    }

    _defaultInsertTag(textarea, tagOpen, tagClose, sampleText) {
      let startPos = textarea.selectionStart;
      let endPos = textarea.selectionEnd;
      let scrollTop = textarea.scrollTop;
      let textareaVal = $(textarea).val();
      let subst, myText = (textareaVal).substring(startPos, endPos);
      if (!myText) {
        myText = sampleText;
      }
      if (myText.charAt(myText.length - 1) === " ") {
        // exclude ending space char, if any
        subst = tagOpen + myText.substring(0, (myText.length - 1)) + tagClose + " ";
      } else {
        subst = tagOpen + myText + tagClose;
      }
      let textBegin = textareaVal.substring(0, startPos);
      let textEnd = textareaVal.substring(endPos, textareaVal.length);
      $(textarea).val(textBegin + subst + textEnd);
      $(textarea).focus();

      let cPos = startPos + (tagOpen.length + myText.length + tagClose.length);
      textarea.selectionStart = cPos;
      textarea.selectionEnd = cPos;
      textarea.scrollTop = scrollTop;
    }

    _unescapeLineBreaks(text) {
      return text.replace(/\\n/g, '\n');
    }

    _initTextarea(textarea, self) {
      let buttonMenu = $('<div class="leftmenu2"></div>');
      let buttonConfig = this._parseConfiguration();
      const toolbarElement = buttonConfig.toolbarElements || [];
      for (let item of toolbarElement) {
        let button = $('<button></button>');
        button.attr({
            type: 'button',
            class: 'wikitoolbar-button',
            title: item.speedTip
        });
        let image = $('<img />');
        image.attr({
          src: item.image,
          alt: item.speedTip,
          title: item.speedTip
        });
        button.on('click', function () {
          self._insertTagFunction(textarea[0],
            self._unescapeLineBreaks(item.tagOpen),
            self._unescapeLineBreaks(item.tagClose),
            item.sampleText);
        });
        button.append(image);
        buttonMenu.append(button);
      }
      textarea.before(buttonMenu);
      $(document).trigger('xwiki:dom:updated', {'elements': [buttonMenu.parent()[0]]});
    }

    /**
     * @returns {Object} the resolved configuration as an unstructured object, or the empty object in case of error.
     */
    _parseConfiguration()
    {
      try {
        return $('#simpletoolbar-configuration').data('xwiki-simpletoolbar-configuration');
      } catch (e) {
        console.error('Error parsing simpletoolbar configuration: ', e);
        return {};
      }
    }
  }

  let init = function () {
    XWiki = window.XWiki || {};
    XWiki.editors = XWiki.editors || {};
    XWiki.editors.SimpleToolbar = new SimpleToolbar();
  };
  (XWiki && XWiki.isInitialized && init()) || document.observe('xwiki:dom:loading', init);
});