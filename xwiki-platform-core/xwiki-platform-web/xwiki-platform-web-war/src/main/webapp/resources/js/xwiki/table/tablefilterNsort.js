/*
    Note: We got the agreement from Max Guglielmi (on 21/5/2007) to include this script in XWiki's
          distribution. Same from Jan Eldenmalm. The script from Joost de Valk is under the MIT
          license.
    Source: http://www.eldenmalm.com/tableFilterNSort.jsp
    Source: http://mguglielmi.free.fr/scripts/TableFilter/
*/
/*#####################################################
    Version: 1.5
    Date: 6 Aug 2006
    Editor & code Fixes: Jan (at) Eldenmalm . com
    Coders: Joost de Valk, Max Guglielmi
    This script is a merger of two brilliant scripts
    and some improvements to make them easier to use.
    With this one script it is easy to filter and sort
    a table at client side without adding anything but
    "classes" to the TD and TR tags and a link this script.
    This joint script still contains legacy code from the two
    original coders  - for functionality that currently is untested.

    Documentation of currently tested functionality:

    Example:
    <table class="sortable filterable doOddEven" id="myUniqueTableId">
    <tr><td></td><td></td><td></td></tr>
    <tr><td></td><td></td><td></td></tr>
    <tr class="sortHeader"><td class="selectFilter"></td><td></td><td class="unsortable noFilter"></td></tr>
    <tr><td></td><td></td><td></td></tr>
    <tr><td></td><td></td><td></td></tr>
    <tr class="sortBottom"><td></td><td></td><td></td></tr>
    <tr><td></td><td></td><td></td></tr>
    <tr><td></td><td></td><td></td></tr>
    </table>

    CLASS names and their meaning:
    id (table)(mandatory) = a unique identifier not shared with other objects.
    sortable (table)(optional) = Makes the table sortable
    filterable (table)(optional) = Makes the table filterable
    doOddEven (table) (optional) = declares that visible table rows between the sortHeader row and
                                   sortBottom should contain class names "odd" and "even"
                                   for creating alternating tables
    sortHeader (tr)(mandatory) = is the row in the table that is the header row
    noFilter (td)(optional) =  declares a column as non filterable
    selectFilter (td) (optional)  = declares a column as filterable using a selct box,
                                    that is autimatically populated with "available" line values
    unsortable (td) (optional) = declares a column as non sortable
    sortBottom (tr) (optional) = declares a row as the bottom of the sortable rows ( used for totals etc...)

    Notice: Images are sorted on the "alt" attribute.

    The sort function required access to three a few pictures to "look good"
    Change these values with regards to your specific set up */
    var image_path = "";
    var image_up = "$xwiki.getSkinFile('js/xwiki/table/img/arrow-up.gif')";
    var image_down = "$xwiki.getSkinFile('js/xwiki/table/img/arrow-down.gif')";
    var image_none = "$xwiki.getSkinFile('js/xwiki/table/img/arrow-none.gif')";

/*====================================================
  - HTML Table Filter Generator v1.3.jan1
  - By Max Guglielmi
  - mguglielmi.free.fr/scripts/TableFilter/?l=en
  - please do not change this comment
  - don't forget to give some credit... it's always
  good for the author
=====================================================*/

/*
Table sorting script, taken from http://www.kryogenix.org/code/browser/sorttable/ .
Distributed under the MIT license: http://www.kryogenix.org/code/browser/licence.html .

Adaptation by Joost de Valk ( http://www.joostdevalk.nl/ ) to add alternating row classes as well.

Copyright (c) 1997-2006 Stuart Langridge, Joost de Valk.
*/


/* Don't change anything below this unless you know what you're doing */

var TblId, StartRow, SearchFlt, ModFn, ModFnId;
TblId = new Array(), StartRow = new Array();
ModFn = new Array(), ModFnId = new Array();

// creates and event that triggers onload and enables the filtering and sorting.
addEvent(window, "load", init_sortnfilter);

var SORT_COLUMN_INDEX;

function init_sortnfilter() {
// Intiates the sorting and filtering triggered on an event
  sortables_init();
  filterable_init();
}

