function computeBounds(){
  leftPanelsLeft   = getX(leftPanels);
  leftPanelsRight  = leftPanelsLeft  + leftPanels.offsetWidth;
  rightPanelsLeft  = getX(rightPanels);
  rightPanelsRight = rightPanelsLeft + rightPanels.offsetWidth;
  allpanelsLeft    = getX(allPanels);
  allpanelsTop     = getY(allPanels);
}

function debugwrite(sometext){
  document.getElementById("headerglobal").appendChild(document.createTextNode(sometext));
}

function isPanel(el){
  if (el.className && ((el.className == "panel") || (el.className.indexOf("panel ") >=0) || (el.className.indexOf(" panel") >=0))){
     return true;
  }
  return false;
}

function getX(el) {
  if (el.offsetParent) {
    return el.offsetLeft + getX(el.offsetParent); 
  }
  else {
    if (el.x) {
      return el.x;
    }
    else
      return 0;
  }
}

function getY(el) {
  if (el.offsetParent)
    return el.offsetTop + getY(el.offsetParent); 
  else {
    if (el.y)
      return el.y;
    else
      return 0;
  }
}

function getBlocList(el) {
  var list = [];
  var nb = el.childNodes.length;
  for (var i=0; i<nb; i++) {
    var el2 = el.childNodes[i];
    if(isPanel(el2)) {
      if (!el2.isDragging)  {
        list.push(el2);
      }
    }
  }
  return list;
}

function getDragBoxPos(list, y) {
  var nb = list.length;
  if (nb == 0) return 0;
  for (var i=0; i<nb; i++) {
    if (list[i]==dragel)
      return i;
  }
  return -1;
}

function getAllPanels(el){
  var list=[];
  var divs = el.getElementsByTagName("div");
  var j = 0;
  for(var i = 0; i < divs.length; i++) {
    if (isPanel(divs[i])){
      list[j] = divs[i];
      j++;
    }
  }
  return list;
}

function getClosestDropTarget(x, y, w, h) {
  if (window.showLeftColumn == 1 && ( x <= leftPanelsRight    && (x+w) >= leftPanelsLeft))  return leftPanels;
  if (window.showRightColumn == 1 && ((x+w) >= rightPanelsLeft &&  x <= rightPanelsRight ))   return rightPanels;
  return allPanels;
}

function onDragStart(el,x,y) {
  if (el.isDragging) return;
  if (enabletip==true) hideddrivetip();
  realParent = el.parentNode;
  parentNode = el.parentNode;
  var isAdded = (realParent != leftPanels && realParent != rightPanels);
  var x = getX(el);
  var y = getY(el);
  //dragel.style.height = (el.offsetHeight-2) +"px";
  dragel.style.height = (el.offsetHeight ? (el.offsetHeight-2) : el.displayHeight) + "px";
  dragel.style.display = "block";
  // Make the current absolute
  el.style.left = x + "px";
  el.style.top =  y + "px";
  el.style.zIndex = "10";

  if (isAdded){
    parentNode = allPanels;
    //debugwrite("y");
    el.placeholder = document.createElement("div");
    el.placeholder.className="placeholder";
    //el.placeholder.style.height = (el.offsetHeight-2) +"px";
    el.placeholder.style.height = (el.offsetHeight ? (el.offsetHeight-2) : el.displayHeight) + "px";
    realParent.replaceChild(el.placeholder, el);
    el.placeholder.style.display = "block";
    allPanels.appendChild(dragel);
    allPanels.style.backgroundColor = "#EEE";
    //realParent.insertBefore(el.placeHolder, dragel);
  }
  else {
    realParent.replaceChild(dragel, el);
  }
  el.style.position = "absolute";
  document.body.appendChild(el);
  el.isDragging = true;
  prevcolumn = parentNode;
  //dragel.style.height = (el.offsetHeight-2) +"px";
  dragel.style.height = (el.offsetHeight ? (el.offsetHeight-2) : el.displayHeight) + "px";
}

