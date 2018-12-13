function computeBounds() {
  leftPanelsLeft   = getX(leftPanels);
  leftPanelsRight  = leftPanelsLeft  + leftPanels.offsetWidth;
  rightPanelsLeft  = getX(rightPanels);
  rightPanelsRight = rightPanelsLeft + rightPanels.offsetWidth;
  allpanelsLeft    = getX(allPanels);
  allpanelsTop     = getY(allPanels);
}

function debugwrite(sometext) {
  document.getElementById("headerglobal").appendChild(document.createTextNode(sometext));
}

function isPanel(node) {
  return node && node.nodeType === 1 && node.classList.contains('panel') &&
    $(node).up('#leftPanels, #rightPanels, #allviewpanels');
}

function getX(el) {
  if (el.offsetParent) {
    if (window.ActiveXObject) {
      return el.offsetLeft + getX(el.offsetParent) + el.clientLeft;
    } else {
      return el.offsetLeft + getX(el.offsetParent) + (el.scrollWidth - el.clientWidth);
    }
  } else {
    if (el.x) {
      return el.x;
    } else {
      return 0;
    }
  }
}

function getY(el) {
  if (el.offsetParent) {
    if (window.ActiveXObject) {
      return el.offsetTop + getY(el.offsetParent) + el.clientTop;
    } else {
      return el.offsetTop + getY(el.offsetParent) + (el.scrollHeight - el.clientHeight);
    }
  } else {
    if (el.y) {
      return el.y;
    } else {
      return 0;
    }
  }
}

function getBlocList(el) {
  var list = [];
  var nb = el.childNodes.length;
  for (var i = 0; i < nb; ++i) {
    var el2 = el.childNodes[i];
    if (isPanel(el2)) {
      if (!el2.isDragging) {
        list.push(el2);
      }
    }
  }
  return list;
}

function getDragBoxPos(list, y) {
  var nb = list.length;
  if (nb == 0) {
    return 0;
  }
  for (var i = 0; i < nb; ++i) {
    if (list[i] == dragel)
      return i;
  }
  return -1;
}

function getAllPanels(el){
  var list = [];
  var divs = el.getElementsByTagName("div");
  var j = 0;
  for (var i = 0; i < divs.length; ++i) {
    if (isPanel(divs[i])) {
      list[j] = divs[i];
      j++;
    }
  }
  return list;
}

function getClosestDropTarget(x, y, w, h) {
  if (window.showLeftColumn == 1 && (x <= leftPanelsRight && (x + w) >= leftPanelsLeft)) {
    return leftPanels;
  }
  if (window.showRightColumn == 1 && ((x + w) >= rightPanelsLeft &&  x <= rightPanelsRight )) {
    return rightPanels;
  }
  return allPanels;
}

function onDragStart(el, x, y) {
  if (el.isDragging) {
    return;
  }
  hideTip();
  window.isDraggingPanel = true;
  if (enabletip==true) {
    hideTip();
  }
  realParent = el.parentNode;
  parentNode = el.parentNode;
  var isAdded = (realParent != leftPanels && realParent != rightPanels);
  var coords = Position.cumulativeOffset(el);
  var coords2 = Position.realOffset(el);
  var x = coords[0];
  var y = coords[1] - coords2[1] + (document.documentElement.scrollTop - 0 + document.body.scrollTop - 0);
  if (window.ActiveXObject) {
    dragel.style.height = (el.offsetHeight ? (el.offsetHeight) : el.displayHeight) + "px";
  } else {
    dragel.style.height = (el.offsetHeight ? (el.offsetHeight-2) : el.displayHeight) + "px";
  }
  dragel.style.display = "block";
  // Make the current absolute
  el.style.left = x + "px";
  el.style.top =  y + "px";
  el.style.zIndex = "10";

  if (isAdded) {
    parentNode = allPanels;
    el.placeholder = document.createElement("div");
    el.placeholder.className="placeholder";
    if (window.ActiveXObject) {
      el.placeholder.style.height = (el.offsetHeight ? (el.offsetHeight) : el.displayHeight) + "px";
    } else {
      el.placeholder.style.height = (el.offsetHeight ? (el.offsetHeight-2) : el.displayHeight) + "px";
    }
    realParent.replaceChild(el.placeholder, el);
    el.placeholder.style.display = "block";
    addClass(allPanels, "dropTarget");
  } else {
    realParent.replaceChild(dragel, el);
  }
  // Make the current absolute
  el.style.position = "absolute";
  document.body.appendChild(el);
  el.isDragging = true;
  prevcolumn = parentNode;
}

