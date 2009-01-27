function insertImage() {
    var src = document.forms[0].href.value;
    var width = document.forms[0].width.value;
    var height = document.forms[0].height.value;
    var align = document.forms[0].align.options[document.forms[0].align.selectedIndex].value;
    var halign = document.forms[0].halign.options[document.forms[0].halign.selectedIndex].value;

    tinyMCEPopup.restoreSelection();

    tinyMCE.themes['wikieditor'].insertImage(src, width, height, align, halign);

    tinyMCEPopup.close();
}

function init() {
    var editor_id = tinyMCE.getWindowArg('editor_id');
    var href = tinyMCE.getWindowArg('src')
    document.forms[0].href.value = href.replace(/%20/gi, " ");
    document.forms[0].width.value = tinyMCE.getWindowArg('width');
	document.forms[0].height.value = tinyMCE.getWindowArg('height');

    for (var i=0; i<document.forms[0].align.options.length; i++) {
		if (document.forms[0].align.options[i].value == tinyMCE.getWindowArg('align'))
			document.forms[0].align.options.selectedIndex = i;
	}

    for (var i=0; i<document.forms[0].halign.options.length; i++) {
		if (document.forms[0].halign.options[i].value == tinyMCE.getWindowArg('halign'))
			document.forms[0].halign.options.selectedIndex = i;
	}

    document.forms[0].insert.value = tinyMCE.getLang('lang_' + tinyMCE.getWindowArg('action'), 'Insert', true);
}

function cancelAction() {
    tinyMCEPopup.close();
}

var preloadImg = new Image();

function resetImageData() {
	var formObj = document.forms[0];
	formObj.width.value = formObj.height.value = "";
}

function updateImageData() {
	var formObj = document.forms[0];

	if (formObj.width.value == "")
		formObj.width.value = preloadImg.width;

	if (formObj.height.value == "")
		formObj.height.value = preloadImg.height;
}

function getImageData() {
	preloadImg = new Image();
	tinyMCE.addEvent(preloadImg, "load", updateImageData);
	tinyMCE.addEvent(preloadImg, "error", function () {var formObj = document.forms[0];formObj.width.value = formObj.height.value = "";});
	preloadImg.src = tinyMCE.convertRelativeToAbsoluteURL(tinyMCE.settings['base_href'], document.forms[0].src.value);
}

