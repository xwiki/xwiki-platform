/**
 * XWiki namespace.
 * TODO: move everything in it.
 *
 * @type object
 */
var XWiki = {

constants: {
  /**
   * Current wiki.
   */
  currentWiki: "$context.getDatabase()",

  /**
   * Main wiki.
   */
  mainWiki: "$context.getMainWikiName()",

  /**
   * Context path.
   */
  contextPath: "$request.getContextPath()",

  /**
   * Character that separates wiki from space in a page fullName (example: xwiki:Main.WebHome).
   */
  wikiSpaceSeparator: ":",

  /**
   * Character that separates space from page in a page fullName (example: xwiki:Main.WebHome).
   */
  spacePageSeparator: ".",

  /**
   * Character that separates page from attachment in an attachment fullName (example: xwiki:Main.WebHome@Archive.tgz).
   */
  pageAttachmentSeparator: "@",

  /**
   * URL Anchor separator.
   */
  anchorSeparator: "#",

  /**
   * URL Anchor for page comments.
   */
  docextraCommentsAnchor: "#Comments",

  /**
   * URL Anchor for page comments.
   */
  docextraAttachmentsAnchor: "#Attachments",

  /**
   * URL Anchor for page comments.
   */
  docextraHistoryAnchor: "#History",

  /**
   * URL Anchor for page comments.
   */
  docextraInformationAnchor: "#Information"
},

/**
 * Build a resource object from a wiki resource descriptor (aka fullName).
 *
 * Examples of resource objects:
 * { wiki: "xwiki", space: "Main", prefixedSpace: "xwiki:Main",
 *   fullName: "Main.WebHome", prefixedFullName: "xwiki:Main.WebHome",
 *   name: "WebHome", attachment: "" }
 * { wiki: "xwiki", space: "Main", prefixedSpace: "xwiki:Main",
 *   fullName: "Main.WebHome", prefixedFullName: "xwiki:Main.WebHome",
 *   name: "WebHome", attachment: "attach.zip" }
 *
 * @param fullName fullName of the resource to create (examples: xwiki:Main.WebHome, xwiki:Main.WebHome@Archive.tgz).
 * @return the newly created resource object.
 */
getResource : function(fullName) {
	var resource = {
			wiki : "",
			space : "",
			prefixedSpace : "",
			fullName : fullName,
			prefixedFullName : "",
			name : "",
			attachment : "",
			anchor: ""
	};

	// Extract wiki and set prefixedFullName.
	if (fullName.include(this.constants.wikiSpaceSeparator)) {
	  	resource.wiki = fullName.substring(0, fullName.indexOf(this.constants.wikiSpaceSeparator));
	  	// Remove wiki from fullName.
      	resource.fullName = fullName.substring(fullName.indexOf(this.constants.wikiSpaceSeparator) + 1, fullName.length);
	  	resource.prefixedFullName = fullName;
	} else {
        if (fullName.include(this.constants.spacePageSeparator)) {
		    // Fallback on current wiki.
		    resource.wiki = this.constants.currentWiki;
		    resource.prefixedFullName = resource.wiki + this.constants.wikiSpaceSeparator + fullName;
        } else {
            resource.wiki = fullName;
        }
	}

	// Extract attachment and remove it from fullName and prefixedFullName if any.
	if (resource.fullName.include(this.constants.pageAttachmentSeparator)) {
        // Attachment name.
		resource.attachment = fullName.substring(fullName.indexOf(this.constants.pageAttachmentSeparator) + 1, fullName.length);
		resource.fullName = resource.fullName.substring(0, resource.fullName.indexOf(this.constants.pageAttachmentSeparator));
        fullName = resource.fullName;
		resource.prefixedFullName = resource.prefixedFullName.substring(0, resource.prefixedFullName.indexOf(this.constants.pageAttachmentSeparator));
	}

	// Extract anchor and remove it from fullName and prefixedFullName if any.
	if (resource.fullName.include(this.constants.anchorSeparator)) {
		resource.anchor = resource.fullName.substring(resource.fullName.indexOf(this.constants.anchorSeparator) + 1, resource.fullName.length);
		resource.fullName = resource.fullName.substring(0, resource.fullName.indexOf(this.constants.anchorSeparator));
		fullName = resource.fullName;
        resource.prefixedFullName = resource.prefixedFullName.substring(0, resource.prefixedFullName.indexOf(this.constants.anchorSeparator));
	}

	// Extract space and page name.
	if (fullName.include(this.constants.spacePageSeparator)) {
		// Space
		resource.space = fullName.substring(fullName.indexOf(this.constants.wikiSpaceSeparator) + 1, fullName.indexOf(this.constants.spacePageSeparator));
		resource.prefixedSpace = resource.wiki + this.constants.wikiSpaceSeparator + resource.space;
		if (fullName.length - fullName.indexOf(this.constants.spacePageSeparator) > 0) {
		  // Page name.
	      resource.name = fullName.substring(fullName.indexOf(this.constants.spacePageSeparator) + 1, fullName.length);
		}
	} else {
        resource.space = resource.fullName;
    }

	return resource;
}
};

