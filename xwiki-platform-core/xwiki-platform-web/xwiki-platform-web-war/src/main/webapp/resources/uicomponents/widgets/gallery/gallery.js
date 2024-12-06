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
XWiki.Gallery = Class.create({
  initialize : function(container) {
    this.images = this._collectImages(container);
    this.container = container.update(
      '<button class="maximize" title="${escapetool.xml($services.localization.render("core.widgets.gallery.maximize"))}"></button>' +
      '<button class="previous" title="${escapetool.xml($services.localization.render("core.widgets.gallery.previousImage"))}">&lt;</button>' +
      '<img class="currentImage" alt="${escapetool.xml($services.localization.render("core.widgets.gallery.currentImage"))}"/>' +
      '<button class="next" title="${escapetool.xml($services.localization.render("core.widgets.gallery.nextImage"))}">&gt;</button>' +
      '<div class="index" tabindex="0" title="${escapetool.xml($services.localization.render("core.widgets.gallery.index.description"))}" aria-description="${escapetool.xml($services.localization.render("core.widgets.gallery.index.description"))}">0 / 0</div>'
    );
    this.container.addClassName('xGallery');    
    
    /* Instead of an arbitrary element to catch focus, we use the index.
    * This index already stores the current image state, might as well be responsible for providing quick controls and
    * explanations about these quick controls.
    * Note that wrapping the image in an interactive container to handle this would have been a good solution too.
    * However, this wrapping caused the image to overflow the CSS grid vertically when in maximized mode. 
    * Technically I couldn't find a CSS solution to prevent this, so I decided to make do without wrapping. */
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
      images.push({url: imageElement.getAttribute('src'), title: imageElement.title});
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
      "${escapetool.javascript($services.localization.render('core.widgets.gallery.maximize'))}" :
      "${escapetool.javascript($services.localization.render('core.widgets.gallery.minimize'))}";
    this.container.toggleClassName('maximized');
    $(document.documentElement).toggleClassName('maximized');
  },
  show : function(index) {
    if (index < 0 || index >= this.images.length || index == this.index) {
      return;
    }
    // Update only if it's a different image. Some browsers, e.g. Chrome, don't fire the load event if the image URL
    // doesn't change. Another trick would be to reset the src attribute before setting the actual URL (set to '').
    if (this.currentImage.src != this.images[index].url) {
      this.currentImage.style.visibility = 'hidden';
      Element.addClassName(this.currentImage.parentNode, 'loading');
      this.currentImage.title = this.images[index].title;
      this.currentImage.src = this.images[index].url;
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