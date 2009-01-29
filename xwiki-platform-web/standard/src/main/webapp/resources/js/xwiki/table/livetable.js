/**
  * The class representing an AJAX-populated live grid.
  * It is (almost) independent of the underlying HTML markup, a function passed as an argument being
  * responsible with displaying the content corresponding to a row. Uses JSON for the response
  * encoding.
  *
  * Created by Cristian Vrabie, maintained at XWiki.
  *
  * @todo Separate table classes from our callbacks and from the TripleStateCheckbox.
  * @todo Add the right Copyright Notice.
  */
var ASSTable = Class.create({
  /**
    * @param url The base address for requesting the table data.
    * @param limit The maximum number of rows to display in the table at a moment.
    * @param domNode The node supposed to hold the data rows, should be a <tbody>. DOM element or
    *   identifier.
    * @param scrollNode The node where the scrollbar should be placed, should be a <td> next to
    *   the userlist table. DOM element or identifier.
    * @param filterNode The node containing the input/select fields with request parameters. DOM
    *   element or identifier.
    * @param getHandler A javascript function called for displaying fetched rows. The function
    *   accepts a JSON-parsed object and returns a DOM <tr> node.
    * @param hasFilters Indicates whether the filters are enabled or not.
    * @todo Remove hasFilters, just check on filterNode.
    * @todo Default values for limit, getHandler.
    * @todo Make this a valid ARIA grid: http://www.w3.org/TR/aria-role/#structural
    */
  initialize: function(url, limit, domNode, scrollNode, filterNode, getHandler, hasFilters, action)
  {
    this.domNode = $(domNode);
    this.scroller = new ASSScroller(this, scrollNode);
    if (hasFilters) {
      this.filter = new ASSFilter(this, filterNode);
    }
    if(action) {
      this.action = action;
    }
    else {
      this.action = "view";
    }
    this.hasFilters = hasFilters;
    this.filters = "";
    this.getHandler = getHandler;
    this.totalRows = -1;
    this.fetchedRows = new Array();
    this.limit = limit;
    this.getUrl = url;
    this.lastoffset = 1;
    this.sendReqNo = 0;
    this.recvReqNo = 0;

    // Show initial rows
    this.showRows(1, limit);
  },

  /**
    * Initializes an AJAX request for retrieving some table data. Uses two ranges, one defines the
    * range that must be retrieved from the server, and one defines the range that should be
    * displayed. Two ranges are needed as some of the displayed rows can already be available from
    * a previous request.
    * @param offset Starting request offset; the index of the first row that should be retrieved.
    * @param limit Maximum number of rows to retrieve.
    * @param doffset Starting display offset; the index of the first row that should be displayed.
    * @param dlimit Maximum number of rows to display.
    */
  getRows: function(reqOffset, reqLimit, displayOffset, displayLimit)
  {
    var url =  this.getUrl + '&offset='+reqOffset+'&limit='+reqLimit+'&reqNo='+ (++this.sendReqNo);

    if (this.hasFilters) {
      this.filters = this.filter.getFilters();
      if (this.filters != "" && this.filters != undefined) {
        url += this.filters;
      }
    }

    var self = this;

    $('ajax-loader').style.display = "block";

    new Ajax.Request(url,
    {
      method: 'get',
      onComplete: function( transport ) {
        $('ajax-loader').style.display = "none";
      },

      onSuccess: function( transport ) {
        var res = eval( '(' + transport.responseText + ')');
        if (res.reqNo < self.sendReqNo) {
          return;
        }
        self.recvReqNo = res.reqNo;
        $('ajax-loader').style.display = "none";

        if(res.totalrows <= res.returnedrows) {
          self.scroller.domNode.style.display = "none";
        } else {
          self.scroller.domNode.style.display = "block";
        }
        self.updateFetchedRows(res);
        self.displayRows(displayOffset, displayLimit);
      }
    });
  },

  /**
    * Add/refresh items to the cache of fetched data.
    * @param json Returned data from the server, as a parsed JSON object.
    */
  updateFetchedRows: function(json)
  {
    this.json = json;
    this.totalRows = json.totalrows;
    for (var i = json.offset; i < json.offset + json.returnedrows; ++i) {
      this.fetchedRows[i] = json.rows[i-json.offset];
    }
  },

  /**
    * Removes the displayed rows from the XHTML document.
    */
  clearDisplay: function()
  {
    var object = this.domNode;
    while (object.hasChildNodes()) {
      object.removeChild(object.firstChild);
    }
  },

  /**
    * Displays already fetched rows. Calls {@link #getHandler} for creating the XHTML elements, and
    * inserts them in {@link domNode}.
    * @param offset Starting offset; the index of the first row that should be displayed.
    * @param limit Maximum number of rows to display.
    */
  displayRows: function(offset, limit)
  {
    var f = offset + limit - 1;
    if (f > this.totalRows) f = this.totalRows;
    var off = (this.totalRows > 0) ? offset : 0;
    var msg = "$msg.get('from') <strong>" + off + "</strong> $msg.get('to') <strong>" + f + "</strong> $msg.get('ui.ajaxTable.outof') <strong>" + this.totalRows + "</strong>";
    var msg = msg.toLowerCase();
    $('showLimits').innerHTML = "$msg.get('rightsmanager.displayrows') " + msg;

    this.clearDisplay();

    for (var i = off; i <= f; i++) {
      var elem = this.getHandler(this.fetchedRows[i], i, this);
      this.domNode.appendChild(elem);
    }

    this.scroller.refreshScrollbar();
  },

  /**
    * Fetch and display rows. This method checks the existing fetched data to determine which (if
    * any) rows should be fetched from the server, then forwards the call to {@link #displayRows}.
    * @param offset Starting offset; the index of the first row that should be displayed.
    * @param limit Maximum number of rows to display.
    */
  showRows: function(offset, limit)
  {
    this.lastoffset = offset;
    // This is some debugging string.
    var buff  = 'request to display rows '+offset+' to '+(offset+limit)+' <br />\n';

    // If no rows fetched yet, get all we need
    if (this.totalRows == -1) {
      this.getRows(offset, limit, offset, limit);
      buff += 'table is empty so we get all rows';
      return buff;
    }

    // Make a range of required rows
    var min = -1;
    var max = -1;

    for (var i = offset; i < (offset + limit); ++i) {
      if (this.fetchedRows[i] == undefined) {
        if (min == -1) {
          min = i;
        }
        max = i;
      }
    }

    // If we don't need any new row
    if (min == -1) {
      buff += 'no need to get new rows <br />\n';
      this.displayRows(offset, limit);
    } else {
      // We need to get new rows
      buff += 'we need to get rows '+min+' to '+ (max+1) +' <br />\n';
      this.getRows(min, max - min + 1, offset, limit);
    }
    return buff;
  },

  /**
    * Delete a row from the fetch cache, shifting the remaining rows accordingly.
    */
  deleteAndShiftRows: function(indx)
  {
    for(i in this.fetchedRows) {
      if(i >= indx)
      this.fetchedRows[i] = this.fetchedRows[''+(parseInt(i)+1)];
    }
  },

  /**
    * Debug method. Dumps the content of the fetch cache (row indexes only).
    */
  debugFetchedRows: function() {
    var buf = '';
    for (i in this.fetchedRows) {
      if (this.fetchedRows[i] != undefined) {
        buf += i+' ';
      }
    }
    return buf;
  },

  /**
    * Delete a row and redisplay the table.
    * @param indx The index of the row that must be deleted.
    */
  deleteRow: function(indx) {
    this.deleteAndShiftRows(indx);

    // Compute new refresh offset
    var newoffset = this.lastoffset;
    if(indx > this.totalRows - this.limit - 1) {
      newoffset -= 1;
    }
    if(newoffset <= 0) {
      newoffset = 1;
    }
    this.totalRows -= 1;
    if(this.totalRows < this.limit) {
      this.showRows(newoffset, this.totalRows);
    }
    else {
      this.showRows(newoffset, this.limit);
    }
    this.scroller.refreshScrollbar();
  }
});