function sortables_init() {
// Initiates the sorting capability
  // Find all tables with class sortable and make them sortable
  if (!document.getElementsByTagName) {
    return;
  }
  var tbls = document.getElementsByTagName("table");
  for (var ti = 0; ti < tbls.length; ti++) {
    var thisTbl = tbls[ti];
    if (((' ' + thisTbl.className + ' ').indexOf("sortable") != -1) && (thisTbl.id)) {
      //initTable(thisTbl.id);
      ts_makeSortable(thisTbl);
      //sum_up(thisTbl);
    }
  }
}

function filterable_init() {
// Initiates the filtering capability
  // Find all tables with class doFilter and make them filterable
  if (!document.getElementsByTagName) {
    return;
  }
  var tbls = document.getElementsByTagName("table");
  for (var ti = 0; ti < tbls.length; ++ti) {
    var thisTbl = tbls[ti];
    if (((' '+thisTbl.className+' ').indexOf("filterable") != -1) && (thisTbl.id)) {
      //initTable(thisTbl.id);
      //ts_makeSortable(thisTbl);
      setFilterGrid(thisTbl.id, (getHeaderRow(thisTbl)));
      //sum_up(thisTbl);
    }
  }
}

function ts_resortTable(lnk) {
  // get the span
  var span, ARROW;
  for (var ci = 0; ci < lnk.childNodes.length; ++ci) {
    if (lnk.childNodes[ci].tagName && lnk.childNodes[ci].tagName.toLowerCase() == 'span') span = lnk.childNodes[ci];
  }
  var td = lnk.parentNode;
  var column = td.cellIndex;
  var table = getParent(td,'TABLE');

  // Work out a type for the column
  if (table.rows.length <= 1) {
    return;
  }
  var itm = ts_getInnerText(table.rows[getHeaderRow(table)+1].cells[column]);
  var nextRow = getHeaderRow(table) + 2;
  // this loop will get the contents from the first line with actual content
  while (!itm) {
    itm = ts_getInnerText(table.rows[nextRow].cells[column]) ;
    ++nextRow;
  }
  // Ensures for sorting and evaluation purposes that itm is never undefined
  if (!itm) {
    itm = '';
  }
  var sortfn = ts_sort_caseinsensitive;
  if (itm.match(/^\d\d[\/-]\d\d[\/-]\d\d\d\d$/)) {
    sortfn = ts_sort_date;
  }
  if (itm.match(/^\d\d[\/-]\d\d[\/-]\d\d$/)) {
    sortfn = ts_sort_date;
  }
  if (itm.match(/^[£$]/)) {
    sortfn = ts_sort_currency;
  }
  if (itm.match(/^[\d\.]+$/)) {
    sortfn = ts_sort_numeric;
  }
  SORT_COLUMN_INDEX = column;
  var newRows = new Array();
  var nonSortedRows = new Array();
  // the new rows are added to the array to sort...get all rows from the "sortHeader" to the first sortBottom row.
  var firstNonSortedRow = null != getSortBottomRow(table) ? (getSortBottomRow(table)+1) : table.rows.length ;

  for (var j = firstNonSortedRow; j < table.rows.length; ++j) {
    nonSortedRows[nonSortedRows.length] = table.rows[j];
  }

  for (var j = getHeaderRow(table) + 1; j < table.rows.length; ++j) {
    newRows[newRows.length] = table.rows[j];
  }
  newRows.sort(sortfn);

  if (span.getAttribute("sortdir") == 'down') {
    ARROW = '<img border="0" src="'+ image_path + image_up + '" alt="&#x2191;"/>';
    newRows.reverse();
    span.setAttribute('sortdir','up');
  } else {
    ARROW = '<img border="0" src="'+ image_path + image_down + '" alt="&#x2193;"/>';
    span.setAttribute('sortdir','down');
  }

  // We appendChild rows that already exist to the tbody, so it moves them rather than creating new ones
  // don't do sortbottom rows
  for (var i = 0; i < newRows.length; ++i) {
    if (!newRows[i].className || (newRows[i].className && (newRows[i].className.indexOf('sortBottom') == -1))) {
      table.tBodies[0].appendChild(newRows[i]);
    }
  }
  // do sortBottom rows only
  for (var i=0; i<newRows.length; i++) {
    if (newRows[i].className && (newRows[i].className.indexOf('sortBottom') != -1)) {
      table.tBodies[0].appendChild(newRows[i]);
    }
  }
  // ad the non sorted rows...
  for (var i = 0; i < nonSortedRows.length; ++i) {
    table.tBodies[0].appendChild(nonSortedRows[i]);
  }

  // Delete any other arrows there may be showing
  var allspans = document.getElementsByTagName("span");
  for (var ci = 0; ci < allspans.length; ++ci) {
    if (allspans[ci].className == 'sortarrow') {
      if (getParent(allspans[ci],"table") == getParent(lnk,"table")) { // in the same table as us?
        allspans[ci].innerHTML = '<img border="0" src="'+ image_path + image_none + '" alt="&#x21F5;"/>';
      }
    }
  }

  span.innerHTML = ARROW;
  alternate(table);
}

