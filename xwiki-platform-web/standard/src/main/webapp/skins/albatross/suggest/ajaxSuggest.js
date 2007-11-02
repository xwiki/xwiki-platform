

var useXWKns;

if (useXWKns)
{
	if (typeof(xwk) == "undefined")
		xwk = {}
	_xwk = xwk;
}
else
{
	_xwk = this;
}


if (typeof(_xwk.ajaxSuggest) == "undefined")
	_xwk.ajaxSuggest = {}


_xwk.ajaxSuggest = function (fld, param)
{

	this.fld = fld;

	if (!this.fld)
		return false;
	
	
	
	
	// init variables
	//
	this.sInput 		= "";
	this.nInputChars 	= 0;
	this.aSuggestions 	= [];
	this.iHighlighted 	= 0;
	
	// parameters object
	//
	this.oP = (param) ? param : {};
	
	// defaults	
	//
	if (!this.oP.minchars)									this.oP.minchars = 1;
	if (!this.oP.method)									this.oP.meth = "get";
	if (!this.oP.varname)									this.oP.varname = "input";
	if (!this.oP.className)									this.oP.className = "ajaxsuggest";
	if (!this.oP.timeout)									this.oP.timeout = 2500;
	if (!this.oP.delay)									this.oP.delay = 500;
	if (!this.oP.offsety)									this.oP.offsety = -5;
	if (!this.oP.shownoresults)								this.oP.shownoresults = true;
	if (!this.oP.noresults)									this.oP.noresults = "No results!";
	if (!this.oP.maxheight && this.oP.maxheight !== 0)		this.oP.maxheight = 250;
	if (!this.oP.cache && this.oP.cache != false)			this.oP.cache = false;
	if (this.oP.seps)                                       this.seps = this.oP.seps;
	else													this.seps = "";
	
	
	
	
	// set keyup handler for field
	// and prevent autocomplete from client
	//
	var pointer = this;
	
	// NOTE: not using addEventListener because UpArrow fired twice in Safari
	//_xwk.DOM.addEvent( this.fld, 'keyup', function(ev){ return pointer.onKeyPress(ev); } );
	
	this.fld.onkeypress 	= function(ev){ return pointer.onKeyPress(ev); }
	this.fld.onkeyup 		= function(ev){ return pointer.onKeyUp(ev); }
	
	this.fld.setAttribute("autocomplete", "off");
}



_xwk.ajaxSuggest.prototype.onKeyPress = function(ev)
{
	
	var key = (window.event) ? window.event.keyCode : ev.keyCode;



	// set responses to keydown events in the field
	// this allows the user to use the arrow keys to scroll through the results
	// ESCAPE clears the list
	// TAB sets the current highlighted value
	//
	var RETURN = 13;
	var TAB = 9;
	var ESC = 27;
	
	var bubble = true;

	switch(key)
	{

		case RETURN: {
		        if(this.aSuggestions.length == 1) {
		        	this.setHighlight(1);
		        }
		     	this.setHighlightedValue();
				bubble = false;
			}
			break;


		case ESC:
			this.clearSuggestions();
			break;
	}

	return bubble;
}