function onDrag(el, x, y) {
  if (enabletip==true) {
    hideTip();
  }
  parentNode = getClosestDropTarget(x,y, el.offsetWidth, el.offsetHeight);
  if (parentNode != prevcolumn) {
    if (prevcolumn != allPanels) {
      prevcolumn.removeChild(dragel);
    }
    if (parentNode != allPanels) {
      parentNode.appendChild(dragel);
      rmClass(allPanels, "dropTarget");
    } else{
      addClass(allPanels, "dropTarget");
    }
  }
  prevcolumn = parentNode;
  var list = getBlocList(parentNode);
  var pos = getDragBoxPos(list, y);
  if (pos == -1) {
    return;
  }
  if (list.length == 0) {
    if (parentNode != allPanels) {
      parentNode.appendChild(dragel);
    }
  } else if (pos != 0 && y <= getY(list[pos-1])) {
    parentNode.insertBefore(dragel, list[pos-1]);
  } else if (pos != (list.length-1) && y >= getY(list[pos+1])) {
    if (list[pos+2]) {
      parentNode.insertBefore(dragel, list[pos+2]);
    } else {
      if (parentNode != allPanels) {
        parentNode.appendChild( dragel);
      } else {
        dragel.parentNode.removeChild(dragel);
      }
    }
  }
}

function onDragEnd(el, x, y) {
  el.isDragging = false;
  window.isDraggingPanel = false;
  el.style.position = "static";
  if (parentNode == allPanels) {
    el.placeholder.parentNode.replaceChild(el, el.placeholder);
    el.placeholder = undefined;
    rmClass(allPanels, "dropTarget");
  } else{
    parentNode.replaceChild(el, dragel);
  }
  dragel.style.display = "none";
  updatePanelLayout();
}

var updatePanelLayout = function() {
  if (leftPanelsInput && !leftPanelsInput.disabled) {
    leftPanelsInput.value = getBlocNameList(leftPanels);
  }
  if (rightPanelsInput && !rightPanelsInput.disabled) {
    rightPanelsInput.value = getBlocNameList(rightPanels);
  }
};

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
          new XWiki.widgets.Notification('no callback defined', 'error');
        }
      } else {
        new XWiki.widgets.Notification("There was a problem retrieving the xml data:\n" + ajaxRequest.status + ":\t"
            + ajaxRequest.statusText + "\n" + ajaxRequest.responseText, 'error');
      }
    }
  }
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
    } else{
      new XWiki.widgets.Notification('your browser does not support xmlhttprequest', 'error');
    }
  } else{
    new XWiki.widgets.Notification("your browser does not support xmlhttprequest", 'error');
  }
}