function Filter(id) {
/*====================================================
  - gets search strings from SearchFlt array
  - retrieves data from each td in every single tr
  and compares to search string for current
  column
  - tr is hidden if all search strings are not
  found
=====================================================*/
  getFilters(id);
  var t = document.getElementById(id);
  var SearchArgs = new Array();
  var ncells = getCellsNb(id);

  // Vincent Massol: Modified orinigal line so that it's compatible with Prototype.
  // Note: this is fixed in Max's script at http://mguglielmi.free.fr/scripts/TableFilter/?l=fr
  // Original: for(var i in SearchFlt) SearchArgs.push((document.getElementById(SearchFlt[i]).value).toLowerCase());
  for (var i = 0; i < SearchFlt.length; ++i) {
    SearchArgs.push((document.getElementById(SearchFlt[i]).value).toLowerCase());
  }

  var start_row = getStartRow(id);
  var row = t.getElementsByTagName("tr");
  var lastFilteredRow = null != getSortBottomRow(t) ? getSortBottomRow(t) : t.rows.length;

  for(var k = start_row; k < row.length; ++k) {
    var isRowValid = true;
    /*** if table already filtered some rows are not visible ***/
    if(row[k].style.display == "none") {
      row[k].style.display = "";
    }

    var cell = getChildElms(row[k]).childNodes;
    var nchilds = cell.length;
    if (nchilds == ncells) {// checks if row has exact cell #
      var cell_value = new Array();
      var occurence = new Array();

      for (var j=0; j<nchilds; j++) {// this loop retrieves cell data
        var cell_data = getCellText(cell[j]).toLowerCase();
        cell_value.push(cell_data);

        if (SearchArgs[j] != "") {
          var num_cell_data = parseFloat(cell_data);
          if(/<=/.test(SearchArgs[j]) && !isNaN(num_cell_data)) {// first checks if there is an operator (<,>,<=,>=)
            num_cell_data <= parseFloat(SearchArgs[j].replace(/<=/, "")) ? occurence[j] = 3 : occurence[j] = 1;
          } else if(/>=/.test(SearchArgs[j]) && !isNaN(num_cell_data)) {
            num_cell_data >= parseFloat(SearchArgs[j].replace(/>=/,"")) ? occurence[j] = 3 : occurence[j] = 1;
          } else if(/</.test(SearchArgs[j]) && !isNaN(num_cell_data)) {
            num_cell_data < parseFloat(SearchArgs[j].replace(/</,"")) ? occurence[j] = 3 : occurence[j] = 1;
          } else if(/>/.test(SearchArgs[j]) && !isNaN(num_cell_data)) {
            num_cell_data > parseFloat(SearchArgs[j].replace(/>/,"")) ? occurence[j] = 3 : occurence[j] = 1;
          } else {
            occurence[j] = cell_data.split(SearchArgs[j]).length;
          }
        }
      }//for j

      for(var u=0; u<ncells; u++) {
        if(SearchArgs[u]!="" && occurence[u]<2) {
          isRowValid = false;
        }
      }//for t
    }//if

    if(isRowValid==false && k < lastFilteredRow) {
      row[k].style.display = "none";
    } else {
      row[k].style.display = "";
    }
  }// for k
  // After filtering call the alternation function to alternate...
  alternate(t);
}