/**
 * Add click listeners on all rendereing error messages to let the user read the detailed error description.
 */
document.observe("dom:loaded", function() {
    $$('[class="xwikirenderingerror"]').each(function(error) {
        if(error.nextSibling.innerHTML !== "" && error.nextSibling.hasClassName("xwikirenderingerrordescription")) {
            error.style.cursor="pointer";
            error.title = "$msg.get('platform.core.rendering.error.readTechnicalInformation')";
            Event.observe(error, "click", function(event){
                   toggleClass(event.element().nextSibling,'hidden');
           });
        }
    });

});

/**
 * Hide the fieldset inside the given form.
 *
 * @param form  {element} The form element.
 * @return
 */
function hideForm(form){
	form.getElementsByTagName("fieldset").item(0).className = "collapsed";
}

/**
 * Hide the fieldset inside the given form if visible, show it if it's not.
 *
 * @param form  {element} The form element.
 * @return
 */
function toggleForm(form){
	var fieldset = form.getElementsByTagName("fieldset").item(0);
	if(fieldset.className == "collapsed"){
		fieldset.className = "expanded";
	}
	else{
		fieldset.className = "collapsed";
	}
}

/**
 * Expand the given panel if collapsed, collapse if visible.
 *
 * @param form  {element} The panel element.
 * @return
 */
function togglePanelVisibility(element){
	if(element.className.indexOf("expanded") >= 0){
		element.className = element.className.replace('expanded', 'collapsed');
	}
	else{
		element.className = element.className.replace('collapsed', 'expanded');
	}
}

/**
 * Show items under the given entry in the top menu (menuview.vm).
 *
 * @param element The selected item
 * @return
 */
function showsubmenu(element){
	if(element.lastChild.tagName.toLowerCase() == "span"){
		if(window.hidetimer){
			if(window.hideelement == element.lastChild){
				clearTimeout(window.hidetimer);
				window.hidetimer = null;
				window.hideelement = null;
			}
			else{
				doHide();
			}
		}
		var coords = Position.positionedOffset(element);
		if (element.getStyle("float") == "left") {
			element.lastChild.style.left = (coords[0]  - 10) + "px";
		} else {
			element.lastChild.style.left = (coords[0]  - 70) + "px";
		}
		element.lastChild.style.top = (coords[1] + element.offsetHeight) + "px";
		element.lastChild.className = element.lastChild.className.replace("hidden", "visible");
	}
}

/**
 * hide items under the given entry in the top menu (menuview.vm).
 *
 * @param element The selected item
 * @return
 */
function hidesubmenu(element){
	if(element.lastChild.tagName.toLowerCase() == "span"){
		window.hideelement = element.lastChild;
		window.hidetimer = setTimeout(doHide, 100);
	}
}

/**
 * Method doing the hide action on the element set by hidesubmenu() in the window object.
 *
 * @return
 */
function doHide(){
	window.hideelement.className = window.hideelement.className.replace("visible", "hidden");
	clearTimeout(window.hidetimer);
	window.hidetimer = null;
	window.hideelement = null;
}

/**
 * Toggle CSS class in the given element.
 *
 * @param o Element.
 * @param className CSS class.
 * @return
 */
function toggleClass(o, className){
	if(!eltHasClass(o,className)) {
		o.className += ' ' + className
	}
	else {
		rmClass(o, className);
	}
}

/**
 * Add a CSS class to an element.
 *
 * @param o Element.
 * @param className CSS class.
 * @return
 */
function addClass(o, className){
	if(!eltHasClass(o,className))
		o.className += ' ' + className
}

/**
 * Check if an element has a CSS class.
 *
 * @param o Element.
 * @param className CSS class.
 * @return True if the element has the class.
 */
function eltHasClass(o,className){
	if(!o.className)
		return false;
	return new RegExp('\\b' + className + '\\b').test(o.className)
}

