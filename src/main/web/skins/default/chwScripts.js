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
             Titles : ["ChartTitle"]
             },
    Bar    : {
             Data   : [],
             Type   : ["ChartType"],
             Titles : ["ChartTitle", "XAxisTitle", "YAxisTitle"],
             Axes   : ["XAxis", "YAxis"]
             }
  }
  var backEnabled = false;
  var nextEnabled = true;
  var finishEnabled =false;

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
    selectedChartType = document.getElementById("chwChartTypeInput").value;
    for(var wizardPage in activatedElements[selectedChartType]){
      document.getElementById("chw" + wizardPage + "WizardButton").className = "chwNavigationImageDisabled";
      adjustPage(activatedElements[selectedChartType][wizardPage], []);
    }
    document.getElementById("chw" + activePage + "WizardButton").className = "chwNavigationImage";
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
//        visiblePages[wizardPage] = false;
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
}

window.wizard = new chwWizard();
