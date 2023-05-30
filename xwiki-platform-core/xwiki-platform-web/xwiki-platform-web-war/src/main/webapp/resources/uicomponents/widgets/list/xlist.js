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
 * var xlist = new XWiki.widgets.XList(
 *      [ // array of initial list elements  (or just content it works too
 *         new XWiki.widgets.XListItem( "A first element" ),
 *         new XWiki.widgets.XListItem( "A second element", {'value' : '10'} ),
 *         "A third item passed as string content",
 *         new Element("blink").update('An hip-hop item passed as DOM element')
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
 * $('insertionNode').insert( xlist.getElement() );
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
 */

var XWiki = function(XWiki){

    var widgets = XWiki.widgets = XWiki.widgets || {};

    widgets.XList = Class.create({
        initialize: function(items, options) {
          this.items = items || [];
          this.options = options || {}
          this.listElement = new Element(this.options.ordered ? "ol" : "ul", {
            'class' : 'xlist' + (this.options.classes ? (' ' + this.options.classes) : '')
          });
          if (this.items && this.items.length > 0) {
            for (var i=0;i<this.items.length;i++) {
              this.addItem(this.items[i]);
            }
          }
        },
        addItem: function(item){ /* future: position (top, N) */
          if (!item || !(item instanceof XWiki.widgets.XListItem)) {
             item = new XWiki.widgets.XListItem(item);
          }
          var listItemElement = item.getElement();
          if (this.options.itemClasses && !this.options.itemClasses.blank()) {
            listItemElement.addClassName(this.options.itemClasses);
          }
          this.listElement.insert(listItemElement);
          if (typeof this.options.eventListeners == 'object') {
            item.bindEventListeners(this.options.eventListeners);
          }
          if (this.options.icon && !this.options.icon.blank()) {
            item.setIcon(this.options.icon, this.options.overrideItemIcon);
          }
          item.list = this; // associate list item to this XList
        },
        getElement: function() {
          return this.listElement;
        }
    });

    widgets.XListItem = Class.create({
        initialize: function(content, options) {
          this.options = options || {};
          var classes = 'xitem ' + (this.options.noHighlight ? '' : 'xhighlight ');
          classes += this.options.classes ? this.options.classes: '';
          const containerTagName = this.options.containerTagName || 'div';
          this.containerElement = new Element(containerTagName, {'class': 'xitemcontainer'}).insert(content || '');
          this.containerElement.addClassName(this.options.containerClasses || '');
          this.containerElement.setStyle({textIndent: '0px'});
          if (this.options.value) {
            this.containerElement.insert(new Element('div', {'class':'hidden value'}).insert(this.options.value));
          }
          this.listItemElement = new Element("li", {'class' : classes}).update( this.containerElement );
          if (this.options.icon && !this.options.icon.blank()) {
            this.setIcon(this.options.icon);
            this.hasIcon = true;
          }
          if (typeof this.options.eventListeners == 'object') {
            this.bindEventListeners(this.options.eventListeners);
          }
        },
        getElement: function() {
          return this.listItemElement;
        },
        setIcon: function(icon, override) {
          if (!this.hasIcon || override) {
            this.iconImage = new Image();
            this.iconImage.onload = function(){
                this.listItemElement.setStyle({
                  backgroundImage: "url(" + this.iconImage.src + ")",
                  backgroundRepeat: 'no-repeat',
                  // TODO: support background position as option
                  backgroundPosition : '3px 3px'
                });
                this.listItemElement.down(".xitemcontainer").setStyle({
                  textIndent:(this.iconImage.width + 6) + 'px'
                });
            }.bind(this)
            this.iconImage.src = icon;
          }
        },
        bindEventListeners: function(eventListeners) {
          var events = Object.keys(eventListeners);
          for (var i=0;i<events.length;i++) {
            this.listItemElement.observe(events[i], eventListeners[events[i]].bindAsEventListener(this.options.eventCallbackScope ? this.options.eventCallbackScope : this));
          }
        }
    });

    return XWiki;

}(XWiki || {});