_xwk.ajaxSuggest.prototype.onKeyUp = function(ev)
{
	var key = (window.event) ? window.event.keyCode : ev.keyCode;
	


	// set responses to keydown events in the field
	// this allows the user to use the arrow keys to scroll through the results
	// ESCAPE clears the list
	// TAB sets the current highlighted value
	//

	var ARRUP = 38;
	var ARRDN = 40;
	
	var bubble = true;

	switch(key)
	{


		case ARRUP:
			this.changeHighlight(key);
			bubble = false;
			break;


		case ARRDN:
			this.changeHighlight(key);
			bubble = false;
			break;
		
		
		default: {
		    //if there are separators in the input string,
		    // get suggestions only for the text after the last separator
		    if(this.seps) 
		    {
		        var lastIndx = -1;
				for(var i = 0; i < this.seps.length; i++)
				 	if(this.fld.value.lastIndexOf(this.seps.charAt(i)) > lastIndx)
				 		lastIndx = this.fld.value.lastIndexOf(this.seps.charAt(i));
				if(lastIndx == -1)
				    this.getSuggestions(this.fld.value);
				else 
				    this.getSuggestions(this.fld.value.substring(lastIndx+1));
		    }
			else 
				this.getSuggestions(this.fld.value);
		}
	}

	return bubble;
	

}

	        
_xwk.ajaxSuggest.prototype.getSuggestions = function (val)
{
	
	// if input stays the same, do nothing
	//
	if (val == this.sInput)
		return false;
	
	
	// input length is less than the min required to trigger a request
	// reset input string
	// do nothing
	//
	if (val.length < this.oP.minchars)
	{
		this.sInput = "";
		return false;
	}
	
	
	// if caching enabled, and user is typing (ie. length of input is increasing)
	// filter results out of aSuggestions from last request
	//
	if (val.length>this.nInputChars && this.aSuggestions.length && this.oP.cache)
	{
		var arr = [];
		for (var i=0;i<this.aSuggestions.length;i++)
		{
			if (this.aSuggestions[i].value.substr(0,val.length).toLowerCase() == val.toLowerCase())
				arr.push( this.aSuggestions[i] );
		}
		
		
		this.sInput = val;
		this.nInputChars = val.length;
		this.aSuggestions = arr;
		
		this.createList(this.aSuggestions);
		
		
		
		return false;
	}
	else
	// do new request
	//
	{
	    this.sInput = val;
		this.nInputChars = val.length;
		
		var pointer = this;
		clearTimeout(this.ajID);
		this.ajID = setTimeout( function() { pointer.doAjaxRequest() }, this.oP.delay );
	}
  
	return false;
}





_xwk.ajaxSuggest.prototype.doAjaxRequest = function ()
{
	
	var pointer = this;
	
	// create ajax request
	var url = this.oP.script+this.oP.varname+"="+escape(this.fld.value);
	var meth = this.oP.meth;
	
	var onSuccessFunc = function (req) { pointer.setSuggestions(req) };
	var onErrorFunc = function (status) { alert("AJAX error: "+status); };

	var myAjaxReq = new _xwk.AjaxReq();
	myAjaxReq.makeRequest( url, meth, onSuccessFunc, onErrorFunc );
}





_xwk.ajaxSuggest.prototype.setSuggestions = function (req)
{
	this.aSuggestions = [];
	
	if (this.oP.json)
	{
		var jsondata = eval('(' + req.responseText + ')');
		
		for (var i=0;i<jsondata.results.length;i++)
		{
			this.aSuggestions.push(  { 'id':jsondata.results[i].id, 'value':jsondata.results[i].value, 'info':jsondata.results[i].info }  );
		}
	}
	else
	{

		var xml = req.responseXML;
	
		// traverse xml
		//
		var results = xml.getElementsByTagName('results')[0].childNodes;

		for (var i=0;i<results.length;i++)
		{
			if (results[i].hasChildNodes()) {
				this.aSuggestions.push(  { 'id':results[i].getAttribute('id'), 'value':results[i].childNodes[0].nodeValue, 'info':results[i].getAttribute('info') }  );
			}
		}
	
	}
	
	this.idAs = "as_"+this.fld.id;
	

	this.createList(this.aSuggestions);

}