//  Subroutines below

function getHeaderRow (table) {
// This function takes a table and verifies in which row the class name of <TR> tag contains "sortHeader"
// When found it returns this row....i.e the header row - the next row is normally the first sortable row
// if no row is found or the sort header is the "last" row - than it returns 0.
  for (var i = 0; i < table.rows.length - 1; ++i) {
    if (table.rows[i].className.indexOf("sortHeader") > -1) {
      return i ;
    }
  }
  return 0 ;
}

function getSortBottomRow (table) {
// This function takes a table and verifies in which row the class name of <TR> tag contains "sortHeader"
// When found it returns this row....i.e the header row - the next row is normally the first sortable row
// if no row is found or the sort header is the "last" row - than it returns 0.
  for (var i = 0; i < table.rows.length; ++i) {
    if (table.rows[i].className.indexOf("sortBottom") > -1) {
      return i;
    }
  }
  return null ;
}

function ts_getInnerText(el) {
  if (typeof el == "string") {
    return el;
  }
  if (typeof el == "undefined") {
    return '';
  };
  if (typeof el == "object" && el.tagName.toLowerCase() == 'img') {
    // if the contents of the table are images - they can be sorted on the "Alt" text
    return el.alt ;
  }
  if (el.innerText) {
    return el.innerText;  //Not needed but it is faster
  }
  var str = "";

  var cs = el.childNodes;
  var l = cs.length;
  for (var i = 0; i < l; i++) {
    switch (cs[i].nodeType) {
      case 1: //ELEMENT_NODE
        str += ts_getInnerText(cs[i]);
        break;
      case 3: //TEXT_NODE
        str += cs[i].nodeValue;
        break;
    }
  }
  return str;
}

function ts_makeSortable(table) {
  var firstRow;
  if (table.rows && table.rows.length > 0) {
    firstRow = table.rows[getHeaderRow(table)];
  }
  if (!firstRow) {
    return;
  }

  // We have a first row: assume it's the header, and make its contents clickable links
  for (var i = 0; i < firstRow.cells.length; ++i) {
    var cell = firstRow.cells[i];
    var txt = ts_getInnerText(cell);
    if (cell.className != "unsortable" && cell.className.indexOf("unsortable") == -1) {
      cell.innerHTML = '<a href="#" class="sortHeader" onclick="ts_resortTable(this);return false;">' + txt + '<span class="sortarrow"><img border="0" src="'+ image_path + image_none + '" alt="&#x21F5;"/></span></a>';
    }
  }
  alternate(table);
}

function alternate(table) {
/*====================================================
  - check if the table passed to the function is set
  to doOddEven, if that is the case it colors the lines acordingly.
=====================================================*/
  if (table.className.indexOf("doOddEven") > -1) {
    var visibleRows = 1;
    var endAlternation = null != getSortBottomRow(table) ? getSortBottomRow(table) : table.rows.length ;
    // Take object table and get all it's tbodies.
    var tableBodies = table.getElementsByTagName("tbody");
    // Loop through these tbodies
    for (var i = 0; i < tableBodies.length; i++) {
      // Take the tbody, and get all it's rows
      var tableRows = tableBodies[i].getElementsByTagName("tr");
      // Loop through these rows
      // Start at 1 because we want to leave the heading row untouched
      // we change this to start at 2 to leave two rows untouched
      for (var j = getHeaderRow(table) + 1; j < endAlternation; ++j) {
        // Check if j is even, and apply classes for both possible results
        if (tableRows[j].style.display=="") {
          visibleRows++ ;
          swapOddEven(tableRows[j],visibleRows)
        }
      }
    }
  }
}

function getParent(el, pTagName) {
  if (el == null) {
    return null;
  } else if (el.nodeType == 1 && el.tagName.toLowerCase() == pTagName.toLowerCase()) {  // Gecko bug, supposed to be uppercase
    return el;
  } else {
    return getParent(el.parentNode, pTagName);
  }
}

