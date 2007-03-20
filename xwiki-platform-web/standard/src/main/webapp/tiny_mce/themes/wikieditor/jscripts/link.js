function init() {
    var text = tinyMCE.getWindowArg('text').toString();
    var href = tinyMCE.getWindowArg('href').toString();
    var target = tinyMCE.getWindowArg('target').toString();
    document.forms[0].wiki_text.value = text;
    document.forms[0].web_text.value = text;
    document.forms[0].file_text.value = text;
    document.forms[0].attach_text.value = text;
    document.forms[0].email_text.value = text;
    document.forms[0].wiki_page.value = text;

    if (linkPopupHasTab("wiki_tab")) {
        document.getElementById("wiki_tab").className = "current";
        document.getElementById("wiki_panel").className = "current";
    } else if (linkPopupHasTab("web_tab")) {
        document.getElementById("web_tab").className = "current";
        document.getElementById("web_panel").className = "current";
    } else if (linkPopupHasTab("attachments_tab")) {
        document.getElementById("attachments_tab").className = "current";
        document.getElementById("attachments_panel").className = "current";
    } else if (linkPopupHasTab("file_tab")) {
        document.getElementById("file_tab").className = "current";
        document.getElementById("file_panel").className = "current";
    } else if (linkPopupHasTab("email_tab")) {
        document.getElementById("email_tab").className = "current";
        document.getElementById("email_panel").className = "current";
    }

    if ((href != null) && (href != "")) {
	    if (href.search(/(https?|ftp):\/\/[-a-zA-Z0-9+&@#\/%?=~_|!:,.;]*[-a-zA-Z0-9+&@#\/%=~_|]/gi)>-1) {
            if (linkPopupHasTab("web_tab")) {
                mcTabs.displayTab('web_tab','web_panel');
                document.forms[0].web_page.value = href;
                document.forms[0].web_target.value = target;
            }
        } else if (href.search(/wikiattachment:-:(.*?)/gi) > -1) {
            if (linkPopupHasTab("attachments_tab")) {
                mcTabs.displayTab('attachments_tab','attachments_panel');
                document.forms[0].attach_file.value = href.replace(/wikiattachment:-:/gi, "").replace(/%20/gi, " ");
            }
        } else if (href.search(/mailto:(.*?)/gi) > -1) {
            if (linkPopupHasTab("email_tab")) {
                mcTabs.displayTab('email_tab','email_panel')
                document.forms[0].email.value = href.replace(/mailto:/gi, "");
            }
        } else if (href.search(/file:(\/\/\/\/\/)(.*?)/gi) > -1) {
            if (linkPopupHasTab("file_tab")) {
                mcTabs.displayTab('file_tab','file_panel');
            }
        } else if (href.search(/file:(\/\/)(.*?)/gi) > -1) {
            if (linkPopupHasTab("file_tab")) {
                mcTabs.displayTab('file_tab','file_panel');
            }
        } else {
            if (linkPopupHasTab("wiki_tab")) {
                mcTabs.displayTab('wiki_tab','wiki_panel');
                var space = "", whref = href;
                if (href.indexOf(".") > -1) {
                    space = href.substring(0, href.indexOf("."));
                    whref = href.substring(href.indexOf(".") + 1, href.length);
                }
                document.forms[0].wiki_space.value = space;
                document.forms[0].wiki_page.value = whref;
                document.forms[0].wiki_target.value = target;
            }
        }
    }

    document.forms[0].insert.value = tinyMCE.getLang('lang_' + tinyMCE.getWindowArg('action'), 'Insert', true);

    var className = tinyMCE.getWindowArg('className');
    var editor_id = tinyMCE.getWindowArg('editor_id');
}

function insertLink() {
    var wikiTabElm = document.getElementById("wiki_tab");
    var webTabElm = document.getElementById("web_tab");
    var fileTabElm = document.getElementById("file_tab");
    var attachTabElm = document.getElementById("attachments_tab");
    var emailTabElm = document.getElementById("email_tab");
    var dummy;
    tinyMCEPopup.restoreSelection();

    if (wikiTabElm != null && wikiTabElm.className == "current") {
        var href = document.forms[0].wiki_page.value;
        var space = document.forms[0].wiki_space.value;
        var wikitext = document.forms[0].wiki_text.value;
        var target = document.forms[0].wiki_target.value;
        tinyMCE.themes['wikieditor'].insertLink(href, target, wikitext, space, "", dummy, "");

    } else if (webTabElm != null && webTabElm.className == "current") {
        var webtext = document.forms[0].web_text.value;
        var href = document.forms[0].web_page.value;
        var target = document.forms[0].web_target.value;
        tinyMCE.themes['wikieditor'].insertLink(href, target , webtext, "", "", dummy, "");

    } else if (attachTabElm != null && attachTabElm.className == "current") {
        var href = document.forms[0].attach_file.value;
        var text = document.forms[0].attach_text.value;
        tinyMCE.themes['wikieditor'].insertLink("wikiattachment:-:" + href, "", text, "", "", dummy, "");
    } else if (fileTabElm != null && fileTabElm.className == "current") {
        var text = document.forms[0].file_text.value;
        var href = document.forms[0].filepaths.value;
        var filepaths="";
        if (":" == href.charAt(href.indexOf("\\") - 1))
            filepaths = "file:\/\/" + href.replace(/\\/gi, "\/");
        else if (href.substring(0, 2) == "\\\\")
            filepaths = "file:\/\/\/" + href.replace(/\\/gi, "\/");
        tinyMCE.themes['wikieditor'].insertLink(filepaths, "", text, "", "", dummy, "");
    } else if (emailTabElm != null && emailTabElm.className == "current") {
        var text = document.forms[0].email_text.value;
        var email = document.forms[0].email.value;
        href = "mailto:" + email;

        tinyMCE.themes['wikieditor'].insertLink(href, "", text, "", "", dummy, "");
    }

    tinyMCEPopup.close();
}

function cancelAction() {
    tinyMCEPopup.close();
}

function populateWikiForm(value) {
    document.forms[0].href.value = value;
}

function updateAttachName(form) {
    form.xredirect.value=location;

    var fname = form.filepath.value;
    if (fname=="") {
        return false;
    }

    var i = fname.lastIndexOf('\\');
    if (i==-1)
        i = fname.lastIndexOf('/');

    fname = fname.substring(i+1);
    if (form.filename.value==fname)
        return true;

    if (form.filename.value=="")
        form.filename.value = fname;

    return true;
}

function linkPopupHasTab(str) {
    var linktabparam = tinyMCE.getParam("use_linkeditor_tabs");
    if (linktabparam == null || linktabparam == "") {
        linktabparam = "wiki_tab";
    }
    var linktabs = linktabparam.split(",");

    var hasTab = false;
    for (var i=0; i < linktabs.length; i++) {
        var re = /(\S+(\s+\S+)*)+/i;
        var r = re.exec(linktabs[i]);
        var tab = (r && r[1])?r[1]:"";
        if (tab == str) {
            hasTab = true;
        }
    }
    return hasTab;
}

