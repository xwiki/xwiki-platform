Ajax.XWikiRequest = Class.create();

Object.extend(Object.extend(Ajax.XWikiRequest.prototype, Ajax.Request.prototype), {
    initialize: function(space, docName, options, action) {

        this.transport = Ajax.getTransport();
        this.setOptions(options);
        if (action)
            this.action = action;
        else
            this.action = "view";
        this.baseUrl = "${request.contextPath}/bin/" + action;

        var onComplete = this.options.onComplete || Prototype.emptyFunction;
        this.options.onComplete = (function() {
            this.returnValue(onComplete);
            //onComplete(this.transport);
        }).bind(this);

        this.request(this.generateUrl(space, docName));
    },

    generateUrl: function(space, docName){
        return this.baseUrl + "/" + space + "/" + docName;
    },

    returnValue: function(callBack) {

        if (callBack)
            callBack(this.transport);
        else
            alert("error, callback");
    }
});

var XWiki = Class.create();

XWiki.prototype = {
    initialize: function(wikiUrl){this.wikiUrl = wikiUrl;},
    getSpaces: function(callBack){
        var params = '';
        var myAjax = new Ajax.XWikiRequest( "Ajax", "getSpaces", {method: 'get', parameters: params, onComplete: getSpacesCallBack} );
    },

    getSpacesCallBack: function(ajaxResponse){
        var xml = ajaxResponse.responseXML;

    }
}

function selectPackage(name)
{
    var str = "<input type=\"hidden\" name=\"name\" value=\"" +  name +  "\" />";
    $('importDocName').innerHTML = str;
    var pars = "action=getPackageInfos&name="+name+"&xpage=plain";
    var myAjax = new Ajax.XWikiRequest( "XWiki", "Import", {method: 'get', parameters: pars, onComplete: showPackageInfos} , "import");
}

function show(name)
{
    document.getElementById(name).style.display = "block";
}

function hide(name)
{
    document.getElementById(name).style.display = "none";
}

function getXmlValue(tag, xml)
{
    var nodes = xml.getElementsByTagName(tag);
    if (nodes.length > 0 && nodes[0].firstChild)
        return nodes[0].firstChild.data;
    else
        return "";
}

function showPackageInfos(res)
{
    var xml = res.responseXML;
    var name = getXmlValue("name", xml);

    $('selectedDocs').innerHTML = "";

    var nodes = xml.getElementsByTagName("file");
    if (nodes.length > 0)
    {
        hide("noSelectedDocs");
        hide("noDocsInArchive");

        for (var i = 0; i < nodes.length; i++)
        {
            var doc = nodes[i];
            var pageName = doc.firstChild.data;

            var language = doc.getAttribute("language");
            if (language!=null)
             pageName += ":" + language;

            insertNewDoc("selectedDocs", pageName, language);
        }

        show("importDocs");
        show("selectDocsActions");
    }
    else
    {
        hide("importDocs");
        hide("selectDocsActions");
        show("noSelectedDocs");
        show("noDocsInArchive");
    }
}

function insertNewDoc(id, value, language)
{
    var str = "<div class='importDoc'>";
    str += "<input type='checkBox' name='pages' value='" + value + "' class='selCheckedDoc' id='sel_" + value + "' checked />";

    // Add language
    var sLanguage = "";
    if (language!=null)
     sLanguage = "" + language;
    var htmlLanguage = "<input type=\"hidden\" name=\"language_" + value + "\" value=\"" + sLanguage + "\" />";
    str += htmlLanguage;

    // Add name
    str += value;
    if ((language!=null)&&(language!="")) {
        str += " (" + language + ")";
    }
    str  += "</div>";
    new Insertion.Bottom(id, str);
}

function actionToString(actionId)
{
    if (actionId == "0")
        return "overwrite";
    else
        return "skip";
}

function actionToInt(action)
{
    if (action == "overwrite")
        return 0;
    else
        return 1;
}


function setDocsAction(action)
{
    var docs = document.getElementsByClassName("exportDocName");
    var i;
    for (i = 0; i < docs.length; i++)
    {
        var doc = docs[i];
        if ($('sel_' + doc.innerHTML).checked)
            $('action_' + doc.innerHTML).innerHTML = action;
    }
}

function deleteDocs(force)
{
    var docs = document.getElementsByClassName("exportDocName");
    var i;
    for (i = 0; i < docs.length; i++)
    {
        var doc = docs[i];
        if ($('sel_' + doc.innerHTML).checked || force)
            Element.remove('tr_' + doc.innerHTML);
    }
}

function selectItems(classId, selected)
{
    var docs = document.getElementsByClassName(classId);
    var i;
    for (i = 0; i < docs.length; i++)
    {
        var doc = docs[i];
        doc.checked = selected;
    }
}