function addEvent(elm, evType, fn, useCapture) {
// addEvent and removeEvent
// cross-browser event handling for IE5+,  NS6 and Mozilla
// By Scott Andrew
  if (elm.addEventListener) {
    elm.addEventListener(evType, fn, useCapture);
    return true;
  } else if (elm.attachEvent) {
    var r = elm.attachEvent("on"+evType, fn);
    return r;
  } else {
    alert("Handler could not be removed");
  }
}

function replace(s, t, u) {
  /*
  **  Replace a token in a string
  **    s  string to be processed
  **    t  token to be found and removed
  **    u  token to be inserted
  **  returns new String
  */
  var i = s.indexOf(t);
  var r = "";
  if (i == -1) {
    return s;
  }
  r += s.substring(0,i) + u;
  if ( i + t.length < s.length) {
    r += replace(s.substring(i + t.length, s.length), t, u);
  }
  return r;
}

function setFilterGrid(id) {
/*====================================================
  - Checks if id exists and is a table
  - Then looks for additional params
  - Calls fn that adds inputs and button
=====================================================*/
  var tbl = document.getElementById(id);
  var ref_row, fObj;
  if(tbl != null && tbl.nodeName.toLowerCase() == "table") {
    TblId.push(id);
    if (arguments.length>1) {
      for(var i=0; i<arguments.length; i++) {
        var argtype = typeof arguments[i];

        switch(argtype.toLowerCase()) {
          case "number":
            ref_row = arguments[i];
          break;
          case "object":
            fObj = arguments[i];
          break;
        }//switch
      }//for
    }//if

    ref_row == undefined ? StartRow.push(2) : StartRow.push(ref_row+2);
    var ncells = getCellsNb(id,ref_row);
    AddRow(id,ncells,fObj);
  }
}

function AddRow(id,n,f) {
/*====================================================
  - adds a row containing the filtering grid
=====================================================*/
  var t = document.getElementById(id);
  var fltrow = t.insertRow(0);
  // get the filtering settings from the header row...
  var checkRow = t.rows[getHeaderRow(t)];
  //var checkRow = t.rows[1];
  var inpclass, displayBtn, btntext, enterkey, modfilter_fn;

  f!=undefined && f["btn"]==false ? displayBtn = false : displayBtn = true;
  f!=undefined && f["btn_text"]!=undefined ? btntext = f["btn_text"] : btntext = "Filter";
  f!=undefined && f["enter_key"]==false ? enterkey = false : enterkey = true;
  f!=undefined && f["mod_filter_fn"] ? modfilter_fn = true : modfilter_fn = false;
  if(modfilter_fn) {
    ModFnId.push(id);
    ModFn.push(f["mod_filter_fn"]);
  }

  for(var i = 0; i < n; ++i) {
    var fltcell = fltrow.insertCell(i);
    var checkCell = checkRow.cells[i] ;
    i==n-1 && displayBtn==true ? inpclass = "flt_s" : inpclass = "flt";
    // this IF statement is mixture between the "old" way of initialising the filtering and the new way based on classes - not nice. :(
    if ((f == undefined || f["col_"+i] == undefined || f["col_"+i] == "none") && checkCell.className.indexOf("selectFilter") == -1) {
      var inp = document.createElement("input");
      inp.setAttribute("id", "flt" + i + "_" + id);
      inp.setAttribute("size", "5");
      if (checkCell.className.indexOf("noFilter") == -1) {
        inp.setAttribute("type","text");
      } else {
        inp.setAttribute("type","hidden");
      }
      // inp.setAttribute("class","flt"); //doesn't seem to work on ie<=6
      inp.className = inpclass;
      fltcell.appendChild(inp);
      if (enterkey) {
        inp.onkeypress = DetectKey;
      }
    } else if(checkCell.className.indexOf("selectFilter") != -1) {
      // create a select box and popultate it of the column is marked to do a "selectFilter"
      var slc = document.createElement("select");
      slc.setAttribute("id", "flt" + i + "_" + id);
      slc.className = inpclass;
      fltcell.appendChild(slc);
      PopulateOptions(id, i, n);
      if (enterkey) {
        slc.onkeypress = DetectKey;
      }
    }

    if (i == n-1 && displayBtn == true) {// this adds button
      var btn = document.createElement("input");

      btn.setAttribute("id", "btn" + i + "_" + id);
      btn.setAttribute("type", "button");
      btn.setAttribute("value", btntext);
      btn.className = "btnflt";

      fltcell.appendChild(btn);
      (!modfilter_fn) ? btn.onclick = function() { Filter(id) } : btn.onclick = f["mod_filter_fn"];
    }//if
  }// for i
}

