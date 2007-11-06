/* the class representing the table in wich we will get the users through ajax - made by Cristian Vrabie;
  it can use other html elements than table, with a function (passed as getHandler) to display the content;   */
var ASSTable = Class.create();

ASSTable.prototype = {

  initialize: function( url, limit, domNode, scrollNode, filterNode, getHandler, hasFilters )
  {
    this.domNode = $( domNode );
    this.scroller = new ASSScroller( this, scrollNode );
    if(hasFilters)
    this.filter = new ASSFilter( this, filterNode );
    this.hasFilters = hasFilters;
    this.filters = "";
    this.getHandler = getHandler;
    this.totalRows = -1;
    this.fetchedRows = new Array();
    this.limit = limit;
    this.getUrl = url;
    this.lastoffset = 1;

    //show initial rows
    this.showRows( 1, limit );
  },

  getRows: function( offset, limit, doffset, dlimit )
  {
    var url =  this.getUrl + '&offset='+offset+'&limit='+limit;

    if( this.hasFilters )
    {
      this.filters = this.filter.getFilters();
      if(this.filters != "" && this.filters != undefined) 
      url += this.filters;	
    }

    var pivot = this;

    $('ajax-loader').style.display = "block";

    new Ajax.Request(url,
    {
      method: 'get',
      onComplete: function( transport ) {
        $('ajax-loader').style.display = "none";
      },

      onSuccess: function( transport ) {
        $('ajax-loader').style.display = "none";
        var res = eval( '(' + transport.responseText + ')');
        if(res.totalrows <= res.returnedrows)
          pivot.scroller.domNode.style.display = "none";
        else
          pivot.scroller.domNode.style.display = "block";
        pivot.updateFetchedRows( res );
        pivot.displayRows( doffset, dlimit );
      }
    });
  },
    
  updateFetchedRows: function( json )
  {
    this.json = json;
    this.totalRows = json.totalrows;
    for( var i = json.offset; i < json.offset + json.returnedrows; i++)
       this.fetchedRows[i] = json.rows[i-json.offset];
  },
    
  clearDisplay: function()
  {
    var object = this.domNode;
    while (object.hasChildNodes())
    {
      object.removeChild(object.firstChild);
    }
  },
    
  displayRows: function( offset, limit ) { 
  	
    var f = offset + limit - 1;
    if(f > this.totalRows) f = this.totalRows;
    var off = (this.totalRows > 0 ) ? offset : 0;
    $('showLimits').innerHTML = "Displaying rows from <strong>" + off + "</strong> to <strong>" + f + "</strong> out of <strong>" + this.totalRows + "</strong>";

    this.clearDisplay();

    for( var i = offset; i < (offset + limit); i++)
    {           
      var elem = this.getHandler( this.fetchedRows[i], i, this );
      this.domNode.appendChild( elem );
    }

    var raport = this.totalRows / limit;
    var outheight = this.domNode.parentNode.offsetHeight; 
    // the header?
    var inheight = Math.round(outheight * raport) + 10;
      
    this.scroller.domNode.style.height = outheight + "px";
    this.scroller.domNode.firstChild.style.height = inheight + "px";
  },

  showRows: function( offset, limit )
  {
    this.lastoffset = offset;
    var buff  = 'request to display rows '+offset+' to '+(offset+limit)+' <br />\n';

    //if no rows fetched get all we need
    if( this.totalRows == -1 )
    {
      this.getRows( offset, limit, offset, limit );
      buff += 'table is empty so we get all rows';
      return buff;
    }
            
    //make a range of required rows
    var min = -1;
    var max = -1;

    for( var i = offset; i < (offset + limit); i++ )
      if( this.fetchedRows[i] == undefined )
      {
        if(min == -1)  min = i;
        max = i;
      }
        
    //if we don't need any new row
    if(min == -1)
    {
      buff += 'no need to get new rows <br />\n';
      this.displayRows( offset, limit );
    }
    
    //we need get new rows
    else
    {
      buff += 'we need to get rows '+min+' to '+(max+1)+' <br />\n';
      this.getRows( min, max - min + 1, offset, limit );
    }
    return buff;        
  },

  deleteAndShiftRows: function(indx)
  {
    for(i in this.fetchedRows)
    {
      if(i >= indx) 
      this.fetchedRows[i] = this.fetchedRows[''+(parseInt(i)+1)];
    }
  },

  debugFetchedRows: function()
  {
    var buf = '';
    for(i in this.fetchedRows)
    if( this.fetchedRows[i] != undefined )
    buf += i+' ';
    return buf;
  }, 


  deleteRow: function( indx )
  { 
    this.deleteAndShiftRows(indx);

    //compute new refresh offset
    var newoffset = this.lastoffset;
    if(indx > this.totalRows - this.limit - 1)
    newoffset -= 1;
    if(newoffset <= 0)
    newoffset = 1;
    this.totalRows -= 1;
    this.showRows(newoffset, this.limit);
    this.scroller.refreshScrollbar();
  } /* ,

  compareStrings: function( s1, s2 )
  {

    s1 = s1.toLowerCase();
    s2 = s2.toLowerCase();
    var l1 = s1.length;
    var l2 = s2.length;
    var lower = (l1 < l2) ? l1 : l2;

    for(i = 0; i < lower; i++){
      if(s1.charAt(i) == s2.charAt(i))
      continue;
      else if(s1.charAt(i) < s2.charAt(i))
      return -1;
      else
        return 1;
    }
    return 0;
  },

  searchAddPosition: function( start, end, fullname )
  {
    if(start >= end)
      return start;

    var pos = Math.floor((start + end) / 2);
    var comp = this.compareStrings(fullname,this.fetchedRows[pos].username);
    if( comp == 0 )
     return pos + 1;
    else if(comp == -1)
     return this.searchAddPosition(start, pos-1, fullname);
    else
      return this.searchAddPosition(pos+1, end, fullname);
  },

  getMax: function()
  {
    var max = 0;
    for(i in this.fetchedRows){
      var ii = parseInt(i);
      if(ii > max)
      max = ii;
    }
    return max;
  },

  shiftRight: function( poz, max )
  {
    if(max == null)
    max = this.getMax();
    for(i = max; i >= poz; i -= 1)
    this.fetchedRows[i+1] = this.fetchedRows[i];
  },

  addRow: function( fullname )
  {
     var max = this.getMax();
    var pos = this.searchAddPosition(0, max, fullname);
    this.shiftRight(pos, max);
    this.fetchedRows[pos] = eval( json );
    var rest = 0;
    var start = pos - Math.round(this.limit / 2);
    if(start < 1 ) { 
    	start = 1; 
    	rest = Math.round(this.limit / 2) - pos; 
    }
    var end = pos + Math.round(this.limit / 2) + rest;
    if(end > this.totalRows) end = this.totalRows;
    this.showRows(start, end); 
  },

  createAddHandler: function(pivot)
  {
    return function(){
      //
    }
  } */
}

