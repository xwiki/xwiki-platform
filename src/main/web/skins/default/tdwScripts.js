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
  /*
     The 'div' element where all available tables that 
     can be used as data sources are inserted
   */
  var container;
  /*
     The prefix address of the table fetch page
   */
  var baseAddress;

  /*
    Wizard initialization:
    - adds global event listeners
   */ 
  this.init = function(address){
    container = document.getElementById("tdwTables")
    baseAddress = address;
    if(document.documentElement.addEventListener){
      document.documentElement.addEventListener('mouseup', onMouseUp, true);
    }
    else{
      document.documentElement.attachEvent('onmouseup', onMouseUpIE);
      document.documentElement.attachEvent('onselectstart', onSelectStartIE);
      document.documentElement.attachEvent('onselect', onSelectIE);
    }
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
    selectionState = 'none';
    var tidx = getTableIndex(id);
    var ridx = getRowIndex(id);
    var cidx = getColumnIndex(id);
    if(ridx == 0){
      if(cidx == 0){
        // The whole table is selected
        document.getElementById('T' + tidx).className = 'tdwSelectedTable';
        selectionType = 'table';
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
      var tbody = foreignContainer.childNodes.item(tidx).lastChild;
      for(var ridx = 0; ridx < tbody.childNodes.length; ridx++){
        var row = tbody.childNodes.item(ridx);
        if(row.nodeType != row.ELEMENT_NODE) continue;
        for(var cidx = 0; cidx < row.childNodes.length; cidx++){
          var cell = row.childNodes.item(cidx);
          if(cell.nodeType != cell.ELEMENT_NODE) continue;
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
     Request the table data
  */
  this.getTablesFromPage = function(page){
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
      if(request.responseXML){
        try{
          if(request.responseXML.getElementsByTagName('div').item(0).getAttribute('id') != 'tdwEnvelope'){
            alert("The requested document is not valid.");
            return;
          }
          else{
            var envelope = request.responseXML.getElementsByTagName('div').item(0);
            clearOldTables();
            try{
              addTables(envelope);
              prepareTables(container);
            }
            catch(e){
              container.innerHTML = request.responseText.substring(request.responseText.indexOf("<table"), request.responseText.indexOf("</div>"));
              prepareTables(container);
            }
            return;
          }
        }
        catch(e){
          alert("The requested document is not valid.");
        }
      }
      else{
        alert("The requested document is not valid.");
        return;
      }
    }
  }
}

// The wizard - as a property of the global object
window.wizard = new tdwWizard();
