
//
// Store the Query Mode mode
//
var currentQueryMode = 0;

//
// Show/Hide the Query Menu 
//
function switchQueryMenu() {
 var list = document.body.getElementsByTagName("div")["queries"].getElementsByTagName("a");
 for (var i=0;i<list.length;i++) {
  list[i].style.display = (!currentQueryMode) ? "block" : "none";
 }
 currentQueryMode = !currentQueryMode;
 return false;
}

var currentAdminMode = 0;
var currentContentRight;

//
// Show/Hide the Admin Menu
//
function switchAdmin() {
 var list = document.body.getElementsByTagName("div")["admin"].getElementsByTagName("a");
 for (var i=0;i<list.length;i++) {
  list[i].style.display = (!currentAdminMode) ? "block" : "none";
 }
 currentAdminMode = !currentAdminMode;
 return false;
}

//
// Store the current View/Edit/Search mode (normal/advanced) 
//
var currentMode = 0;
var currentViewEditMode = 0;

function switchMode(mode, mode1, mode2) {
 var mode1 = document.body.getElementsByTagName("div")[mode1];
 var mode2 = document.body.getElementsByTagName("div")[mode2];
 if (mode1) mode1.style.display =  (mode == 0) ? "block" : "none"; 
 if (mode2) mode2.style.display =  (mode == 1) ? "block" : "none"; 
 return (1 - mode);
}

function switchCreateMode() {
 currentMode = switchMode(currentMode,null,"advanced");
}

function switchShowViewMode() {
 currentMode = switchMode(currentMode,null,"viewadv");
}

function switchShowEditMode() {
 currentMode = switchMode(currentMode,null,"editadv");
}

function switchSearchMode() {
 var knob = document.body.getElementsByTagName("div").knob;
 var bchart = document.body.getElementsByTagName("div").bchart;

 currentMode = switchMode(currentMode,null,"advanced");
 if (knob) knob.style.display =  (currentMode == 0) ? "block" : "none"; 
 if (bchart) bchart.style.display =  (currentMode == 0) ? "block" : "none"; 
}

function switchTabs(mode) {
 if (currentViewEditMode != mode) {   
  currentViewEditMode = switchMode(currentViewEditMode,"show_edit","show_view");
  var mode = (currentViewEditMode) ? "show_view" : "show_edit";
  var adv = document.body.getElementsByTagName("div")[mode + "adv"];
  if (adv) adv.style.display = (currentMode) ? "block" : "none";
 }   
}

function showHideComment(name) {
  var mydiv = document.getElementById(name);
  if (mydiv.style.display == "none")
   mydiv.style.display = "block";
  else
   mydiv.style.display = "none";
 }

function showAllComments() {
 var count = 0;

 if (document.getElementById("allbutlast") != null)
  document.getElementById("allbutlast").style.display = "block";
 if (document.getElementById("last") != null)
  document.getElementById("last").style.display = "block";

 while (count<1000) {
  var mydiv = document.getElementById("dc" + count);
  if (mydiv != null ) {
   mydiv.style.display = "block";
  } 
  else {
   return;
  }
  count++;
 }
}

function hideAllComments() {
 var count = 0;

 if (document.getElementById("dc0") != null)
  document.getElementById("dc0").style.display = "none";
 if (document.getElementById("allbutlast") != null)
  document.getElementById("allbutlast").style.display = "none";
 if (document.getElementById("last") != null)
  document.getElementById("last").style.display = "none";

 while (1) {
  var mydiv = document.getElementById("dc" + count);
  if (mydiv != null ) {
   mydiv.style.display = "none";
  } 
  else {
   return;
  }
  count++;
 }
}

