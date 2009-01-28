//function to get a style property of an element
//////////////////////////////////////////////
function getStyle(el, style) {
  if(!document.getElementById) return;
  var value = el.style[toCamelCase(style)];
  if(!value) {
    if(document.defaultView) {
      value = document.defaultView.getComputedStyle(el, "").getPropertyValue(style);
    } else if(el.currentStyle) {
      value = el.currentStyle[toCamelCase(style)];
    }
  }
  return value;
}




//function to set a style property for an element
////////////////////////////////////
function setStyle(el, style, value) {
  el.style[style] = value;
}




//utility function
///////////////////
function toCamelCase( sInput ) {
  var oStringList = sInput.split('-');
  if(oStringList.length == 1) {
    return oStringList[0];
  }
  var ret = sInput.indexOf("-") == 0 ?
     oStringList[0].charAt(0).toUpperCase() + oStringList[0].substring(1) : oStringList[0];
  for(var i = 1, len = oStringList.length; i < len; i++) {
    var s = oStringList[i];
    ret += s.charAt(0).toUpperCase() + s.substring(1)
  }
  return ret;
}





//move the children of srcNode into destNode
/////////////////////////////////////////////
function moveChildren(srcNode, destNode) {
  var node;
  while(srcNode.hasChildNodes()) {
    node = srcNode.firstChild;
    destNode.appendChild(node);
  }
}





//add the element that will trigger the dialog
/////////////////////////////////////////////
function fullscreenEdit(what) {
  var parentDiv = document.getElementById("xwikitext");
  if(parentDiv) {
    //Fullscreen button
    var showBtn = document.createElement("div");
    showBtn.setAttribute("id", "show-dialog-btn");
    Event.observe(showBtn, "click", new Function(what+"Fullscreen()"), true);
    showBtn.setAttribute("title", "$msg.get("fullScreenTooltip")");
    var textdiv = document.getElementById("xwikieditcontent");
    textdiv.insertBefore(showBtn, textdiv.firstChild);

    //fullscreen div
    var newdiv = document.createElement("div");
    newdiv.setAttribute("id", "fullscreen-dlg");

    //fullscreen div header
    var newdivhd = document.createElement("div");
    newdivhd.setAttribute("id", "fullscreen-dlg-head");

    //close button
    var closeBtn = document.createElement("div");
    closeBtn.setAttribute("id", "closeBtn");
    Event.observe(closeBtn, "click", new Function(what+"Hide()"), true);
    newdivhd.appendChild(closeBtn);

    var title = document.createElement("div");
    title.className = "titleText";
    title.appendChild(document.createTextNode("Fullscreen Editing"));
    newdivhd.appendChild(title);

    //fullscreen div body
    var newdivbd = document.createElement("div");
    newdivbd.setAttribute("id", "fullscreen-dlg-body");
    newdivbd.appendChild(newdivhd);

    var newdivbdtab = document.createElement("div");
    newdivbdtab.setAttribute("id", "fullscreen-dlg-body-tab-"+what);
    newdivbd.appendChild(newdivbdtab);

    newdiv.appendChild(newdivbd);

    var backBtn = document.createElement("div");
    backBtn.className = "backBtn";
    Event.observe(backBtn, "click", new Function(what+"Hide()"), true);
    var btnText = document.createElement("div");
    btnText.className = "btnText";
    btnText.appendChild(document.createTextNode("Back"));
    backBtn.appendChild(btnText);

    newdivbd.appendChild(backBtn);

    document.body.appendChild(newdiv);
  }
}

