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
/*!
#set ($l10nKeys = [
 'core.widgets.gallery.maximize',
 'core.widgets.gallery.previousImage',
 'core.widgets.gallery.currentImage',
 'core.widgets.gallery.nextImage',
 'core.widgets.gallery.index.description',
 'core.widgets.gallery.maximize',
 'core.widgets.gallery.minimize'
])
#set ($l10n = {})
#foreach ($key in $l10nKeys)
  #set ($discard = $l10n.put($key, $services.localization.render($key)))
#end
#[[*/
// Start JavaScript-only code.
(function(l10n) {
  "use strict";
var XWiki = (function (XWiki) {
// Start XWiki augmentation.
XWiki.Gallery = Class.create({
  initialize : function(container) {
    this.images = this._collectImages(container);
    // Generate the different parts of the gallery
    let maximizeButton = new Element('button', {
      'class': 'maximize', 'title': l10n['core.widgets.gallery.maximize']});
    let previousButton = new Element('button', {
      'class': 'previous', 'title': l10n['core.widgets.gallery.previousImage']});
    previousButton.insert("&lt;");
    let currentImage = new Element('img',
      {'class': 'currentImage', 'title': l10n['core.widgets.gallery.currentImage']});
    let nextButton = new Element('button',
      {'class': 'next', 'title': l10n['core.widgets.gallery.nextImage']});
    nextButton.insert("&gt;");
    let imageIndex = new Element('div', {
      'class': 'index', 'tabindex': 0, 'title': l10n['core.widgets.gallery.index.description'],
      'aria-description': l10n['core.widgets.gallery.index.description']});
    imageIndex.insert("0 / 0");
    // Remove the content that's left in the container
    container.update("");
    // Add the gallery parts in the container, in the correct order.
    container.insert(maximizeButton);
    container.insert(previousButton);
    container.insert(currentImage);
    container.insert(nextButton);
    container.insert(imageIndex);
    this.container = container;
    this.container.addClassName('xGallery');    
    
    // Instead of an arbitrary element to catch focus, we use the index.
    // This index already stores the current image state, might as well be responsible for providing quick controls and
    // explanations about these quick controls.
    // Note that wrapping the image in an interactive container to handle this would have been a good solution too.
    // However, this wrapping caused the image to overflow the CSS grid vertically when in maximized mode. 
    // Technically I couldn't find a CSS solution to prevent this, so I decided to make do without wrapping.
    this.focusCatcher = this.container.down('.index');
    this.focusCatcher.observe('keydown', this._onKeyDown.bindAsEventListener(this));

    this.container.down('.previous').observe('click', this._onPreviousImage.bind(this));
    this.container.down('.next').observe('click', this._onNextImage.bind(this));
    this.container.observe('click', function() {
      this.focusCatcher.focus();
    }.bind(this));

    this.currentImage = this.container.down('.currentImage');
    this.currentImage.observe('load', this._onLoadImage.bind(this));
    this.currentImage.observe('error', this._onErrorImage.bind(this));
    this.currentImage.observe('abort', this._onAbortImage.bind(this));

    this.indexDisplay = this.container.down('.index');

    this.maximizeToggle = this.container.down('.maximize');
    this.maximizeToggle.observe('click', this._onToggleMaximize.bind(this));

    this.show(0);
  },
  _collectImages : function(container) {
    var images = [];
    var imageElements = container.select('img');
    for(var i = 0; i < imageElements.length; i++) {
      var imageElement = imageElements[i];
      images.push({url: imageElement.getAttribute('src'), title: imageElement.title, alt: imageElement.alt});
      imageElement.removeAttribute('src');
    }
    return images;
  },
  _onPreviousImage : function() {
    this.show(this.index > 0 ? this.index - 1 : this.images.length - 1);
  },
  _onNextImage : function() {
    this.show(this.index < this.images.length - 1 ? this.index + 1 : 0);
  },
  _onLoadImage : function() {
    Element.removeClassName(this.currentImage.parentNode, 'loading');
    this.currentImage.style.visibility = 'visible';
  },
  _onErrorImage: function() {
  },
  _onAbortImage: function() {
  },
  _onKeyDown : function(event) {
    var stop = true;
    switch(event.keyCode) {
      case Event.KEY_LEFT:
        this._onPreviousImage();
        break;
      case Event.KEY_RIGHT:
        this._onNextImage();
        break;
      case Event.KEY_HOME:
        this.show(0);
        break;
      case Event.KEY_END:
        this.show(this.images.length - 1);
        break;
      case Event.KEY_ESC:
        if (this.container.hasClassName('maximized')) {
          this._onToggleMaximize();
        }
        break;
      case 70: /* F */
        this._onToggleMaximize();
        break;
      default:
        stop = false;
        break;
    }
    if (stop) {
      Event.stop(event);
    }
  },
  _onToggleMaximize : function() {
    this.maximizeToggle.toggleClassName('maximize');
    this.maximizeToggle.toggleClassName('minimize');
    this.maximizeToggle.title = this.maximizeToggle.hasClassName('maximize') ?
      l10n['core.widgets.gallery.maximize'] : l10n['core.widgets.gallery.minimize'];
    this.container.toggleClassName('maximized');
    $(document.documentElement).toggleClassName('maximized');
    // When a keyboard shortcut is used, the gallery is not focused by default. In order to keep the screen at the
    // level of the gallery even when minimizing, we need to make sure it's always focused.
    // Without this forced focus, minimizing the gallery by pressing the `Escape` key will
    // unexpectedly send the user to the top of the page.
    this.maximizeToggle.focus();
  },
  show : function(index) {
    if (index < 0 || index >= this.images.length || index == this.index) {
      return;
    }
    // Update only if it's a different image. Some browsers, e.g. Chrome, don't fire the load event if the image URL
    // doesn't change. Another trick would be to reset the src attribute before setting the actual URL (set to '').
    const imageData = this.images[index];
    if (this.currentImage.src !== imageData.url) {
      this.currentImage.style.visibility = 'hidden';
      Element.addClassName(this.currentImage.parentNode, 'loading');
      this.currentImage.title = imageData.title;
      const filename = imageData.url.split('/').pop().split('?')[0];
      // If the alt is just the name of the file, we instead fall back on the human-readable currentImage translation.
      if (filename !== imageData.alt) {
        this.currentImage.alt = imageData.alt;
      } else {
        this.currentImage.alt = l10n['core.widgets.gallery.currentImage'];
      }
      this.currentImage.src = imageData.url;
    }
    this.index = index;
    this.indexDisplay.update((index + 1) + ' / ' + this.images.length);
  }
});

function init(event) {
  var elements = (event && event.memo.elements) || [$('body')];
  elements.forEach(function(element) {
    var galleries = element.hasClassName('gallery') ? [element] : element.select('.gallery');
    galleries.forEach(function (gallery) {
      new XWiki.Gallery(gallery);
    });
  });
}

// Don't initialize the galleries when exporting to PDF because we want to include all the images.
if (XWiki.contextaction !== 'export') {
  // When the document is loaded, install galleries
  (XWiki.isInitialized && init())
  || document.observe('xwiki:dom:loading', init);

  // Initialize the gallery when it is added after the page is loaded.
  document.observe('xwiki:dom:updated', init);
}

// End XWiki augmentation.
return XWiki;
}(XWiki || {}));
// End JavaScript-only code.
}).apply(']]#', $jsontool.serialize([$l10n]));