function onDrag(el,x,y) {
  if (enabletip==true) hideddrivetip();
  parentNode = getClosestDropTarget(x,y, el.offsetWidth, el.offsetHeight);
  if(parentNode != prevcolumn){
    prevcolumn.removeChild(dragel);
    if (parentNode != allPanels){
      parentNode.appendChild(dragel);
      allPanels.style.backgroundColor = "#FFF";
    }
    else{
      allPanels.style.backgroundColor = "#EEE";
    }
  }
  prevcolumn = parentNode;
  var list = getBlocList(parentNode);
  var pos = getDragBoxPos(list, y);
  if(pos == -1) return;
  if (list.length==0){
    parentNode.appendChild(dragel);
  }
  else if (pos!=0 && y<=getY(list[pos-1])) {
    parentNode.insertBefore(dragel, list[pos-1]);
  }
  else if (pos!=(list.length-1) && y>=getY(list[pos+1])) {
    if (list[pos+2]) {
      parentNode.insertBefore(dragel, list[pos+2]);
    } else {
      parentNode.appendChild( dragel); 
    }
  }
}

function onDragEnd(el,x,y) {
  el.isDragging = false;
  el.style.position = "static";
  if (parentNode == allPanels){
    el.placeholder.parentNode.replaceChild(el, el.placeholder);
    el.placeholder = undefined;
    dragel.parentNode.removeChild(dragel);
    allPanels.style.backgroundColor = "#FFF";
  }
  else{
    parentNode.replaceChild(el, dragel);
  }
  dragel.style.display = "none";
}

//------------------
// threadsafe asynchronous XMLHTTPRequest code
function executeCommand(url, callback) {


    // we use a javascript feature here called "inner functions"
    // using these means the local variables retain their values after the outer function
    // has returned. this is useful for thread safety, so
    // reassigning the onreadystatechange function doesn't stomp over earlier requests.


  function ajaxBindCallback() {
    if (ajaxRequest.readyState == 4) {
      if (ajaxRequest.status == 200) {
        if (ajaxCallback) {
          ajaxCallback(ajaxRequest.responseText);
        } else {
          alert('no callback defined');
        }
      } else {
        alert("There was a problem retrieving the xml data:\n" + ajaxRequest.status + ":\t" + ajaxRequest.statusText + "\n" + ajaxRequest.responseText);
      }
    }
  }

  // addMessage(url);
  // use a local variable to hold our request and callback until the inner function is called...
  var ajaxRequest = null;
  var ajaxCallback = callback;

  // bind our callback then hit the server...
  if (window.XMLHttpRequest) {
    // moz et al
    ajaxRequest = new XMLHttpRequest();
    ajaxRequest.onreadystatechange = ajaxBindCallback;
    ajaxRequest.open("GET", url, true);
    ajaxRequest.send(null);
  } else if (window.ActiveXObject) {
    // ie
    ajaxRequest = new ActiveXObject("Microsoft.XMLHTTP");
    if (ajaxRequest) {
      ajaxRequest.onreadystatechange = ajaxBindCallback;
      ajaxRequest.open("GET", url, true);
      ajaxRequest.send();
    }
    else{
      alert("your browser does not support xmlhttprequest" )
    }
  }
  else{
    alert("your browser does not support xmlhttprequest" )
  }
}


