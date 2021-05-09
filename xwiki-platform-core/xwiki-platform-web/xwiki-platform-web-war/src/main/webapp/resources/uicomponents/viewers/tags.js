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
 * Tag editing.
 */
viewers.Tags = Class.create({
  /**
   * Initialization: add listeners for all tag actions, to perform them via AJAX
   */
  initialize : function() {
    // delete tags
    $$('.doc-tags .tag-delete').each(this.ajaxTagDelete);
    $$('.doc-tags .tag-add a').each(this.createTagAddForm.bind(this));
    if ($$('.doc-tags .tag-add-form').length > 0) {
      this.ajaxifyForm($$('.doc-tags .tag-add-form')[0]);
    }
  },
  /** AJAX tag removal */
  ajaxTagDelete : function (item) {
    item.observe('click', function(event) {
      if (event) {
        event.stop();
      }
      if (!item.disabled) {
        new Ajax.Request(
          item.readAttribute('href').replace(/&xredirect=.+$/, "&ajax=1"),
          {
            onCreate : function () {
              // ignore "cascade" clicks
              item.disabled = true;
              item.notification = new XWiki.widgets.Notification("$services.localization.render('core.tags.deleting')", "inprogress");
            },
            onSuccess : function () {
              // delete the corresponding element
              item.up('.tag-wrapper').remove();
            },
            onFailure : function (response) {
               new XWiki.widgets.Notification(response.responseText || 'Server not responding', "error");
            },
            // 0 is returned for network failures, except on IE where a strange large number (12031) is returned.
            on0 : function(response) {
              response.request.options.onFailure(response);
            },
            onComplete : function () {
              item.disabled = false;
              item.notification.hide();
            }
          }
        );
      }
    }.bindAsEventListener());
  },
  createTagAddForm : function (item) {
    item.observe('click', function(event) {
      if (event) {
        event.stop();
      }
      if (!item._x_form) {
        if (!item.disabled) {
          new Ajax.Request(
            item.readAttribute('href').replace(/#.+$/, "&ajax=1&xpage=documentTags"),
            {
              onCreate : function () {
                // ignore "cascade" clicks
                item.disabled = true;
                item.notification = new XWiki.widgets.Notification("$services.localization.render('core.tags.fetchform')", "inprogress");
              },
              onSuccess : function (response) {
                var iParent = item.up();
                item.remove();
                iParent.update(response.responseText);
                item._x_form = iParent.firstDescendant();
                item._x_form._x_activator = item;
                item._x_form.down('input[type=text]').focus();
                this.ajaxifyForm(item._x_form);
              }.bind(this),
              onFailure : function (response) {
                new XWiki.widgets.Notification(response.responseText || 'Server not responding', "error");
              },
              // 0 is returned for network failures, except on IE where a strange large number (12031) is returned.
              on0 : function(response) {
                response.request.options.onFailure(response);
              },
              onComplete : function () {
                item.disabled = false;
                item.notification.hide();
              }
            }
          );
        }
      } else {
        Element.replace(item, item._x_form);
        item._x_form.down('input[type=text]').focus();
      }
    }.bindAsEventListener(this));
  },
  ajaxifyForm : function(form) {
    form.setAttribute('autocomplete', 'off');
    form.down('input[type=text]').setAttribute('autocomplete', 'off');
    form.down('input[type=text]').setAttribute('autocomplete', 'off');
    form.observe('submit', function(event) {
      event.stop();
      form.down('input[type=text]').focus();
      if (form.tag.value != '') {
        new Ajax.Request(
          form.action.replace(/&xredirect=.+$/, '&ajax=1&tag=') + encodeURIComponent(form.tag.value),
          {
            onCreate : function () {
              // ignore "cascade" clicks
              form.disable();
              form.notification = new XWiki.widgets.Notification("$services.localization.render('core.tags.adding')", "inprogress");
            },
            onSuccess : function (response) {
              var wrapper = new Element('span');
              wrapper.insert(response.responseText + ' ');
              wrapper.select('.tag-delete').each(this.ajaxTagDelete);
              while (wrapper.childNodes.length > 0) {
                form.up('.tag-add').insert({before : wrapper.firstChild});
                form.up('.tag-add').insert({before : ' '});
                wrapper.removeChild(wrapper.firstChild);
              }
              form.reset();
            }.bind(this),
            onFailure : function (response) {
              new XWiki.widgets.Notification(response.responseText || 'Server not responding', "error");
            },
            onComplete : function () {
              form.enable();
              form.notification.hide();
            },
            // 0 is returned for network failures, except on IE where a strange large number (12031) is returned.
            on0 : function(response) {
              response.request.options.onFailure(response);
            }
          }
        );
      }
    }.bindAsEventListener(this));
    form.observe('reset', function(event) {
      Element.replace(form, form._x_activator);
    }.bindAsEventListener(this));
    // Replace the Cancel link (which is supposed to 
    var cancel = new Element("input", {type: "reset", value : form.down('.button-add-tag-cancel').innerHTML, "class" : "button secondary"});
    form.down('.button-add-tag-cancel').replace(cancel);

    new XWiki.widgets.Suggest(form.down('input[type=text]'), {
      script: "${xwiki.getURL('Main.WebHome', 'view', 'xpage=suggest&classname=XWiki.TagClass&fieldname=tags&firCol=-&secCol=-')}&",
      varname: 'input',
      seps: "${xwiki.getDocument('XWiki.TagClass').xWikiClass.tags.getProperty('separators').value}",
      shownoresults : false,
      icon: "${xwiki.getSkinFile('icons/silk/tag_yellow.png')}"
    });
  }
});

function init() {
  return new viewers.Tags();
}

// When the document is loaded, trigger the Tags enhancements.
(XWiki.domIsLoaded && init())
|| document.observe("xwiki:dom:loaded", init);

// End XWiki augmentation.
return XWiki;
}(XWiki || {}));