////////////////////////////////////////////////////////////////////////
/* the class representing the dynamic scroller */
ASSScroller = Class.create();

ASSScroller.prototype = {

  initialize: function( table, domNode )
  {
    this.table = table; 
    this.domNode = $(domNode);
    this.advanceRTG = 1;
    this.timer = null; 
    this.linkEvent();
  },
    
  linkEvent: function()
  {
    Event.observe( this.domNode, 'scroll', this.makeScrollHandler( this ) );
  },
    
  makeScrollHandler: function( pivot )
  {
    return function()
    {
      pivot.onscroll();
    }
  },
    
  computeScroll: function( )
  {
    var h = $('scrollbar1').scrollHeight - 100;
    var y = $('scrollbar1').scrollTop;
    var p = y / h;

    if(this.table.totalRows == -1)
      var rtg = 1;
    else
      var rtg = Math.round(this.table.totalRows * p); 
                       
    if( (rtg + this.table.limit) > this.table.totalRows )
      rtg = this.table.totalRows - this.table.limit + 1;
    
    if( rtg < 1 ) rtg = 1;
  
    return rtg;
  },
    
  applyscroll: function( )
  {
    this.table.showRows( this.advanceRTG, this.table.limit );
    this.timer = null;
  },
    
    //closure
  makeTimeoutHandler: function( pivot )
  {
    return function()
    {
      pivot.applyscroll();
    }
  },
    
  onscroll: function()
  {
    this.advanceRTG = this.computeScroll(); 
                
    if( this.timer == null )
    this.timer = setTimeout( this.makeTimeoutHandler( this ), 800 );
    else
    {
      //////
    }
  },

  refreshScrollbar: function( )
  { 
    var raport = this.table.totalRows / this.table.limit;
    var outheight = this.table.domNode.parentNode.offsetHeight; 
    var inheight = Math.round(outheight * raport);
      
    this.domNode.style.height = outheight + "px";
    this.domNode.firstChild.style.height = inheight + "px";
  }
    
}

