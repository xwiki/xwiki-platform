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
var ASSTable = Class.create();

ASSTable.prototype = {
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
    // TODO: msg.get
    $('showLimits').innerHTML = "Displaying rows from <strong>" + off + "</strong> to <strong>" + f + "</strong> out of <strong>" + this.totalRows + "</strong>";

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
}

////////////////////////////////////////////////////////////////////////
/* The class representing the dynamic scroller */
ASSScroller = Class.create();

ASSScroller.prototype = {
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
}

////////////////////////////////////////////////////////////////////////
/* the class that deals with the filtering in a table */
ASSFilter = Class.create();

ASSFilter.prototype = {
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
        Event.observe(inputs[i], 'change', this.makeRefreshHandler(this));
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
}

/* this represent a triple state checkbox */
MSCheckbox = Class.create();

MSCheckbox.prototype = {
  /**
    * @todo Make confirmations generic.
    * @todo msg.get
    * @todo Send the state number, or a generic map {state => sendValue}
    * @todo Configuration: automatic save, or just change the value.
    * @todo "Busy" icon when saving.
    * @todo Changing the value should change the cache, not invalidate it.
    * @todo The new state should be taken from the response, not just increment it.
    * @todo Make this a valid ARIA checkbox: http://www.w3.org/TR/aria-role/#checkbox
    */
  initialize: function(domNode, right, saveUrl, defaultState, table, idx)
  {
    this.table = table;
    this.idx = idx;
    this.domNode = $(domNode);
    this.right = right;
    this.saveUrl = saveUrl;
    this.defaultState = defaultState;
    this.state = defaultState;
    this.states = [0,1,2]; // 0 = inherit; 1 = allow, 2 == deny
    this.nrstates = this.states.length;
    this.images = ["$xwiki.getSkinFile('icons/rights-manager/none.png')","$xwiki.getSkinFile('icons/rights-manager/allow.png')","$xwiki.getSkinFile('icons/rights-manager/deny1.png')"];
    this.labels = ['','',''];

    this.draw(this.state);
    this.attachEvents();
  },

  /**
    * @todo Draw with the current this.state, don't pass as an argument.
    */
  draw: function(state)
  {
    //remove child nodes
    while (this.domNode.childNodes.length > 0) {
      this.domNode.removeChild(this.domNode.firstChild);
    }
    //add new image
    var img = document.createElement('img');
    img.src = this.images[state];
    this.domNode.appendChild(img);
    //add label
    if (this.labels[state] != '') {
      var la = document.createElement('span');
      la.appendChild(document.createTextNode(this.labels[state]));
      this.domNode.appendChild(la);
    }
  },

  next: function()
  {
    this.state = (this.state + 1) % this.nrstates;
    if (this.table != undefined) {
      // TODO: Just update the cache, don't invalidate the row, once the rights are as stored as an
      // array, and not as a string.
      delete this.table.fetchedRows[this.idx];
    }
    this.draw(this.state);
  },

  createClickHandler: function(self)
  {
    return function() {
      if (self.req) {
        return;
      }
      // TODO: put $msg.get messages!!!
      var nxtst = (self.state + 1) % self.nrstates;
      if (self.right == "admin" && nxtst == 2) {
        if (!confirm("You are about to deny the admin right for this user. Continue?")) {
          return;
        }
      } else if (self.right == "admin" && nxtst == 0) {
        if (!confirm("You are about to clear the admin right for this user. Continue?")) {
          return;
        }
      }

      var action = "";
      if (nxtst == 0) {
        action = "clear";
      } else if (nxtst == 1) {
        action = "allow";
      } else {
        action = "deny";
      }

      // Compose the complete URI
      var url = self.saveUrl + "&action=" + action + "&right=" + self.right;

      self.req = new Ajax.Request(url,
      {
        method: 'get',
        onSuccess: function() {
          self.next();
        },
        onFailure: function() {
          alert("$msg.get('platform.core.rightsManagement.ajaxFailure')");
        },
        onComplete: function() {
          delete self.req;
        }
      });
    }
  },

  attachEvents: function()
  {
    Event.observe(this.domNode, 'click', this.createClickHandler(this));
  }
}