/**
 * Remove a CSS class from an element.
 *
 * @param o Element.
 * @param className CSS class.
 * @return
 */
function rmClass(o, className){
	o.className = o.className.replace(new RegExp('\\s*\\b' + className + '\\b'),'')
}

/**
 * Open an URL in a pop-up.
 *
 * @param url URL to open.
 * @return
 */
function openURL(url) {
	win = open( url, "win", "titlebar=0,width=990,height=500,resizable,scrollbars");
	if( win ) {
		win.focus();
	}
}

/**
 * Open XWiki syntax documentation in a pop-up.
 *
 * @deprecated
 * @return
 */
function openHelp() {
	win = open( "http://platform.xwiki.org/xwiki/bin/view/Main/XWikiSyntax?xpage=print", "XWikiSyntax", "titlebar=0,width=750,height=480,resizable,scrollbars");
	if( win ) {
		win.focus();
	}
}

/**
 * Ajax request wrapper.
 *
 * @deprecated
 */
var XWikiAjax = {
		requests: new Array(),
		start: function(status) {
	this.status = $(status);
	ajaxEngine.registerRequest ('setValue', 'SetValueResponse?xpage=rdf');
	ajaxEngine.registerAjaxElement (this.status.id);
},
addRequest: function(dName, cName, field, value) {
	var request = Object.extend({
		type: 'set'
	});
	request.className = cName;
	request.document = dName;
	request.field = field;
	request.value = value;
	this.requests.push(request);
},
end: function() {
	this.requests.reverse();
	var req = this.requests.pop();
	while (req) {
		if (req.type=='set') {
			ajaxEngine.sendRequest ('setValue', 'status='+this.status.id, 'doc='+req.document,
					'typedoc='+req.className, 'field='+req.field, 'value='+req.value);
		};
		req = this.requests.pop();
	}
	this.status.innerHtml = 'updated';
}
};

/**
 * Remove special characters from text inputs.
 *
 * @param field1 Text input
 * @param field2 Text input
 * @param removeclass
 * @return true if the text empty after the operation.
 */
function updateName(field1, field2, removeclass) {
	var name = field1.value;
	name = noaccent(name);
	if (removeclass!=false) {
		name = name.replace(/class$/gi,"");
	}
	if (field2 == null) {
		field1.value = name;
	} else {
		field2.value = name;
	}
	if (name=="") {
		return false;
	}
	return true;
}

/**
 * Replace accented chars by non-accented chars in a string.
 *
 * @param txt String to clean.
 * @return The cleaned string.
 */