////////////////////////////////////////////////////////////////////////
/* the class that deals with the filtering in a table */

ASSFilter = Class.create();

ASSFilter.prototype = {
  initialize: function( table, filterNode)
  {
    this.table = table;
    this.filterNode = $(filterNode);
    this.filters = new Object();
        
    this.linkEvents();
  },
    
  makeRefreshHandler: function( pivot )
  {
    return function()
    {
      pivot.refreshContent();
    }
  },
    
  linkEvents : function()
  {
    var inputs = this.filterNode.getElementsByTagName('input');
    var selects = this.filterNode.getElementsByTagName('select');
        
    for(var i = 0; i < inputs.length; i++)
    Event.observe(inputs[i], 'keyup', this.makeRefreshHandler(this));
            
    for(var i = 0; i < selects.length; i++)
    Event.observe(selects[i], 'change', this.makeRefreshHandler(this));
  },
    
    
  getFilters : function()
  {
    var inputs = this.filterNode.getElementsByTagName('input');
    for(var i = 0; i < inputs.length; i++) 
    {
      var key = inputs[i].name;
      this.filters[key] = trim(inputs[i].value);
    }

    var selects = this.filterNode.getElementsByTagName('select');
    for(var i = 0; i < selects.length; i++)
    {
      this.filters[selects[i].name] = trim(selects[i].options[selects[i].selectedIndex].value);
    }
      
    var filterString = "";
    for(key in this.filters)
    if(key != "extend" && this.filters[key] != "") filterString += '&' + key + '=' + this.filters[key];
                
    return filterString;
  },
    
  refreshContent : function()
  {
    this.table.filters = this.getFilters();
        
    this.table.totalRows = -1;
    this.table.fetchedRows = new Array();
    this.table.showRows(1, this.table.limit);
  }
    
}

/* the class that deals with the filtering in a table */
/* this represent a triple state checkbox */

MSCheckbox = Class.create();

MSCheckbox.prototype = {

  initialize: function( domNode, childId,  defaultState )
  {
    this.domNode = $(domNode);
    this.childId = childId;
    this.defaultState = defaultState;
    this.state = defaultState;
    this.states = [0,1,2]; // 0 = none; 1 = allow, 2 == deny
    this.nrstates = this.states.length;
    this.images = ["$xwiki.getSkinFile("icons/rights-manager/none.png")","$xwiki.getSkinFile("icons/rights-manager/allow.png")","$xwiki.getSkinFile("icons/rights-manager/deny1.png")"];
    this.labels = ['','',''];
        // buttons with actions upon checkboxes
    this.buttons = new Array();

    this.draw(this.state);
    this.attachEvents();
  },

  draw: function(state)
  {
                //remove image
    if(this.domNode.childNodes.length > 0) 
    this.domNode.removeChild( this.domNode.firstChild );
                //remove label
    if(this.domNode.childNodes.length > 0)
    this.domNode.removeChild( this.domNode.lastChild );
                //add new image
    var img = document.createElement('img');
    img.src = this.images[ state ];
    img.id = this.childId;
    this.domNode.appendChild( img );
                //add label
    if( this.labels[ state ] != '')
    {
      var la = document.createElement('span');
      la.appendChild( document.createTextNode(this.labels[state]));
      this.domNode.appendChild( la );
    }
  },

  next: function()
  {
    this.state = (this.state + 1) % this.nrstates;
    this.draw(this.state);
  },

  back: function()
  {
    this.draw(this.prevState);
  },

  createClickHandler: function( pivot )
  {
    return function()
    {
      pivot.next();
      for(var i = 0; i < pivot.buttons.length; i++)
      pivot.buttons[i].notifyChange();
    }
  },

  equalize: function()
  {
    this.defaultState = this.state;
    this.notifyButtons();
  },

  notifyButtons : function()
  {
    for(var i = 0; i < this.buttons.length; i++)
    this.buttons[i].notifyChange();
  },

  attachEvents: function()
  {
    Event.observe( this.domNode, 'click', this.createClickHandler(this));
  },
  
  attachButton : function(b)
  {
    this.buttons.push(b);
  }
}