_xwk.ajaxSuggest.prototype.createList = function(arr)
{
	var pointer = this;
	
	
	// get rid of old list
	// and clear the list removal timeout
	//
	_xwk.DOM.removeElement(this.idAs);
	this.killTimeout();
	
	// if no results, and shownoresults is false, do nothing
	if (arr.length == 0 && !this.oP.shownoresults)
		return false;
		
	// create holding div
	//
	var div = _xwk.DOM.createElement("div", {id:this.idAs, className:this.oP.className});	
	
	var hcorner = _xwk.DOM.createElement("div", {className:"as_corner"});
	var hbar = _xwk.DOM.createElement("div", {className:"as_bar"});
	var header = _xwk.DOM.createElement("div", {className:"as_header"});
	header.appendChild(hcorner);
	header.appendChild(hbar);
	div.appendChild(header);
	
	
	
	
	// create and populate ul
	//
	var ul = _xwk.DOM.createElement("ul", {id:"as_ul"});
	
	
	
	
	// loop throught arr of suggestions
	// creating an LI element for each suggestion
	//
	for (var i=0;i<arr.length;i++)
	{
		// format output with the input enclosed in a EM element
		// (as HTML, not DOM)
		//
		
		var val = arr[i].value;
		var st = val.toLowerCase().indexOf( this.sInput.toLowerCase() );
		var output = val.substring(0,st) + "<em>" + val.substring(st, st+this.sInput.length) + "</em>" + val.substring(st+this.sInput.length);
		
		
		var span = _xwk.DOM.createElement("span", {}, output, true);
		/*if (arr[i].info != "")
		{
			var br			= _xwk.DOM.createElement("br", {});
			span.appendChild(br);
			var small		= _xwk.DOM.createElement("small", {}, arr[i].info);
			span.appendChild(small);
		}*/
		
		var a = _xwk.DOM.createElement("a", { href:"#" });
				
		var tl 	= _xwk.DOM.createElement("span", {className:"tl"}, " ");
		var tr 	= _xwk.DOM.createElement("span", {className:"tr"}, " ");
		a.appendChild(tl);
		a.appendChild(tr);
		
		a.appendChild(span);
		
		a.name = i+1;
		a.onclick = function () { pointer.setHighlightedValue(); return false; }
		a.onmouseover = function () { pointer.setHighlight(this.name); }
		
		var li 	= _xwk.DOM.createElement(  "li", {}, a  );
		
		ul.appendChild( li );
	}
	
	
	// no results
	//
	if (arr.length == 0)
	{
		var li 			= _xwk.DOM.createElement(  "li", {className:"as_warning"}, this.oP.noresults  );
		
		ul.appendChild( li );
		
	}
	
	
	div.appendChild( ul );
	
	
	var fcorner = _xwk.DOM.createElement("div", {className:"as_corner"});
	var fbar = _xwk.DOM.createElement("div", {className:"as_bar"});
	var footer = _xwk.DOM.createElement("div", {className:"as_footer"});
	footer.appendChild(fcorner);
	footer.appendChild(fbar);
	div.appendChild(footer);
	
	
	
	// get position of target textfield
	// position holding div below it
	// set width of holding div to width of field
	//
	var pos = _xwk.DOM.getPos(this.fld);
	
	div.style.left 		= pos.x + "px";
	
	div.style.top 		= (pos.y + this.fld.offsetHeight + this.oP.offsety) + "px";
	
	div.style.width 	= this.fld.offsetWidth + "px";
	
	
	
	// set mouseover functions for div
	// when mouse pointer leaves div, set a timeout to remove the list after an interval
	// when mouse enters div, kill the timeout so the list won't be removed
	//
	div.onmouseover 	= function(){ pointer.killTimeout() }
	div.onmouseout 		= function(){ pointer.resetTimeout() }


	// add DIV to document
	//
	document.getElementsByTagName("body")[0].appendChild(div);
	
	
	
	// currently no item is highlighted
	//
	this.iHighlighted = 0;
	
	
	
	
	
	
	// remove list after an interval
	//
	var pointer = this;
	this.toID = setTimeout(function () { pointer.clearSuggestions() }, this.oP.timeout);
}




_xwk.ajaxSuggest.prototype.changeHighlight = function(key)
{	
	var list = _xwk.DOM.getElement("as_ul");
	if (!list)
		return false;
	
	var n;

	if (key == 40)
		n = this.iHighlighted + 1;
	else if (key == 38)
		n = this.iHighlighted - 1;
	
	
	if (n > list.childNodes.length)
		n = list.childNodes.length;
	if (n < 1)
		n = 1;
	
	
	this.setHighlight(n);
}



_xwk.ajaxSuggest.prototype.setHighlight = function(n)
{
	var list = _xwk.DOM.getElement("as_ul");
	if (!list)
		return false;
	
	if (this.iHighlighted > 0)
		this.clearHighlight();
	
	this.iHighlighted = Number(n);
	
	list.childNodes[this.iHighlighted-1].className = "as_highlight";


	this.killTimeout();
}


_xwk.ajaxSuggest.prototype.clearHighlight = function()
{
	var list = _xwk.DOM.getElement("as_ul");
	if (!list)
		return false;
	
	if (this.iHighlighted > 0)
	{
		list.childNodes[this.iHighlighted-1].className = "";
		this.iHighlighted = 0;
	}
}


