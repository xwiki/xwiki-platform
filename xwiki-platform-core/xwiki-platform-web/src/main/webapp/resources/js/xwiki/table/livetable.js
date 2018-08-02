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
var XWiki = (function(XWiki) {
// Start XWiki augmentation.
var widgets = XWiki.widgets = XWiki.widgets || {};

/**
  * The class representing an AJAX-populated live table.
  * It is (almost) independent of the underlying HTML markup, a function passed as an argument being
  * responsible with displaying the content corresponding to a row. Uses JSON for the response
  * encoding.
  */
XWiki.widgets.LiveTable = Class.create({
  /**
    * @param url The base address for requesting the table data.
    * @param domNodeName The node supposed to hold the data rows, should be a <tbody>. DOM element or
    *   identifier.
    * @param handler A javascript function called for displaying fetched rows. The function
    *   accepts a JSON-parsed object and returns a DOM <tr> node
    * @param options An object with options for the live table. Supported options:
    * <ul>
    * <li>"limit" the maximum number of row entries in the table</li>
    * <li>"maxPages" the maximum number of pages to display at the same time in the pagination section.</li>
    * <li>"selectedTags" the list of tags that should be selected initially in the tag cloud</li>
    * </ul>
    * @todo Make this a valid ARIA table: http://www.w3.org/TR/aria-role/#structural
    */
  initialize: function(url, domNodeName, handler, options)
  {
    options = options || {};

    // id of the root element that encloses this livetable
    this.domNodeName = domNodeName;

    // Remove, if present, the message that indicates the table cannot execute.
    // (It can since we are executing JavaScript).
    if ($(this.domNodeName).down('tr.xwiki-livetable-initial-message')) {
      $(this.domNodeName).down('tr.xwiki-livetable-initial-message').remove();
    }

    // id of the display element (the inner dynamic table) of this livetable
    // defined by convention as the root node id on which is appended "-display".
    // fallback on the unique "display1" id for backward compatibility.
    this.displayNode = $(domNodeName + "-display") || $('display1');

    // Nodes under which all forms controls (input, selects, etc.) will be filters for this table
    this.filtersNodes = [
          options.filterNodes     // Option that specifies an array of filter nodes. Each item is either a reference to a
                                  // DOM element or a String that is either the identifier of an element or a CSS selector.
       || $(options.filtersNode)  // Deprecated option (kept for backward compatibility)
       || $(domNodeName).down(".xwiki-livetable-display-filters") // Default filter node when none precised
    ].flatten().compact();
    this.filtersNodes = this.filtersNodes.collect(function(item) {
      // If it's a String then the item must be either the identifier of an element or a CSS selector.
      return typeof item === 'string' ? ($(item) || $(document.body).down(item)) : item;
    });

    // Array of nodes under which pagination for this livetable will be displayed.
    this.paginationNodes = options.paginationNodes || $(this.domNodeName).select(".xwiki-livetable-pagination");

     // Array of nodes under which a page size control will be displayed
    this.pageSizeNodes = options.pageSizeNodes || $(this.domNodeName).select(".xwiki-livetable-pagesize");

    this.action = options.action || "view"; // FIXME check if this can be removed safely.

    // Initialize table params that are used for permalinks
    this.limit = options.limit || 10;
    this.lastOffset = 1;
    $(this.domNodeName).select('th.sortable').each(function(el) {
        if (el.hasClassName('selected')) {
          this.selectedColumn = el;
        }
      },this);

    // Initialize the throttling delay, that is the delay a user can type a second filter key after a first one without
    // sending an AJAX request to the server.
    this.throttlingDelay = options.throttlingDelay || 0.5;

    // If permalinks is enable, load initial hash, using the URL hash if available or the default above
    this.permalinks = new LiveTableHash(this,(typeof options.permalinks == "undefined" || options.permalinks));

    // Get params from permalinks
    this.limit = this.permalinks.getLimit();
    var initialPage = this.permalinks.getPage();

    // Initialize page size control bounds
    if (typeof this.pageSizeNodes != "undefined") {
      this.pageSizer = new LiveTablePageSizer(this, this.pageSizeNodes, options.pageSizeBounds, this.limit);
    }

    // Initialize pagination
    if (typeof this.paginationNodes != "undefined") {
       this.paginator = new LiveTablePagination(this, this.paginationNodes, options.maxPages || 10);
    }

    // Initialize filters
    if (this.filtersNodes.length > 0) {
      this.filter = new LiveTableFilter(this, this.filtersNodes, this.permalinks.getFilters(), {
        throttlingDelay: this.throttlingDelay
      });
    }

    if ($(domNodeName + "-tagcloud")) {
      this.tags = options.selectedTags;
      this.tagCloud = new LiveTableTagCloud(this, domNodeName + "-tagcloud");
    }
    this.loadingStatus = $(this.domNodeName + '-ajax-loader') || $('ajax-loader');
    this.limitsDisplays = $(this.domNodeName).select('.xwiki-livetable-limits') || [];
    this.filters = "";
    this.handler = handler || function(){};
    this.totalRows = -1;
    this.fetchedRows = new Array();
    this.renderedRows = new Array();
    this.getUrl = url;
    this.sendReqNo = 0;
    this.recvReqNo = 0;

    // Initialize sort column and observe sort events
    this.observeSortableColumns(this.permalinks.getSortColumn(), this.permalinks.getSortDirection());

    this.currentOffset = (initialPage - 1) * this.limit + 1;

    // Show initial rows
    this.showRows(this.currentOffset, this.limit);
  },

  /**
   * Set the page size of the table and refresh the display
   * @param pageSize The new maximum number of rows to display per page
   **/
  setPageSize: function(pageSize)
  {
    this.limit = pageSize;
    this.showRows(1, this.limit);
  },

  /**
    * Initializes an AJAX request for retrieving some table data. Uses two ranges, one defines the
    * range that must be retrieved from the server, and one defines the range that should be
    * displayed. Two ranges are needed as some of the displayed rows can already be available from
    * a previous request.
    * @param reqOffset Starting request offset; the index of the first row that should be retrieved.
    * @param reqLimit Maximum number of rows to retrieve.
    * @param displayOffset Starting display offset; the index of the first row that should be displayed.
    * @param displayLimit Maximum number of rows to display.
    * @param delay An possible delay before firing the request to allow that request to be cancelled (used
    *          for submission throttling on filters)
    */
  getRows: function(reqOffset, reqLimit, displayOffset, displayLimit, delay)
  {
    var self = this;

    if (this.nextRequestTimeoutId) {
      // If a request was queued previously, cancel it
      window.clearTimeout(this.nextRequestTimeoutId);
      delete this.nextRequestTimeoutId;
    }

    var doRequest = function(){

      var url =  self.getUrl + '&offset='+reqOffset+'&limit='+reqLimit+'&reqNo='+ (++self.sendReqNo);
      if (self.filter) {
        self.filters = self.filter.serializeFilters();
        if (self.filters != undefined && self.filters != "") {
          url += self.filters;
        }
      }
      if (typeof self.tags != "undefined" && self.tags.length > 0) {
         self.tags.each(function(tag) {
            url += ("&tag=" + encodeURIComponent(tag.unescapeHTML()));
         });
      }
      url += self.getSortURLFragment();

      // Let code know the table is about to load new entries.
      // 1. Named event (for code interested by that table only)
      document.fire("xwiki:livetable:" + self.domNodeName + ":loadingEntries");
      // 2. Generic event (for code potentially interested in any livetable)
      document.fire("xwiki:livetable:loadingEntries", {
        "tableId" : self.domNodeName
      });

      var ajx = new Ajax.Request(url,
      {
        method: 'get',
        onComplete: function( transport ) {
          // Let code know loading is finished
          // 1. Named event (for code interested by that table only)
          document.fire("xwiki:livetable:" + self.domNodeName + ":loadingComplete", {
            "status" : transport.status
          });
          // 2. Generic event (for code potentially interested in any livetable)
          document.fire("xwiki:livetable:loadingComplete", {
            "status" : transport.status,
            "tableId" : self.domNodeName
          });

          self.loadingStatus.addClassName("hidden");
        },
        onSuccess: function( transport ) {
          var res = eval( '(' + transport.responseText + ')');

          if (res.reqNo < self.sendReqNo) {
            return;
          }

          self.recvReqNo = res.reqNo;

          if (self.tagCloud && res.matchingtags) {
            self.tagCloud.updateTagCloud(res.tags, res.matchingtags);
          }

          // Let code know new entries arrived
          // 1. Named event (for code interested by that table only)
          document.fire("xwiki:livetable:" + self.domNodeName + ":receivedEntries", {
            "data" : res
          });
          // 2. Generic event (for code potentially interested in any livetable)
          document.fire("xwiki:livetable:receivedEntries", {
            "data" : res,
            "tableId" : self.domNodeName
          });

          self.updateFetchedRows(res);
          self.displayRows(displayOffset, displayLimit);
        }
      });

    }

    // Make sure to set show the loading as soon as possible (instead of waiting the delay for the actual ajax request)
    // so that it really reflect the status of the livetable
    self.loadingStatus.removeClassName("hidden");

    if (typeof delay != 'undefined' && delay > 0) {
      // fire the request after a withdrawal period in which it can be cancelled
      this.nextRequestTimeoutId = Function.delay.call(doRequest, delay);
    }
    else {
      // no withdrawal period
      doRequest();
    }
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
    var object = this.displayNode;
    while (object.hasChildNodes()) {
      object.removeChild(object.firstChild);
    }
  },

  /**
    * Displays already fetched rows. Calls {@link #handler} for creating the XHTML elements, and
    * inserts them in {@link domNode}.
    * @param offset Starting offset; the index of the first row that should be displayed.
    * @param limit Maximum number of rows to display.
    */
  displayRows: function(offset, limit)
  {
    var f = offset + limit - 1;
    if (f > this.totalRows) f = this.totalRows;
    var off = (this.totalRows > 0) ? offset : 0;
    var msg = "<strong>" + off + "</strong> - <strong>" + f + "</strong> $services.localization.render('platform.livetable.paginationResultsOf') <strong>" + this.totalRows + "</strong>";
    msg = msg.toLowerCase();

    this.limitsDisplays.each(function(limitsDisplay) {
      limitsDisplay.innerHTML = "$services.localization.render('platform.livetable.paginationResults') " + msg;
    });
    this.clearDisplay();

    for (var i = off; i <= f; i++) {
      if (this.fetchedRows[i]) {
        if (this.renderedRows[i] === undefined) {
          this.renderedRows[i] = this.handler(this.fetchedRows[i], i, this);
        }
        var elem = this.renderedRows[i];
        this.displayNode.appendChild(elem);
        var memo = {
          "data": this.fetchedRows[i],
          "row":elem,
          "table":this,
          "tableId":this.domNodeName
        };
        // 1. Named event (for code interested by that table only)
        document.fire("xwiki:livetable:" + this.domNodeName + ":newrow", memo);
        // 2. Generic event (for code potentially interested in any livetable)
        document.fire("xwiki:livetable:newrow", memo);
      }
    }
    if (this.paginator) this.paginator.refreshPagination();

    // Let code know displaying is finished
    // 1. Named event (for code interested by that table only)
    document.fire("xwiki:livetable:" + this.domNodeName + ":displayComplete")
    // 2. Generic event (for code potentially interested in any livetable)
    document.fire("xwiki:livetable:displayComplete", {
      "tableId" : this.domNodeName
    });
  },

  /**
    * Fetch and display rows. This method checks the existing fetched data to determine which (if
    * any) rows should be fetched from the server, then forwards the call to {@link #displayRows}.
    * @param offset Starting offset; the index of the first row that should be displayed.
    * @param limit Maximum number of rows to display.
    * @param in case we need to fetch rows, an optional delay before the rows are actually fetched
    *         against the server (allows submission throttling)
    */
  showRows: function(offset, limit, delay)
  {
    this.lastOffset = offset;

    // Update permalinks
    this.permalinks.update();

    // This is some debugging string.
    var buff  = 'request to display rows '+offset+' to '+(offset+limit)+' <br />\n';

    // If no rows fetched yet, get all we need
    if (this.totalRows == -1) {
      this.getRows(offset, limit, offset, limit, delay);
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

    if(this.paginator) this.paginator.refreshPagination();

    return buff;
  },

  /**
    * Delete a row from the fetch cache, shifting the remaining rows accordingly.
    */
  deleteAndShiftRows: function(indx)
  {
    for(var i in this.fetchedRows) {
      if(i >= indx)
      this.fetchedRows[i] = this.fetchedRows[''+(parseInt(i)+1)];
    }
  },

  /**
    * Debug method. Dumps the content of the fetch cache (row indexes only).
    */
  debugFetchedRows: function() {
    var buf = '';
    for (var i in this.fetchedRows) {
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
    var newoffset = this.lastOffset;
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

    if (this.paginator) this.paginator.refreshPagination();

    // Let code know displaying is finished
    // 1. Named event (for code interested by that table only)
    document.fire("xwiki:livetable:" + this.domNodeName + ":displayComplete")
    // 2. Generic event (for code potentially interested in any livetable)
    document.fire("xwiki:livetable:displayComplete", {
      "tableId" : this.domNodeName
    });
  },

  /**
   * Return the URL fragment with sort parameters depending on the state of the table.
   */
  getSortURLFragment:function()
  {
    var column = this.getSortColumn();
    var fragment = "";
    if( column != null ) {
      fragment += "&sort=" + column;
      var direction = this.getSortDirection();
      if( direction != null ) {
        return fragment += "&dir=" + direction;
      }
    }
    return fragment;
  },

  /**
   * Return the name of the current sorted column, null if no sortable column is selected
   */
  getSortColumn: function()
  {
    if (!this.selectedColumn) {
      return null;
    }
    var a = this.selectedColumn.down("a");
    if (!a) {
      return null;
    }
    return this.getColumnNameAttribute(a);
  },
  
  /**
   * Return the attribute of a link element where the name of the column is stored. The "data-rel" attribute is
   * normally used, but for compatibility reason with skins using the XHTML1.0 syntax, it could end-up in the "rel"
   * attribute ('data-*' attributes are not valid in XHTML 1.0).
   */
  getColumnNameAttribute: function(element)
  {
    return element.hasAttribute('data-rel') ? element.getAttribute('data-rel') : element.getAttribute('rel');
  },

  /**
   * Return the sorting direction of the current sorted column, null if no sortable column is selected
   */
  getSortDirection: function()
  {
    if (!this.selectedColumn) {
      return null;
    }
    return (this.selectedColumn.hasClassName('desc') ? 'desc' : 'asc');
  },

  /**
   * Remove all the fetched data from the cache.
   */
  clearCache: function()
  {
    this.fetchedRows.clear();
    this.totalRows = -1;
  },

  /**
   * Iterate over the column headers that have the sortable class to observe sort changes when user clicks the column header.
   *
   * @param column the sort column to select initially
   * @param direction the sort direction for the sort column selected
   */
  observeSortableColumns: function(column, direction)
  {
    var self = this;
    $(this.domNodeName).select('th.sortable').each(function(el) {
      var colname = el.down("a") ? self.getColumnNameAttribute(el.down("a")) : null;
      if (colname == column) {
        self.selectedColumn = el;
        el.addClassName('selected');
        if (direction == "asc") {
          if (el.hasClassName('desc')) {
            el.removeClassName('desc');
          }
          el.addClassName("asc")
        } else {
          if (el.hasClassName('asc')) {
            el.removeClassName('asc');
          }
          el.addClassName("desc")
        }
      } else {
        if (el.hasClassName('selected')) {
          el.removeClassName('selected');
        }
        if (!el.hasClassName('desc') && !el.hasClassName('asc')) { // no order set in the HTML. Force desc
          el.addClassName('desc');
        }
      }
      Event.observe(el, "click", function(event) {
         var elem = event.element();
         if (!elem.hasClassName('sortable')) {
            elem = elem.up('th.sortable');
         }
         if (elem == null) {
            // This should never happen in real life, but better safe than sorry...
            return;
         }
         if (elem.hasClassName("selected")) { // Sort column already selected. Change direction
            var direction = elem.hasClassName("asc") ? "asc" : "desc";
            var newDirection = direction == "asc" ? "desc" : "asc";
            elem.removeClassName(direction);
            elem.addClassName(newDirection);
         }
         else { // sort column was not selected, do not change direction, just column
            if (self.selectedColumn){
              self.selectedColumn.removeClassName("selected");
            }
            elem.addClassName("selected");
            self.selectedColumn = elem;
         }
         self.clearCache(); //reset
         self.showRows(1, self.limit);
      });
   });
  },

  /**
   * Refresh the display of the livetable (clear the cache and fetch content again).
   * @since 9.5RC1
   */
  refresh: function() {
    var start = Math.max(this.lastOffset, 1);
    var end   = this.limit;
    this.clearCache();
    this.getRows(start, end, start, end);
  }

});

/**
 * Helper class to manage permalinks
 **/
var LiveTableHash = Class.create({
  /**
   * new LiveTableHash( table, enable )
   * @param table the livetable link to this hash
   * @param enable if false, the hash will be disabled (not updated)
   */
  initialize: function(table, enable)
  {
    this.table = table;
    this.enable = enable;
    this.loadFromHash();
  },

  /**
   * Returns the parameters for the current table from hash.
   * If permalink is disabled, or there is no parameter for the current table in hash, returns null
   */
  loadFromHash: function()
  {
    this.params = this.getTableParams();
    this.filters = new Object();

    if (!this.enable) return;

    var hashString = window.location.hash.substring(1);
    if (!hashString.blank()) {
      var tables = hashString.split("|");
      for (var i = 0; i < tables.length; i++) {
        var params = tables[i].toQueryParams();
        if (params["t"] == this.table.domNodeName) {
          for (var param in params) {
            if (param.length == 1 && "tplsd".indexOf(param) != -1) {
              this.params[param] = params[param];
            } else {
              this.filters[param] = params[param];
            }
          }
        }
      }
    }
  },

  /**
   * Returns a parameter map of current livetable state
   */
  getTableParams: function()
  {
    var result = new Object();
    result["t"] = this.table.domNodeName;
    result["p"] = ((this.table.lastOffset - 1) / this.table.limit) + 1;
    result["l"] = this.table.limit;
    if (this.table.getSortColumn() != null) {
      result["s"] = this.table.getSortColumn();
      result["d"] = this.table.getSortDirection();
    }
    return result;
  },

  /**
   * Get helpers, private
   */
  getParam: function(name) { return this.params[name]; },
  getIntParam: function(name) { return parseInt(this.params[name]); },

  /**
   * Return individual parameter from permlinks hash
   */
  getLimit: function() { return this.getIntParam("l"); },
  getPage: function() { return this.getIntParam("p"); },
  getSortColumn: function() { return this.getParam("s"); },
  getSortDirection: function() { return this.getParam("d"); },

  /**
   * Returns filters from permalink hash
   * Note: Currently filters are not kept/updated after initialization
   */
  getFilters: function(name)
  {
    return this.filters;
  },

  /**
   * Serialize permalink parameters to query string.
   * This function ensure serialization is always applied in the same order so that comparing strings is meaningful
   *
   * @param newParams Optional argument to serialize future parameters in place of current ones
   */
  serializeParams: function(newParams)
  {
    var params = $H((newParams) ? newParams : this.params);
    params = params.inject({}, function(params, pair) {
      params[pair.key] = encodeURIComponent(pair.value);
      return params;
    });

    var result = "t=#{t}&p=#{p}&l=#{l}".interpolate(params);
    if (params["s"]) {
      result += "&s=#{s}&d=#{d}".interpolate(params);
    }
    return result;
  },

  /**
   * Re-write location hash with current page and filters values
   * Nothing is done if the hash is disabled
   */
  update: function()
  {
    if (!this.enable) return;

    var params = this.getTableParams();
    var paramsString = this.serializeParams(params);
    var filterString = this.table.filter ? this.table.filter.serializeFilters() : "";

    var shouldUpdate = !filterString.blank() || (paramsString != this.serializeParams());

    if (shouldUpdate) {
      var currentHash = window.location.hash.substring(1);
      var tables = currentHash.split("|"), newHash = "";
      for (var i = 0; i < tables.length; i++) {
        if (tables[i].toQueryParams()["t"] != params["t"]) {
          // Don't override other tables params
          newHash += (tables[i] + "|");
        }
      }
      newHash += paramsString;
      newHash += filterString;

      this.params = params;
      this.filters = null; //better, but useless: this.filters = filterString.toQueryParams();
      window.location.hash = "#" + newHash;
    }
  }
});

  /**
 * Helper class to display pagination
 */
var LiveTablePagination = Class.create({
    initialize: function(table, domNodes, max)
    {
      this.table = table;
      var self = this;
      this.pagesNodes = [];
      domNodes.each(function(elem){
         self.pagesNodes.push(elem.down(".xwiki-livetable-pagination-content"));
      });
      this.max = max;
      $(this.table.domNodeName).select(".prevPagination").invoke("observe", "click", this.gotoPrevPage.bind(this));
      $(this.table.domNodeName).select(".nextPagination").invoke("observe", "click", this.gotoNextPage.bind(this));
    },
    refreshPagination: function()
    {
      var self = this;
      this.pagesNodes.each(function(elem){
         elem.innerHTML = "";
      });
      var pages = Math.ceil(this.table.totalRows / this.table.limit);
      var currentMax = (!this.max) ? pages : this.max;
      var currentPage = Math.floor( this.table.lastOffset / this.table.limit) + 1;
      var startPage = Math.floor(currentPage / currentMax) * currentMax - 1;
      // always display the first page
      if (startPage>1) {
         this.pagesNodes.each(function(elem){
             elem.insert(self.createPageLink(1, false));
         });
         if (startPage>2) {
            this.pagesNodes.invoke("insert", " ... ");
         }
      }
      // display pages
      for (var i=(startPage<=0) ? 1 : startPage;i<=Math.min(startPage + currentMax + 1, pages);i++) {
         var selected = (currentPage == i);
         this.pagesNodes.each(function(elem){
             elem.insert(self.createPageLink(i, selected));
         });
         this.pagesNodes.invoke("insert", " ");
      }
      // alwyas display the last page.
      if (i<pages) {
        if (i+1 < pages) {
          this.pagesNodes.invoke("insert", " ... ");
        }
        //this.pagesNodes.invoke("insert", pageSpan.clone());
        this.pagesNodes.each(function(elem){
             elem.insert(self.createPageLink(pages, false));
        });
      }
      if (currentPage <= 1) {
        this.pagesNodes.each(function(item) {
          if (!item.up().previous('.controlPagination')) {
            return;
          }
          var prevPage = item.up().previous('.controlPagination').down('.prevPagination');
          if (prevPage) {
            prevPage.addClassName('noPrevPagination').removeClassName('prevPagination');
          }
        });
      } else {
        this.pagesNodes.each(function(item) {
          if (!item.up().previous('.controlPagination')) {
            return;
          }
          var prevPage = item.up().previous('.controlPagination').down('.noPrevPagination');
          if (prevPage) {
            prevPage.addClassName('prevPagination').removeClassName('noPrevPagination');
          }
        });
      }
      if (currentPage >= pages) {
        this.pagesNodes.each(function(item) {
          if (!item.up().previous('.controlPagination')) {
            return;
          }
          var nextPage = item.up().previous('.controlPagination').down('.nextPagination');
          if (nextPage) {
            nextPage.addClassName('noNextPagination').removeClassName('nextPagination');
          }
        });
      } else {
        this.pagesNodes.each(function(item) {
          if (!item.up().previous('.controlPagination')) {
            return;
          }
          var nextPage = item.up().previous('.controlPagination').down('.noNextPagination');
          if (nextPage) {
            nextPage.addClassName('nextPagination').removeClassName('noNextPagination');
          }
        });
      }
    },
    createPageLink:function(page, selected) {
        var pageSpan = new Element("a", {'class':'pagenumber', 'href':'#'}).update(page);
        if (selected) {
           pageSpan.addClassName("selected");
        }
        var self = this;
        pageSpan.observe("click", function(ev){
            ev.stop();
            self.gotoPage(ev.element().innerHTML);
        });
        return pageSpan;
    },
    gotoPage: function(page)
    {
      this.table.showRows(((parseInt(page) - 1 )* this.table.limit) + 1, this.table.limit);
    },
    gotoPrevPage: function(ev) {
      ev.stop();
      var currentPage = Math.floor( this.table.lastOffset / this.table.limit) + 1;
      var prevPage = currentPage - 1;
      if (prevPage > 0) {
        this.table.showRows(((parseInt(prevPage) - 1) * this.table.limit) + 1, this.table.limit);
      }
    },
    gotoNextPage: function(ev) {
      ev.stop();
      var currentPage = Math.floor( this.table.lastOffset / this.table.limit) + 1;
      var pages = Math.ceil(this.table.totalRows / this.table.limit);
      var nextPage = currentPage + 1;
      if (nextPage <= pages) {
        this.table.showRows(((parseInt(nextPage) - 1) * this.table.limit) + 1, this.table.limit);
      }
    }
});

/**
 * Helper class to display the page size control
 **/
 var LiveTablePageSizer = Class.create({
   /**
    * Create a new instance
    * @param table The LiveTable
    * @param domNodes An array of nodes indicating where the controls will be created
    * @param pageSizeBounds The bounds specification for acceptable values (an array of min, max, step)
    * @param currentPageSize The currently selected page size.
    **/
  initialize: function(table, domNodes, pageSizeBounds, currentPageSize) {
    this.table = table;
    this.currentValue = currentPageSize;
    var bounds = pageSizeBounds || [];
    this.startValue = bounds[0] || 10;
    this.step = bounds[2] || 10;
    this.maxValue = bounds[1] || 100;

    var self = this;
    this.pageSizeNodes = [];
    domNodes.each(function(elem) {
      self.pageSizeNodes.push(elem.down(".xwiki-livetable-pagesize-content"));
    });

    this.pageSizeNodes.each(function(elem) {
      elem.insert(self.createPageSizeSelectControl());
    });
  },

  /**
   * Create the page size control using a select node and returns it
   * @return an Element containing the select
   **/
  createPageSizeSelectControl: function() {
    var select = new Element('select', {'class':'pagesizeselect'});
    for (var i=this.startValue; i<=this.maxValue; i += this.step) {
      var attrs = {'value':i, 'text':i};
      if (i == this.currentValue) {
        attrs.selected = true;
      } else {
        var prevStep = i - this.step;
        if (this.currentValue > prevStep && this.currentValue < i) {
          select.appendChild(new Element('option', {'value':this.currentValue, 'text':this.currentValue, selected:true}).update(this.currentValue));
        }
      }
      select.appendChild(new Element('option', attrs).update(i));
    }
    select.observe("change", this.changePageSize.bind(this));
    return select;
  },

  /**
   * Change the page size of the table
   **/
  changePageSize: function(event) {
    var newLimit =  parseInt($F(Event.element(event)));
    this.table.setPageSize(newLimit);
  }
});

/*
 * The class that deals with the filtering in a table
 */
var LiveTableFilter = Class.create({
  initialize: function(table, filterNodes, filters, options)
  {
    this.table = table;
    this.filterNodes = filterNodes;
    this.filters = filters || new Object();
    this.throttlingDelay = options.throttlingDelay || 0;

    this.inputs = this.filterNodes.invoke('select','input').flatten();
    this.selects = this.filterNodes.invoke('select','select').flatten();

    this.initializeFilters();

    this.attachEventHandlers();
  },

  /**
   * Initialize DOM values of the filters elements based on the passed map of name/value.
   */
  initializeFilters: function()
  {
    for (var i = 0; i < this.inputs.length; ++i) {
      var key = this.inputs[i].name;
      if ((this.inputs[i].type == "radio") || (this.inputs[i].type == "checkbox")) {
        var filter = this.filters[key];
        if (filter) {
          if (Object.isArray(filter)) {
            this.inputs[i].checked = (filter.indexOf(this.inputs[i].value.strip()) != -1);
          } else {
            this.inputs[i].checked = (filter == this.inputs[i].value.strip());
          }
        }
      } else {
        if (this.filters[key]) {
          this.inputs[i].value = this.filters[key];
        }
        this.applyActiveFilterStyle(this.inputs[i]);
      }
    }

    this.selects.each(function(select) {
      var values = this.filters[select.name];
      if (!Object.isArray(values)) {
        values = [values];
      }
      var selectedValues = [];
      for (var i = 0; i < select.options.length; i++) {
        var option = select.options[i];
        option.selected = values.indexOf(option.value) >= 0;
        if (option.selected) {
          selectedValues.push(option.value);
        }
      };
      values.each(function(value) {
        // Add missing values.
        if (typeof value === 'string' && selectedValues.indexOf(value) < 0) {
          var option = document.createElement('option');
          option.value = value;
          option.selected = true;
          option.update(value.escapeHTML());
          select.insert(option);
        }
      }, this);
      this.applyActiveFilterStyle(select);
    }, this);
  },

  serializeFilters: function()
  {
    // It's a shame we can't use prototype Form methods on non-form elements.
    // In the future, we need to have the livetable filters in a real form (for a degraded version w/o js)
    // Then we can write :
    // return Form.serializeElements(Form.getElements(this.domNodeName);
    var result = "";
    var filters = [this.inputs, this.selects].flatten();
    for (var i = 0; i < filters.length; i++) {
      var filter = filters[i];
      // Ignore filters with blank value if are not used as filters yet
      if (!this._isBlank(filter.getValue()) || !this._isBlank(this.filters[filter.name])) {
        if ((filter.type != "radio" && filter.type != "checkbox") || filter.checked) {
          result += ("&" + filter.serialize());
        }
      }
    }
    return result;
  },

  _isBlank: function(value) {
    var values = Object.isArray(value) ? value : [value];
    return values.every(function(value) {
      return typeof value !== 'string' || value.blank();
    });
  },

  attachEventHandlers: function()
  {
    var refreshHandler = this.refreshHandler.bind(this);
    for (var i = 0; i < this.inputs.length; ++i) {
      var input = this.inputs[i];
      var events = ['change'];
      if (input.type == "text") {
        events.push('keyup');
      } else {
        //IE is buggy on "change" events for checkboxes and radios
        events.push('click');
      }
      events.each(function(event) {
        Event.observe(input, event, refreshHandler);
      });
    }

    for (var i = 0; i < this.selects.length; ++i) {
      Event.observe(this.selects[i], 'change', refreshHandler);
    }

    // Allow custom filters to trigger filter change from non-native events
    document.observe("xwiki:livetable:" + this.table.domNodeName + ":filtersChanged", refreshHandler);
  },

  /**
   * Refresh event fired by filter changes
   */
  refreshHandler : function(event) {
    this.applyActiveFilterStyle(event.element());
    this.refreshContent();
  },

  /**
    * Refresh the table when the filters have changed.
    */
  refreshContent : function()
  {
    var newFilters = this.serializeFilters();
    if (newFilters == this.table.filters) {
      return;
    }
    this.table.totalRows = -1;
    this.table.fetchedRows = new Array();
    this.table.filters = newFilters;
    this.table.showRows(1, this.table.limit, this.throttlingDelay);
  },

  /**
   * Apply style to livetable filters that are applied
   */
  applyActiveFilterStyle: function(element) {
    if(element && element.tagName && ((element.tagName.toLowerCase() == "input" && element.type == "text") || element.tagName.toLowerCase() == "select")) {
      // The filter value can be a string, an array or null.
      var filterValue = $F(element);
      if (filterValue != null && filterValue.length) {
        element.addClassName('xwiki-livetable-filter-active');
      } else {
        element.removeClassName('xwiki-livetable-filter-active');
      }
    }
  }
});

/**
 * Helper class to filter on tags/display tags matching filters.
 */
var LiveTableTagCloud = Class.create({
   /**
    * Constructor.
    */
   initialize: function(table, domNodeName, tags) {
      this.table = table;
      this.domNode = $(domNodeName);
      this.cloudFilter = false;
      this.selectedTags = {};
      for (var i = 0; i < table.tags.size(); i++) {
        this.selectedTags[table.tags[i]] = {};
      }
      if (typeof tags == "array") {
         this.tags = tags;
         if (tags.length > 0) {
           this.updateTagCloud(tags);
         }
      }
   },

   /**
    * Tags cardinality map. Empty at first.
    */
   tags: [],

   /**
    * Tags matching the current filters
    */
   matchingTags: {},

   /**
    * Tags selected as filters
    */
   selectedTags: {},

   /**
    * Default popularity levels. Used as CSS class on the tag list items.
    */
   popularityLevels: ["notPopular", "notVeryPopular", "somewhatPopular", "popular", "veryPopular", "ultraPopular"],

   /**
    * Update the tag cloud with new tags values.
    * This is the only hook the table will call us from.
    */
   updateTagCloud: function(tags, matchingTags) {
      if (!this.hasTags && tags.length > 0) {
        this.tags = tags;
        this.map = this.buildPopularityMap(this.tags);
        this.hasTags = true;
        this.domNode.removeClassName("hidden");
      }
      // Normalize the list of matching tags (all lower case).
      this.matchingTags = Object.toJSON(matchingTags || {}).toLowerCase().evalJSON();
      this.displayTagCloud();
   },

   displayTagCloud: function(){
      this.domNode.down('.xwiki-livetable-tagcloud').innerHTML = "";
      var cloud = new Element("ol", {'class':'tagCloud'});
      var levels = this.map ? this.map.keys().sortBy(function(k){return parseInt(k)}).reverse() : [];
      var liClass;
      for (var i=0;i<this.tags.length;i++) {
         liClass = "";
         for (var j=0;j<levels.length;j++) {
            if (this.tags[i].count >= levels[j] || (j == (levels.length - 1))) {
               liClass = this.map.get(levels[j]);
               break;
            }
         }
         var tagLabel = this.tags[i].tag;
         var tagSpan = new Element("span").update(tagLabel.escapeHTML());
         var tag = new Element("li", {'class':liClass}).update(tagSpan);
         // Determine if the tag is selectable (matched) ignoring the case because multiple documents can be tagged with
         // the same tag but in different cases (e.g. tag, Tag, TAG etc.)
         if (typeof this.matchingTags[tagLabel.toLowerCase()] != "undefined") {
            tag.addClassName("selectable");
            Event.observe(tagSpan, "click", function(event) {
                var tag = event.element().up("li").down("span").innerHTML.unescapeHTML();
                event.element().up("li").toggleClassName("selected");
                if (event.element().up("li").hasClassName("selected")) {
                  self.selectedTags[tag] = {};
                }
                else {
                  delete self.selectedTags[tag];
                }
                self.table.tags = self.getSelectedTags();
                self.table.totalRows = -1;
                self.table.fetchedRows = new Array();
                self.table.showRows(1, self.table.limit);
            });
         }
         if (typeof this.selectedTags[tagLabel] == "object") {
            tag.addClassName("selected");
         }
         var self = this;
         tag.appendChild(document.createTextNode(" "));
         cloud.appendChild(tag);
      }
      this.domNode.down('.xwiki-livetable-tagcloud').appendChild(cloud);
   },

   getSelectedTags: function() {
      var result = new Array();
      this.domNode.select("li.selected").each(function(tag) {
         result.push(tag.down("span").innerHTML);
      });
      return result;
   },

   /**
    * Transform the cardinality map of tags in a map of CSS classes.
    * Ported to JS from XWiki.TagCloud for consistency.
    */
   buildPopularityMap:function(tags){
      var totalCount = 0;
      var minCount = 0;
      var maxCount = -1;
      tags.each(function(tag){
          totalCount += tag.count;
          if(tag.count < minCount || minCount === 0) {
              minCount = tag.count;
          }
          if(tag.count > maxCount || maxCount === -1) {
              maxCount = tag.count;
          }
      });
      var countAverage = totalCount / tags.length;
      var levelsHalf = this.popularityLevels.length / 2;
      var firstHalfCountDelta = countAverage - minCount;
      var secondHalfCountDelta = maxCount - countAverage;

      var firstHalfIntervalSize = firstHalfCountDelta / levelsHalf;
      var secondHalfIntervalSize = secondHalfCountDelta / levelsHalf;
      var previousPopularityMax = minCount;
      var intervalSize = firstHalfIntervalSize;
      var halfPassed = false;
      var count = 0;
      var currentPopularityMax;

      var popularityMap = new Hash();

      this.popularityLevels.each(function(level){
         count++;
         if(count > levelsHalf && !halfPassed) {
              intervalSize = secondHalfIntervalSize;
              halfPassed = true;
         }
         currentPopularityMax = previousPopularityMax + intervalSize;
         popularityMap.set(currentPopularityMax, level);
         previousPopularityMax = currentPopularityMax;
      });

      return popularityMap;
   }
});


// Trigger table loading when document and scripts are ready
function init() {
  document.fire("xwiki:livetable:loading");
}

(XWiki.isInitialized && init())
|| document.observe("xwiki:dom:loading", init);

// End XWiki augmentation.
return XWiki;
}(XWiki || {}));