////////////////////////////////////////////////////////////////////////
/* class that represents an 'action' button    */
MButton = Class.create();

MButton.prototype = {
  initialize : function() {
    //
  },

  init: function(domNode, type, table, indx, checkboxes)
  {
    this.domNode = $(domNode);
    this.type = type;
    this.table = table;
    this.indx = indx;
    this.checkboxes = checkboxes;

    this.draw(); 
    this.attachCheckboxes();
    this.attachHandler();
  },
    
  attachHandler : function()
  {
    Event.observe(this.domNode.firstChild, 'click', this.createHandler(this));
  },

  attachCheckboxes : function()
  {
    for(var i = 0; i < this.checkboxes.length; i++) 
    this.checkboxes[i].attachButton(this); 
  },

  checkEqual : function()
  {
    for(var i = 0; i < this.checkboxes.length; i++)
    if(this.checkboxes[i].state != this.checkboxes[i].defaultState)
    return false;
    return true;
  },

  checkClear : function()
  {
    for(var i = 0; i < this.checkboxes.length; i++)
    if(this.checkboxes[i].state != 0)
    return false;
    return true;
  }
}


MSaveButton = Class.create();

MSaveButton.prototype = Object.extend(new MButton(), {

  initialize : function(domNode, table, indx, checkboxes)
  {
    this.ready = false;
    this.init(domNode, 'save', table, indx, checkboxes);
  },

  draw : function()
  {
    var img = document.createElement('img');
    img.src = '$xwiki.getSkinFile("icons/rights-manager/saveg.png")';
    img.alt = "";
    img.className = "icon-manage";
    this.domNode.appendChild(img);
  },
    
  createHandler : function(pivot)
  {
    return function()
    {  
      if(pivot.ready)
      {
        var allows = "";
        var denys = "";
        var j = 0, k = 0;
        for(var i = 0; i < pivot.checkboxes.length; i++)
        {
          if(pivot.checkboxes[i].state == 1)
          {
            if(j > 0)  allows += ",";
            allows += pivot.checkboxes[i].childId.substring(0, pivot.checkboxes[i].childId.indexOf('_'));
            j++; 
          }
      
          else if(pivot.checkboxes[i].state == 2)
          {
            if(k > 0) denys += ",";
            denys += pivot.checkboxes[i].childId.substring(0, pivot.checkboxes[i].childId.indexOf('_'));
            k++;
          }      
        }

        var url = "?xpage=saverights";
        url += "&fullname=" + pivot.table.fetchedRows[pivot.indx].fullname + "&clsname=" + pivot.table.json.clsname + "&uorg=" + pivot.table.json.uorg + "&allows=" + allows + "&denys=" + denys;

        new Ajax.Request(url,
        {
          method: 'get',
          onSuccess: function()
          {
            pivot.table.fetchedRows[pivot.indx].allows = allows;
            pivot.table.fetchedRows[pivot.indx].denys = denys;
            for(var i = 0; i < pivot.checkboxes.length; i++)
            pivot.checkboxes[i].equalize();
          }
        }); 
      }
    }
  },

  notifyChange : function()
  {
    if(! this.checkEqual())
    {
      this.ready = true;
      this.domNode.firstChild.src = '$xwiki.getSkinFile("icons/rights-manager/save.png")';
                //icon-manage-enabled
    }
    else
    {
      this.ready = false;
      this.domNode.firstChild.src = '$xwiki.getSkinFile("icons/rights-manager/saveg.png")';
                //icon-manage-disabled
    }
  }
});