_xwk.ajaxSuggest.prototype.setHighlightedValue = function ()
{
	if (this.iHighlighted)
	{
		if(this.sInput == "" && this.fld.value == "")
			this.sInput = this.fld.value = this.aSuggestions[ this.iHighlighted-1 ].value;
		else {
				if(this.seps) 
				{
				 	var lastIndx = -1;
				 	for(var i = 0; i < this.seps.length; i++)
				 		if(this.fld.value.lastIndexOf(this.seps.charAt(i)) > lastIndx)
				 			lastIndx = this.fld.value.lastIndexOf(this.seps.charAt(i));
				    if(lastIndx == -1)
				    	this.sInput = this.fld.value = this.aSuggestions[ this.iHighlighted-1 ].value;
				    else 
				    {
				    	this.fld.value = this.fld.value.substring(0, lastIndx+1) + this.aSuggestions[ this.iHighlighted-1 ].value;
				 	    this.sInput = this.fld.value.substring(lastIndx+1);
				 	} 
				}
				else
					this.sInput = this.fld.value = this.aSuggestions[ this.iHighlighted-1 ].value;
		}
		
		this.fld.focus();
		/*
		// move cursor to end of input (safari)
		//
		if (this.fld.selectionStart)
			this.fld.setSelectionRange(this.sInput.length, this.sInput.length);*/
		

		this.clearSuggestions();
		
		// pass selected object to callback function, if exists
		//
		if (typeof(this.oP.callback) == "function")
			this.oP.callback( this.aSuggestions[this.iHighlighted-1] );
				
		//there is a hidden input
		if(this.fld.id.indexOf("_suggest") > 0) {
			var hidden_id = this.fld.id.substring(0, this.fld.id.indexOf("_suggest"));
			var hidden_inp = document.getElementById(hidden_id);
				
			if(hidden_inp)
			 	hidden_inp.value = this.aSuggestions[ this.iHighlighted-1 ].info;
			 			
		}
	
	}
}






_xwk.ajaxSuggest.prototype.killTimeout = function()
{
	clearTimeout(this.toID);
}

_xwk.ajaxSuggest.prototype.resetTimeout = function()
{
	clearTimeout(this.toID);
	var pointer = this;
	this.toID = setTimeout(function () { pointer.clearSuggestions() }, 1000);
}







_xwk.ajaxSuggest.prototype.clearSuggestions = function ()
{
	
	this.killTimeout();
	
	var ele = _xwk.DOM.getElement(this.idAs);
	var pointer = this;
	if (ele)
	{
		var fade = new _xwk.Fader(ele,1,0,250,function () { _xwk.DOM.removeElement(pointer.idAs) });
	}
}










// AJAX PROTOTYPE _____________________________________________


if (typeof(_xwk.AjaxReq) == "undefined")
	_xwk.AjaxReq = {}



_xwk.AjaxReq = function ()
{
	this.req = {};
	this.isIE = false;
}



_xwk.AjaxReq.prototype.makeRequest = function (url, meth, onComp, onErr)
{
	
	if (meth != "POST")
		meth = "GET";
	
	this.onComplete = onComp;
	this.onError = onErr;
	
	var pointer = this;
	
	// branch for native XMLHttpRequest object
	if (window.XMLHttpRequest)
	{
		this.req = new XMLHttpRequest();
		this.req.onreadystatechange = function () { pointer.processReqChange() };
		this.req.open("GET", url, true); //
		this.req.send(null);
	// branch for IE/Windows ActiveX version
	}
	else if (window.ActiveXObject)
	{
		this.req = new ActiveXObject("Microsoft.XMLHTTP");
		if (this.req)
		{
			this.req.onreadystatechange = function () { pointer.processReqChange() };
			this.req.open(meth, url, true);
			this.req.send();
		}
	}
}


_xwk.AjaxReq.prototype.processReqChange = function()
{
	
	// only if req shows "loaded"
	if (this.req.readyState == 4) {
		// only if "OK"
		if (this.req.status == 200)
		{
			this.onComplete( this.req );
		} else {
			this.onError( this.req.status );
		}
	}
}










// DOM PROTOTYPE _____________________________________________


if (typeof(_xwk.DOM) == "undefined")
	_xwk.DOM = {}




_xwk.DOM.createElement = function ( type, attr, cont, html )
{
	var ne = document.createElement( type );
	if (!ne)
		return false;
		
	for (var a in attr)
		ne[a] = attr[a];
		
	if (typeof(cont) == "string" && !html)
		ne.appendChild( document.createTextNode(cont) );
	else if (typeof(cont) == "string" && html)
		ne.innerHTML = cont;
	else if (typeof(cont) == "object")
		ne.appendChild( cont );

	return ne;
}





