(function() {
var DEMO_MODE = "$!request.getParameter('demoMode')" || false;
DEMO_MODE = (DEMO_MODE === true || DEMO_MODE === "true") ? true : false;
// Not in edit mode?
if (!DEMO_MODE && window.XWiki.contextaction !== 'edit') { return false; }
var path = "$xwiki.getURL('RTFrontend.LoadEditors','jsx')" + '?minify=false&demoMode='+DEMO_MODE;
var pathErrorBox = "$xwiki.getURL('RTFrontend.ErrorBox','jsx')" + '?';
require([path, pathErrorBox, 'jquery'], function(Loader, ErrorBox, $) {
    if(!Loader) { return; }
    // VELOCITY
    #set ($document = $xwiki.getDocument('RTWiki.WebHome'))
    var PATHS = {
        RTWiki_WebHome_realtime_netflux: "$document.getAttachmentURL('realtime-wikitext.js')",
    };
    // END_VELOCITY

    for (var path in PATHS) { PATHS[path] = PATHS[path].replace(/\.js$/, ''); }
    require.config({paths:PATHS});


    var getWikiLock = function () {
        var force = document.querySelectorAll('a[href*="editor=wiki"][href*="force=1"][href*="/edit/"]');
        return force.length? true : false;
    };

    var lock = Loader.getDocLock();
    var wikiLock = getWikiLock();

    var info = {
        type: 'rtwiki',
        href: '&editor=wiki&force=1',
        name: "Wiki"
    };

    var getKeyData = function(config) {
        return [
            {doc: config.reference, mod: config.language+'/events', editor: "1.0"},
            {doc: config.reference, mod: config.language+'/events', editor: "userdata"},
            {doc: config.reference, mod: config.language+'/content',editor: "rtwiki"}
        ];
    };

    var parseKeyData = function(config, keysResultDoc) {
        var keys = {};
        var keysResult = keysResultDoc[config.reference];
        if (!keysResult) { console.error("Unexpected error with the document keys"); return keys; }

        var keysResultContent = keysResult[config.language+'/content'];
        if (!keysResultContent) { console.error("Missing content keys in the document keys"); return keys; }

        var keysResultEvents = keysResult[config.language+'/events'];
        if (!keysResultEvents) { console.error("Missing event keys in the document keys"); return keys; }

        if (keysResultContent.rtwiki && keysResultEvents["1.0"]) {
            keys.rtwiki = keysResultContent.rtwiki.key;
            keys.rtwiki_users = keysResultContent.rtwiki.users;
            keys.events = keysResultEvents["1.0"].key;
            keys.userdata = keysResultEvents["userdata"].key;
        }
        else { console.error("Missing mandatory RTWiki key in the document keys"); return keys; }

        var activeKeys = keys.active = {};
        for (var key in keysResultContent) {
            if (key !== "rtwiki" && keysResultContent[key].users > 0) {
                activeKeys[key] = keysResultContent[key];
            }
        }
        return keys;
    };

    var updateKeys = function (cb) {
        var config = Loader.getConfig();
        var keysData = getKeyData(config);
        Loader.getKeys(keysData, function(keysResultDoc) {
            var keys = parseKeyData(config, keysResultDoc);
            cb(keys);
        });
    };

    var launchRealtime = function (config, keys) {
        require(['jquery', 'RTWiki_WebHome_realtime_netflux'], function ($, RTWiki) {
            if (RTWiki && RTWiki.main) {
                keys._update = updateKeys;
                RTWiki.main(config, keys);
            } else {
                console.error("Couldn't find RTWiki.main, aborting");
            }
        });
    };

    if (lock) {
        // found a lock link : check active sessions
        Loader.checkSessions(info);
    } else if (window.XWiki.editor === 'wiki' || DEMO_MODE) {
        // No lock and we are using wiki editor : start realtime
        var config = Loader.getConfig();
        updateKeys(function (keys) {
            if(!keys.rtwiki || !keys.events) {
                ErrorBox.show('unavailable');
                console.error("You are not allowed to create a new realtime session for that document.");
            }
            if (Object.keys(keys.active).length > 0) {
                if (keys.rtwiki_users > 0 || Loader.isForced) {
                    launchRealtime(config, keys);
                } else {
                    var callback = function() {
                        launchRealtime(config, keys);
                    };
                    console.log("Join the existing realtime session or create a new one");
                    Loader.displayModal("rtwiki", Object.keys(keys.active), callback, info);
                }
            } else {
                launchRealtime(config, keys);
            }
        });
    }

    var displayButtonModal = function() {
        if ($('.realtime-button-rtwiki').length) {
            var button = new Element('button', {'class': 'btn btn-success'});
            var br =  new Element('br');
            button.insert(Loader.messages.redirectDialog_join.replace(/\{0\}/g, "Wiki"));
            $('.realtime-button-rtwiki').prepend(button);
            $('.realtime-button-rtwiki').prepend(br);
            $(button).on('click', function() {
                window.location.href = Loader.getEditorURL(window.location.href, info);
            });
        } else if(lock && wikiLock) {
            var button = new Element('button', {'class': 'btn btn-primary'});
            var br =  new Element('br');
            button.insert(Loader.messages.redirectDialog_create.replace(/\{0\}/g, "Wiki"));
            $('.realtime-buttons').append(br);
            $('.realtime-buttons').append(button);
            $(button).on('click', function() {
                window.location.href = Loader.getEditorURL(window.location.href, info);
            });
        }
    };
    displayButtonModal();
    $(document).on('insertButton', displayButtonModal);
});
})();