function noaccent(txt) {
	temp = txt.replace(/[\u00c0\u00c1\u00c2\u00c3\u00c4\u00c5\u0100\u0102\u0104\u01cd\u01de\u01e0\u01fa\u0200\u0202\u0226]/g,"A");
	temp = temp.replace(/[\u00e0\u00e1\u00e2\u00e3\u00e4\u00e5\u0101\u0103\u0105\u01ce\u01df\u01e1\u01fb\u0201\u0203\u0227]/g,"a");
	temp = temp.replace(/[\u00c6\u01e2\u01fc]/g,"AE");
	temp = temp.replace(/[\u00e6\u01e3\u01fd]/g,"ae");
	temp = temp.replace(/[\u008c\u0152]/g,"OE");
	temp = temp.replace(/[\u009c\u0153]/g,"oe");
	temp = temp.replace(/[\u00c7\u0106\u0108\u010a\u010c]/g,"C");
	temp = temp.replace(/[\u00e7\u0107\u0109\u010b\u010d]/g,"c");
	temp = temp.replace(/[\u00d0\u010e\u0110]/g,"D");
	temp = temp.replace(/[\u00f0\u010f\u0111]/g,"d");
	temp = temp.replace(/[\u00c8\u00c9\u00ca\u00cb\u0112\u0114\u0116\u0118\u011a\u0204\u0206\u0228]/g,"E");
	temp = temp.replace(/[\u00e8\u00e9\u00ea\u00eb\u0113\u0115\u0117\u0119\u011b\u01dd\u0205\u0207\u0229]/g,"e");
	temp = temp.replace(/[\u011c\u011e\u0120\u0122\u01e4\u01e6\u01f4]/g,"G");
	temp = temp.replace(/[\u011d\u011f\u0121\u0123\u01e5\u01e7\u01f5]/g,"g");
	temp = temp.replace(/[\u0124\u0126\u021e]/g,"H");
	temp = temp.replace(/[\u0125\u0127\u021f]/g,"h");
	temp = temp.replace(/[\u00cc\u00cd\u00ce\u00cf\u0128\u012a\u012c\u012e\u0130\u01cf\u0208\u020a]/g,"I");
	temp = temp.replace(/[\u00ec\u00ed\u00ee\u00ef\u0129\u012b\u012d\u012f\u0131\u01d0\u0209\u020b]/g,"i");
	temp = temp.replace(/[\u0132]/g,"IJ");
	temp = temp.replace(/[\u0133]/g,"ij");
	temp = temp.replace(/[\u0134]/g,"J");
	temp = temp.replace(/[\u0135]/g,"j");
	temp = temp.replace(/[\u0136\u01e8]/g,"K");
	temp = temp.replace(/[\u0137\u0138\u01e9]/g,"k");
	temp = temp.replace(/[\u0139\u013b\u013d\u013f\u0141]/g,"L");
	temp = temp.replace(/[\u013a\u013c\u013e\u0140\u0142\u0234]/g,"l");
	temp = temp.replace(/[\u00d1\u0143\u0145\u0147\u014a\u01f8]/g,"N");
	temp = temp.replace(/[\u00f1\u0144\u0146\u0148\u0149\u014b\u01f9\u0235]/g,"n");
	temp = temp.replace(/[\u00d2\u00d3\u00d4\u00d5\u00d6\u00d8\u014c\u014e\u0150\u01d1\u01ea\u01ec\u01fe\u020c\u020e\u022a\u022c\u022e\u0230]/g,"O");
	temp = temp.replace(/[\u00f2\u00f3\u00f4\u00f5\u00f6\u00f8\u014d\u014f\u0151\u01d2\u01eb\u01ed\u01ff\u020d\u020f\u022b\u022d\u022f\u0231]/g,"o");
	temp = temp.replace(/[\u0156\u0158\u0210\u0212]/g,"R");
	temp = temp.replace(/[\u0157\u0159\u0211\u0213]/g,"r");
	temp = temp.replace(/[\u015a\u015c\u015e\u0160\u0218]/g,"S");
	temp = temp.replace(/[\u015b\u015d\u015f\u0161\u0219]/g,"s");
	temp = temp.replace(/[\u00de\u0162\u0164\u0166\u021a]/g,"T");
	temp = temp.replace(/[\u00fe\u0163\u0165\u0167\u021b\u0236]/g,"t");
	temp = temp.replace(/[\u00d9\u00da\u00db\u00dc\u0168\u016a\u016c\u016e\u0170\u0172\u01d3\u01d5\u01d7\u01d9\u01db\u0214\u0216]/g,"U");
	temp = temp.replace(/[\u00f9\u00fa\u00fb\u00fc\u0169\u016b\u016d\u016f\u0171\u0173\u01d4\u01d6\u01d8\u01da\u01dc\u0215\u0217]/g,"u");
	temp = temp.replace(/[\u0174]/g,"W");
	temp = temp.replace(/[\u0175]/g,"w");
	temp = temp.replace(/[\u00dd\u0176\u0178\u0232]/g,"Y");
	temp = temp.replace(/[\u00fd\u00ff\u0177\u0233]/g,"y");
	temp = temp.replace(/[\u0179\u017b\u017d]/g,"Z");
	temp = temp.replace(/[\u017a\u017c\u017e]/g,"z");
	temp = temp.replace(/[\u00df]/g,"SS");
	temp = temp.replace(/[^a-zA-Z0-9_]/g,"");
	return temp;
}

/**
 * Method used by register.vm to concatenate first name and last name to generate
 * the name of the profile page of the user who is registering.
 *
 * @param form The register form.
 * @return
 */
function prepareName(form) {
	var fname = form.register_first_name.value;
	var lname = form.register_last_name.value;
	var cxwikiname = form.xwikiname;
	if (fname != "") {
		fname = fname.substring(0,1).toUpperCase() + fname.substring(1);
		fname.replace(/ /g,"");
	}
	if (lname != "") {
		lname = lname.substring(0,1).toUpperCase() + lname.substring(1)
		lname.replace(/ /g,"");
	}
	if (cxwikiname.value == "") {
		cxwikiname.value = noaccent(fname + lname);
	}
}

/**
 * Create a cookie, with or without expiration date.
 *
 * @param name Name of the cookie.
 * @param value Value of the cookie.
 * @param days Days to keep the cookie (can be null).
 * @return
 */