function PopulateOptions(id, cellIndex, ncells) {
/*====================================================
  - populates select
  - adds only 1 occurence of a value
=====================================================*/
  var t = document.getElementById(id);
  var start_row = getStartRow(id);
  var row = t.getElementsByTagName("tr");
  var OptArray = new Array();
  var optIndex = 0; // option index

  for (var k = start_row; k < row.length; ++k) {
    var cell = getChildElms(row[k]).childNodes;
    var nchilds = cell.length;

    if (nchilds == ncells) {// checks if row has exact cell #
      for (var j = 0; j < nchilds; ++j) {// this loop retrieves cell data
        if (cellIndex == j) {
          var cell_data = getCellText(cell[j]);
          if (OptArray.toString().toUpperCase().search(cell_data.toUpperCase()) == -1) {
            // checks if celldata is already in array
            optIndex++;
            OptArray.push(cell_data);
            var currOpt = new Option(cell_data,cell_data,false,false);
            document.getElementById("flt"+cellIndex+"_"+id).options[optIndex] = currOpt;
          }
        }//if cellIndex==j
      }//for j
    }//if
  }//for k
}

function getCellsNb(id, nrow) {
/*====================================================
  - returns number of cells in a row
  - if nrow param is passed returns number of cells
  of that specific row
=====================================================*/
  var t = document.getElementById(id);
  var tr;
  if (nrow == undefined) {
    tr = t.getElementsByTagName("tr")[0];
  } else {
    tr = t.getElementsByTagName("tr")[nrow];
  }
  var n = getChildElms(tr);
  return n.childNodes.length;
}

function getFilters(id) {
/*====================================================
  - filter (input or select) ids are stored in
  SearchFlt array
=====================================================*/
  SearchFlt = new Array();
  var t = document.getElementById(id);
  var tr = t.getElementsByTagName("tr")[0];
  var enfants = tr.childNodes;

  for (var i = 0; i < enfants.length; ++i) {
    SearchFlt.push(enfants[i].firstChild.getAttribute("id"));
  }
}

function getStartRow(id) {
/*====================================================
  - returns starting row for Filter fn for a
  given table id
=====================================================*/
  var r;
  for(var j in TblId) {
    if(TblId[j] == id) {
      r = StartRow[j];
    }
  }
  return r;
}

function getChildElms(n) {
/*====================================================
  - checks passed node is a ELEMENT_NODE nodeType=1
  - removes TEXT_NODE nodeType=3
=====================================================*/
  if (n.nodeType == 1) {
    var enfants = n.childNodes;
    for(var i = 0; i < enfants.length; ++i) {
      var child = enfants[i];
      if (child.nodeType == 3) {
        n.removeChild(child);
      }
    }
    return n;
  }
}

function getCellText(n) {
/*====================================================
  - returns text + text of child nodes of a cell
=====================================================*/
  var s = "";
  var enfants = n.childNodes;
  for(var i = 0; i < enfants.length; ++i) {
    var child = enfants[i];
    if (child.nodeType == 3) {
      s += child.data;
    } else {
      s += getCellText(child);
    }
  }
  return s;
}

