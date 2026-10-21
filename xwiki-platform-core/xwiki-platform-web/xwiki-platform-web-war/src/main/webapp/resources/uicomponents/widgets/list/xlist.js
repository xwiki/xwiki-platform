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
/**
 * Usage :
 *
 * var blink = document.createElement("blink");
 * blink.textContent = 'An hip-hop item passed as DOM element';
 * var xlist = new XWiki.widgets.XList(
 *      [ // array of initial list elements  (or just content it works too
 *         new XWiki.widgets.XListItem( "A first element" ),
 *         new XWiki.widgets.XListItem( "A second element", {'value' : '10'} ),
 *         "A third item passed as string content",
 *         blink
 *      ],
 *      { // options
 *         numbered: false,
 *         icon: "$xwiki.getSkinFile('icons/silk/sport_basketball.png')",
 *         classes : "myListExtraClass",
 *         itemClasses : "myListItemExtraClasses",
 *         eventListeners : {
 *           // Event listeners defined for each of this list items.
 *           // listeners call backs are bound to the list item object (XWiki.widgets.XListItem) from which they emerge
 *           'click' : function() { console.log('clicked !', this); },
 *           'mouseover' : function() { console.log('mouse over !', this); }
 *         }
 *      });
 *
 * document.getElementById('insertionNode').append( xlist.getElement() );
 *
 * xlist.addItem(
 *   new XWiki.widgets.XListItem('A fifth element added later', {
 *     icon : "$xwiki.getSkinFile('icons/silk/bomb.png')", // this overrides the one defined for the whole list
 *     eventListeners: {
 *       // Event listeners defined just for this specific list item
 *       'mouseout' : function() { console.log('just this list item is bound to this event', this); }
 *     }
 *   })
 * );
 *
 * Note that these classes can be extended using Prototype.js' Class.create(XWiki.widgets.XList, {...}). This is why
 * their constructors only delegate to the initialize method (Prototype.js calls only the initialize method when
 * creating an instance of a subclass).
 */

var XWiki = function(XWiki){

    var widgets = XWiki.widgets = XWiki.widgets || {};

    function isBlank(value) {
      return typeof value !== 'string' || value.trim() === '';
    }

    /**
     * Adds the given space-separated CSS classes to the given element.
     */
    function addClassNames(element, classNames) {
      if (!isBlank(classNames)) {
        element.classList.add(...classNames.trim().split(/\s+/));
      }
    }

    /**
     * Appends the given content to the given element. The content can be either a DOM node or a string, in which case
     * it is parsed as HTML.
     */
    function insertContent(element, content) {
      if (content instanceof Node) {
        element.append(content);
      } else if (content !== undefined && content !== null) {
        element.insertAdjacentHTML('beforeend', String(content));
      }
    }

    class XList {
        constructor(...args) {
          this.initialize(...args);
        }

        initialize(items, options) {
          this.items = items || [];
          this.options = options || {};
          this.listElement = document.createElement(this.options.ordered ? "ol" : "ul");
          this.listElement.className = 'xlist';
          addClassNames(this.listElement, this.options.classes);
          for (const item of this.items) {
            this.addItem(item);
          }
        }

        addItem(item) { /* future: position (top, N) */
          if (!item || !(item instanceof XWiki.widgets.XListItem)) {
             item = new XWiki.widgets.XListItem(item);
          }
          var listItemElement = item.getElement();
          addClassNames(listItemElement, this.options.itemClasses);
          this.listElement.append(listItemElement);
          if (typeof this.options.eventListeners == 'object') {
            item.bindEventListeners(this.options.eventListeners);
          }
          if (!isBlank(this.options.icon)) {
            item.setIcon(this.options.icon, this.options.overrideItemIcon);
          }
          item.list = this; // associate list item to this XList
        }

        getElement() {
          return this.listElement;
        }
    }

    /** Required by Prototype.js' Class.create(), which registers each new subclass on its parent class. */
    XList.subclasses = [];

    widgets.XList = XList;

    class XListItem {
        constructor(...args) {
          this.initialize(...args);
        }

        initialize(content, options) {
          this.options = options || {};
          const containerTagName = this.options.containerTagName || 'div';
          this.containerElement = document.createElement(containerTagName);
          this.containerElement.className = 'xitemcontainer';
          insertContent(this.containerElement, content || '');
          addClassNames(this.containerElement, this.options.containerClasses);
          this.containerElement.style.textIndent = '0px';
          if (this.options.value) {
            const valueElement = document.createElement('div');
            valueElement.className = 'hidden value';
            insertContent(valueElement, this.options.value);
            this.containerElement.append(valueElement);
          }
          this.listItemElement = document.createElement("li");
          this.listItemElement.className = 'xitem';
          if (!this.options.noHighlight) {
            this.listItemElement.classList.add('xhighlight');
          }
          addClassNames(this.listItemElement, this.options.classes);
          this.listItemElement.append(this.containerElement);
          if (!isBlank(this.options.icon)) {
            this.setIcon(this.options.icon);
            this.hasIcon = true;
          }
          if (typeof this.options.eventListeners == 'object') {
            this.bindEventListeners(this.options.eventListeners);
          }
        }

        getElement() {
          return this.listItemElement;
        }

        setIcon(icon, override) {
          if (!this.hasIcon || override) {
            this.iconImage = new Image();
            this.iconImage.onload = () => {
                Object.assign(this.listItemElement.style, {
                  backgroundImage: "url(" + this.iconImage.src + ")",
                  backgroundRepeat: 'no-repeat',
                  // TODO: support background position as option
                  backgroundPosition : '3px 3px'
                });
                this.listItemElement.querySelector(".xitemcontainer").style.textIndent =
                  (this.iconImage.width + 6) + 'px';
            };
            this.iconImage.src = icon;
          }
        }

        bindEventListeners(eventListeners) {
          const scope = this.options.eventCallbackScope || this;
          for (const [eventName, listener] of Object.entries(eventListeners)) {
            this.listItemElement.addEventListener(eventName, event => listener.call(scope, event));
          }
        }
    }

    /** Required by Prototype.js' Class.create(), which registers each new subclass on its parent class. */
    XListItem.subclasses = [];

    widgets.XListItem = XListItem;

    return XWiki;

}(XWiki || {});
