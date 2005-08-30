function chwPositionSelector(property, type, defaultPosition){
  this.property = property;
  this.type = type;
  /*
    The curently chosen position
    Possible values: 
    - Type 1 (4 positions): Top, Left, Right, Bottom
    - Type 2 (9 positions): TopLeft, TopCenter, TopRight,
                            CenterLeft, CenterCenter, CenterRight,
                            BottomLeft, BottomCenter, BottomRight
   */
  this.selected = defaultPosition;
  /*
     Is the left mouse button pressed at the moment?
   */
  this.leftButtonDown = false;
  /*
     Highlight the element
     (set the apropriate css class)
   */
  this.onmouseover = function(element, position){
    if (this.selected == position){
      element.className = "chwSelectedCellHighlighted";
    }
    else if (this.leftButtonDown == true){
      document.getElementById('chw' + this.property + this.selected).className = "normal";
      this.selected = position;
      element.className = "chwSelectedCellHighlighted";
      switch(this.type){
        case "Position":
          document.getElementById('chw' + this.property + 'Input').value = position.toLowerCase();
          break;
        case "Alignment":
          document.getElementById('chw' + this.property + 'HorizontalInput').value = position.substring(position.indexOf('_') + 1).toLowerCase();
          document.getElementById('chw' + this.property + 'VerticalInput').value = position.substring(0, position.indexOf('_')).toLowerCase();
          break;
      }
    }
    else {
      element.className = "chwNormalCellHighlighted";
    }
  } 
  /*
     Highlight the element
     (set the apropriate css class)
   */
  this.onmouseout = function(element, position){
    if (this.selected == position){
      element.className = "chwSelectedCell";
    }
    else {
      element.className = "chwNormalCell";
    }
  }

  /*
     Selection started...
   */
  this.onmousedown = function(evt, position){
    if (evt.button != window.wizard.LMB) return;
    this.leftButtonDown = true;
    if (this.selected == position) return;
    document.getElementById('chw' + this.property + this.selected).className = "chwNormalCell";
    this.selected = position;
      switch(this.type){
        case "Position":
          document.getElementById('chw' + this.property + 'Input').value = position.toLowerCase();
          break;
        case "Alignment":
          document.getElementById('chw' + this.property + 'HorizontalInput').value = position.substring(position.indexOf('_') + 1).toLowerCase();
          document.getElementById('chw' + this.property + 'VerticalInput').value = position.substring(0, position.indexOf('_')).toLowerCase();
          break;
      }
    document.getElementById('chw' + this.property + position).className = "chwSelectedCellHighlighted";
  }

  /*
     ... Selection ended
   */
  this.onmouseup = function(evt){
    if (evt.button != window.wizard.LMB) return;
    this.leftButtonDown = false;
  }
}

/*
   Color chooser
   Provides validation and visualizing functions
 */