function createCookie(name,value,days) {
	if (days) {
		var date = new Date();
		date.setTime(date.getTime()+(days*24*60*60*1000));
		var expires = "; expires="+date.toGMTString();
	}
	else var expires = "";
	document.cookie = name+"="+value+expires+"; path=/";
}

/**
 * Read a cookie.
 *
 * @param name Name of the cookie.
 * @return Value for the given cookie.
 */
function readCookie(name) {
	var nameEQ = name + "=";
	var ca = document.cookie.split(';');
	for(var i=0;i < ca.length;i++) {
		var c = ca[i];
		while (c.charAt(0)==' ') {
			c = c.substring(1,c.length);
		}
		if (c.indexOf(nameEQ) == 0) {
			return c.substring(nameEQ.length,c.length);
		}
	}
	return null;
}

/**
 * Erase a cookie.
 *
 * @param name Name of the cookie to erase.
 * @return
 */
function eraseCookie(name) {
	createCookie(name,"",-1);
}

/**
 * Method used by docextra.vm to emulate tabbed panes.
 *
 * @param extraID Id of the pane to show.
 * @param extraTemplate Velocity template to retrieve and display in the pane.
 * @param scrollToAnchor Jump to the pane anchor.
 * @return
 */
function displayDocExtra(extraID, extraTemplate, scrollToAnchor)
{

	// Nested function: hides the previously displayed extra pane (window.activeDocExtraPane)
	// and display the one that is passed as an argument (extraID).
	var dhtmlSwitch = function(extraID) {
		var tab = document.getElementById(extraID + "tab");
		var pane = document.getElementById(extraID + "pane");
		if (window.activeDocExtraTab != null) {
			window.activeDocExtraTab.className="";
			window.activeDocExtraPane.className="hidden";
		}
		window.activeDocExtraTab = tab;
		window.activeDocExtraPane = pane;
		window.activeDocExtraTab.className="active";
		window.activeDocExtraPane.className="";
		tab.blur();
	};

	// Nested function: insert a save button near a tages input field if the extra pane
	// passed as an argument (extraID) is the doc information pane.
	var insertTagSaveButton = function(extraID) {
		if (extraID == "Information" && $('tageditsavewrapper') != null) {
			$('tageditsavewrapper').className = "buttonwrapper";
			Event.observe($('tageditsave'), 'click', function() {
				new Ajax.Request(
						window.docsaveurl,
						{
							method: 'post',
							postBody: "tags=" + $('tags').value,
							onSuccess: function() { $('tageditsavesuccess').className=''; $('tageditsaveerror').className='hidden'; },
							onError: function() { $('tageditsavesuccess').className='hidden'; $('tageditsaveerror').className=''; }
						});
				return false;
			} , false);
		}
	};

	// Use Ajax.Updater to display the requested pane (extraID) : comments, attachments, etc.
	// On complete :
	//   1. Call insertTagSaveButton()
	//   2. Call dhtmlSwitch()
	//   3. If the function call has been triggered by an event : reset location.href to #extraID
	//      (because when the link has been first clicked the anchor was not loaded)
	if ($(extraID + "pane").className.indexOf("empty") != -1) {
		if (window.activeDocExtraPane != null) {
			window.activeDocExtraPane.className="invisible";
		}
		$("docextrapanes").className="loading";
		new Ajax.Updater(
				extraID + "pane",
				window.docviewurl + '?xpage=xpart&vm=' + extraTemplate,
				{
					method: 'post',
					evalScripts: true,
					onComplete: function(){
					insertTagSaveButton(extraID);
					$("docextrapanes").className="";
					dhtmlSwitch(extraID);
					if (scrollToAnchor) {
						// Yes, this is a POJW (Plain Old JavaScript Ha^Wworkaround) which
						// prevents the anchor 'jump' after a click event but enable it
						// when the user is arriving from a direct /Space/Page#Section URL
						$(extraID + 'anchor').id = extraID;
						location.href='#' + extraID;
						$(extraID).id = extraID + 'anchor';
					}
				}
				});
	} else {
		dhtmlSwitch(extraID);
		if (scrollToAnchor) {
			$(extraID + 'anchor').id = extraID;
			location.href='#' + extraID;
			$(extraID).id = extraID + 'anchor';
		}
	}
}

/**
 * Method used by editmodes.vm to warn the user if he tries to go to the WYSIWYG editor
 * with HTML in his content.
 *
 * @param message Translated warning message.
 */