//maximize or minimize the editing area
///////////////////////////////////////
function wysiwygFullscreen() {
  var flscrDiv = document.getElementById("fullscreen-dlg");
  var flscrDivBdTab = document.getElementById("fullscreen-dlg-body-tab-wysiwyg");
  setStyle(flscrDiv, "display", "block");

  var parentswapDiv = document.getElementById("parentswapDiv");
  var swapDiv = document.getElementById("swapDiv");

  if(parentswapDiv == null || parentswapDiv == undefined) {
    parentswapDiv = document.createElement("div");
    parentswapDiv.setAttribute("id", "parentswapDiv");
    if(swapDiv == null || swapDiv == undefined) {
      swapDiv = document.createElement("div");
      swapDiv.setAttribute("id", "swapDiv");
    }
    parentswapDiv.appendChild(swapDiv);
    flscrDivBdTab.appendChild(parentswapDiv);
  }
  //swaps the swapDiv with xwikieditcontent
  var xwikidiv = document.getElementById("xwikieditcontent");

  //remove the editor
  tinyMCE.execCommand('mceRemoveControl', false, 'content');

  var auxdiv = document.createElement("div");
  document.body.appendChild(auxdiv);
  moveChildren(parentswapDiv, auxdiv);
  moveChildren(xwikidiv, parentswapDiv);
  moveChildren(auxdiv, xwikidiv);
  document.body.removeChild(auxdiv);

  //hide scrollbars
  document.body.style.overflow="hidden";
  // hide the fullscreen button
  document.getElementById("show-dialog-btn").style.display="none";
   
  //put the editor back
  tinyMCE.execCommand('mceAddControl', false, 'content');
}




//hide dialog and restore the textarea
/////////////////////////////////////
function wysiwygHide() {
  var flscrDiv = document.getElementById("fullscreen-dlg");
  setStyle(flscrDiv, "display", "none");

  var parentswapDiv = document.getElementById("parentswapDiv");
  var xwikidiv = document.getElementById("xwikieditcontent");
  var swapDiv = document.getElementById("swapDiv");

  //remove the editor
  tinyMCE.execCommand('mceRemoveControl', false, 'content');

  var auxdiv = document.createElement("div");
  document.body.appendChild(auxdiv);
  moveChildren(parentswapDiv, auxdiv);
  moveChildren(xwikidiv, parentswapDiv);
  moveChildren(auxdiv, xwikidiv);
  document.body.removeChild(auxdiv);

  //show the scrollbars
  document.body.style.overflow="auto";
  // show the fullscreen button
  document.getElementById("show-dialog-btn").style.display="block";
  
  //put the editor back
  window.setTimeout("tinyMCE.execCommand('mceAddControl', false, 'content')", 10);
}




//maximize or minimize the editing area
///////////////////////////////////////
function wikiFullscreen() {
  var flscrDiv = document.getElementById("fullscreen-dlg");
  setStyle(flscrDiv, "display", "block");

  var parentswapDiv = document.getElementById("parentswapDiv");
  var swapDiv = document.getElementById("swapDiv");

  if(parentswapDiv == null || parentswapDiv == undefined) {
  parentswapDiv = document.createElement("div");
  parentswapDiv.setAttribute("id", "parentswapDiv");
  if(swapDiv == null || swapDiv == undefined) {
    swapDiv = document.createElement("div");
    swapDiv.setAttribute("id", "swapDiv");
  }

  parentswapDiv.appendChild(swapDiv);
  var flscrDivBdTab = document.getElementById("fullscreen-dlg-body-tab-wiki");
  flscrDivBdTab.appendChild(parentswapDiv);
  }

  //swaps the swapDiv with xwikieditcontent
  var xwikidiv = document.getElementById("xwikieditcontentinner");

  var auxdiv = document.createElement("div");
  document.body.appendChild(auxdiv);
  moveChildren(parentswapDiv, auxdiv);
  moveChildren(xwikidiv, parentswapDiv);
  moveChildren(auxdiv, xwikidiv);
  document.body.removeChild(auxdiv);
  
  //hide scrollbars
  document.body.style.overflow="hidden";
}

//hide dialog and restore the textarea
/////////////////////////////////////
function wikiHide() {
  var flscrDiv = document.getElementById("fullscreen-dlg");
  setStyle(flscrDiv, "display", "none");

  var parentswapDiv = document.getElementById("parentswapDiv");
  var xwikidiv = document.getElementById("xwikieditcontentinner");
  var swapDiv = document.getElementById("swapDiv");

  var auxdiv = document.createElement("div");
  document.body.appendChild(auxdiv);
  moveChildren(parentswapDiv, auxdiv);
  moveChildren(xwikidiv, parentswapDiv);
  moveChildren(auxdiv, xwikidiv);
  document.body.removeChild(auxdiv);

  //show the scrollbars
  document.body.style.overflow="auto";
}