function start1() {
  var i;
  var j;
  var pos;
  //attaching events to all panels
  var divs = document.getElementsByTagName("div");
  for(i = 0; i < divs.length; i++) {
    el = divs[i];
    var id = el.id;
    if(isPanel(el)) {
      attachDragHandler(el);
    }
  }
  //replacing used panels in the list with placeholders and attaching placeholders
  var panelsInList  = getAllPanels(allPanels);
  var panelsOnLeft  = getBlocList(leftPanels);
  var panelsOnRight = getBlocList(rightPanels);
  //
  var el;
  for (i = 0; i < panelsInList.length; i++){
    pos = window.allPanelsPlace[i]['left'];
    if (pos != -1){
      el = panelsOnLeft[pos];
      el.fullname=window.allPanelsPlace[i].fullname;
      el.placeholder = document.createElement("div");
      el.placeholder.className="placeholder";
      el.displayHeight = panelsInList[i].offsetHeight-2;
      el.placeholder.style.height = (el.displayHeight) +"px";
      el.placeholder.style.display = "block";
      panelsInList[i].parentNode.replaceChild(el.placeholder, panelsInList[i]);
    }
    pos = window.allPanelsPlace[i]['right'];
    if (pos != -1){
      el = panelsOnRight[pos];
      el.fullname=window.allPanelsPlace[i].fullname;
      el.placeholder = document.createElement("div");
      el.placeholder.className="placeholder";
      el.displayHeight = panelsInList[i].offsetHeight-2;
      el.placeholder.style.height = (el.displayHeight) +"px";
      el.placeholder.style.display = "block";
      panelsInList[i].parentNode.replaceChild(el.placeholder, panelsInList[i]);
    }
    panelsInList[i].fullname=window.allPanelsPlace[i].fullname;
  }
  //this is for the "revert" button:
  leftPanels.savedPanelList = getBlocList(leftPanels);
  rightPanels.savedPanelList = getBlocList(rightPanels);
  leftPanels.isVisible = window.showLeftColumn;
  rightPanels.isVisible = window.showRightColumn;
  if(!leftPanels.isVisible){
    leftPanels.panels = getBlocList(leftPanels);
  }
  if(!rightPanels.isVisible){
    rightPanels.panels = getBlocList(rightPanels);
  }
  //
  var layoutMaquettesTD = document.getElementById("PageLayoutSection").getElementsByTagName("td");
  layoutMaquettes = new Object();
  for (i = 0; i < layoutMaquettesTD.length; i++){
    for (j = 0; j < layoutMaquettesTD[i].childNodes.length; ++j){
      if (layoutMaquettesTD[i].childNodes[j].tagName == "DIV"){
        layoutMaquettes[i] = layoutMaquettesTD[i].childNodes[j];
        break;
      }
    }
  }
  new Rico.Accordion('panellistaccordion', {panelHeight:'max'} );
  window.activeWizardPage = document.getElementById("PageLayoutSection");
  window.activeWizardTab  = document.getElementById("firstwtab");
  document.getElementById("PanelListSection").style.display = "none";
  //document.getElementById("CreateSection").style.display = "none";

}

function attachDragHandler(el){
  el.ondblclick = function(ev) {};
  Drag.init(el,el);
  el.onDragStart = function (x,y) { onDragStart(this,x,y);};
  el.onDrag = function (x,y) { onDrag(this,x,y);};
  el.onDragEnd = function (x,y) { onDragEnd(this,x,y);};
  var titlebar = el.getElementsByTagName("h5").item(0);
  if(titlebar){
    titlebar.onclick=function(ev) {};
    titlebar.onClick=function(ev) {};
  }
}

function getBlocNameList(el) {
  var list = "";
  var nb = el.childNodes.length;
  for (var i=0; i<nb; i++) {
    var el2 = el.childNodes[i];
    if(isPanel(el2)) {
      if (!el2.isDragging)  {
        if (list!="")
          list+=",";
          list += el2.fullname;
      }
    }
  }
  return list;
}

function save() {
  var res = true;//confirm("The Panel Layout saving is in Beta. Please confirm you want to save your layout");
  if (res==true) {
  url = window.ajaxurl;
    var leftPanelsList = getBlocNameList(leftPanels);
    url += "&leftPanels=" + leftPanelsList;
    url += "&showLeftPanels=" + window.showLeftColumn;
    var rightPanelsList = getBlocNameList(rightPanels);
    url += "&rightPanels=" + rightPanelsList;
    url += "&showRightPanels=" + window.showRightColumn;
    executeCommand(url, saveResult);
  }
}

function saveResult(html) {
  if(html=="SUCCESS"){
    alert("Panels Layout have been saved propertly")
    //this is for the "revert" button:
    leftPanels.savedPanelList = getBlocList(leftPanels);
    rightPanels.savedPanelList = getBlocList(rightPanels);
    leftPanels.isVisible = window.showLeftColumn;
    rightPanels.isVisible = window.showRightColumn;
  }
  else {
    alert("An error occured while trying to save the panel layout")
    alert(html)
  }
}

function releasePanels(column){
  column.panels = getBlocList(column);
  for (var i = 0; i < column.panels.length; ++i){
    releasePanel(column.panels[i]);
  }
}

function releasePanel(el){
  el.parentNode.removeChild(el);
  el.placeholder.parentNode.replaceChild(el, el.placeholder);
  el.placeholder = undefined;
}

function restorePanels(column){
  for (var i = 0; i < column.panels.length; ++i){
    if (!column.panels[i].placeholder)
      restorePanel(column.panels[i], column);
  }
  column.panels = undefined;
}

function revertPanels(column){
  for (var i = 0; i < column.savedPanelList.length; ++i){
      restorePanel(column.savedPanelList[i], column);
  }
}