function DetectKey(e) {
/*====================================================
  - common fn that detects return key for a given
  element (onkeypress attribute on input)
=====================================================*/
  var evt = (e) ? e : (window.event) ? window.event : null;
  if (evt) {
    var key = (evt.charCode) ? evt.charCode : ((evt.keyCode) ? evt.keyCode : ((evt.which) ? evt.which : 0));
    if (key == "13") {
      var cid, leftstr, tblid, CallFn;
      cid = this.getAttribute("id");
      leftstr = this.getAttribute("id").split("_")[0];
      tblid = cid.substring(leftstr.length+1,cid.length);
      for (var i in ModFn) {
        ModFnId[i] == tblid ? CallFn=true : CallFn=false;
      }
      (CallFn) ? ModFn[i].call() : Filter(tblid);
    }//if key
  }//if evt
}

function swapOddEven(row, orderNr) {
/*====================================================
  - swaps style from odd to even depending on order number,
   do provide the real order number as in "visible"...
=====================================================*/
  if ((orderNr % 2) == 0) {
    if ((row.className.indexOf('odd') > -1)) {
      row.className = replace(row.className, 'odd', 'even');
    } else {
      row.className = row.className.indexOf("even") > -1 ? row.className : row.className + " even";
    }
  } else {
    if ((row.className.indexOf('even') > -1)) {
      row.className = replace(row.className, 'even', 'odd');
    }
    row.className = row.className.indexOf("odd") > -1 ? row.className : row.className + " odd";
  }
}

// #############################  Sort functions ############################

function ts_sort_date(a, b) {
  // y2k notes: two digit years less than 50 are treated as 20XX, greater than 50 are treated as 19XX
  var aa = ts_getInnerText(a.cells[SORT_COLUMN_INDEX]);
  var bb = ts_getInnerText(b.cells[SORT_COLUMN_INDEX]);
  var dt1, yr, dt2;
  if (aa.length == 10) {
    dt1 = aa.substr(6, 4) + aa.substr(3, 2) + aa.substr(0, 2);
  } else {
    yr = aa.substr(6, 2);
    if (parseInt(yr) < 50) {
      yr = '20' + yr;
    } else {
      yr = '19' + yr;
    }
    dt1 = yr + aa.substr(3, 2) + aa.substr(0, 2);
  }
  if (bb.length == 10) {
    dt2 = bb.substr(6, 4) + bb.substr(3, 2) + bb.substr(0, 2);
  } else {
    yr = bb.substr(6, 2);
    if (parseInt(yr) < 50) {
      yr = '20' + yr;
    } else {
      yr = '19' + yr;
    }
    dt2 = yr + bb.substr(3, 2) + bb.substr(0, 2);
  }
  if (dt1 == dt2) {
    return 0;
  }
  if (dt1 < dt2) {
    return -1;
  }
  return 1;
}

function ts_sort_currency(a, b) {
  var aa = ts_getInnerText(a.cells[SORT_COLUMN_INDEX]).replace(/[^0-9.]/g,'');
  var bb = ts_getInnerText(b.cells[SORT_COLUMN_INDEX]).replace(/[^0-9.]/g,'');
  return isNaN(parseFloat(aa) - parseFloat(bb)) ? -1 : parseFloat(aa) - parseFloat(bb) ;
  // if the value isNaN we return -1 else the last sortable number isn't sorted correctly
}


function ts_sort_numeric(a, b) {
  var aa = parseFloat(ts_getInnerText(a.cells[SORT_COLUMN_INDEX]));
  var bb = parseFloat(ts_getInnerText(b.cells[SORT_COLUMN_INDEX]));

  if (isNaN(aa)) {
    aa = 0;
  }
  if (isNaN(bb)) {
    bb = 0;
  }
  return aa - bb;
}

function ts_sort_caseinsensitive(a, b) {
  var aa = ts_getInnerText(a.cells[SORT_COLUMN_INDEX]).toLowerCase();
  var bb = ts_getInnerText(b.cells[SORT_COLUMN_INDEX]).toLowerCase();

  if (aa == bb) {
    return 0;
  }
  if (aa < bb) {
    return -1;
  }
  return 1;
}

function ts_sort_default(a, b) {
  var aa = ts_getInnerText(a.cells[SORT_COLUMN_INDEX]);
  var bb = ts_getInnerText(b.cells[SORT_COLUMN_INDEX]);
  if (aa == bb) {
    return 0;
  }
  if (aa < bb) {
    return -1;
  }
  return 1;
}