function chwColorChooser(property){
  this.property = property;
  this.element = document.getElementById('chw' + this.property + 'Input');
  this.customGroup = document.getElementById('chw' + this.property + 'CustomGroup');
  this.custom = document.getElementById('chw' + this.property + 'CustomInput');
  this.customOption = document.getElementById('chw' + this.property + 'CustomOption');
  /*
     The color code that replaces the value entered
     by the user, if that vakue is wrong
   */
  this.storedColorCode = "#000000";
  /*
     Choice of color changed
   */
  this.colorChoiceChanged = function(){
    if (this.element.value.indexOf('#') == 0){
      this.customGroup.style.display="block";
    }
    else {
      this.customGroup.style.display="none";
    }
  }

  /*
     Show the color with the color code the user entered
   */
  this.showCustomColor = function(){
    if (this.custom.value.match("^#(([0-9a-fA-F][9a-fA-F][0-9a-fA-F])|((([0-9a-fA-F]{2})[9a-fA-F]([0-9a-fA-F]){3})))$")){
//    if (this.custom.value.match("^#(([9a-fA-F]{3})|(([9a-fA-F][0-9a-fA-F]){3}))$")){
      this.custom.style.backgroundColor = this.custom.value;
      this.custom.style.color = "#000";
    }
    else if (this.custom.value.match("^(([0-9a-fA-F][9a-fA-F][0-9a-fA-F])|((([0-9a-fA-F]{2})[9a-fA-F]([0-9a-fA-F]){3})))$")){
//    else if (this.custom.value.match("^(([9a-fA-F]{3})|(([9a-fA-F][0-9a-fA-F]){3}))$")){
      this.custom.style.backgroundColor = '#' + this.custom.value;
      this.custom.style.color = "#000";
    }
    else if(this.custom.value.match("(^#[0-9a-fA-F]{3}$)|(^#[0-9a-fA-F]{6}$)")){
      this.custom.style.backgroundColor = this.custom.value;
      this.custom.style.color = "#FFF";
    }
    else if(this.custom.value.match("(^[0-9a-fA-F]{3}$)|(^[0-9a-fA-F]{6}$)")){
      this.custom.style.backgroundColor = '#' + this.custom.value;
      this.custom.style.color = "#FFF";
    }
  }

  /*
     Store the previeous color code, which is valid
   */
  this.customColorValueFocus = function(){
    this.storedColorCode = this.custom.value;
  }

  /*
     Validate the color code entered by the user
   */
  this.validateCustomColor = function(){
    if(this.custom.value.match("^#[0-9a-fA-F]{3}$")){
      this.customOption.value = '#' + this.custom.value.charAt(1) + this.custom.value.charAt(1) +
          this.custom.value.charAt(2) + this.custom.value.charAt(2) +
          this.custom.value.charAt(3) + this.custom.value.charAt(3);
    }
    else if(this.custom.value.match("^[0-9a-fA-F]{3}$")){
      this.customOption.value = '#' + this.custom.value.charAt(0) + this.custom.value.charAt(0) +
          this.custom.value.charAt(1) + this.custom.value.charAt(1) +
          this.custom.value.charAt(2) + this.custom.value.charAt(2);
    }
    else if(this.custom.value.match("^[0-9a-fA-F]{6}$")){
      this.customOption.value = '#' + this.custom.value;
    }
    else if(this.custom.value.match("^#[0-9a-fA-F]{6}$")){
      this.customOption.value = this.custom.value;
    }
    else{
      this.custom.value = this.storedColorCode;
      this.showCustomColor();
      return false;
    }
  }
  this.showCustomColor();
}

/*
Data       => Data sources
Type       => Chart type
Titles     => Chart title, Axes names
Axes       => Displayed values on axes
Grid       => Gridline options 
Labels     => Displayed labels (series name, values)
Legend     => Legend position
Space      => Element spacing
Colors     => Color customization
Insert     => Insertion point
*/

/**
 * Client side JavaScript code for the chart creation wizard.
 */