////////////////////////////////////////////////////////////////////////
/* The class representing the dynamic scroller */
ASSScroller = Class.create({
  /**
    * @param table The ASSTable instance this scrollbar belongs to.
    * @param domNode The node where the scrollbar should be placed, should be a <div> inside a <td>
    *   next to the userlist table, containing another div inside. DOM element or identifier.
    * @todo Auto-create the inner div if it does not exist.
    */
  initialize: function(table, domNode)
  {
    this.table = table;
    this.domNode = $(domNode);
    this.advanceRTG = 1;
    this.timer = null;
    this.attachEventHandlers();
    this.refreshDelay = 800;
  },

  /**
    * Register this as an event handler for scrolling events. In order to pass this object to the
    * actual handling function, use a closure.
    * @todo Implement this as a standard DOM EventListener, not with closures.
    */
  attachEventHandlers: function()
  {
    Event.observe(this.domNode, 'scroll', this.makeScrollHandler(this));
  },

  /**
    * Creates a closure as the onscroll event handler, enclosing "this" as a variable.
    */
  makeScrollHandler: function(self)
  {
    return function() {
      self.onscroll();
    }
  },

  /**
    * Actual event handler for the scroll event.
    */
  onscroll: function()
  {
    if (this.timer != null) {
      clearTimeout(this.timer);
    }
    this.advanceRTG = this.computeScroll();

    this.timer = setTimeout(this.makeTimeoutHandler(this), this.refreshDelay);
  },

  /**
    * Computes the row number corresponding to the current scroll position.
    * @return The index of the row that should be at the top of the displayed data range.
    */
  computeScroll: function()
  {
    var h = this.domNode.scrollHeight - this.table.domNode.parentNode.offsetHeight;
    var y = this.domNode.scrollTop;
    var p = y / h;

    if(this.table.totalRows == -1 || this.table.totalRows <= this.table.limit) {
      var rtg = 1;
    } else {
      var rtg = Math.round((this.table.totalRows - this.table.limit) * p) + 1;
    }

    if ((rtg + this.table.limit) > this.table.totalRows) {
      rtg = this.table.totalRows - this.table.limit + 1;
    }
    if (rtg < 1) {
      rtg = 1;
    }

    return rtg;
  },

  // Closure
  makeTimeoutHandler: function(self)
  {
    return function() {
      self.applyScroll();
    }
  },

  applyScroll: function()
  {
    this.table.showRows(this.advanceRTG, this.table.limit);
    this.timer = null;
  },

  /**
    * Refreshes the scrollbar dimension/position according to reflect the currently displayed range.
    */
  refreshScrollbar: function()
  {
    if (this.table.totalRows < this.table.limit) {
      this.domNode.style.display = "none";
      return;
    }
    var raport = this.table.totalRows / this.table.limit;
    var outheight = this.table.domNode.parentNode.offsetHeight;
    var inheight = Math.round(outheight * raport);
    var scrollTop = Math.round(((this.table.lastoffset - 1) / (this.table.totalRows - this.table.limit)) * (inheight - outheight));

    this.domNode.style.height = outheight + "px";
    this.domNode.firstChild.style.height = inheight + "px";
    if (this.domNode.scrollTop != scrollTop) {
      this.domNode.scrollTop = scrollTop;
    }
    this.domNode.style.display = "block";
  }
});

