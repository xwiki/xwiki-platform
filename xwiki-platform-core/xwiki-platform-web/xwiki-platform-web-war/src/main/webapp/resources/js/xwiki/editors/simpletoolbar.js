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
          $('textarea').each(this.initTextarea);
      };

      insertTag = function(textarea, tagOpen, tagClose, sampleText) {
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

          // reposition cursor if possible
          // FIXME: double check this
          if (textarea.createTextRange) {
              textarea.caretPos = document.selection.createRange().duplicate();
          }
      }

      initTextarea = function () {
          let textarea = $(this);
          let buttonMenu = $('<div class="leftmenu2"></div>');
          let self = this;
          let buttonConfig = JSON.parse($('#simpletoolbar-configuration').text());
          for (let item of buttonConfig.toolbarElements) {
              let link = $('<button type="button" class="wikitoolbar-button"></button>');
              let image = $('<img />');
              image.attr({
                  src: item.image,
                  alt: item.speedTip,
                  title: item.speedTip
              });
              link.on('click', function () {
                  self.insertTag(textarea[0],
                      self.unescapeLineBreaks(item.tagOpen),
                      self.unescapeLineBreaks(item.tagClose),
                      item.sampleText);
              });
              link.append(image);
              buttonMenu.append(link);
          }
          textarea.before(buttonMenu);
      }

      unescapeLineBreaks = function (text) {
          return text.replace(/\\n/g, '\n');
      }
  }

  let init = function () {
      XWiki = window.XWiki || {};
      XWiki.editors = XWiki.editors || {};
      XWiki.editors.SimpleToolbar = new SimpleToolbar();
  };
  (XWiki && XWiki.isInitialized && init()) || document.observe('xwiki:dom:loaded', init);
});