function init() {
    var text = tinyMCE.getWindowArg('text').toString();
    var href = tinyMCE.getWindowArg('href').toString();
    document.forms[0].wiki_text.value = text;
    document.forms[0].web_text.value = text;
    document.forms[0].attach_text.value = text;
    document.forms[0].email_text.value = text;

    if ((href != null) && (href != "")) {
	    if (href.search(/(https?|ftp):\/\/[-a-zA-Z0-9+&@#\/%?=~_|!:,.;]*[-a-zA-Z0-9+&@#\/%=~_|]/gi)>-1) {
           mcTabs.displayTab('web_tab','web_panel');
           document.forms[0].web_page.value = href;
        } else if (href.search(/wikiattachment:-:(.*?)/gi) > -1) {
            mcTabs.displayTab('attachments_tab','attachments_panel');
            document.forms[0].attach.value = href.replace(/wikiattachment:-:/gi, "").replace(/%20/gi, " ");
        } else if (href.search(/mailto:(.*?)/gi) > -1) {
            mcTabs.displayTab('email_tab','email_panel')
            document.forms[0].email.value = href.replace(/mailto:/gi, "");
        } else {
            mcTabs.displayTab('wiki_tab','wiki_panel');
            var space = "", whref = href;
            if (href.indexOf(".") > -1) {
                space = href.substring(0, href.indexOf("."));
                whref = href.substring(href.indexOf(".") + 1, href.length);
            }
            document.forms[0].wiki_space.value = space;
            document.forms[0].wiki_page.value = whref;
        }
    }

    document.forms[0].insert.value = tinyMCE.getLang('lang_' + tinyMCE.getWindowArg('action'), 'Insert', true);

    var className = tinyMCE.getWindowArg('className');
    var editor_id = tinyMCE.getWindowArg('editor_id');
}

function insertLink() {
    var wikiTabElm = document.getElementById("wiki_tab");
    var webTabElm = document.getElementById("web_tab");
    var attachTabElm = document.getElementById("attachments_tab");
    var emailTabElm = document.getElementById("email_tab");
    var dummy;
    tinyMCEPopup.restoreSelection();

    if (wikiTabElm.className == "current") {
        var href = document.forms[0].wiki_page.value;
        var space = document.forms[0].wiki_space.value;
        var wikitext = document.forms[0].wiki_text.value;
        tinyMCE.themes['wikieditor'].insertLink(href, "", wikitext, space, "", dummy, "");

    } else if (webTabElm.className == "current") {
        var webtext = document.forms[0].web_text.value;
        var href = document.forms[0].web_page.value;
        tinyMCE.themes['wikieditor'].insertLink(href, "", webtext, "", "", dummy, "");

    } else if (attachTabElm.className == "current") {
        var href = document.forms[0].attach.value;
        var text = document.forms[0].attach_text.value;
        tinyMCE.themes['wikieditor'].insertLink("wikiattachment:-:" + href, "", text, "", "", dummy, "");

    } else if (emailTabElm.className == "current") {
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