MRevertButton = Class.create();

MRevertButton.prototype = Object.extend(new MButton(), {

  initialize : function(domNode, table, indx, checkboxes)
  {
    this.ready = false;
    this.init(domNode, 'revert', table, indx, checkboxes);
  },

  draw : function()
  {
    var img = document.createElement('img');
    img.src = '$xwiki.getSkinFile("icons/rights-manager/revertg.png")';
    img.alt = "";
    img.className = "icon-manage";
    this.domNode.appendChild(img);
  },
    
  createHandler : function(pivot)
  {
    return function()
    {  
      if(pivot.ready)
      {
        for(var i = 0; i < pivot.checkboxes.length; i++)
        {
          pivot.checkboxes[i].state = pivot.checkboxes[i].defaultState;
          pivot.checkboxes[i].draw(pivot.checkboxes[i].state);
          pivot.checkboxes[i].notifyButtons();
        }
      }
    }
  },

  notifyChange : function()
  {
    if(! this.checkEqual())
    {
      this.ready = true;
      this.domNode.firstChild.src = '$xwiki.getSkinFile("icons/rights-manager/revert.png")';
      //icon-manage-enabled
    }
    else
    {
      this.ready = false;
      this.domNode.firstChild.src = '$xwiki.getSkinFile("icons/rights-manager/revertg.png")';
      //icon-manage-disabled
    }
  }
});


MClearButton = Class.create();

MClearButton.prototype = Object.extend(new MButton(), {

  initialize : function(domNode, table, indx, checkboxes)
  {
    this.ready = true;
    this.init(domNode, 'clear', table, indx, checkboxes);
    this.notifyChange(); // verify if all checkboxes are clear to disable the clear button
  },

  draw : function()
  {
    var img = document.createElement('img');
    img.src = '$xwiki.getSkinFile("icons/rights-manager/clear.png")';
    img.alt = "";
    img.className = "icon-manage";
    this.domNode.appendChild(img);
  },
    
  createHandler : function(pivot)
  {
    return function()
    {  
      if(pivot.ready)
      {
        for(var i = 0; i < pivot.checkboxes.length; i++)
        {
          pivot.checkboxes[i].state = 0;
          pivot.checkboxes[i].draw(pivot.checkboxes[i].state);
        }
        pivot.checkboxes[pivot.checkboxes.length - 1].notifyButtons();
      }
    }
  },

  notifyChange : function()
  {
    if(this.checkClear())
    {
      this.ready = false;
      this.domNode.firstChild.src = '$xwiki.getSkinFile("icons/rights-manager/clearg.png")';
                //icon-manage-disabled
    }
    else
    {
      this.ready = true;
      this.domNode.firstChild.src = '$xwiki.getSkinFile("icons/rights-manager/clear.png")';
                //icon-manage-disabled
    }
  }
});


/** user list element creator **/
function displayUsers( row, i, table)
{
  var userurl = row.userurl;
  var usersaveurl = row.usersaveurl;
  var userinlineurl = row.userinlineurl;
  var wikiname = row.wikiname;
  var docurl = row.docurl;
                
  var tr = document.createElement('tr'); 
  if(i % 2 == 0)  tr.className = "even";
  else tr.className = "odd";
        
  var username = document.createElement('td');
  if(wikiname == "local")
  {
    var a = document.createElement('a');
    a.href = userurl;
    a.appendChild( document.createTextNode( row.username ) );
    username.appendChild( a );
  }
  else
    username.appendChild( document.createTextNode( row.username ) );
  
  tr.appendChild(username);
            
  var firstname = document.createElement('td');
  firstname.appendChild(document.createTextNode(row.firstname) );
  tr.appendChild(firstname);
          
  var lastname = document.createElement('td');
  lastname.appendChild(document.createTextNode(row.lastname) );
  tr.appendChild(lastname);
            
  var manage = document.createElement('td');
  manage.className = "manage";
  
  if(wikiname == "local")
  {
    //edit user
    var edit = document.createElement('img');
    edit.src = '$xwiki.getSkinFile("icons/rights-manager/edit.png")';
    edit.title = '$msg.get("edit")';
    Event.observe(edit, 'click', editUserOrGroup(userinlineurl, usersaveurl, docurl));
    edit.className = 'icon-manage';
    manage.appendChild(edit);

    //delete group
    var del = document.createElement('img');
    del.src = '$xwiki.getSkinFile("icons/rights-manager/clear.png")';
    del.title = '$msg.get("delete")';
    Event.observe(del, 'click', deleteUserOrGroup(i, table, row.fullname));
    del.className = 'icon-manage';
    manage.appendChild(del);
  }
  tr.appendChild(manage);
  return tr;
}

