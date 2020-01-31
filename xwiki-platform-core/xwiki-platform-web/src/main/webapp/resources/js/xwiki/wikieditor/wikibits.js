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
var noOverwrite = false;
var alertText;
var clientPC = navigator.userAgent.toLowerCase(); // Get client info
var is_gecko = ((clientPC.indexOf('gecko') != -1) && (clientPC.indexOf('spoofer') == -1) &&
               (clientPC.indexOf('khtml') == -1) && (clientPC.indexOf('netscape/7.0')==-1));
var is_safari = ((clientPC.indexOf('AppleWebKit')!=-1) && (clientPC.indexOf('spoofer')==-1));

// this function generates the actual toolbar buttons with localized text
// we use it to avoid creating the toolbar where javascript is not enabled
function addButton(imageFile, speedTip, tagOpen, tagClose, sampleText) {
  speedTip = escapeQuotes(speedTip);
  tagOpen = escapeQuotes(tagOpen);
  tagClose = escapeQuotes(tagClose);
  sampleText = escapeQuotes(sampleText);

  document.write("<a href=\"javascript:insertTags");
  document.write("('"+tagOpen+"','"+tagClose+"','"+sampleText+"');\">");

  document.write("<img src=\""+imageFile+"\" border=\"0\" ALT=\""+speedTip+"\" TITLE=\""+speedTip+"\">");
  document.write("</a>");
  return;
}

function escapeQuotes(text) {
  var re = new RegExp("'", "g");
  text = text.replace(re, "\\'");
  re = new RegExp('"', "g");
  text = text.replace(re, '&quot;');
  re = new RegExp("\\n", "g");
  text = text.replace(re, "\\n");
  return text;
}

// apply tagOpen/tagClose to selection in textarea,
// use sampleText instead of selection if there is none
// copied and adapted from phpBB
function insertTags(tagOpen, tagClose, sampleText) {
  var txtarea = ($('edit') && $('edit').content) || $('content');

  // IE
  if (document.selection && !is_gecko) {
    var theSelection = document.selection.createRange().text;
    if (!theSelection) {
      theSelection = sampleText;
    }
    txtarea.focus();
    if (theSelection.charAt(theSelection.length - 1) == " "){
      // exclude ending space char, if any
      theSelection = theSelection.substring(0, theSelection.length - 1);
      document.selection.createRange().text = tagOpen + theSelection + tagClose + " ";
    } else {
      document.selection.createRange().text = tagOpen + theSelection + tagClose;
    }

  // Mozilla
  } else if (txtarea.selectionStart || txtarea.selectionStart == '0') {
    var startPos = txtarea.selectionStart;
    var endPos = txtarea.selectionEnd;
    var scrollTop = txtarea.scrollTop;
    var myText = (txtarea.value).substring(startPos, endPos);
    if (!myText) {
      myText = sampleText;
    }
    if (myText.charAt(myText.length - 1) == " ") {
      // exclude ending space char, if any
      subst = tagOpen + myText.substring(0, (myText.length - 1)) + tagClose + " ";
    } else {
      subst = tagOpen + myText + tagClose;
    }
    var textBegin = txtarea.value.substring(0, startPos);
    var textEnd = txtarea.value.substring(endPos, txtarea.value.length);
    txtarea.value = textBegin + subst + textEnd;
    txtarea.focus();

    var cPos = startPos + (tagOpen.length + myText.length + tagClose.length);
    txtarea.selectionStart = cPos;
    txtarea.selectionEnd = cPos;
    txtarea.scrollTop = scrollTop;

  // All others
  } else {
    var copy_alertText = alertText;
    var re1 = new RegExp("\\$1", "g");
    var re2 = new RegExp("\\$2", "g");
    copy_alertText = copy_alertText.replace(re1, sampleText);
    copy_alertText = copy_alertText.replace(re2, tagOpen + sampleText + tagClose);
    var text;
    if (sampleText) {
      text = prompt(copy_alertText);
    } else {
      text = "";
    }
    if (!text) {
      text = sampleText;
    }
    text = tagOpen + text + tagClose;
    // in Safari this causes scrolling
    if (!is_safari) {
      txtarea.focus();
    }
    noOverwrite = true;
  }
  // reposition cursor if possible
  if (txtarea.createTextRange) {
    txtarea.caretPos = document.selection.createRange().duplicate();
  }
}