function checkAdvancedContent(message) {
	result = false;
	if (!document.forms.edit) {
		return true;
	}
	data = document.forms.edit.content.value;
	myRE = new RegExp("</?(html|body|img|a|i|b|embed|script|form|input|textarea|object|font|li|ul|ol|table|center|hr|br|p) ?([^>]*)>", "ig")
	results = data.match(myRE)
	if (results&&results.length>0)
		result = true;

	myRE2 = new RegExp("(#(set|include|if|end|for)|#(#) Advanced content|public class|/\* Advanced content \*/)", "ig")
	results = data.match(myRE2)
	if (results&&results.length>0)
		result = true;

	if (result==true)
		return confirm(message);

	return true;
}

/**
 * Keyboard Shortcuts.
 * Version: 2.01.A
 * URL: http://www.openjs.com/scripts/events/keyboard_shortcuts/
 * Author: Binny VA
 * License : BSD
 */
shortcut = {
		'all_shortcuts':{},//All the shortcuts are stored in this array
		'add': function(shortcut_combination,callback,opt) {
			//Provide a set of default options
			var default_options = {
					'type':'keydown',
					'propagate':false,
					'disable_in_input':false,
					'target':document,
					'keycode':false
			}
			if(!opt) opt = default_options;
			else {
				for(var dfo in default_options) {
					if(typeof opt[dfo] == 'undefined') opt[dfo] = default_options[dfo];
				}
			}

			var ele = opt.target
			if(typeof opt.target == 'string') ele = document.getElementById(opt.target);
			var ths = this;
			shortcut_combination = shortcut_combination.toLowerCase();

			//The function to be called at keypress
			var func = function(e) {
				e = e || window.event;

				if(opt['disable_in_input']) { //Don't enable shortcut keys in Input, Textarea fields
					var element;
					if(e.target) element=e.target;
					else if(e.srcElement) element=e.srcElement;
					if(element.nodeType==3) element=element.parentNode;

					if(element.tagName == 'INPUT' || element.tagName == 'TEXTAREA') return;
				}

				//Find Which key is pressed
				if (e.keyCode) code = e.keyCode;
				else if (e.which) code = e.which;
				var character = String.fromCharCode(code).toLowerCase();

				if(code == 188) character=","; //If the user presses , when the type is onkeydown
				if(code == 190) character="."; //If the user presses , when the type is onkeydown

				var keys = shortcut_combination.split("+");
				//Key Pressed - counts the number of valid keypresses - if it is same as the number of keys, the shortcut function is invoked
				var kp = 0;

				//Work around for stupid Shift key bug created by using lowercase - as a result the shift+num combination was broken
				var shift_nums = {
						"`":"~",
						"1":"!",
						"2":"@",
						"3":"#",
						"4":"$",
						"5":"%",
						"6":"^",
						"7":"&",
						"8":"*",
						"9":"(",
						"0":")",
						"-":"_",
						"=":"+",
						";":":",
						"'":"\"",
						",":"<",
						".":">",
						"/":"?",
						"\\":"|"
				}
				//Special Keys - and their codes
				var special_keys = {
						'esc':27,
						'escape':27,
						'tab':9,
						'space':32,
						'return':13,
						'enter':13,
						'backspace':8,

						'scrolllock':145,
						'scroll_lock':145,
						'scroll':145,
						'capslock':20,
						'caps_lock':20,
						'caps':20,
						'numlock':144,
						'num_lock':144,
						'num':144,

						'pause':19,
						'break':19,

						'insert':45,
						'home':36,
						'delete':46,
						'end':35,

						'pageup':33,
						'page_up':33,
						'pu':33,

						'pagedown':34,
						'page_down':34,
						'pd':34,

						'left':37,
						'up':38,
						'right':39,
						'down':40,

						'f1':112,
						'f2':113,
						'f3':114,
						'f4':115,
						'f5':116,
						'f6':117,
						'f7':118,
						'f8':119,
						'f9':120,
						'f10':121,
						'f11':122,
						'f12':123
				}

				var modifiers = {
						shift: { wanted:false, pressed:false},
						ctrl : { wanted:false, pressed:false},
						alt  : { wanted:false, pressed:false},
						meta : { wanted:false, pressed:false}	//Meta is Mac specific
				};

				if(e.ctrlKey)	modifiers.ctrl.pressed = true;
				if(e.shiftKey)	modifiers.shift.pressed = true;
				if(e.altKey)	modifiers.alt.pressed = true;
				if(e.metaKey)   modifiers.meta.pressed = true;

				for(var i=0; k=keys[i],i<keys.length; i++) {
					//Modifiers
					if(k == 'ctrl' || k == 'control') {
						kp++;
						modifiers.ctrl.wanted = true;

					} else if(k == 'shift') {
						kp++;
						modifiers.shift.wanted = true;

					} else if(k == 'alt') {
						kp++;
						modifiers.alt.wanted = true;
					} else if(k == 'meta') {
						kp++;
						modifiers.meta.wanted = true;
					} else if(k.length > 1) { //If it is a special key
						if(special_keys[k] == code) kp++;

					} else if(opt['keycode']) {
						if(opt['keycode'] == code) kp++;

					} else { //The special keys did not match
						if(character == k) kp++;
						else {
							if(shift_nums[character] && e.shiftKey) { //Stupid Shift key bug created by using lowercase
								character = shift_nums[character];
								if(character == k) kp++;
							}
						}
					}
				}

				if(kp == keys.length &&
						modifiers.ctrl.pressed == modifiers.ctrl.wanted &&
						modifiers.shift.pressed == modifiers.shift.wanted &&
						modifiers.alt.pressed == modifiers.alt.wanted &&
						modifiers.meta.pressed == modifiers.meta.wanted) {
					callback(e);

					if(!opt['propagate']) { //Stop the event
						//e.cancelBubble is supported by IE - this will kill the bubbling process.
						e.cancelBubble = true;
						e.returnValue = false;

						// XWIKI : added to force Alt+key events not to be propagated to IE7
						if (document.all && !window.opera && window.XMLHttpRequest) {
							e.keyCode = 0;
						}

						//e.stopPropagation works in Firefox.
						if (e.stopPropagation) {
							e.stopPropagation();
							e.preventDefault();
						}
						return false;
					}
				}
			}
			this.all_shortcuts[shortcut_combination] = {
					'callback':func,
					'target':ele,
					'event': opt['type']
			};
			//Attach the function with the event
			if(ele.addEventListener) ele.addEventListener(opt['type'], func, false);
			else if(ele.attachEvent) ele.attachEvent('on'+opt['type'], func);
			else ele['on'+opt['type']] = func;
		},

		//Remove the shortcut - just specify the shortcut and I will remove the binding
		'remove':function(shortcut_combination) {
			shortcut_combination = shortcut_combination.toLowerCase();
			var binding = this.all_shortcuts[shortcut_combination];
			delete(this.all_shortcuts[shortcut_combination])
			if(!binding) return;
			var type = binding['event'];
			var ele = binding['target'];
			var callback = binding['callback'];

			if(ele.detachEvent) ele.detachEvent('on'+type, callback);
			else if(ele.removeEventListener) ele.removeEventListener(type, callback, false);
			else ele['on'+type] = false;
		}
}

