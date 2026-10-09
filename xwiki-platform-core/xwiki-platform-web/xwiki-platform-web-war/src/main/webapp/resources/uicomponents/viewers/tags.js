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
// ======================================
// Ajax tag editing
//
var XWiki = (function (XWiki) {
// Start XWiki augmentation.
var viewers = XWiki.viewers = XWiki.viewers || {};

/**
 * Sends a POST request to the given URL, showing an in-progress notification while the request is pending and an error
 * notification if the request fails.
 *
 * @param url the URL to send the request to
 * @param inProgressMessage the message to show while the request is pending
 * @return a promise that resolves with the response text when the request succeeds, and with undefined otherwise
 */
async function post(url, inProgressMessage) {
  const notification = XWiki.widgets.Notification.show(inProgressMessage, "inprogress");
  let errorMessage;
  try {
    const response = await fetch(url, {method: 'POST'});
    const responseText = await response.text();
    if (response.ok) {
      notification.hide();
      return responseText;
    }
    errorMessage = responseText;
  } catch {
    // Network failure, handled below.
  }
  notification.replace(XWiki.widgets.Notification.show(errorMessage || 'Server not responding', "error"));
}

function stopEvent(event) {
  event?.preventDefault();
  event?.stopPropagation();
}

function setFormDisabled(form, disabled) {
  for (const element of form.elements) {
    element.disabled = disabled;
  }
}

/**
 * Tag editing.
 *
 * Note that this class can be extended using Prototype.js' Class.create(XWiki.viewers.Tags, {...}). This is why the
 * constructor only delegates to the initialize method (Prototype.js calls only the initialize method when creating an
 * instance of a subclass).
 */
class Tags {
  constructor(...args) {
    this.initialize(...args);
  }

  /**
   * Initialization: add listeners for all tag actions, to perform them via AJAX
   */
  initialize() {
    // delete tags
    document.querySelectorAll('.doc-tags .tag-delete').forEach(item => this.ajaxTagDelete(item));
    document.querySelectorAll('.doc-tags .tag-add a').forEach(item => this.createTagAddForm(item));
    const tagAddForm = document.querySelector('.doc-tags .tag-add-form');
    if (tagAddForm) {
      this.ajaxifyForm(tagAddForm);
    }
  }

  /** AJAX tag removal */
  ajaxTagDelete(item) {
    item.addEventListener('click', async event => {
      stopEvent(event);
      if (!item.disabled) {
        // ignore "cascade" clicks
        item.disabled = true;
        const responseText = await post(item.getAttribute('href').replace(/&xredirect=.+$/, "&ajax=1"),
          $jsontool.serialize($services.localization.render('core.tags.deleting')));
        if (responseText !== undefined) {
          // delete the corresponding element
          item.closest('.tag-wrapper').remove();
        }
        item.disabled = false;
      }
    });
  }

  createTagAddForm(item) {
    item.addEventListener('click', async event => {
      stopEvent(event);
      if (!item._x_form) {
        if (!item.disabled) {
          // ignore "cascade" clicks
          item.disabled = true;
          const responseText = await post(item.getAttribute('href').replace(/#.*/, "&ajax=1&xpage=documentTags"),
            $jsontool.serialize($services.localization.render('core.tags.fetchform')));
          if (responseText !== undefined) {
            const iParent = item.parentElement;
            item.remove();
            iParent.innerHTML = responseText;
            item._x_form = iParent.firstElementChild;
            item._x_form._x_activator = item;
            item._x_form.querySelector('input[type=text]').focus();
            this.ajaxifyForm(item._x_form);
          }
          item.disabled = false;
        }
      } else {
        item.replaceWith(item._x_form);
        item._x_form.querySelector('input[type=text]').focus();
      }
    });
  }

  ajaxifyForm(form) {
    const tagInput = form.querySelector('input[type=text]');
    form.setAttribute('autocomplete', 'off');
    tagInput.setAttribute('autocomplete', 'off');
    form.addEventListener('submit', async event => {
      stopEvent(event);
      tagInput.focus();
      if (form.tag.value != '') {
        const url = form.action.replace(/&xredirect=.+$/, '&ajax=1&tag=') + encodeURIComponent(form.tag.value);
        // ignore "cascade" clicks
        setFormDisabled(form, true);
        const responseText = await post(url, $jsontool.serialize($services.localization.render('core.tags.adding')));
        setFormDisabled(form, false);
        if (responseText !== undefined) {
          const wrapper = document.createElement('span');
          wrapper.innerHTML = responseText;
          wrapper.querySelectorAll('.tag-delete').forEach(item => this.ajaxTagDelete(item));
          // Insert the added tags before the "add" button, separated by a space.
          const tagAdd = form.closest('.tag-add');
          for (const tag of Array.from(wrapper.children)) {
            tagAdd.before(tag, ' ');
          }
          form.reset();
        }
      }
    });
    form.addEventListener('reset', () => {
      // The form is displayed without an activator when the page is loaded with the add form already shown.
      if (form._x_activator) {
        form.replaceWith(form._x_activator);
      } else {
        form.remove();
      }
    });
    // Replace the Cancel link (which is supposed to 
    const cancelLink = form.querySelector('.button-add-tag-cancel');
    const cancel = document.createElement("input");
    Object.assign(cancel, {type: "reset", value: cancelLink.innerHTML, className: "button secondary"});
    cancelLink.replaceWith(cancel);

    new XWiki.widgets.Suggest(tagInput, {
      script: new XWiki.Document('WebHome', 'Main').getURL('view',
        'xpage=suggest&classname=XWiki.TagClass&fieldname=tags&firCol=-&secCol=-') + '&',
      parentContainer: 'tag-add-form-suggest', // Generate the suggest drop-down at a correct place in the DOM for easy keyboard access.
      varname: 'input',
      seps: $jsontool.serialize($xwiki.getDocument('XWiki.TagClass').xWikiClass.tags.getProperty('separators').value),
      shownoresults : false,
      icon: $jsontool.serialize($xwiki.getSkinFile('icons/silk/tag_yellow.png'))
    });
  }
}

/** Required by Prototype.js' Class.create(), which registers each new subclass on its parent class. */
Tags.subclasses = [];

viewers.Tags = Tags;

function init() {
  return new viewers.Tags();
}

// When the document is loaded, trigger the Tags enhancements.
if (document.readyState === 'loading') {
  document.addEventListener('DOMContentLoaded', init);
} else {
  init();
}

// End XWiki augmentation.
return XWiki;
}(XWiki || {}));

