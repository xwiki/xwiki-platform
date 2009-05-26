/**
 * XWiki's custom WYSIWYG controller.
 * Usage: \$xwiki.jsfx.use("path/to/XWikiWysiwyg.js", {'forceSkinAction': true, 'lazy': true})
 *
 * @type object
 * @param lazy {@code true} if you want to load the WYSIWYG code on demand, {@code false} if you want to load the
 *            WYSIWYG code when the page is loaded
 */
var Wysiwyg =
{
    /**
     * This is the entry pointy for XWiki GWT services.
     *
     * @see web.xml
     */
    xwikiservice: '${request.contextPath}/XWikiService',

    /**
     * This is the entry pointy for the new WYSIWYG's GWT services.
     *
     * @see web.xml
     */
    wysiwygServiceURL: '${request.contextPath}/WysiwygService',

    /**
     * Loads the WYSIWYG code on demand.
     */
    load : function()
    {
        // Test if the code has been already loaded.
        // GWT loads the WYSIWYG code in an in-line frame with the 'com.xpn.xwiki.wysiwyg.Wysiwyg' id.
        if (document.getElementById('com.xpn.xwiki.wysiwyg.Wysiwyg')) {
            return;
        }

        // Create the script tag to be used for importing the GWT script loader.
        var script = document.createElement('script');
        script.type = 'text/javascript';
        script.src = '$xwiki.getSkinFile("js/xwiki/wysiwyg/gwt/com.xpn.xwiki.wysiwyg.Wysiwyg/com.xpn.xwiki.wysiwyg.Wysiwyg.nocache.js")';

        // The default GWT script loader calls document.write() twice which prevents us from loading the WYSIWYG code
        // on demand, after the document has been loaded. To overcome this we have to overwrite the document.write()
        // method before the GWT script loader is executed and restore it after.
        // NOTE: The GWT script loader uses document.write() to compute the URL from where it is loaded.
        var counter = 0;
        var oldWrite = document.write;
        var newWrite = function(html) {
            if (counter < 2) {
                counter++;
                // Fail silently if the script element hasn't been attached to the document.
                if (!script.parentNode) {
                    return;
                }
                // Create a DIV and put the HTML inside.
                var div = document.createElement('div');
                // We have to replace all the script tags because otherwise IE drops them.
                div.innerHTML = html.replace(/<script\b([\s\S]*?)<\/script>/gi, "<pre script=\"script\"$1</pre>");
                // Move DIV contents after the GWT script loader.
                var nextSibling = script.nextSibling;
                while(div.firstChild) {
                    var child = div.firstChild;
                    // Recover the script tags.
                    if (child.nodeName.toLowerCase() == 'pre' && child.getAttribute('script') == 'script') {
                        var pre = child;
                        pre.removeAttribute('script');
                        // Create the script tag.
                        child = document.createElement('script');
                        // Copy all the attributes.
                        for (var i = 0; i < pre.attributes.length; i++) {
                            var attrNode = pre.attributes[i];
                            // In case of IE we have to copy only the specified attributes.
                            if (typeof attrNode.specified == 'undefined'
                                || (typeof attrNode.specified == 'boolean' && attrNode.specified)) {
                                child.setAttribute(attrNode.nodeName, attrNode.nodeValue);
                            }
                        }
                        // Copy the script text.
                        child.text = typeof pre.innerText == 'undefined' ? pre.textContent : pre.innerText;
                        // Don't forget to remove the placeholder.
                        div.removeChild(pre);
                    }
                    if (nextSibling) {
                        script.parentNode.insertBefore(child, nextSibling);
                    } else {
                        script.parentNode.appendChild(child);
                    }
                }
            } else {
                document.write = oldWrite;
                oldWrite = undefined;
                script = undefined;
                counter = undefined;
            }
        }

        // Append the script tag to the head.
        var heads = document.getElementsByTagName('head');
        if (heads.length > 0) {
            document.write = newWrite;
            heads[0].appendChild(script);
        }
    },

    /**
     * Looks for WYSIWYG configurations and loads the corresponding editors.
     * TODO: Move this method in the WYSIWYG code.
     * NOTE: This method is unsafe right now!
     */
    reload : function()
    {
        var iframe = document.getElementById('com.xpn.xwiki.wysiwyg.Wysiwyg');
        if (iframe) {
            var wnd = iframe.contentWindow;
            wnd.gwtOnLoad(Wysiwyg.onLoadError, wnd.$moduleName, wnd.$moduleBase);
        }
    },

    /**
     * Logs an error message to the console if the WYSIWYG editors couldn't be loaded.
     */
    onLoadError : function()
    {
        if (console) {
            console.error("Couldn't load the WYSIWYG editors!");
        }
    }
};

#if("$!request.lazy" != "true")
Wysiwyg.load();
#end