/**
  * user list element creator. Used in adminusers.vm.
  */
function displayUsers(row, i, table)
{
  var userurl = row.userurl;
  var usersaveurl = row.usersaveurl;
  var userinlineurl = row.userinlineurl;
  var wikiname = row.wikiname;
  var docurl = row.docurl;

  var tr = document.createElement('tr');
  if (i % 2 == 0) {
    tr.className = "even";
  } else {
    tr.className = "odd";
  }

  var username = document.createElement('td');
  if (wikiname == "local") {
    var a = document.createElement('a');
    a.href = userurl;
    a.appendChild(document.createTextNode(row.username));
    username.appendChild(a);
  } else {
    username.appendChild(document.createTextNode(row.username));
  }
  username.className="username";
  tr.appendChild(username);

  var firstname = document.createElement('td');
  firstname.appendChild(document.createTextNode(row.firstname) );
  tr.appendChild(firstname);

  var lastname = document.createElement('td');
  lastname.appendChild(document.createTextNode(row.lastname) );
  tr.appendChild(lastname);

  var manage = document.createElement('td');
  manage.className = "manage";

  if (wikiname == "local") {
    //edit user
    var edit = document.createElement('img');
    edit.src = '$xwiki.getSkinFile("icons/rights-manager/edit.png")';
    edit.title = '$msg.get("edit")';
    Event.observe(edit, 'click', editUserOrGroup(userinlineurl, usersaveurl, docurl));
    edit.className = 'icon-manage';
    manage.appendChild(edit);

    //delete group
    var del = document.createElement('img');

    if(row.grayed == "true") {
      del.src = '$xwiki.getSkinFile("icons/rights-manager/clearg.png")';
      del.className = 'icon-manageg';
    } else {
      del.src = '$xwiki.getSkinFile("icons/rights-manager/clear.png")';
      Event.observe(del, 'click', deleteUserOrGroup(i, table, row.fullname, "user"));
      del.className = 'icon-manage';
    }
    del.title = '$msg.get("delete")';
    manage.appendChild(del);
  }

  tr.appendChild(manage);
  return tr;
}

/** group list element creator **/
function displayGroups(row, i, table)
{
  var userurl = row.userurl;
  var userinlineurl = row.userinlineurl;
  var usersaveurl = row.usersaveurl;
  var wikiname = row.wikiname;
  var docurl = row.docurl;

  var tr = document.createElement('tr');

  if (i % 2 == 0) {
    tr.className = "even";
  } else {
    tr.className = "odd";
  }

  var username = document.createElement('td');
  if (wikiname == "local") {
    var a = document.createElement('a');
    a.href = userurl;
    a.appendChild( document.createTextNode( row.username ) );
    username.appendChild( a );
  } else {
    username.appendChild(document.createTextNode(row.username));
  }
  username.className="username";
  tr.appendChild(username);

  var members = document.createElement('td');
  if (wikiname == "local") {
    members.appendChild(document.createTextNode(row.members));
  } else {
    members.appendChild(document.createTextNode("-"));
  }
  tr.appendChild(members);

  var manage = document.createElement('td');
  manage.className = "manage";

  if (wikiname == "local") {
    //delete group
    var del = document.createElement('img');
    del.src = '$xwiki.getSkinFile("icons/rights-manager/clear.png")';
    del.title = '$msg.get("delete")';
    Event.observe(del, 'click', deleteUserOrGroup(i, table, row.fullname, "group"));
    del.className = 'icon-manage';

    //edit user
    var edit = document.createElement('img');
    edit.src = '$xwiki.getSkinFile("icons/rights-manager/edit.png")';
    edit.title = '$msg.get("edit")';
    Event.observe(edit, 'click', editUserOrGroup(userinlineurl, usersaveurl, docurl));
    edit.className = 'icon-manage';

    manage.appendChild(edit);
    manage.appendChild(del);
  }

  tr.appendChild(manage);

  return tr;
}