/** group list element creator **/
function displayGroups( row, i, table)
{
  var userurl = row.userurl;
  var userinlineurl = row.userinlineurl;
  var usersaveurl = row.usersaveurl;
  var wikiname = row.wikiname;
  var docurl = row.docurl;     
         
  var tr = document.createElement('tr'); 
  
  if(i % 2 == 0) tr.className = "even";
  else tr.className = "odd";
        
  var username = document.createElement('td');
  if(wikiname == "local")
  {
    var a = document.createElement('a');
    a.href = userurl;
    a.appendChild( document.createTextNode( row.username ) );
    username.appendChild( a );
  }
  else
    username.appendChild( document.createTextNode( row.username ) );

  tr.appendChild(username);
  
  var members = document.createElement('td');
  if(wikiname == "local")
   members.appendChild(document.createTextNode(row.members));
  else
    members.appendChild(document.createTextNode("-"));
  tr.appendChild(members);
                        
  var manage = document.createElement('td');
  manage.className = "manage";
  
  if(wikiname == "local")
  {
    //delete group
    var del = document.createElement('img');
    del.src = '$xwiki.getSkinFile("icons/rights-manager/clear.png")';
    del.title = '$msg.get("delete")';
    Event.observe(del, 'click', deleteUserOrGroup(i, table, row.fullname));
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
function displayMembers( row, i, table )
{
  var tr = document.createElement('tr');
  if(i % 2 == 0) tr.className = "even";
  else tr.className = "odd";
        
  var membername = document.createElement("td");
  
  if(row.wikiname == "local")
  {
      var a = document.createElement("a");
      a.href = row.memberurl;
      a.appendChild(document.createTextNode(row.fullname));
      membername.appendChild(a);
  }
  else
     membername.appendChild(document.createTextNode(row.fullname));
        
  var membermanage = document.createElement("td");
  membermanage.className = "manage";
  var del = document.createElement('img');
  del.src = '$xwiki.getSkinFile("icons/rights-manager/clear.png")';
  del.title = '$msg.get("delete")';
  Event.observe(del, 'click', deleteMember(i, table, row.fullname, row.docurl));
  del.className = 'icon-manage';
  membermanage.appendChild(del);

  tr.appendChild(membername);
  tr.appendChild(membermanage);
  
  return tr;
}


/** user and groups list element creator **/
function displayUsersAndGroups( row, i, table )
{ 
  var userurl = row.userurl;
  var uorg = table.json.uorg;
  var allows = row.allows;
  var denys = row.denys;
  var objs = new Array(); //array with checkboxes objects
  var tr = document.createElement('tr');
 
  if(i % 2 == 0) tr.className = "even";
  else tr.className = "odd";
  
  var username = document.createElement('td');
  if(row.wikiname == "local")
  {
    var a = document.createElement('a');
    a.href = userurl;
    a.appendChild( document.createTextNode( row.username ) );
    username.appendChild( a );
  }
  else
    username.appendChild( document.createTextNode( row.username ) );
  
  tr.appendChild(username);

  var view = document.createElement('td');
  view.className = "rights";
  var r = 0;
  if(allows.indexOf("view") >= 0) r = 1;
  else if(denys.indexOf("view") >= 0) r = 2;
  var chbx1 = new MSCheckbox(view, "view_"+i, r);
  tr.appendChild(view);
  objs.push(chbx1);
        
  var comment = document.createElement('td');
  comment.className = "rights";
  r = 0;
  if(allows.indexOf("comment") >= 0) r = 1;
  else if(denys.indexOf("comment") >= 0) r = 2;
  var chbx2 = new MSCheckbox(comment, "comment_"+i, r);
  tr.appendChild(comment);
  objs.push(chbx2);
        
  var edit = document.createElement('td');
  edit.className = "rights";
  r = 0;
  if(allows.indexOf("edit") >= 0) r = 1;
  else if(denys.indexOf("edit") >= 0) r = 2;
  var chbx3 = new MSCheckbox(edit, "edit_"+i, r);
  tr.appendChild(edit);
  objs.push(chbx3);
        
  var del = document.createElement('td');
  del.className = "rights";
  r = 0;
  if(allows.indexOf("delete") >= 0)  r = 1;
  else if(denys.indexOf("delete") >= 0) r = 2;
  var chbx4 = new MSCheckbox(del, "delete_"+i, r);
  tr.appendChild(del);
  objs.push(chbx4);
        
  if(table.json.reg == true)
  {
    var register = document.createElement('td');
    register.className = "rights";
    r = 0;
    if(allows.indexOf("register") >= 0)  r = 1;
    else if(denys.indexOf("register") >= 0) r = 2;
    var chbx5 = new MSCheckbox(register, "register_"+i, r);
    tr.appendChild(register);
    objs.push(chbx5);
  }
        
  if(table.json.admin == true)
  {
    var admin = document.createElement('td');
    admin.className = "rights";
    r = 0;
    if(allows.indexOf("admin") >= 0) r = 1;
    else if(denys.indexOf("admin") >= 0) r = 2;
    var chbx6 = new MSCheckbox(admin, "admin_"+i, r);
    tr.appendChild(admin);
    objs.push(chbx6);
  }
        
  if(table.json.progr == true)
  {
    var progr = document.createElement('td');
    progr.className = "rights";
    r = 0;
    if(allows.indexOf("programming") >= 0) r = 1;
    else if(denys.indexOf("programming") >= 0) r = 2;
    var chbx7 = new MSCheckbox(progr, "programming_"+i, r);
    tr.appendChild(progr);
    objs.push(chbx7);
  }
        
  var manage = document.createElement('td');
  manage.className = "manage";
        
  var spansave = document.createElement('span');
  var spanrevert = document.createElement('span');
  var spanclear = document.createElement('span');

  manage.appendChild(spansave);
  manage.appendChild(spanrevert);
  manage.appendChild(spanclear);

   //save rights
  var save = new MSaveButton(spansave, table, i, objs);
  var revert = new MRevertButton(spanrevert, table, i, objs);
  var clear = new MClearButton(spanclear, table, i, objs);
                
  tr.appendChild(manage);
        
  return tr;
}

////////////////////////////////////////////////////////////////

function editUserOrGroup(userinlineurl, usersaveurl, userredirecturl) 
{
  return function()
  {
    window.lb = new Lightbox(userinlineurl, usersaveurl, userredirecturl);
  }
}


//function to delete a user with ajax
function deleteUserOrGroup(i, table, docname)
{
  return function()
  {
    var url = "?xpage=deleteuorg&docname=" + docname;
    if(confirm('$msg.get("rightsmanager.confirmdeleteuserorgroup")')) {
      new Ajax.Request(url, {
        method: 'get',
        onSuccess: function(transport) {
          table.deleteRow(i);
        }
      });
    }
  }
}

//deletes a member of a group (only the object)
function deleteMember(i, table, docname, docurl)
{
  return function()
  {
    var url = docurl + "?xpage=deletegroupmember&fullname=" + docname;
    if(confirm('$msg.get("rightsmanager.confirmdeletemember")')) {
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
  return function()
  {
    window.lb = new Lightbox(url, saveurl, redirecturl);
  }
}


//utility function
function trim(str)
{
  while (str.substring(0,1) == ' ')
  str = str.substring(1, str.length);
  while (str.substring(str.length-1, str.length) == ' ')
  str = str.substring(0,str.length-1);
  return str;
}


//////////////////////////////////////////////////