function start1() {
  var i;
  var j;
  var pos;
  //attaching events to all panels
  var divs = document.getElementsByTagName("div");
  for (i = 0; i < divs.length; ++i) {
    el = divs[i];
    var id = el.id;
    if (isPanel(el)) {
      attachDragHandler(el);
    }
  }
  //replacing used panels in the list with placeholders and attaching placeholders
  window.panelsInList = getAllPanels(allPanels);
  window.panelsOnLeft = getBlocList(leftPanels);
  window.panelsOnRight = getBlocList(rightPanels);
  //
  var el;
  for (i = 0; i < panelsInList.length; ++i){
    pos = window.allPanelsPlace[i]['left'];
    if (pos != -1) {
      el = panelsOnLeft[pos];
      if (el) {
        el.fullname = window.allPanelsPlace[i].fullname;
        el.placeholder = document.createElement("div");
        el.placeholder.className = "placeholder";
        if (window.ActiveXObject) {
          el.displayHeight = (el.offsetHeight ? (el.offsetHeight) : 0);
        } else {
          el.displayHeight = (el.offsetHeight ? (el.offsetHeight-2) : 0);
        }
        el.placeholder.style.height = (el.displayHeight) +"px";
        el.placeholder.style.display = "block";
        panelsInList[i].parentNode.replaceChild(el.placeholder, panelsInList[i]);
      }
    }
    pos = window.allPanelsPlace[i]['right'];
    if (pos != -1) {
      el = panelsOnRight[pos];
      if (el) {
        el.fullname = window.allPanelsPlace[i].fullname;
        el.placeholder = document.createElement("div");
        el.placeholder.className = "placeholder";
        if (window.ActiveXObject) {
          el.displayHeight = (el.offsetHeight ? (el.offsetHeight) : 0);
        } else {
          el.displayHeight = (el.offsetHeight ? (el.offsetHeight-2) : 0);
        }
        el.placeholder.style.height = (el.displayHeight) +"px";
        el.placeholder.style.display = "block";
        if (panelsInList[i].parentNode) {
          panelsInList[i].parentNode.replaceChild(el.placeholder, panelsInList[i]);
        }
      }
    }
    panelsInList[i].fullname=window.allPanelsPlace[i].fullname;
  }
  //this is for the "revert" button:
  leftPanels.savedPanelList = getBlocList(leftPanels);
  rightPanels.savedPanelList = getBlocList(rightPanels);
  leftPanels.isVisible = window.showLeftColumn;
  rightPanels.isVisible = window.showRightColumn;
  if (!leftPanels.isVisible) {
    leftPanels.panels = getBlocList(leftPanels);
  }
  if (!rightPanels.isVisible) {
    rightPanels.panels = getBlocList(rightPanels);
  }
  layoutMaquettes = $('PageLayoutSection').select('.pagelayoutoption');
}

function attachDragHandler(el) {
  el.ondblclick = function(ev) {};
  Drag.init(el,el);
  el.onDragStart = function (x,y) {
    onDragStart(this,x,y);
  };
  el.onDrag = function (x,y) {
    onDrag(this,x,y);
  };
  el.onDragEnd = function (x,y) {
    onDragEnd(this,x,y);
  };
}

function isAttachedPanel(element) {
  if (element.ondblclick) {
    return true;
  }

  return false;
}

function getBlocNameList(el) {
  var list = "";
  var nb = el.childNodes.length;
  for (var i = 0; i < nb; ++i) {
    var el2 = el.childNodes[i];
    if (isPanel(el2)) {
      if (!el2.isDragging) {
        if (list != "") {
          list += ",";
        }
        list += el2.fullname;
      }
    }
  }
  return list;
}

function save() {
  url = window.ajaxurl;  
  url += "&showLeftPanels=" + window.showLeftColumn;
  url += "&showRightPanels=" + window.showRightColumn;
  if (window.showLeftColumn) {
    var leftPanelsList = leftPanelsInput ? leftPanelsInput.value : getBlocNameList(leftPanels);
    url += "&leftPanels=" + leftPanelsList;
    url += "&leftPanelsWidth=" + leftPanelsWidthInput.value;
  }
  if (window.showRightColumn) {
    var rightPanelsList = rightPanelsInput ? rightPanelsInput.value : getBlocNameList(rightPanels);
    url += "&rightPanels=" + rightPanelsList;
    url += "&rightPanelsWidth=" + rightPanelsWidthInput.value;
  }
  executeCommand(url, saveResult);
}

function saveResult(html) {
  if (html=="SUCCESS") {
    new XWiki.widgets.Notification(window.panelsavesuccess, 'done');
    // this is for the "revert" button:
    leftPanels.savedPanelList = getBlocList(leftPanels);
    rightPanels.savedPanelList = getBlocList(rightPanels);
    leftPanels.isVisible = window.showLeftColumn;
    rightPanels.isVisible = window.showRightColumn;
  } else {
    // Alerts are more appropriate to display the "html" message
    alert(window.panelsaveerror);
    alert(html);
  }
}

function releasePanels(column) {
  column.panels = getBlocList(column);
  for (var i = 0; i < column.panels.length; ++i) {
    releasePanel(column.panels[i]);
  }
}

function releasePanel(el) {
  el.parentNode.removeChild(el);
  el.placeholder.parentNode.replaceChild(el, el.placeholder);
  el.placeholder = undefined;
}