/**
 * Browser Detect
 * Version: 2.1.6
 * URL: http://dithered.chadlindstrom.ca/javascript/browser_detect/index.html
 * License: http://creativecommons.org/licenses/by/1.0/
 * Author: Chris Nott (chris[at]dithered[dot]com)
*/
function BrowserDetect() {
	var ua = navigator.userAgent.toLowerCase();

    // browser engine name
	this.isGecko       = (ua.indexOf('gecko') != -1 && ua.indexOf('safari') == -1);
	this.isAppleWebKit = (ua.indexOf('applewebkit') != -1);

    // browser name
	this.isKonqueror   = (ua.indexOf('konqueror') != -1);
	this.isSafari      = (ua.indexOf('safari') != - 1);
	this.isOmniweb     = (ua.indexOf('omniweb') != - 1);
	this.isOpera       = (ua.indexOf('opera') != -1);
	this.isIcab        = (ua.indexOf('icab') != -1);
	this.isAol         = (ua.indexOf('aol') != -1);
	this.isIE          = (ua.indexOf('msie') != -1 && !this.isOpera && (ua.indexOf('webtv') == -1) );
	this.isMozilla     = (this.isGecko && ua.indexOf('gecko/') + 14 == ua.length);
	this.isFirefox     = (ua.indexOf('firefox/') != -1 || ua.indexOf('firebird/') != -1);
	this.isNS          = ( (this.isGecko) ? (ua.indexOf('netscape') != -1) : ( (ua.indexOf('mozilla') != -1) && !this.isOpera && !this.isSafari && (ua.indexOf('spoofer') == -1) && (ua.indexOf('compatible') == -1) && (ua.indexOf('webtv') == -1) && (ua.indexOf('hotjava') == -1) ) );

    // spoofing and compatible browsers
	this.isIECompatible = ( (ua.indexOf('msie') != -1) && !this.isIE);
	this.isNSCompatible = ( (ua.indexOf('mozilla') != -1) && !this.isNS && !this.isMozilla);

    // rendering engine versions
	this.geckoVersion = ( (this.isGecko) ? ua.substring( (ua.lastIndexOf('gecko/') + 6), (ua.lastIndexOf('gecko/') + 14) ) : -1 );
	this.equivalentMozilla = ( (this.isGecko) ? parseFloat( ua.substring( ua.indexOf('rv:') + 3 ) ) : -1 );
	this.appleWebKitVersion = ( (this.isAppleWebKit) ? parseFloat( ua.substring( ua.indexOf('applewebkit/') + 12) ) : -1 );

    // browser version
	this.versionMinor = parseFloat(navigator.appVersion);

    // correct version number
	if (this.isGecko && !this.isMozilla) {
		this.versionMinor = parseFloat( ua.substring( ua.indexOf('/', ua.indexOf('gecko/') + 6) + 1 ) );
	}
	else if (this.isMozilla) {
		this.versionMinor = parseFloat( ua.substring( ua.indexOf('rv:') + 3 ) );
	}
	else if (this.isIE && this.versionMinor >= 4) {
		this.versionMinor = parseFloat( ua.substring( ua.indexOf('msie ') + 5 ) );
	}
	else if (this.isKonqueror) {
		this.versionMinor = parseFloat( ua.substring( ua.indexOf('konqueror/') + 10 ) );
	}
	else if (this.isSafari) {
		this.versionMinor = parseFloat( ua.substring( ua.lastIndexOf('safari/') + 7 ) );
	}
	else if (this.isOmniweb) {
		this.versionMinor = parseFloat( ua.substring( ua.lastIndexOf('omniweb/') + 8 ) );
	}
	else if (this.isOpera) {
		this.versionMinor = parseFloat( ua.substring( ua.indexOf('opera') + 6 ) );
	}
	else if (this.isIcab) {
		this.versionMinor = parseFloat( ua.substring( ua.indexOf('icab') + 5 ) );
	}

	this.versionMajor = parseInt(this.versionMinor);

    // dom support
	this.isDOM1 = (document.getElementById);
	this.isDOM2Event = (document.addEventListener && document.removeEventListener);

    // css compatibility mode
	this.mode = document.compatMode ? document.compatMode : 'BackCompat';

    // platform
	this.isWin    = (ua.indexOf('win') != -1);
	this.isWin32  = (this.isWin && ( ua.indexOf('95') != -1 || ua.indexOf('98') != -1 || ua.indexOf('nt') != -1 || ua.indexOf('win32') != -1 || ua.indexOf('32bit') != -1 || ua.indexOf('xp') != -1) );
	this.isMac    = (ua.indexOf('mac') != -1);
	this.isUnix   = (ua.indexOf('unix') != -1 || ua.indexOf('sunos') != -1 || ua.indexOf('bsd') != -1 || ua.indexOf('x11') != -1)
	this.isLinux  = (ua.indexOf('linux') != -1);

    // specific browser shortcuts
	this.isNS4x = (this.isNS && this.versionMajor == 4);
	this.isNS40x = (this.isNS4x && this.versionMinor < 4.5);
	this.isNS47x = (this.isNS4x && this.versionMinor >= 4.7);
	this.isNS4up = (this.isNS && this.versionMinor >= 4);
	this.isNS6x = (this.isNS && this.versionMajor == 6);
	this.isNS6up = (this.isNS && this.versionMajor >= 6);
	this.isNS7x = (this.isNS && this.versionMajor == 7);
	this.isNS7up = (this.isNS && this.versionMajor >= 7);

	this.isIE4x = (this.isIE && this.versionMajor == 4);
	this.isIE4up = (this.isIE && this.versionMajor >= 4);
	this.isIE5x = (this.isIE && this.versionMajor == 5);
	this.isIE55 = (this.isIE && this.versionMinor == 5.5);
	this.isIE5up = (this.isIE && this.versionMajor >= 5);
	this.isIE6x = (this.isIE && this.versionMajor == 6);
	this.isIE6up = (this.isIE && this.versionMajor >= 6);

	this.isIE4xMac = (this.isIE4x && this.isMac);
}
var browser = new BrowserDetect();