////////////////////////////////////////////////////////////////////////
/* the class that deals with the filtering in a table */
ASSFilter = Class.create({
  initialize: function(table, filterNode)
  {
    this.table = table;
    this.filterNode = $(filterNode);
    this.filters = new Object();

    this.attachEventHandlers();
  },

  makeRefreshHandler: function(self)
  {
    return function() {
      self.refreshContent();
    }
  },

  attachEventHandlers: function()
  {
    var inputs = this.filterNode.getElementsByTagName('input');
    var selects = this.filterNode.getElementsByTagName('select');

    for(var i = 0; i < inputs.length; i++) {
      if (inputs[i].type == "text") {
        Event.observe(inputs[i], 'keyup', this.makeRefreshHandler(this));
      } else {
        //IE is buggy on "change" events for checkboxes and radios
        Event.observe(inputs[i], 'click', this.makeRefreshHandler(this));
      }
    }

    for(var i = 0; i < selects.length; i++) {
      Event.observe(selects[i], 'change', this.makeRefreshHandler(this));
    }
  },

  getFilters : function()
  {
    var inputs = this.filterNode.getElementsByTagName('input');
    this.filters = new Object();
    var existing = new Object();
    for(var prop in this.filters) {
      existing[prop] = true;
    }
    for (var i = 0; i < inputs.length; i++) {
      var key = inputs[i].name;
      if (inputs[i].type == "radio" || inputs[i].type == "checkbox" ) {
        if (inputs[i].checked) {
          this.filters[key] = inputs[i].value.strip();
        }
      } else {
        this.filters[key] = inputs[i].value.strip();
      }
    }

    var selects = this.filterNode.getElementsByTagName('select');
    for(var i = 0; i < selects.length; i++) {
      this.filters[selects[i].name] = selects[i].value.strip();
    }

    var filterString = "";
    for (key in this.filters) {
      if (!existing[key] && this.filters[key] != "") {
        filterString += '&' + key + '=' + this.filters[key];
      }
    }
    delete existing;

    return filterString;
  },

  /**
    * Refresh the table when the filters have changed.
    */
  refreshContent : function()
  {
    var newFilters = this.getFilters();
    if (newFilters == this.table.filters) {
      return;
    }
    this.table.totalRows = -1;
    this.table.fetchedRows = new Array();
    this.table.filters = newFilters;
    this.table.showRows(1, this.table.limit);
  }
});