function restorePanel(el, column){
  el.placeholder = document.createElement("div");
  el.placeholder.className="placeholder";
  //el.placeholder.style.height = (el.offsetHeight-2) +"px";
  el.placeholder.style.height = (el.offsetHeight ? (el.offsetHeight-2) : el.displayHeight) + "px";
  el.placeholder.style.display = "block";
  el.parentNode.replaceChild(el.placeholder, el);
  column.appendChild(el);
}

function changePreviewLayout(element, code){
  document.getElementById("selectedoption").id = "";
  element.id="selectedoption";
  switch(code){
    case 0:
      //hide left; hide right;
      if (window.showLeftColumn == 1){
        window.showLeftColumn = 0;
        leftPanels.style.display = "none";
        releasePanels(leftPanels);
      }
      if (window.showRightColumn == 1){
        window.showRightColumn = 0;
        rightPanels.style.display = "none";
        releasePanels(rightPanels);
      }
      mainContent.className = "contenthidelefthideright";
      mainContainer.className = "hidelefthideright";
      break;
    case 1:
      //show left; hide right;
      if (window.showLeftColumn == 0){
        window.showLeftColumn = 1;
        leftPanels.style.display = "block";
        restorePanels(leftPanels);
      }
      if (window.showRightColumn == 1){
        window.showRightColumn = 0;
        rightPanels.style.display = "none";
        releasePanels(rightPanels);
      }
      mainContent.className = "contenthideright";
      mainContainer.className = "hideright";
      break;
    case 2:
      //hide left; show right;
      if (window.showLeftColumn == 1){
        window.showLeftColumn = 0;
        leftPanels.style.display = "none";
        releasePanels(leftPanels);
      }
      if (window.showRightColumn == 0){
        window.showRightColumn = 1;
        rightPanels.style.display = "block";
        restorePanels(rightPanels);
      }
      mainContent.className = "contenthideleft";
      mainContainer.className = "hideleft";
      break;
    case 3:
      //show left; show right;
      if (window.showLeftColumn == 0){
        window.showLeftColumn = 1;
        leftPanels.style.display = "block";
        restorePanels(leftPanels);
      }
      if (window.showRightColumn == 0){
        window.showRightColumn = 1;
        rightPanels.style.display = "block";
        restorePanels(rightPanels);
      }
      mainContent.className = "content";
      mainContainer.className = "hidenone";
      break;
    default:
      // ignore
      break;
  }
  computeBounds();
}

function revert(){
  releasePanels(leftPanels);
  releasePanels(rightPanels);
  revertPanels(leftPanels);
  revertPanels(rightPanels);
  var layoutCode = 0;
  if (leftPanels.isVisible) layoutCode  += 1;
  if (rightPanels.isVisible) layoutCode += 2;
  changePreviewLayout(layoutMaquettes[layoutCode], layoutCode);
}

//----------------------------------------------------------------

function switchToWizardPage(el, toShowID){
  window.activeWizardPage.style.display="none";
  window.activeWizardTab.className="";
  window.activeWizardTab = el;
  window.activeWizardTab.className="active";
  window.activeWizardPage = document.getElementById(toShowID)
  window.activeWizardPage.style.display = "block";
  el.blur();
}

//----------------------------------------------------------------


function panelEditorInit(){
  tipobj=document.all? document.all["dhtmltooltip"] : document.getElementById? document.getElementById("dhtmltooltip") : ""

  parentNode = null;
  realParent = null;
  dragel = document.createElement("div");
  dragel.id = "dragbox";
  dragel.className = "panel";
  dragWidth = 0;
  nb = 0;

  layoutMaquetes = null;
  window.leftPanels    = document.getElementById("leftPanels");
  window.rightPanels   = document.getElementById("rightPanels");
  allPanels     = document.getElementById("allviewpanels");
  mainContent   = document.getElementById("contentcolumn");
  mainContainer = document.getElementById("contentcontainer");

  leftPanelsLeft   = getX(leftPanels);
  leftPanelsRight  = leftPanelsLeft  + leftPanels.offsetWidth;
  rightPanelsLeft  = getX(rightPanels);
  rightPanelsRight = rightPanelsLeft + rightPanels.offsetWidth;
  allpanelsLeft    = getX(allPanels);
  allpanelsTop     = getY(allPanels);

  prevcolumn = allPanels;

  start1();
}

setTimeout("panelEditorInit()", 100);