function chwWizard(){
  var skinDirectory;
  var pageOrder = ["Data", "Type", "Titles", "Axes", "Grid", "Labels", "Legend", "Space", "Colors", "Insert"];
  /** The active (selected) wizard page. */
  var activePage;
  /** The selected chart type. */
  var selectedChartType;
  /** The enabled wizard pages. Blocks activation of disabled pages. */
  var enabledPages = {
    Data   : true,
    Type   : false,
    Titles : false,
    Axes   : false,
    Grid   : false,
    Labels : false,
    Legend : false,
    Space  : false,
    Colors : false,
    Insert : false
  }
  var activatedElements = {
    Pie    : {
             Data   : [],
             Type   : ["ChartType"],
             Titles : ["ChartTitle", "ChartSubtitle"]
             },
    Bar    : {
             Data   : [],
             Type   : ["ChartType"],
             Titles : ["ChartTitle", "ChartSubtitle"],
             Axes   : ["XAxis", "YAxis"]
             }
  }
  var selectorObjects = new Object();
  var colorObjects = new Object();
  var backEnabled = false;
  var nextEnabled = true;
  var finishEnabled =false;

  // Used for form element validation
  var storedValue;

  adjustPage = function(show, hide){
    var a = 0, b=0;
    while(a < show.length && b < hide.length){
      if(show[a] < hide[b]){
        document.getElementById("chw" + show[a++] + "Div").className = "chwVisible";
      }
      else if(show[a] > hide[b]){
        document.getElementById("chw" + hide[b++] + "Div").className = "chwHidden";
      }
      else{
        a++; b++;
      }
    }
    while(a < show.length){
      document.getElementById("chw" + show[a++] + "Div").className = "chwVisible";
    }
    while(b < hide.length){
      document.getElementById("chw" + hide[b++] + "Div").className = "chwHidden";
    }
  }

  getPageIndex = function(pageName){
    for(var i = 0; i < pageOrder.length; i++){
      if(pageOrder[i] == pageName) return i;
    }
  }

  getNextPageIndex = function(pageIndex){
    while(!activatedElements[selectedChartType][pageOrder[++pageIndex]]){
      if(pageIndex == pageOrder.length - 1){
        return -1;
      }
    }
    return pageIndex;
  }
  getPrevPageIndex = function(pageIndex){
    while(!activatedElements[selectedChartType][pageOrder[--pageIndex]]){
      if(pageIndex == 0){
        return -1;
      }
    }
    return pageIndex;
  }

  this.initialize = function(theSkinDirectory){
    skinDirectory = theSkinDirectory;
    activePage = pageOrder[0];
    document.getElementById('chw' + activePage + 'Wizard').className = "chwActivePage";
    selectedChartType = document.getElementById("chwChartTypeInput").value;
    for(var wizardPage in activatedElements[selectedChartType]){
      document.getElementById("chw" + wizardPage + "WizardButton").className = "chwNavigationImageDisabled";
      adjustPage(activatedElements[selectedChartType][wizardPage], []);
    }
    document.getElementById("chw" + activePage + "WizardButton").className = "chwNavigationImage";
    selectorObjects.ChartTitlePosition = new chwPositionSelector("ChartTitlePosition", "Position", "Top");
    selectorObjects.ChartTitleAlignment = new chwPositionSelector("ChartTitleAlignment", "Alignment", "Center_Center");
    selectorObjects.ChartSubtitlePosition = new chwPositionSelector("ChartSubtitlePosition", "Position", "Top");
    selectorObjects.ChartSubtitleAlignment = new chwPositionSelector("ChartSubtitleAlignment", "Alignment", "Center_Center");
    colorObjects.ChartTitleColor = new chwColorChooser("ChartTitleColor");
    colorObjects.ChartTitleBackgroundColor = new chwColorChooser("ChartTitleBackgroundColor");
    colorObjects.ChartSubtitleColor = new chwColorChooser("ChartSubtitleColor");
    colorObjects.ChartSubtitleBackgroundColor = new chwColorChooser("ChartSubtitleBackgroundColor");
    if(document.implementation && document.implementation.hasFeature("HTMLEvents", "2.0")){
      this.LMB = 0;
    }
    else{
      this.LMB = 1;
    }
  }

  this.showWizardPage = function(newPage){
    if(activePage == newPage) return;
    if(!enabledPages[newPage]) return;

    // See if this was the first visible page, in order to enable the Back button
    var currentPage = getPageIndex(activePage);
    if(currentPage == 0){
      // Enable the Back button
      document.getElementById("chwBackButton").className = "chwButton";
      backEnabled = true;
    }
    // See if this was the last visible page, in order to enable the Next button
    var nextPage = getNextPageIndex(currentPage);
    if(nextPage == -1){
      // Enable the Next button
      document.getElementById("chwNextButton").className = "chwButton";
      nextEnabled = true;
    }

    document.getElementById("chw" + activePage + "Wizard").className = 'chwInactivePage';
    var button = document.getElementById("chw" + activePage + "WizardButton");
    if(button.src.indexOf("Hover.png") >= 0){
      button.src = skinDirectory + "chwTaskCompletedHover.png";
    }
    else{
      button.src = skinDirectory + "chwTaskCompleted.png";
    }

    activePage = newPage;

    // See if this is the first visible page, in order to disable the Back button
    var currentPage = getPageIndex(activePage);
    if(currentPage == 0){
      // Disable the Back button
      document.getElementById("chwBackButton").className = "chwButtonDisabled";
      backEnabled = false;
    }
    // See if this is the last visible page, in order to disable the Next button
    var nextPage = getNextPageIndex(currentPage);
    if(nextPage == -1){
      // Disable the Next button
      document.getElementById("chwNextButton").className = "chwButtonDisabled";
      nextEnabled = false;
    }

    document.getElementById("chw" + activePage + "Wizard").className = 'chwActivePage';
    button = document.getElementById("chw" + activePage + "WizardButton");
    if(button.src.indexOf("Hover.png") >= 0){
      button.src = skinDirectory + "chwTaskCompletingHover.png";
    }
    else{
      button.src = skinDirectory + "chwTaskCompleting.png";
    }
  }

  this.enterButton = function(elementName){
    if(!enabledPages[elementName]) return false;
    var element = document.getElementById("chw" + elementName + "WizardButton");
    var src = element.src;
    src = src.substring(0, src.indexOf(".png")) + "Hover.png";
    element.src = src;
  }

  this.leaveButton = function(elementName){
    if(!enabledPages[elementName]) return;
    var element = document.getElementById("chw" + elementName + "WizardButton");
    var src = element.src;
    src = src.substring(0, src.indexOf("Hover.png")) + ".png";
    element.src = src;
  }

  this.changeChartType = function(newChartType){
    var dImage = document.getElementById('chwPreviewImg');
    dImage.setAttribute('src', skinDirectory + "/chwSample" + newChartType + "Chart.png");
    dImage.setAttribute('alt', 'Chart Type: ' + newChartType);
    dImage.setAttribute('title', 'Chart Type: ' + newChartType);
    // Hide old pages
    for(var wizardPage in activatedElements[selectedChartType]){
      if(!activatedElements[newChartType][wizardPage]){
        enabledPages[wizardPage] = false;
        adjustPage([], activatedElements[selectedChartType][wizardPage]);
        document.getElementById("chw" + wizardPage + "WizardButton").className = "chwNavigationImageHidden";
      }
    }
    // Show new pages and adjust remaining pages
    for(var wizardPage in activatedElements[newChartType]){
      if(!activatedElements[selectedChartType][wizardPage]){
        // Previously hidden page, show it
        adjustPage(activatedElements[newChartType][wizardPage], []);
      }
      else{
        adjustPage(activatedElements[newChartType][wizardPage], activatedElements[selectedChartType][wizardPage]);
      }
      enabledPages[wizardPage] = false;
      document.getElementById("chw" + wizardPage + "WizardButton").className = "chwNavigationImageDisabled";
      document.getElementById("chw" + wizardPage + "WizardButton").src = skinDirectory + "chwTaskWaiting.png";
    }
    selectedChartType = newChartType;
    var currentPage = getPageIndex(activePage);
    for(var i = 0; i < currentPage; i++){
      if(!activatedElements[selectedChartType][pageOrder[i]]) continue;
      document.getElementById("chw" + pageOrder[i] + "WizardButton").className = "chwNavigationImage";
      document.getElementById("chw" + pageOrder[i] + "WizardButton").src = skinDirectory + "chwTaskCompleted.png";
      enabledPages[pageOrder[i]] = true;
    }
    enabledPages[activePage] = true;
    document.getElementById("chwFinishButton").className = "chwButtonDisabled";
    finishEnabled = false;
    document.getElementById("chw" + activePage + "WizardButton").className = "chwNavigationImage";
    document.getElementById("chw" + activePage + "WizardButton").src = skinDirectory + "chwTaskCompleting.png";
  }

  this.flipAdvanced = function(elementName){
    var legend = document.getElementById('chw' + elementName + 'Legend');
    if(legend.firstChild.nodeValue.indexOf(">>") >= 0){
      legend.firstChild.nodeValue = legend.firstChild.nodeValue.replace(">>", "<<");
      document.getElementById('chw' + elementName + 'Div').className = 'chwVisible';
    }
    else{
      legend.firstChild.nodeValue = legend.firstChild.nodeValue.replace("<<", ">>");
      document.getElementById('chw' + elementName + 'Div').className = 'chwHidden';
    }
  }

  this.flipEnabled = function(elementName){
    if(document.getElementById('chw' + elementName + 'Enabled').checked){
      document.getElementById('chw' + elementName + 'Input').disabled = false;
//      document.getElementById('chw' + elementName + 'ShowAdvanced').className = 'chwExpander';
    }
    else{
      document.getElementById('chw' + elementName + 'Input').disabled = true;
//      document.getElementById('chw' + elementName + 'ShowAdvanced').className = 'chwExpanderHidden';
//      document.getElementById('chw' + elementName + 'HideAdvanced').className = 'chwExpanderHidden';
//      document.getElementById('chw' + elementName + 'AdvancedDiv').className = 'chwHidden';
    }
  }

  this.showNextPage = function(){
    if(!nextEnabled) return false;
    var currentPage = getPageIndex(activePage);
    var nextPage = getNextPageIndex(currentPage);
    if(activePage == "Type"){
      for(var page in activatedElements[selectedChartType]){
        enabledPages[page] = true;
        document.getElementById("chw" + page + "WizardButton").className = "chwNavigationImage";
        document.getElementById("chw" + page + "WizardButton").src = skinDirectory + "chwTaskCompleted.png";
      }
      document.getElementById("chwFinishButton").className = "chwButton";
      finishEnabled = true;
    }
    nextPage = pageOrder[nextPage];
    enabledPages[nextPage] = true;
    document.getElementById("chw" + nextPage + "WizardButton").className = "chwNavigationImage";
    this.showWizardPage(nextPage);
    return false;
  }

  this.showPrevPage = function(){
    if(!backEnabled) return false;
    var currentPage = getPageIndex(activePage);
    var prevPage = getPrevPageIndex(currentPage);
    prevPage = pageOrder[prevPage];
    this.showWizardPage(prevPage);
    return false;
  }

  this.finish = function(){
    if(!finishEnabled) return false;
    document.getElementById('chwForm').submit();
    return false;
  }

  this.storeValue = function(value){
    storedValue = value;
  }

  this.validateNumber = function(element, min, max, precision){
    if(! (Boolean(Number(element.value)) || (Number(element.value) == 0))){
      element.value = storedValue;
      return false;
    }
    else{
      if(precision === undefined){
        precision = 0;
      }
      var value = Number(element.value).toFixed(precision) - 0;
      if(min !== undefined && value < min){
        value = min;
      }
      if(max !== undefined && value > max){
        value = max;
      }
      element.value = value;
    }
    return true;
  }

  this.changeInserts = function(elementName){
    var element = document.getElementById('chw' + elementName + 'Input');
    element.value = 'left:' + document.getElementById('chw' + elementName + 'LeftInput').value + ';';
    element.value += 'top:' + document.getElementById('chw' + elementName + 'TopInput').value + ';';
    element.value += 'right:' + document.getElementById('chw' + elementName + 'RightInput').value + ';';
    element.value += 'bottom:' + document.getElementById('chw' + elementName + 'BottomInput').value;
  }

  this.changeFont = function(elementName){
    var element = document.getElementById('chw' + elementName + 'Input');
    element.value = 'name:' + document.getElementById('chw' + elementName + 'FamilyInput').value + ';';
    element.value += 'style:' + document.getElementById('chw' + elementName + 'StyleInput').value + ';';
    element.value += 'size:' + document.getElementById('chw' + elementName + 'SizeInput').value;
  }

  this.checkTitle = function(titleName){
    if(document.getElementById('chw' + titleName + 'Input').value == ''){
    }
  }

  this.selectorMouseOver = function(selector, object, value){
    selectorObjects[selector].onmouseover(object, value);
  }
  this.selectorMouseOut = function(selector, object, value){
    selectorObjects[selector].onmouseout(object, value);
  }
  this.selectorMouseDown = function(selector, event, value){
    selectorObjects[selector].onmousedown(event, value);
  }
  this.selectorMouseUp = function(selector, event){
    selectorObjects[selector].onmouseup(event);
  }

  this.colorChoiceChanged = function(color){
    colorObjects[color].colorChoiceChanged();
  }
  this.showCustomColor = function(color){
    colorObjects[color].showCustomColor();
  }
  this.validateCustomColor = function(color){
    colorObjects[color].validateCustomColor();
  }
  this.customColorValueFocus = function(color){
    colorObjects[color].customColorValueFocus();
  }
}

window.wizard = new chwWizard();
