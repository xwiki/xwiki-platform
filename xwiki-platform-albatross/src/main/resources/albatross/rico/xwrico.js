Rico.Accordion.prototype.initialize = function(container, options) {
      this.container            = $(container);
      this.lastExpandedTab      = null;
      this.accordionTabs        = new Array();
      this.setOptions(options);
      this._attachBehaviors();
      if(!container) return;

      // this.container.style.borderBottom = '1px solid ' + this.options.borderColor;
      // validate onloadShowTab
       if (this.options.onLoadShowTab >= this.accordionTabs.length)
        this.options.onLoadShowTab = 0;

      this.lastExpandedTab = this.accordionTabs[this.options.onLoadShowTab];

      if (this.options.panelHeight == 'auto'){
          var tabToCheck = (this.options.onloadShowTab === 0)? 1 : 0;
          if (this.accordionTabs && this.accordionTabs.length > 0)
          var titleBarSize = parseInt(RicoUtil.getElementsComputedStyle(this.accordionTabs[tabToCheck].titleBar, 'height'));
          if (isNaN(titleBarSize))
            titleBarSize = this.accordionTabs[tabToCheck].titleBar.offsetHeight;

          var totalTitleBarSize = this.accordionTabs.length * titleBarSize;
          var parentHeight = parseInt(RicoUtil.getElementsComputedStyle(this.container.parentNode, 'height'));
          if (isNaN(parentHeight))
            parentHeight = this.container.parentNode.offsetHeight;
          this.options.panelHeight = parentHeight - totalTitleBarSize-2;
      }
      else if (this.options.panelHeight == 'flex'){
        this.flexible = true;
        this.tabHeight = new Object();
        var tabToCheck = (this.options.onloadShowTab === 0)? 1 : 0;
          if (this.accordionTabs && this.accordionTabs.length > 0)
            var titleBarSize = parseInt(RicoUtil.getElementsComputedStyle(this.accordionTabs[tabToCheck].titleBar, 'height'));
          if (isNaN(titleBarSize))
            titleBarSize = this.accordionTabs[tabToCheck].titleBar.offsetHeight;

          var totalTitleBarSize = this.accordionTabs.length * titleBarSize;

         for ( var i=0 ; i < this.accordionTabs.length ; i++ ) {
          this.tabHeight[i] = parseInt(RicoUtil.getElementsComputedStyle(this.accordionTabs[i].content, 'height')) + 2;
// + parseInt(RicoUtil.getElementsComputedStyle(this.accordionTabs[i].titleBar, 'height'));
	  if(isNaN(this.tabHeight[i]) || this.tabHeight[i] <= 0){
	    if(this.accordionTabs[i].content.offsetHeight){
	      this.tabHeight[i] = this.accordionTabs[i].content.offsetHeight;
	    }
	  }
        }
        this.options.panelHeight = this.tabHeight[this.options.onloadShowTab ? this.options.onloadShowTab : 0];
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

      if(this.options.maxHeight) {
        if(this.options.panelHeight > this.options.maxHeight) {
          this.options.panelHeight = this.options.maxHeight;
        }
      }
      
      this.lastExpandedTab.content.style.height = this.options.panelHeight + "px";
      this.lastExpandedTab.showExpanded();
      this.lastExpandedTab.titleBar.style.fontWeight = this.options.expandedFontWeight;
      setExpandedClass(this.lastExpandedTab);
   }
Rico.Accordion.prototype.showTabByIndex = function( anIndex, animate ) {
      var doAnimate = arguments.length == 1 ? true : animate;
      this.showTab( this.accordionTabs[anIndex], doAnimate, this.flexible ? this.tabHeight[anIndex] : undefined );
   },


Rico.Accordion.prototype.showTab = function( accordionTab, animate, tabHeight ) {
    if ( this.lastExpandedTab == accordionTab )
        return;
    var oldHeight = this.options.panelHeight;
    var newHeight;

    if (tabHeight){
       this.options.panelHeight = tabHeight;
    }
    else if (this.flexible){
        for ( var i=0 ; i < this.accordionTabs.length ; i++ ) {
          if (accordionTab == this.accordionTabs[i]){
             newHeight = this.tabHeight[i];
             break;
          }
        }
      }
    else{
      newHeight = this.options.panelHeight;
    }


    var doAnimate = arguments.length == 1 ? this.options.doAnimate : animate;

      if ( this.options.onHideTab )
         this.options.onHideTab(this.lastExpandedTab);

      if ( this.options.onShowTab )
         this.options.onShowTab(accordionTab);

      this.lastExpandedTab.showCollapsed(); 
      var accordion = this;
      var lastExpandedTab = this.lastExpandedTab;

      if (this.options.panelHeight)
        this.lastExpandedTab.content.style.height = (this.options.panelHeight - 1) + 'px';

      //accordionTab.content.style.display = "";
      accordionTab.titleBar.style.fontWeight = this.options.expandedFontWeight;

      if(this.options.maxHeight) {
        if(newHeight > this.options.maxHeight) {
          newHeight = this.options.maxHeight;
        }
      }
      if ( doAnimate ) {
         new Rico.Effect.AccordionSize( this.lastExpandedTab.content,
                                   accordionTab.content,
                                   1,
                                   newHeight,
                                   50, 5,
                                   { complete: function() {accordion.showTabDone(lastExpandedTab)} }, oldHeight, 1 );
         this.lastExpandedTab = accordionTab;
      }
      else {
         this.lastExpandedTab.content.style.height = "1px";
         accordionTab.content.style.height = newHeight + "px";
         this.lastExpandedTab = accordionTab;
         this.showTabDone(lastExpandedTab);
      }
      this.options.panelHeight = newHeight;
   }

Rico.Accordion.prototype.setOptions = function(options) {
  this.options = {
     expandedBg          : '#EEE',
     hoverBg             : '#FFD',
     collapsedBg         : '#EEE',
     backgroundColor     : '#FFF',
     textColor           : '#000',
     expandedTextColor   : '#000',
     expandedFontWeight  : 'bold',
     hoverTextColor      : '#000',
     collapsedTextColor  : '#777',
     collapsedFontWeight : 'normal',
     hoverTextColor      : '#000',
     borderColor         : '#FFF',
     panelHeight         : 200,
     onHideTab           : setCollapsedClass,
     onShowTab           : setExpandedClass,
     onLoadShowTab       : 0,
     doAnimate           : true,
     maxHeight           : false
  }
  Object.extend(this.options, options || {});
}

Rico.Accordion.Tab.prototype._attachBehaviors = function() {
  this.content.style.border = "1px solid " + this.accordion.options.borderColor;
  this.content.style.borderTopWidth    = "0px";
  this.content.style.borderBottomWidth = "0px";
  this.content.style.margin            = "0px";
  this.content.style.backgroundColor   = this.accordion.options.backgroundColor;
  this.content.style.color   = this.accordion.options.textColor;

  this.titleBar.onclick     = this.titleBarClicked.bindAsEventListener(this);
  this.titleBar.onmouseover = this.hover.bindAsEventListener(this);
  this.titleBar.onmouseout  = this.unhover.bindAsEventListener(this);
}

Rico.Effect.AccordionSize.prototype = {
   initialize: function(e1, e2, start1, end1, duration, steps, options, start2, end2) {
      this.e1       = $(e1);  // shrinks
      this.e2       = $(e2);  // grows
      this.start    = start1; // 1
      this.end      = end1;   // new height
      this.start2   = start2 ? start2 : end1; // old height
      this.end2     = end2 ? end2 : start1;   // 1
      this.duration = duration;
      this.steps    = steps;
      this.options  = arguments[6] || {};
      this.totalsteps = steps;
      this.accordionSize();
   },

   accordionSize: function() {
      //return;
      if (this.isFinished()) {
        if (this.timer){
          clearInterval(this.timer);
        }
        // just in case there are round errors or such...
        this.e1.style.height = this.start + "px";
        this.e2.style.height = this.end + "px";
        if(this.options.complete)
          this.options.complete(this);
        return;
      }

      this.steps--;
      var stepDuration = Math.round(this.duration/this.totalsteps) ;

      // var diff = this.steps > 0 ? (parseInt(this.e1.offsetHeight) - this.end2)/this.steps : 0;
      // var diff2 = this.steps > 0 ? (this.end - parseInt(this.e2.offsetHeight))/this.steps : 0;
      // this.resizeBy(diff, diff2);
      var h1 = this.steps > 0 ? this.steps * (this.start2 - this.end2) / this.totalsteps - 1: 0;
      if(window.ActiveXObject){
        h1 += 2;
      }
      var h2 = this.steps > 0 ? this.end - this.steps * (this.end - this.start) / this.totalsteps : this.end;
      this.resizeTo(h1, h2);

      if(!this.timer){
        this.timer = setInterval(this.accordionSize.bind(this), stepDuration);
      }
   },

   isFinished: function() {
      return this.steps <= 1;
   },

   resizeBy: function(diff, diff2) {
      var h1Height = this.e1.offsetHeight;
      var h2Height = this.e2.offsetHeight;
      var intDiff = parseInt(diff);
      var intDiff2 = diff2? parseInt(diff2) : parseInt(diff);
      if (intDiff != 0 ) {
         this.e1.style.height = (h1Height - intDiff) + "px";
      }
      if(intDiff2 != 0 ){
         this.e2.style.height = (h2Height + intDiff2) + "px";
      }
   },
   resizeTo: function(h1, h2) {
     this.e1.style.height = h1 + "px";
     this.e2.style.height = h2 + "px";
     this.e1.style.display = '';
     this.e2.style.display = '';
   }
};

function setCollapsedClass(tab){
  tab.titleBar.className = "accordionTabTitleBar";
}

function setExpandedClass(tab){
  tab.titleBar.className = tab.titleBar.className + " tbexpanded";
}