function restorePanels(column) {
  for (var i = 0; i < column.panels.length; ++i) {
    if (!column.panels[i].placeholder) {
      restorePanel(column.panels[i], column);
    }
  }
  column.panels = undefined;
}

function revertPanels(column) {
  for (var i = 0; i < column.savedPanelList.length; ++i) {
    restorePanel(column.savedPanelList[i], column);
  }
}

function restorePanel(el, column) {
  el.placeholder = document.createElement("div");
  el.placeholder.className = "placeholder";
  if (window.ActiveXObject) {
    el.placeholder.style.height = (el.offsetHeight ? (el.offsetHeight) : 0);
  } else {
    el.placeholder.style.height = (el.offsetHeight ? (el.offsetHeight-2) : 0);
  }
  el.placeholder.style.display = "block";
  el.parentNode.replaceChild(el.placeholder, el);
  column.appendChild(el);
}

function changePreviewLayout(element, code) {
  document.getElementById("selectedoption").id = "";
  element.id = "selectedoption";
  mainContainer.removeClassName("hidelefthideright");
  mainContainer.removeClassName("hideright");
  mainContainer.removeClassName("hideleft");
  mainContainer.removeClassName("content");
  switch (code) {
    case 0:
      //hide left; hide right;
      if (window.showLeftColumn == 1) {
        window.showLeftColumn = 0;
        leftPanels.style.display = "none";
        releasePanels(leftPanels);
      }
      if (window.showRightColumn == 1) {
        window.showRightColumn = 0;
        rightPanels.style.display = "none";
        releasePanels(rightPanels);
      }
      // mainContainer.className = "contenthidelefthideright";
      mainContainer.addClassName("hidelefthideright");
      leftPanelsInput && leftPanelsInput.disable();
      leftPanelsWidthInput.disable();
      rightPanelsInput && rightPanelsInput.disable();
      rightPanelsWidthInput.disable();
      break;
    case 1:
      //show left; hide right;
      if (window.showLeftColumn == 0) {
        window.showLeftColumn = 1;
        leftPanels.style.display = "block";
        restorePanels(leftPanels);
      }
      if (window.showRightColumn == 1) {
        window.showRightColumn = 0;
        rightPanels.style.display = "none";
        releasePanels(rightPanels);
      }
      // mainContainer.className = "contenthideright";
      mainContainer.addClassName("hideright");
      leftPanelsInput && leftPanelsInput.enable();
      leftPanelsWidthInput.enable();
      rightPanelsInput && rightPanelsInput.disable();
      rightPanelsWidthInput.disable();
      break;
    case 2:
      //hide left; show right;
      if (window.showLeftColumn == 1) {
        window.showLeftColumn = 0;
        leftPanels.style.display = "none";
        releasePanels(leftPanels);
      }
      if (window.showRightColumn == 0) {
        window.showRightColumn = 1;
        rightPanels.style.display = "block";
        restorePanels(rightPanels);
      }
      // mainContainer.className = "contenthideleft";
      mainContainer.addClassName("hideleft");
      leftPanelsInput && leftPanelsInput.disable();
      leftPanelsWidthInput.disable();
      rightPanelsInput && rightPanelsInput.enable();
      rightPanelsWidthInput.enable();
      break;
    case 3:
      //show left; show right;
      if (window.showLeftColumn == 0) {
        window.showLeftColumn = 1;
        leftPanels.style.display = "block";
        restorePanels(leftPanels);
      }
      if (window.showRightColumn == 0) {
        window.showRightColumn = 1;
        rightPanels.style.display = "block";
        restorePanels(rightPanels);
      }
      mainContainer.addClassName("content");
      leftPanelsInput && leftPanelsInput.enable();
      leftPanelsWidthInput.enable();
      rightPanelsInput && rightPanelsInput.enable();
      rightPanelsWidthInput.enable();
      break;
    default:
      // ignore
      break;
  }
  computeBounds();
}

function revert() {
  releasePanels(leftPanels);
  releasePanels(rightPanels);
  revertPanels(leftPanels);
  revertPanels(rightPanels);
  var layoutCode = 0;
  if (leftPanels.isVisible) {
    layoutCode  += 1;
  }
  if (rightPanels.isVisible) {
    layoutCode += 2;
  }
  setPanelWidth();
  changePreviewLayout(layoutMaquettes[layoutCode], layoutCode);
  updatePanelLayout();
}

