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
      this.customGroup.style.display="inline";
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
    var selectedFirst = this.element.selectedIndex;
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
    this.element.selectedIndex = selectedFirst;
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
  /** The order of the wizard pages */
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
    Bar    : {
             Data   : [],
             Type   : ["ChartType"],
             Titles : ["ChartTitle", "ChartSubtitle"]
/* not yet
,             Axes   : ["XAxis", "YAxis"] */
             },
    Pie    : {
             Data   : [],
             Type   : ["ChartType"],
             Titles : ["ChartTitle", "ChartSubtitle"]
             }
/* these don't work yet
,    Line    : {
             Data   : [],
             Type   : ["ChartType"],
             Titles : ["ChartTitle", "ChartSubtitle"],
             Axes   : ["XAxis", "YAxis"]
             },
    Area    : {
             Data   : [],
             Type   : ["ChartType"],
             Titles : ["ChartTitle", "ChartSubtitle"],
             Axes   : ["XAxis", "YAxis"]
             },
    Time    : {
             Data   : [],
             Type   : ["ChartType"],
             Titles : ["ChartTitle", "ChartSubtitle"],
             Axes   : ["XAxis", "YAxis"]
             }*/
  }
  var selectorObjects = new Object();
  var colorObjects = new Object();
  /** The Next button */
  var nextBtn;
  /** The Back button */
  var backBtn;
  /** The Finish button */
  var finishBtn;
  var backEnabled = false;
  var nextEnabled = false;
  var finishEnabled =false;
  var hasDefinedSource = false;
  var hasReusedSource = true;

  // Used for form element validation
  var storedValue;
  // Locale dependent values for the "show" and "hide" terms
  var showWord, hideWord;

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
  enableBack = function(){
    backBtn.className = 'chwButton';
    backEnabled = true;
  }
  disableBack = function(){
    backBtn.className = 'chwButtonDisabled';
    backEnabled = false;
  }
  enableNext = function(){
    nextBtn.className = 'chwButton';
    nextEnabled = true;
  }
  disableNext = function(){
    nextBtn.className = 'chwButtonDisabled';
    nextEnabled = false;
  }
  enableFinish = function(){
    finishBtn.className = 'chwButton';
    finishEnabled = true;
  }
  disableFinish = function(){
    finishBtn.className = 'chwButtonDisabled';
    finishEnabled = false;
  }

  this.initialize = function(theSkinDirectory, theShowWord, theHideWord){
    skinDirectory = theSkinDirectory;
    showWord = theShowWord;
    hideWord = theHideWord;
    backBtn    = document.getElementById("chwBackButton");
    nextBtn    = document.getElementById("chwNextButton");
    finishBtn  = document.getElementById("chwFinishButton");
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

    /*
       Create color picker...
     */
    window.colorPicker = new ColorPicker(
                         document.getElementById("chwColorpickerHSMap"),
                         document.getElementById("chwColorpickerLMap"),
                         document.getElementById("chwColorpickerLPointer"),
                         document.getElementById("chwColorPickerShow"),
                         document.getElementById("chwColorCodeDisplay"));
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

  /** Highlight the navigation button when the mouse moves over it */
  this.enterButton = function(elementName){
    if(!enabledPages[elementName]) return false;
    var element = document.getElementById("chw" + elementName + "WizardButton");
    var src = element.src;
    src = src.substring(0, src.indexOf(".png")) + "Hover.png";
    element.src = src;
  }

  /** Dehighlight the navigation button when the mouse moves out of it */
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
    document.getElementById('chw' + selectedChartType + 'Subtypes').className = 'chwHidden';
    document.getElementById('chw' + selectedChartType + 'SubtypeInput').disabled = true;
    selectedChartType = newChartType;
    document.getElementById('chw' + selectedChartType + 'Subtypes').className = 'chwVisible';
    document.getElementById('chw' + selectedChartType + 'SubtypeInput').disabled = false;
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

  /*
     Change chart subtype
   */
  this.changeChartSubtype = function(subtype){
  // TODO: Write me
  }

  /*
     Show or hide the contents of a fieldset when the user clicks the legend
   */
  this.flipAdvanced = function(elementName){
    var legend = document.getElementById('chw' + elementName + 'Legend');
    if(legend.firstChild.nodeValue.indexOf(">>") >= 0){
      legend.firstChild.nodeValue = legend.firstChild.nodeValue.replace(">>", "<<");
      legend.title = legend.title.replace(showWord, hideWord);
      document.getElementById('chw' + elementName + 'Div').className = 'chwVisible';
    }
    else{
      legend.firstChild.nodeValue = legend.firstChild.nodeValue.replace("<<", ">>");
      legend.title = legend.title.replace(hideWord, showWord);
      document.getElementById('chw' + elementName + 'Div').className = 'chwHidden';
    }
  }

  /*
     Change the source type (Define or Reuse)
   */
  this.changeSourceType = function(type){
    switch(type){
      case 'Reuse':
        document.getElementById('chwDataDefineDiv').className = 'chwHidden';
        document.getElementById('chwDataReuseDiv').className = 'chwVisible';
        if(hasReusedSource){
          enableNext();
        }
        else{
          disableNext();
        }
        break;
      case 'Define':
        document.getElementById('chwDataDefineDiv').className = 'chwVisible';
        document.getElementById('chwDataReuseDiv').className = 'chwHidden';
        if(hasDefinedSource){
          enableNext();
        }
        else{
          disableNext();
        }
        break;
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

  this.setValidDatasource = function(dataString){
    document.getElementById('chwDataSourceInput').value = dataString;
    document.getElementById('chwDefineHasDatasource').className = 'chwNotice';
    enableNext();
    hasDefinedSource = true;
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


/*
   The color picker class
   Allows choosing a custom color, using, visually,
   the hue, saturation and luminance values
 */
function ColorPicker(hsmap, lmap, lpointer, colorShower, codeDisplay){
  /*
     The image element representing the hue X saturation color map
   */
  this.hsmap       = hsmap;
  /*
     The image element representing the luminance map
   */
  this.lmap        = lmap;
     if (lpointer != null) {
         /*
           The "arrow" that points to the apropriate luminance level
         */
         this.lpointer    = lpointer;
         /*
           The table cell that will contain the
           "arrow" that points to the apropriate luminance level
         */

         this.lpointerContainer = lpointer.parentNode;

     }
  /*
     The div element that will get the chosen color
   */
  this.colorShower = colorShower;
  /*
     The input element that will display the chosen color's code
   */
  this.codeDisplay = codeDisplay;


  this.container = document.getElementById('chwColorPicker');
  this.fieldset  = document.getElementById('chwColorPickerFieldset');
  this.hueComponent = document.getElementById('chwColorpickerHue');
  this.satComponent = document.getElementById('chwColorpickerSaturation');
  this.lumComponent = document.getElementById('chwColorpickerLightness');
  this.redComponent = document.getElementById('chwColorpickerRed');
  this.greenComponent = document.getElementById('chwColorpickerGreen');
  this.blueComponent = document.getElementById('chwColorpickerBlue');

  /*
     The stored HSL values
   */
  this.hue = 159;
  this.sat = 85;
  this.lum = 120;
  /*
     The stored RGB values
   */
  this.red   = 80;
  this.green = 100;
  this.blue  = 160;

  /*
     The stored color code
   */
  this.storedValue = "";

  /*
     The code of the left mouse button
     Browser dependent
   */
  this.LMB = (window.ActiveXObject ? 1 : 0);

  /*
      The element that invoked the color picker,
      which will recieve the picked color code
   */
  this.reqester = null;

  /*
      Positioning data
   */
  this.left = 120;
  this.top = 120;

  /*
     Compute the minimum of 3 numbers
   */
  this.min3 = function(a, b, c){
    if(a <= b && a <= c) return a;
    if(b <= a && b <= c) return b;
    return c;
  }
  /*
     Compute the maximum of 3 numbers
   */
  this.max3 = function(a, b, c){
    if(a >= b && a >= c) return a;
    if(b >= a && b >= c) return b;
    return c;
  }

  /*
     Checks if a string is a valid #xxxxxx code
   */
  this.validCode = function(code){
    if(!code.match("^#[0-9a-fA-F]{6}$")) return false;
    return true;
  }

  /*
     HSL to RGB conversion
     Allow obtaining the actual #rrggbb code from the
     h, s, l values
   */
  this.hsl2rgb = function (hue,sat,lum) {
    var _val, _max, _min, _part, _half, _hi, _lo, _mid;
    var _r, _g, _b;
    _val = (lum / 240) * 255;
    if (lum >= 120) {
      _max = 255;
      _min = _val - (255 - _val);
    } else if(lum < 120) {
      _min = 0;
      _max = _val * 2;
    }
    _part = sat / 240;
    _half = (_max - _min) / 2;
    _hi = _half * _part;
    _lo = _half - _hi;
    if (sat == 0 || _max == _min) {
      _r = _val;
      _g = _val;
      _b = _val;
    } else if (hue < 40) {
      _r = _max - _lo;
      _b = _min + _lo;
      _mid = hue / 40;
      _g = ((_r - _b) * _mid) + _b;
    } else if (hue >= 200 && hue <= 240) {
      _r = _max - _lo;
      _g = _min + _lo;
      _mid = (240 - hue) / 40;
      _b = ((_r - _g) * _mid) + _g;
    } else if (hue >= 80 && hue < 120) {
      _g = _max - _lo;
      _r = _min + _lo;
      _mid = (hue - 80) / 40;
      _b = ((_g - _r) * _mid) + _r;
    } else if (hue >= 40 && hue < 80) {
      _g = _max - _lo;
      _b = _min + _lo;
      _mid = (80 - hue) / 40;
      _r = ((_g - _b) * _mid) + _b;
    } else if (hue >= 160 && hue < 200) {
      _b = _max - _lo;
      _g = _min + _lo;
      _mid = (hue - 160) / 40;
      _r = ((_b - _g) * _mid) + _g;
    } else if (hue >= 120 && hue < 160) {
      _b = _max - _lo;
      _r = _min + _lo;
      _mid = (160 - hue) / 40;
      _g = ((_b - _r) * _mid) + _r;
    }
    red = Math.round(_r);
    green = Math.round(_g);
    blue = Math.round(_b);
    return {red : red, green : green, blue : blue};
  }

  this.hsl2code = function(H, S, L){
    var rgb = this.hsl2rgb(H, S, L);
    return this.rgb2code(rgb.red, rgb.green, rgb.blue);
  }

  /*
     RGB to HSL conversion
     Allow obtaining the h, s, l values from the
     r, g, b values
   */
  this.rgb2hsl = function(red, green, blue){
    var R = red / 255;
    var G = green / 255;
    var B = blue / 255;

    var min = this.min3(R, G, B);
    var max = this.max3(R, G, B);
    var delta = max - min;

    var L = (max + min) / 2;
    var H, S;

    if (delta == 0){
      H = 0;
      S = 0;
    }
    else{
      if (L < 0.5) S = delta / (max + min);
      else S = delta / (2 - (max + min));
      var deltaR = (((max - R) / 6) + (max / 2)) / delta;
      var deltaG = (((max - G) / 6) + (max / 2)) / delta;
      var deltaB = (((max - B) / 6) + (max / 2)) / delta;

      if      ( R == max ) H = deltaB - deltaG
      else if ( G == max ) H = (1 / 3) + deltaR - deltaB
      else if ( B == max ) H = (2 / 3) + deltaG - deltaR

      if ( H < 0 ) H += 1
      if ( H > 1 ) H -= 1
    }
    return {hue : Math.round(H * 240), sat : Math.round(S * 240), lum : Math.round(L * 240)};
  }

  this.setValues = function(R, G, B, H, S, L){
    this.red = R;
    this.green = G;
    this.blue = B;
    this.hue = H;
    this.sat = S;
    this.lum = L;
  }

  this.setComponents = function(R, G, B, H, S, L, code){
    this.redComponent.value = R;
    this.greenComponent.value = G;
    this.blueComponent.value = B;
    this.hueComponent.value = H;
    this.satComponent.value = S;
    this.lumComponent.value = L;
    this.colorShower.style.backgroundColor = code;
    this.codeDisplay.value = code;
    this.lpointerContainer.style.backgroundPosition = "0 " + (240 - L) + "px";
    this.lmap.style.backgroundColor = this.hsl2code(H, S, 120);
    this.storedValue = code;
  }

  /*
     RGB to #xxxxxx conversion
     Join the RGB values into a #xxxxxx string
   */
  this.rgb2code = function(red, green, blue){
    red = Math.round(red).toString(16);
    if (red.length == 1) red = "0" + red;
    green = Math.round(green).toString(16);
    if(green.length == 1) green = "0" + green;
    blue = Math.round(blue).toString(16);
    if(blue.length == 1) blue ="0" + blue;
    return "#" + red + green + blue;
  }
  /*
     Parse a #xxxxxx code into rgb values
   */
  this.code2rgb = function(code){
    if(!this.validCode(code)) return false;
    var red = Number("0x" + code.substring(1, 3));
    var green = Number("0x" + code.substring(3, 5));
    var blue = Number("0x" + code.substring(5));
    return {red : red, green : green, blue : blue};
  }
  /*
     Obtain the coordinates of the hot point in the target
     Browser dependent
   */
  this.getTargetEltX = function(event, elt){
    var x;
    if (event.offsetX !== undefined) x = event.offsetX;
    else {
      x = event.screenX - document.getBoxObjectFor(elt).screenX;
    }
    if(x < 0) x = 0;
    if(x > 240) x = 240;
    return x;
  }

  this.getTargetEltY = function(event, elt){
    var y;
    if (event.offsetY !== undefined) y = event.offsetY;
    else {
      y = event.screenY - document.getBoxObjectFor(elt).screenY;
    }
    if(y < 0) y = 0;
    if(y > 240) y = 240;
    return y;
  }

  /*
     The user "picked" new values for h and s from the hsmap.
     The color is updated.
   */
  this.hsChanged = function(event, elt){
    var hue = this.getTargetEltX(event, elt) - 0;
    if(hue > 240) hue = 240;
    if(hue < 0) hue = 0;
    var sat = 240 - this.getTargetEltY(event, elt);
    if(sat > 240) sat = 240;
    if(sat < 0) sat = 0;
    var rgb = this.hsl2rgb(hue, sat, this.lum);
    var colorcode = this.rgb2code(rgb.red, rgb.green, rgb.blue);
    if(!this.validCode(colorcode)) return;
    this.setValues(rgb.red, rgb.green, rgb.blue, hue, sat, this.lum);
    this.setComponents(rgb.red, rgb.green, rgb.blue, hue, sat, this.lum, colorcode);
  }

  /*
     The user "picked" a new value for l from the lmap.
     The color is updated and the lpointer is moved to the apropriate
     position.
   */
  this.lChanged = function(event, elt){
    var lum = 240 - (this.getTargetEltY(event, elt));
    if(lum > 240) lum = 240;
    if(lum < 0) lum = 0;
    var rgb = this.hsl2rgb(this.hue, this.sat, lum);
    var colorcode = this.rgb2code(rgb.red, rgb.green, rgb.blue);
    if(!this.validCode(colorcode)) return;
    this.setValues(rgb.red, rgb.green, rgb.blue, this.hue, this.sat, lum);
    this.setComponents(this.red, this.green, this.blue, this.hue, this.sat, this.lum, colorcode);
  }
  /*
     The user entered a custom color code in the
     code display input.
     Update the colorShow and the luminance
     pointer.
   */
  this.htmlCodeChanged = function(){
    var colorcode = this.codeDisplay.value;
    if(colorcode.charAt(0) != '#') colorcode = '#' + colorcode;
    if(!this.validCode(colorcode)){
      return;
    }
    var rgb = this.code2rgb(colorcode);
    var hsl = this.rgb2hsl(rgb.red, rgb.green, rgb.blue);
    this.setValues(rgb.red, rgb.green, rgb.blue, hsl.hue, hsl.sat, hsl.lum);
    this.setComponents(this.red, this.green, this.blue, this.hue, this.sat, this.lum, colorcode);
  }
  this.rgbCodeChanged = function(){
    var red = this.redComponent.value - 0;
    if(red > 255) red = 255;
    if(red < 0) red = 0;
    var green = this.greenComponent.value - 0;
    if(green > 255) green = 255;
    if(green < 0) green = 0;
    var blue = this.blueComponent.value - 0;
    if(blue > 255) blue = 255;
    if(blue < 0) blue = 0;
    if(red != red || green != green || blue != blue){
      this.setComponents(this.red, this.green, this.blue, this.hue, this.sat, this.lum, this.storedValue);
      return;
    }
    var colorcode = this.rgb2code(red, green, blue);
    if(!this.validCode(colorcode)){
      this.setComponents(this.red, this.green, this.blue, this.hue, this.sat, this.lum, this.storedValue);
      return;
    }
    var hsl = this.rgb2hsl(red, green, blue);
    this.setValues(red, green, blue, hsl.hue, hsl.sat, hsl.lum);
    this.setComponents(this.red, this.green, this.blue, this.hue, this.sat, this.lum, colorcode);
  }
  this.hslCodeChanged = function(){
    var hue = this.hueComponent.value - 0;
    if(hue > 240) hue = 240;
    if(hue < 0) hue = 0;
    var sat = this.satComponent.value - 0;
    if(sat > 240) sat = 240;
    if(sat < 0) sat = 0;
    var lum = this.lumComponent.value - 0;
    if(lum > 240) lum = 240;
    if(lum < 0) lum = 0;
    if(hue != hue || sat != sat || lum != lum){
      this.setComponents(this.red, this.green, this.blue, this.hue, this.sat, this.lum, this.storedValue);
      return;
    }
    var colorcode = this.hsl2code(hue, sat, lum);
    if(!this.validCode(colorcode)){
      this.setComponents(this.red, this.green, this.blue, this.hue, this.sat, this.lum, this.storedValue);
      return;
    }
    var rgb = this.hsl2rgb(hue, sat, lum);
    this.setValues(rgb.red, rgb.green, rgb.blue, hue, sat, lum);
    this.setComponents(this.red, this.green, this.blue, this.hue, this.sat, this.lum, colorcode);
  }

  /*
     The user chose a new color characteristic from one of the maps.
     The information is updated.
   */
  this.colorPicked = function(event, elt){
    if (elt == this.hsmap){
      this.hsChanged(event, elt);
    }
    else if (elt == this.lmap || elt == this.lpointer){
      this.lChanged(event, elt);
    }
  }

  /*
     Left mouse button pressed.
     Entering "picking mode"...
   */
  this.mouseDown = function(event, elt){
    if (event.button != this.LMB) return true;
    else {
      this.picking = true;
      this.crtPickingTarget = elt;
      this.colorPicked(event, elt);
      try{
        event.preventDefault();
      }
      catch(e){
        event.returnValue = false;
      }
      return false;
    }
  }
  /*
     Mouse moving over the picker's maps.
     If in picking mode, pick the coresponding color.
     Else do nothing.
   */
  this.mouseMove = function(event, elt){
    if (this.picking == true && this.crtPickingTarget == elt){
      if(window.ActiveXObject){
        if(event.srcElement == this.crtPickingTarget){
          this.colorPicked(event, elt);
        }
      }
      else{
        this.colorPicked(event, elt);
      }
    }

    event.returnValue = false;
  }

  /*
     Left mouse button released.
     Exiting "picking mode".
   */
  this.mouseUp = function(event, elt){
    this.picking = false;
    this.crtPickingTarget = null;
  }

  this.filterKeys = function(event){
  }

  this.show = function(requester, color){
    if(color === undefined){
      color = requester.value;
    }
    if (!color.match("(#[0-9a-fA-F]{6})")){
     // alert("nu-i bun!");
     color="#dddddd";
    }

    this.requester = requester;
    this.storedValue = color;
    var rgb = this.code2rgb(color);
    var hsl = this.rgb2hsl(rgb.red, rgb.green, rgb.blue);
    this.setValues(rgb.red, rgb.green, rgb.blue, hsl.hue, hsl.sat, hsl.lum);
    this.setComponents(this.red, this.green, this.blue, this.hue, this.sat, this.lum, color);
    this.container.style.display = 'block';
    if(window.ActiveXObject){
      // this is an ugly way of avoiding an Internet Explorer 'feature'
      //  that puts select elements on top of everything else,
      //  completely ignoring the z-index.
      var selects = document.getElementsByTagName('select');
      for(var i = 0; i < selects.length; i++){
        selects.item(i).style.visibility = 'hidden';
      }
    }
    if(document.width && document.getBoxObjectFor){
      this.fieldset.style.left = this.max3(document.width / 2 - document.getBoxObjectFor(this.fieldset).width / 2, 0, 0) + 'px';
    }
    else if(document.body.clientWidth){
      this.fieldset.style.left = this.max3(document.body.clientWidth / 2 - this.fieldset.clientWidth / 2, 0, 0) + 'px';
    }
  }

  this.OK = function(event){
    if(window.ActiveXObject){
      // this is an ugly way of avoiding an Internet Explorer 'feature'
      //  that puts select elements on top of everything else,
      //  completely ignoring the z-index.
      var selects = document.getElementsByTagName('select');
      for(var i = 0; i < selects.length; i++){
        selects.item(i).style.visibility = 'visible';
      }
    }
    this.requester.value = this.storedValue;
    this.container.style.display = 'none';
    this.requester.focus();
    eval(this.requester.getAttribute('onfocus'));
  }
  this.Cancel = function(event){
    if(window.ActiveXObject){
      // this is an ugly way of avoiding an Internet Explorer 'feature'
      //  that puts select elements on top of everything else,
      //  completely ignoring the z-index.
      var selects = document.getElementsByTagName('select');
      for(var i = 0; i < selects.length; i++){
        selects.item(i).style.visibility = 'visible';
      }
    }
    this.container.style.display = 'none';
    this.requester.focus();
  }
}

function createColorPicker(){
window.colorPicker = new ColorPicker(
                         document.getElementById("chwColorpickerHSMap"),
                         document.getElementById("chwColorpickerLMap"),
                         document.getElementById("chwColorpickerLPointer"),
                         document.getElementById("chwColorPickerShow"),
                         document.getElementById("chwColorCodeDisplay"));
}