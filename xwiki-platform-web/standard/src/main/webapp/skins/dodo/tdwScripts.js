/*
  Created: 2005-08-22
  
  The Table Data Source Wizarsd Class
  is responsible with the visual selection
  of the chart data source
*/
function tdwWizard(){
  /*
    The 'self' data member, pointing to 'this' is necessary because
    'this' does not point to the right object in private methods
  */
  var self = this;

  /*
    The curent state of the selection process
    Possible values: none, cells, rows, columns
  */
  var selectionState = 'none';
  
  /*
    The type of the selection process
    Possible values: none, cells, rows, columns, table
  */
  var selectionType  = 'none';

  // The selection bounds
  var startRowIndex, endRowIndex, 
    startColumnIndex, endColumnIndex;
  // The table in which the selection is made
  var table;
  // The XWiki Document
  var doc;
  var hasValidSelection = false;
  /*
     The 'div' element where all available tables that 
     can be used as data sources are inserted
   */
  var container;
  /** The "Please wait" message */
  var waitingMsg;
  /** The "Error requesting page" message */
  var requestErrorMsg;
  /** The "No tables found" message */
  var notablesMsg;
  /** The "Select range" message */
  var selectMsg;
  /** The Next button */
  var nextBtn;
  /** The Back button */
  var backBtn;
  /** The Finish button */
  var finishBtn;
  /** The prefix address of the table fetch page */
  var baseAddress;
  /** The prefix address of the skin files */
  var skinDirectory;
  /** The address of the page where the definition object should be sent */
  var saveAddress;
  /** The new object number */
  var objectNumber;
  /** The order of the wizard pages */
  var pageOrder = ["Doc", "Range", "Extra"];
  /** The active (selected) wizard page. */
  var activePage;
  /** The enabled wizard pages. Blocks activation of disabled pages. */
  var enabledPages = {
    Doc    : true,
    Range  : false,
    Extra  : false
  }
  var backEnabled = false;
  var nextEnabled = true;
  var finishEnabled = false;

  /*
    Wizard initialization:
    - adds global event listeners
   */
  this.initialize = function(address, directory, saveAddr){
    container   = document.getElementById("tdwTables");
    waitingMsg  = document.getElementById("tdwWaiting");
    requestErrorMsg = document.getElementById("tdwRequestError");
    notablesMsg = document.getElementById("tdwNoTables");
    selectMsg   = document.getElementById("tdwSelectRange");
    backBtn     = document.getElementById("tdwBackButton");
    nextBtn     = document.getElementById("tdwNextButton");
    finishBtn   = document.getElementById("tdwFinishButton");
    baseAddress = address;
    skinDirectory = directory;
    saveAddress = saveAddr;
    if(document.documentElement.addEventListener){
      document.documentElement.addEventListener('mouseup', onMouseUp, true);
    }
    else{
      document.documentElement.attachEvent('onmouseup', onMouseUpIE);
      document.documentElement.attachEvent('onselectstart', onSelectStartIE);
      document.documentElement.attachEvent('onselect', onSelectIE);
    }
    activePage = pageOrder[0];
    document.getElementById('tdw' + activePage + 'Wizard').className = "tdwActivePage";
    document.getElementById("tdw" + activePage + "WizardButton").className = "tdwNavigationImage";
    if(document.getElementById('tdwPageInput').selectedIndex == -1){
      disableNext();
    }
  }

  getPageIndex = function(pageName){
    for(var i = 0; i < pageOrder.length; i++){
      if(pageOrder[i] == pageName) return i;
    }
  }

  getNextPageIndex = function(pageIndex){
    if(pageIndex >= pageOrder.length - 1){
      return -1;
    }
    return pageIndex + 1;
  }
  getPrevPageIndex = function(pageIndex){
    return pageIndex - 1;
  }
  
  enableBack = function(){
    backBtn.className = 'tdwButton';
    backEnabled = true;
  }
  disableBack = function(){
    backBtn.className = 'tdwButtonDisabled';
    backEnabled = false;
  }
  enableNext = function(){
    nextBtn.className = 'tdwButton';
    nextEnabled = true;
  }
  disableNext = function(){
    nextBtn.className = 'tdwButtonDisabled';
    nextEnabled = false;
  }
  enableFinish = function(){
    finishBtn.className = 'tdwButton';
    finishEnabled = true;
  }
  disableFinish = function(){
    finishBtn.className = 'tdwButtonDisabled';
    finishEnabled = false;
  }
  enablePage = function(page){
    enabledPages[page] = true;
    var button = document.getElementById("tdw" + page + "WizardButton");
    button.className = "tdwNavigationImage";
    if(button.src.indexOf("Hover.png") >= 0){
      button.src = skinDirectory + "chwTaskCompletedHover.png";
    }
    else{
      button.src = skinDirectory + "chwTaskCompleted.png";
    }
  }
  disablePage = function(page){
    enabledPages[page] = false;
    var button = document.getElementById("tdw" + page + "WizardButton");
    button.className = "tdwNavigationImageDisabled";
    button.src = skinDirectory + "chwTaskWaiting.png";
  }

  this.showWizardPage = function(newPage){
    if(activePage == newPage) return;
    if(!enabledPages[newPage]) return;

    // See if this was the first visible page, in order to enable the Back button
    var currentPage = getPageIndex(activePage);
    if(currentPage == 0){
      // Enable the Back button
      document.getElementById("tdwBackButton").className = "tdwButton";
      backEnabled = true;
    }
    // See if this was the last visible page, in order to enable the Next button
    var nextPage = getNextPageIndex(currentPage);
    if(nextPage == -1){
      // Enable the Next button
      document.getElementById("tdwNextButton").className = "tdwButton";
      nextEnabled = true;
    }

    // Hide the previous page
    document.getElementById("tdw" + activePage + "Wizard").className = 'tdwInactivePage';
    var button = document.getElementById("tdw" + activePage + "WizardButton");
    if(button.src.indexOf("Hover.png") >= 0){
      button.src = skinDirectory + "chwTaskCompletedHover.png";
    }
    else{
      button.src = skinDirectory + "chwTaskCompleted.png";
    }

    activePage = newPage;
    if(activePage == 'Range' && !hasValidSelection){
      disableNext();
    }

    // See if this is the first visible page, in order to disable the Back button
    var currentPage = getPageIndex(activePage);
    if(currentPage == 0){
      // Disable the Back button
      document.getElementById("tdwBackButton").className = "tdwButtonDisabled";
      backEnabled = false;
      enableNext();
    }
    // See if this is the last visible page, in order to disable the Next button
    var nextPage = getNextPageIndex(currentPage);
    if(nextPage == -1){
      // Disable the Next button
      document.getElementById("tdwNextButton").className = "tdwButtonDisabled";
      nextEnabled = false;
    }

    // Show the selected page
    document.getElementById("tdw" + activePage + "Wizard").className = 'tdwActivePage';
    button = document.getElementById("tdw" + activePage + "WizardButton");
    if(button.src.indexOf("Hover.png") >= 0){
      button.src = skinDirectory + "chwTaskCompletingHover.png";
    }
    else{
      button.src = skinDirectory + "chwTaskCompleting.png";
    }
  }

  /** Show the next wizard page (if the current page is not the last) */
  this.showNextPage = function(){
    if(!nextEnabled) return false;
    var currentPage = getPageIndex(activePage);
    if(currentPage == 0){
      this.getTablesFromPage(document.getElementById('tdwPageInput').value);
    }
    var nextPage = getNextPageIndex(currentPage);
    nextPage = pageOrder[nextPage];
    enablePage(nextPage);
    this.showWizardPage(nextPage);
    return false;
  }

  /** Show the previous wizard page (if the current page is not the first) */
  this.showPrevPage = function(){
    if(!backEnabled) return false;
    enableNext();
    var currentPage = getPageIndex(activePage);
    var prevPage = getPrevPageIndex(currentPage);
    prevPage = pageOrder[prevPage];
    this.showWizardPage(prevPage);
    return false;
  }

  this.change = function(){
    disablePage('Range');
    disablePage('Extra');
    disableFinish();
    enableNext();
  }

  this.flipVisible = function(elementName, visible){
    var element = document.getElementById('tdw' + elementName + 'Div');
    if(visible){
      element.className = 'tdwVisible';
    }
    else{
      element.className = 'tdwHidden';
    }
  }

  var getRange = function(){
    switch(selectionType){
      case 'table':
        return '*';
      case 'columns':
        return String.fromCharCode(64 + startColumnIndex) + '-' + String.fromCharCode(64 + endColumnIndex);
      case 'rows':
        return startRowIndex + '-' + endRowIndex;
      case 'cells':
        return String.fromCharCode(64 + startColumnIndex) + startRowIndex + '-' + String.fromCharCode(64 + endColumnIndex) + endRowIndex;
    }
    return '*';
  }

  this.finish = function(){
    if(!finishEnabled) return false;
    if(!window.opener) return false;
    if(document.getElementById('tdwSaveInput').checked){
      if(document.getElementById('tdwSaveNameInput').value == ""){
        if(!confirm(document.getElementById('tdwSaveNoNameMsg').firstChild.nodeValue)) return false;
      }
      if(saveObject()){
        window.opener.wizard.setValidDatasource('type:object;doc:' + doc + ';class:TableDataSource;object_number:' + objectNumber);
      }
      else{
        return false;
      }
    }
    else{
      window.opener.wizard.setValidDatasource('type:table;doc:' + doc + ';table_number:' + table + ';range:' + getRange() + ';has_header_row:' + document.getElementById('tdwRowHeaderInput').checked + ';has_header_column:' + document.getElementById('tdwColumnHeaderInput').checked + ';ignore_alpha:' + document.getElementById('tdwIgnoreAlphaInput').checked + ';decimal_symbol:' + (document.getElementById('tdwDecimalSymbolInput').checked ? 'comma' : 'period'));
    }
    window.close();
    return false;
  }

  /** Highlight the navigation button when the mouse moves over it */
  this.enterButton = function(elementName){
    if(!enabledPages[elementName]) return false;
    var element = document.getElementById("tdw" + elementName + "WizardButton");
    var src = element.src;
    if(src.indexOf('Hover.png') >= 0) return;
    src = src.substring(0, src.indexOf(".png")) + "Hover.png";
    element.src = src;
  }

  /** Dehighlight the navigation button when the mouse moves out of it */
  this.leaveButton = function(elementName){
    if(!enabledPages[elementName]) return;
    var element = document.getElementById("tdw" + elementName + "WizardButton");
    var src = element.src;
    if(src.indexOf('Hover.png') < 0) return;
    src = src.substring(0, src.indexOf("Hover.png")) + ".png";
    element.src = src;
  }

  // Obtain the table, row and column number from a table cell's id
  var parseId = function(id){
    var table  = getTableIndex(id);
    var row    = id.substring(id.indexOf("R") + 1, id.indexOf("C")) - 0;
    var column = id.substring(id.indexOf("C") + 1) - 0;
    return [table, row, column];
  }
  // Obtain the table index from a table cell's id
  var getTableIndex = function(id){
    return id.substring(id.indexOf("T") + 1, id.indexOf("R")) - 0;
  }
  // Obtain the row index from a table cell's id
  var getRowIndex = function(id){
    return id.substring(id.indexOf("R") + 1, id.indexOf("C")) - 0;
  }
  // Obtain the column index from a table cell's id
  var getColumnIndex = function(id){
    return id.substring(id.indexOf("C") + 1) - 0;
  }
  
  /*
     This method should belong to the Math class
     Returns:  1      if the number is positive
               0      if the number is 0
              -1      if the number is negative
               number otherwise
   */
  var signum = function (number){
    if (number < 0) return -1;
    else if (number > 0) return 1;
    else return number;
  }
  // Remove previous selection when another selection is starting
  var unselect = function(){
    switch(selectionType){
      case 'none': break;
      case 'table':
        document.getElementById('T' + table).className = 'tdwUnselectedTable';
        break;
      case 'rows':
        for(var i = startRowIndex; i <= endRowIndex; i++){
          document.getElementById('T' + table + 'R' + i).className = 'tdwUnselectedRow';
        }
        break;
      case 'columns':
        for(var i = startColumnIndex; i <= endColumnIndex; i++){
          document.getElementById('T' + table + 'C' + i).className = 'tdwUnselectedColumn';
          document.getElementById('T' + table + 'R0C' + i).className = 'tdwUnselectedCell';
        }
        break;
      case 'cells':
        for(var i = startRowIndex; i <= endRowIndex; i++){
          for(var j = startColumnIndex; j <= endColumnIndex; j++){
            document.getElementById('T' + table + 'R' + i + 'C' + j).className = 'tdwUnselectedCell';
          }
        }
        break;
    }
  }

  /*
     Left button pressed on a cell:
     - remove the previous selection
     - start the new selection
  */
  var onMouseDownCommon = function(id){
    unselect();
    hasValidSelection = false;
    selectionState = 'none';
    var tidx = getTableIndex(id);
    var ridx = getRowIndex(id);
    var cidx = getColumnIndex(id);
    if(ridx == 0){
      if(cidx == 0){
        // The whole table is selected
        document.getElementById('T' + tidx).className = 'tdwSelectedTable';
        selectionType = 'table';
        enableNext();
        enableFinish();
        hasValidSelection = true;
      }
      else{
        // A whole column is selected
        document.getElementById('T' + tidx + 'C' + cidx).className = 'tdwSelectedColumn';
        document.getElementById('T' + tidx + 'R0C' + cidx).className = 'tdwSelectedCell';
        selectionState = 'columns';
        selectionType  = 'columns';
      }
    }
    else if(cidx == 0){
      // A whole row is selected
      document.getElementById('T' + tidx + 'R' + ridx).className = 'tdwSelectedRow';
      selectionState = 'rows';
      selectionType  = 'rows';
    }
    else{
      // Cellblock selection
      document.getElementById('T' + tidx + 'R' + ridx + 'C' + cidx).className = 'tdwSelectedCell';
      selectionState = 'cells'
      selectionType  = 'cells';;
    }
    table = tidx;
    startRowIndex = ridx;
    endRowIndex   = ridx;
    startColumnIndex = cidx;
    endColumnIndex   = cidx;
    if(!hasValidSelection){
      disableNext();
      disableFinish();
      disablePage('Extra');
    }
  }

  /*
     Left button pressed on a cell wrapper for most browsers...
   */
  var onMouseDown = function(evt){
    if(evt.button != 0){
      return true;
    }
    if(selectionState != 'none'){
      onMouseUp(evt);
    }
    onMouseDownCommon(evt.target.id);
    evt.preventDefault();
    evt.stopPropagation();
    return false;
  }
   /*
     Left button pressed on a cell wrapper for IE...
   */
  var onMouseDownIE = function(){
    var evt = window.event;
    if(evt.button != 1){
      return true;
    }
    if(selectionState != 'none'){
      onMouseUpIE();
    }
    onMouseDownCommon(evt.srcElement.id);
    evt.returnValue = false;
    evt.cancelBubble = true;
    return false;
  }

  /*
     Mouse enters a cell:
     - do nothing if the user didn't start a selection
     - obtain the table, row and column indices from the cell's id
     - do nothing if the selection was started in another table
     - update the selection bound according to the coordinates of the curent cell
     - update the (visual) selection state of the involved cells
   */
  var onMouseOverCommon = function(id){
    if(selectionState == 'none'){
      return;
    }
    var tidx = getTableIndex(id);
    var ridx = getRowIndex(id);
    var cidx = getColumnIndex(id);

    if (tidx != table){
      return;
    }
    
    
    switch (selectionState){
      case 'rows':
        if(ridx == 0){
          ridx = 1;
        }
        if (endRowIndex == ridx) break;
        if (startRowIndex == endRowIndex || signum(endRowIndex - startRowIndex) == signum(ridx - endRowIndex)){
          var sign = signum(ridx - endRowIndex);
          for(var i = endRowIndex + sign; (ridx - i) * sign >= 0; i += sign){
            document.getElementById('T' + tidx + 'R' + i).className = 'tdwSelectedRow';
          }
        }
        else{
          var sign = signum(endRowIndex - startRowIndex);
          var start = ((startRowIndex + ridx) + sign * signum(startRowIndex - ridx) * (startRowIndex - ridx)) / 2 + sign;
          for(var i = start; (endRowIndex - i) * sign >= 0; i += sign){
            document.getElementById('T' + tidx + 'R' + i).className = 'tdwUnSelectedRow';
          }
          for(var i = startRowIndex - sign; (ridx - i) * sign <= 0; i -= sign){
            document.getElementById('T' + tidx + 'R' + i).className = 'tdwSelectedRow';
          }
        }
        break;
      case 'columns':
        if(cidx == 0){
          cidx = 1;
        }
        if (endColumnIndex == cidx) break;
        if (startColumnIndex == endColumnIndex || signum(endColumnIndex - startColumnIndex) == signum(cidx - endColumnIndex)){
          var sign = signum(cidx - endColumnIndex);
          for(var i = endColumnIndex + sign; (cidx - i) * sign >= 0; i += sign){
            document.getElementById('T' + tidx + 'C' + i).className = 'tdwSelectedColumn';
            document.getElementById('T' + tidx + 'R0C' + i).className = 'tdwSelectedCell';
          }
        }
        else{
          var sign = signum(endColumnIndex - startColumnIndex);
          var start = ((startColumnIndex + cidx) + sign * signum(startColumnIndex - cidx) * (startColumnIndex - cidx)) / 2 + sign;
          for(var i = start; (endColumnIndex - i) * sign >= 0; i += sign){
            document.getElementById('T' + tidx + 'C' + i).className = 'tdwUnSelectedColumn';
            document.getElementById('T' + tidx + 'R0C' + i).className = 'tdwUnselectedCell';
          }
          for(var i = startColumnIndex - sign; (cidx - i) * sign <= 0; i -= sign){
            document.getElementById('T' + tidx + 'C' + i).className = 'tdwSelectedColumn';
            document.getElementById('T' + tidx + 'R0C' + i).className = 'tdwSelectedCell';
          }
        }
        break;
      case 'cells':
        if(ridx == 0){
          ridx = 1;
        }
        if (endRowIndex == ridx) {}
        else if (startRowIndex == endRowIndex || signum(endRowIndex - startRowIndex) == signum(ridx - endRowIndex)){
          var sign = signum(ridx - endRowIndex);
          var csign = signum(endColumnIndex - startColumnIndex);
          if(csign == 0) csign = 1;
          for(var i = endRowIndex + sign; (ridx - i) * sign >= 0; i += sign){
            for(var j = startColumnIndex; (endColumnIndex - j) * csign >= 0; j += csign){
              document.getElementById('T' + tidx + 'R' + i + 'C' + j).className = 'tdwSelectedCell';
            }
          }
        }
        else{
          var sign = signum(endRowIndex - startRowIndex);
          var csign = signum(endColumnIndex - startColumnIndex);
          if(csign == 0) csign = 1;
          var start = ((startRowIndex + ridx) + sign * signum(startRowIndex - ridx) * (startRowIndex - ridx)) / 2 + sign;
          for(var i = start; (endRowIndex - i) * sign >= 0; i += sign){
            for(var j = startColumnIndex; (endColumnIndex - j) * csign >= 0; j += csign){
              document.getElementById('T' + tidx + 'R' + i + 'C' + j).className = 'tdwUnselectedCell';
            }
          }
          for(var i = startRowIndex - sign; (ridx - i) * sign <= 0; i -= sign){
            for(var j = startColumnIndex; (endColumnIndex - j) * csign >= 0; j += csign){
              document.getElementById('T' + tidx + 'R' + i + 'C' + j).className = 'tdwSelectedCell';
            }
          }
        }
        if(cidx == 0){
          cidx = 1;
        }
        if (endColumnIndex == cidx) { break; }
        else if (startColumnIndex == endColumnIndex || signum(endColumnIndex - startColumnIndex) == signum(cidx - endColumnIndex)){
          var sign = signum(cidx - endColumnIndex);
          var rsign = signum(ridx - startRowIndex);
          if(rsign == 0) rsign = 1;
          for(var i = endColumnIndex + sign; (cidx - i) * sign >= 0; i += sign){
            for(var j = startRowIndex; (ridx - j) * rsign >= 0; j += rsign){
              document.getElementById('T' + tidx + 'R' + j + 'C' + i).className = 'tdwSelectedCell';
            }
          }
        }
        else{
          var sign = signum(endColumnIndex - startColumnIndex);
          var rsign = signum(ridx - startRowIndex);
          if(rsign == 0) rsign = 1;
          var start = ((startColumnIndex + cidx) + sign * signum(startColumnIndex - cidx) * (startColumnIndex - cidx)) / 2 + sign;
          for(var i = start; (endColumnIndex - i) * sign >= 0; i += sign){
            for(var j = startRowIndex; (ridx - j) * rsign >= 0; j += rsign){
              document.getElementById('T' + tidx + 'R' + j + 'C' + i).className = 'tdwUnselectedCell';
            }
          }
          for(var i = startColumnIndex - sign; (cidx - i) * sign <= 0; i -= sign){
            for(var j = startRowIndex; (ridx - j) * rsign >= 0; j += rsign){
              document.getElementById('T' + tidx + 'R' + j + 'C' + i).className = 'tdwSelectedCell';
            }
          }
        }
        break;
    }
    endRowIndex    = ridx;
    endColumnIndex = cidx;
  }
  /*
     Mouse entering a cell wrapper for most browsers...
   */
  var onMouseOver = function(evt){
    onMouseOverCommon(evt.target.id);
    evt.preventDefault();
    evt.stopPropagation();
    return false;
  }
   /*
     Mouse entering a cell wrapper for IE...
   */
  var onMouseOverIE = function(){
    var evt = window.event;
    onMouseOverCommon(evt.srcElement.id);
    evt.returnValue = false;
    evt.cancelBubble = true;
    return false;
  }

  // Mouse moves over a cell. Prevent text selection
  var onMouseMove = function(evt){
    evt.preventDefault();
    evt.stopPropagation();
    return false;
  }
  // Mouse moves over a cell - IE version. Prevent text selection
  var onMouseMoveIE = function(){
    var evt = window.event;
    evt.returnValue = false;
    evt.cancelBubble = true;
    return false;
  }

  /*
     Left mouse button released:
     - end selection process;
     - "sort" the selected area's bounds;
  */    
  var onMouseUpCommon = function(evt){
    selectionState = 'none';
    if(startRowIndex > endRowIndex){
      var tmp = startRowIndex;
      startRowIndex = endRowIndex;
      endRowIndex = tmp;
    }
    if(startColumnIndex > endColumnIndex){
      var tmp = startColumnIndex;
      startColumnIndex = endColumnIndex;
      endColumnIndex = tmp;
    }
    hasValidSelection = true;
    enableNext();
    enableFinish();
  }

  // Left button released wrapper for most browsers...
  var onMouseUp = function(evt){
    if(evt.button != 0 || selectionState == 'none'){
      return true;
    }
    onMouseUpCommon();
    evt.preventDefault();
    evt.stopPropagation();
    return false;
  }

  // Left button released wrapper for IE...
  var onMouseUpIE = function(){
    var evt = window.event;
    if(evt.button != 1 || selectionState == 'none'){
      return true;
    }
    onMouseUpCommon();
    evt.returnValue = false;
    evt.cancelBubble = true;
    return false;
  }

  // IE specific events for text selection
  var onSelectStartIE = function(){
    var evt = window.event;
    evt.returnValue = false;
    evt.cancelBubble = true;
    return false;
  }
  var onSelectIE = function(){
    var evt = window.event;
    evt.returnValue = false;
    evt.cancelBubble = true;
    return false;
  }

  // Remove the previous contents of the table container
  var clearOldTables = function(){
    while(container.hasChildNodes()){
      container.removeChild(container.firstChild);
    }
  }

  /* 
     Prepare the possible data sources:
     - add event listeners to the table cells
     - set the appropriate css class names to each cell
   */
  var prepareTables = function(foreignContainer){
    for(var tidx = 0; tidx < foreignContainer.childNodes.length; tidx++){
      var table = foreignContainer.childNodes.item(tidx);
      if(table.nodeType != 1) continue;
      for(k = table.childNodes.length - 1; k >= 0; k--){
        if(table.childNodes.item(k).nodeType == 1) break;
      }
      var tbody = table.childNodes.item(k);
      for(var ridx = 0; ridx < tbody.childNodes.length; ridx++){
        var row = tbody.childNodes.item(ridx);
        if(row.nodeType != 1) continue;
        for(var cidx = 0; cidx < row.childNodes.length; cidx++){
          var cell = row.childNodes.item(cidx);
          if(cell.nodeType != 1) continue;
          cell.className = 'tdwUnselectedTableCell';
          if(cell.addEventListener){
            cell.addEventListener('mousedown', onMouseDown, true);
            cell.addEventListener('mousemove', onMouseMove, true);
            cell.addEventListener('mouseover', onMouseOver, true);
            cell.addEventListener('mouseup', onMouseUp, true);
          }
          else{
            cell.attachEvent('onmousedown', onMouseDownIE);
            cell.attachEvent('onmousemove', onMouseMoveIE);
            cell.attachEvent('onmouseover', onMouseOverIE);
            cell.attachEvent('onmouseup', onMouseUpIE);
            cell.attachEvent('onselectstart', onSelectStartIE);
            cell.attachEvent('onselect', onSelectIE);
          }
        }
      }
    }
  }
  // Insert the tables into the container
  var addTables = function(foreignContainer){
    while(foreignContainer.hasChildNodes()){
      container.appendChild(foreignContainer.firstChild);
    }
  }

  /* 
     The request object is a data memer in order to be visible
     in the callback function
  */
  var request;
  /*
     Save the object by sending an ObjectAd request
   */
  var saveObject = function(){
    var objectData = "classname=XWiki.TableDataSource";
    objectData += "&XWiki.TableDataSource_datasource_name=" + encodeURI(document.getElementById('tdwSaveNameInput').value);
    objectData += "&XWiki.TableDataSource_table_number=" + table;
    objectData += "&XWiki.TableDataSource_range=" + getRange();
    objectData += "&XWiki.TableDataSource_has_header_row=" + (document.getElementById('tdwRowHeaderInput').checked ? '1' : '0');
    objectData += "&XWiki.TableDataSource_has_header_column=" + (document.getElementById('tdwColumnHeaderInput').checked ? '1' : '0');
    objectData += "&XWiki.TableDataSource_ignore_alpha=" + (document.getElementById('tdwIgnoreAlphaInput').checked ? '1' : '0');
    objectData += "&XWiki.TableDataSource_decimal_symbol=" + (document.getElementById('tdwDecimalSymbolInput').checked ? 'comma' : 'period');

    if(window.XMLHttpRequest){
      request = new XMLHttpRequest();
    }
    else{
      request = new ActiveXObject("Microsoft.XMLHTTP");
    }
    request.open("POST", saveAddress + doc.replace('.', '/'), false);
    request.setRequestHeader("Content-Type", "application/x-www-form-urlencoded; charset=UTF-8");
    try{
      request.send(objectData);
    }
    catch(ex){
      alert("Object could not be saved due to a connection problem.\n\n" + ex);
      return false;
    }
    return true;
  }
  /*
     Request the table data
  */
  this.getTablesFromPage = function(page){
    requestErrorMsg.className = 'tdwHidden';
    notablesMsg.className = 'tdwHidden';
    selectMsg.className = 'tdwHidden';
    container.className = 'tdwHidden';
    waitingMsg.className = 'tdwMessage';
    hasValidSelection = false;
    doc = page.replace('/', '.');
    selectionType = 'none';
    selectionState = 'none';
    disablePage('Extra');
    disableNext();
    disableFinish();
    if(window.XMLHttpRequest){
      request = new XMLHttpRequest();
    }
    else{
      request = new ActiveXObject("Microsoft.XMLHTTP");
    }
    page = page.replace(".", "/");
    request.open("GET", baseAddress + page, true);
    request.onreadystatechange = this.handleRequest;
    request.send(null);
  }

  /*
     Handle the request's response
  */
  this.handleRequest = function(){
    if(request.readyState == 4){
      waitingMsg.className = 'tdwHidden';
      try{
        if(request.status !== 200){
          requestErrorMsg.className = 'tdwErrorMessage';
          return;
        }
      }
      catch(e){
        requestErrorMsg.className = 'tdwErrorMessage';
        return;
      }
      if(request.responseXML){
        try{
          if(request.responseXML.getElementsByTagName('div').item(0).getAttribute('id') != 'tdwEnvelope'){
            document.getElementById('tdwNoTablePageName').firstChild.nodeValue = doc;
            notablesMsg.className = 'tdwErrorMessage';
            return;
          }
          else{
            var envelope = request.responseXML.getElementsByTagName('div').item(0);
            objectNumber = request.responseXML.getElementsByTagName('div').item(1).firstChild.nodeValue;
            if(envelope.getElementsByTagName('table').length == 0){
              document.getElementById('tdwNoTablePageName').firstChild.nodeValue = doc;
              notablesMsg.className = 'tdwErrorMessage';
              return;
            }
            clearOldTables();
            try{
              addTables(envelope);
              prepareTables(container);
            }
            catch(e){
              container.innerHTML = request.responseText.substring(request.responseText.indexOf("<table"), request.responseText.indexOf("</div>"));
              prepareTables(container);
            }
            selectMsg.className = 'tdwMessage';
            container.className = 'tdwTables';
            return;
          }
        }
        catch(e){
          document.getElementById('tdwNoTablePageName').firstChild.nodeValue = doc;
          notablesMsg.className = 'tdwErrorMessage';
        }
      }
      else{
        requestErrorMsg.className = 'tdwErrorMessage';
        return;
      }
    }
  }
}

// The wizard - as a property of the global object
window.wizard = new tdwWizard();