//----------------------------------------------------------------

function setPanelWidth() {
  var classesToRemove = ['Small', 'Medium', 'Large'];
  for (var i=0; i<classesToRemove.length; ++i) {
    var c = classesToRemove[i];
    mainContainer.removeClassName('panel-left-width-'+c);
    mainContainer.removeClassName('panel-right-width-'+c);
    window.leftPanels.removeClassName('panel-width-'+c);
    window.rightPanels.removeClassName('panel-width-'+c);
  }
  var leftPanelsWidth = leftPanelsWidthInput.value != '---' ? leftPanelsWidthInput.value : 'Medium';
  var rightPanelsWidth = rightPanelsWidthInput.value != '---' ? rightPanelsWidthInput.value : 'Medium';
  mainContainer.addClassName('panel-left-width-'+leftPanelsWidth);
  mainContainer.addClassName('panel-right-width-'+rightPanelsWidth);
  window.leftPanels.addClassName('panel-width-'+leftPanelsWidth);
  window.rightPanels.addClassName('panel-width-'+rightPanelsWidth);
}

//----------------------------------------------------------------

function panelEditorInit() {
  // Indicate that we started initialize the panel editor to avoid race conditions
  panelInitStarted = true;
  // Stop listening from inserted elements
  panelsObserver.disconnect()

  tipobj = $("dhtmltooltip");

  parentNode = null;
  realParent = null;
  dragel = new Element("div", {'id' : 'dragbox', 'class' : 'panel'});
  dragWidth = 0;
  nb = 0;

  layoutMaquetes = null;
  window.leftPanels = $("leftPanels");
  window.rightPanels = $("rightPanels");
  allPanels = $("allviewpanels");
  mainContent = $("contentcolumn");
  // mainContainer = document.getElementById("contentcontainer");
  mainContainer = $("body");
  leftPanelsLeft   = getX(leftPanels);
  leftPanelsRight  = leftPanelsLeft  + leftPanels.offsetWidth;
  rightPanelsLeft  = getX(rightPanels);
  rightPanelsRight = rightPanelsLeft + rightPanels.offsetWidth;
  allpanelsLeft    = getX(allPanels);
  allpanelsTop     = getY(allPanels);
  leftPanelsInput  = $("XWiki.XWikiPreferences_0_leftPanels");
  leftPanelsWidthInput  = $("XWiki.XWikiPreferences_0_leftPanelsWidth");
  rightPanelsInput = $("XWiki.XWikiPreferences_0_rightPanels");
  rightPanelsWidthInput = $("XWiki.XWikiPreferences_0_rightPanelsWidth");

  leftPanelsWidthInput.observe('change', setPanelWidth);
  rightPanelsWidthInput.observe('change', setPanelWidth);

  prevcolumn = allPanels;

  start1();

  // Update the enabled/disable state of the left/right panel inputs.
  var selectedLayout = $('selectedoption');
  changePreviewLayout(selectedLayout, selectedLayout.previousSiblings().size());
}

// Listen to asynchronously panels
panelsObserver = new MutationObserver(function(mutations)
{
  for (var i = 0; i < mutations.length; i++) {
    var mutation = mutations[i];

    for (var j = 0; i < mutation.addedNodes.length; j++) {
      var element = mutation.addedNodes[j];

      if (element.tagName == 'DIV') {
        if (isPanel(element) && !isAttachedPanel(element)) {
          maybePanelEditorInit();
        }
      }
    }
  }
});
panelsObserver.observe(document, { childList: true, subtree : true});

function maybePanelEditorInit() {
  // Wait until all asyncrhonous panels are loaded before initializing panels editor
  if (!panelInitStarted && !document.getElementsByClassName('xwiki-async').length) {
    panelEditorInit()
  }
}

// Initialize panelInitStarted variable
panelInitStarted = false;

(XWiki && XWiki.isInitialized && maybePanelEditorInit())
|| document.observe('xwiki:dom:loading', maybePanelEditorInit);