/** group members list element creator **/
function displayMembers(row, i, table)
{
  var tr = document.createElement('tr');
  if(i % 2 == 0) tr.className = "even";
  else tr.className = "odd";

  var membername = document.createElement("td");

  if (row.wikiname == "local") {
    var a = document.createElement("a");
    a.href = row.memberurl;
    a.appendChild(document.createTextNode(row.fullname));
    membername.appendChild(a);
  } else {
    membername.appendChild(document.createTextNode(row.fullname));
  }
  membername.className="username";
  tr.appendChild(membername);
  
  /* do not allow to delete users from a group when not in inline mode */
  if(table.action == "inline")
  {
    var membermanage = document.createElement("td");
    membermanage.className = "manage";
  
    var del = document.createElement('img');

    if (row.grayed == "true") {
       del.src = '$xwiki.getSkinFile("icons/rights-manager/clearg.png")';
       del.className = 'icon-manageg';
    } else {
       del.src = '$xwiki.getSkinFile("icons/rights-manager/clear.png")';
       Event.observe(del, 'click', deleteMember(i, table, row.fullname, row.docurl));
       del.className = 'icon-manage';
    }
    del.title = '$msg.get("delete")';
    membermanage.appendChild(del);
    tr.appendChild(membermanage);
  }

  return tr;
}

/**
  * User and groups list element creator.
  * Used in adminglobalrights.vm, adminspacerights.vm, editrights.vm.
  * @todo allows and denys should be arrays, not strings.
  */
function displayUsersAndGroups(row, i, table, idx)
{
  var userurl = row.userurl;
  var uorg = table.json.uorg;
  var allows = row.allows;
  var denys = row.denys;
  var saveUrl = "?xpage=saverights&clsname=" + table.json.clsname + "&fullname=" + row.fullname + "&uorg=" + uorg;

  var tr = document.createElement('tr');

  if (i % 2 == 0) {
    tr.className = "even";
  } else {
    tr.className = "odd";
  }

  var username = document.createElement('td');
  if(row.wikiname == "local") {
    var a = document.createElement('a');
    a.href = userurl;
    a.appendChild( document.createTextNode( row.username ) );
    username.appendChild( a );
  } else {
    username.appendChild(document.createTextNode(row.username));
  }

  username.className = "username";
  tr.appendChild(username);
  activeRights.each(function(right) {
    if(right)
    {
        var td = document.createElement('td');
        td.className = "rights";
        var r = 0;
        if (allows.indexOf(right) >= 0) {
           r = 1;
        } else if (denys.indexOf(right) >= 0) {
           r = 2;
        }
        var chbx = new MSCheckbox(td, right, saveUrl, r, table, i);
        tr.appendChild(td);
    }
  });

  return tr;
}

////////////////////////////////////////////////////////////////

function editUserOrGroup(userinlineurl, usersaveurl, userredirecturl)
{
  return function() {
    window.lb = new Lightbox(userinlineurl, usersaveurl, userredirecturl);
  }
}


//function to delete a user with ajax
function deleteUserOrGroup(i, table, docname, uorg)
{
  return function() {
    var url = "?xpage=deleteuorg&docname=" + docname;
    if(uorg == "user") {
      if (confirm('$msg.get("rightsmanager.confirmdeleteuser")'.replace('__name__', docname))) {
        new Ajax.Request(url, {
            method: 'get',
            onSuccess: function(transport) {
              table.deleteRow(i);
            }
        });
      }
    }
    else {
      if (confirm('$msg.get("rightsmanager.confirmdeletegroup")'.replace('__name__', docname))) {
        new Ajax.Request(url, {
            method: 'get',
            onSuccess: function(transport) {
              table.deleteRow(i);
            }
        });
      }
    }
  }
}

//deletes a member of a group (only the object)
function deleteMember(i, table, docname, docurl)
{
  return function() {
    var url = docurl + "?xpage=deletegroupmember&fullname=" + docname;
    if (confirm('$msg.get("rightsmanager.confirmdeletemember")')) {
      new Ajax.Request(url, {
        method: 'get',
        onSuccess: function(transport) {
          table.deleteRow(i);
        }
      });
    }
  }
}

function makeAddHandler(url, saveurl, redirecturl)
{
  return function() {
    window.lb = new Lightbox(url, saveurl, redirecturl);
  }
}