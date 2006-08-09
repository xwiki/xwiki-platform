Rico.Accordion.prototype.initialize = function(container, options) {
      this.container            = $(container);
      this.lastExpandedTab      = null;
      this.accordionTabs        = new Array();
      this.setOptions(options);
      this._attachBehaviors();
      if(!container) return;

      this.container.style.borderBottom = '1px solid ' + this.options.borderColor;
      // validate onloadShowTab
       if (this.options.onLoadShowTab >= this.accordionTabs.length)
        this.options.onLoadShowTab = 0;

      this.lastExpandedTab = this.accordionTabs[this.options.onLoadShowTab];
      if (this.options.panelHeight == 'auto'){
          var tabToCheck = (this.options.onloadShowTab === 0)? 1 : 0;
          var titleBarSize = parseInt(RicoUtil.getElementsComputedStyle(this.accordionTabs[tabToCheck].titleBar, 'height'));
          if (isNaN(titleBarSize))
            titleBarSize = this.accordionTabs[tabToCheck].titleBar.offsetHeight;
          
          var totalTitleBarSize = this.accordionTabs.length * titleBarSize;
          var parentHeight = parseInt(RicoUtil.getElementsComputedStyle(this.container.parentNode, 'height'));
          if (isNaN(parentHeight))
            parentHeight = this.container.parentNode.offsetHeight;
          this.options.panelHeight = parentHeight - totalTitleBarSize-2;
      }
      else if(this.options.panelHeight == 'max'){
        var tabHeight;
	this.options.panelHeight = 0;
        for ( var i=0 ; i < this.accordionTabs.length ; i++ ) {
          tabHeight = parseInt(RicoUtil.getElementsComputedStyle(this.accordionTabs[i].content, 'height')) + parseInt(RicoUtil.getElementsComputedStyle(this.accordionTabs[i].titleBar, 'height'));
	  if(isNaN(tabHeight) || tabHeight <= 0){
	    if(this.accordionTabs[i].content.offsetHeight){
	      tabHeight = this.accordionTabs[i].content.offsetHeight;
	    }
	  }
	  if(tabHeight > this.options.panelHeight){
	    this.options.panelHeight = tabHeight;
          }
        }
      }
      // set the initial visual state...
      for ( var i=0 ; i < this.accordionTabs.length ; i++ )
      {
        if (i != this.options.onLoadShowTab){
         this.accordionTabs[i].collapse();
         this.accordionTabs[i].content.style.display = 'none';
        }
      }

      this.lastExpandedTab.content.style.height = this.options.panelHeight + "px";
      this.lastExpandedTab.showExpanded();
      this.lastExpandedTab.titleBar.style.fontWeight = this.options.expandedFontWeight;
   }

Rico.Accordion.prototype.setOptions = function(options) {
  this.options = {
     expandedBg          : '#EEE',
     hoverBg             : '#FFD',
     collapsedBg         : '#EEE',
     expandedTextColor   : '#000',
     expandedFontWeight  : 'bold',
     hoverTextColor      : '#000',
     collapsedTextColor  : '#777',
     collapsedFontWeight : 'normal',
     hoverTextColor      : '#000',
     borderColor         : '#FFF',
     panelHeight         : 200,
     onHideTab           : null,
     onShowTab           : null,
     onLoadShowTab       : 0
  }
  Object.extend(this.options, options || {});
}