_xwk.DOM.clearElement = function ( id )
{
	var ele = this.getElement( id );
	
	if (!ele)
		return false;
	
	while (ele.childNodes.length)
		ele.removeChild( ele.childNodes[0] );
	
	return true;
}









_xwk.DOM.removeElement = function ( ele )
{
	var e = this.getElement(ele);
	
	if (!e)
		return false;
	else if (e.parentNode.removeChild(e))
		return true;
	else
		return false;
}





_xwk.DOM.replaceContent = function ( id, cont, html )
{
	var ele = this.getElement( id );
	
	if (!ele)
		return false;
	
	this.clearElement( ele );
	
	if (typeof(cont) == "string" && !html)
		ele.appendChild( document.createTextNode(cont) );
	else if (typeof(cont) == "string" && html)
		ele.innerHTML = cont;
	else if (typeof(cont) == "object")
		ele.appendChild( cont );
}









_xwk.DOM.getElement = function ( ele )
{
	if (typeof(ele) == "undefined")
	{
		return false;
	}
	else if (typeof(ele) == "string")
	{
		var re = document.getElementById( ele );
		if (!re)
			return false;
		else if (typeof(re.appendChild) != "undefined" ) {
			return re;
		} else {
			return false;
		}
	}
	else if (typeof(ele.appendChild) != "undefined")
		return ele;
	else
		return false;
}







_xwk.DOM.appendChildren = function ( id, arr )
{
	var ele = this.getElement( id );
	
	if (!ele)
		return false;
	
	
	if (typeof(arr) != "object")
		return false;
		
	for (var i=0;i<arr.length;i++)
	{
		var cont = arr[i];
		if (typeof(cont) == "string")
			ele.appendChild( document.createTextNode(cont) );
		else if (typeof(cont) == "object")
			ele.appendChild( cont );
	}
}









_xwk.DOM.getPos = function ( ele )
{
	var ele = this.getElement(ele);

	var obj = ele;

	var curleft = 0;
	if (obj.offsetParent)
	{
		while (obj.offsetParent)
		{
			curleft += obj.offsetLeft
			obj = obj.offsetParent;
		}
	}
	else if (obj.x)
		curleft += obj.x;


	var obj = ele;
	
	var curtop = 0;
	if (obj.offsetParent)
	{
		while (obj.offsetParent)
		{
			curtop += obj.offsetTop
			obj = obj.offsetParent;
		}
	}
	else if (obj.y)
		curtop += obj.y;

	return {x:curleft, y:curtop}
}










// FADER PROTOTYPE _____________________________________________



if (typeof(_xwk.Fader) == "undefined")
	_xwk.Fader = {}





_xwk.Fader = function (ele, from, to, fadetime, callback)
{	
	if (!ele)
		return false;
	
	this.ele = ele;
	
	this.from = from;
	this.to = to;
	
	this.callback = callback;
	
	this.nDur = fadetime;
		
	this.nInt = 50;
	this.nTime = 0;
	
	var p = this;
	this.nID = setInterval(function() { p._fade() }, this.nInt);
}




_xwk.Fader.prototype._fade = function()
{
	this.nTime += this.nInt;
	
	var ieop = Math.round( this._tween(this.nTime, this.from, this.to, this.nDur) * 100 );
	var op = ieop / 100;
	
	if (this.ele.filters) // internet explorer
	{
		try
		{
			this.ele.filters.item("DXImageTransform.Microsoft.Alpha").opacity = ieop;
		} catch (e) { 
			// If it is not set initially, the browser will throw an error.  This will set it if it is not set yet.
			this.ele.style.filter = 'progid:DXImageTransform.Microsoft.Alpha(opacity='+ieop+')';
		}
	}
	else // other browsers
	{
		this.ele.style.opacity = op;
	}
	
	
	if (this.nTime == this.nDur)
	{
		clearInterval( this.nID );
		if (this.callback != undefined)
			this.callback();
	}
}



_xwk.Fader.prototype._tween = function(t,b,c,d)
{
	return b + ( (c-b) * (t/d) );
}


/**
 *  Inspired by:		Timothy Groves - http://www.brandspankingnew.net
 *	version:            2.0 - 2007-02-07
 *
 */

