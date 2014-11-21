define(['jquery'], function($) {
  var html = $('html');
  // Case 1: meta information are stored in the data- attributes of the <html> tag
  // (since Flamingo)
  if (html.data('xwiki-document') !== undefined) {
    return {
      'document':   html.data('xwiki-document'),
      'wiki':       html.data('xwiki-wiki'),
      'space':      html.data('xwiki-space'),
      'page':       html.data('xwiki-page'),
      'version':    html.data('xwiki-version'),
      'restURL':    html.data('xwiki-rest-url'),
      'form_token': html.data('xwiki-form-token')
    };
  }
  // Case 2: meta information are stored in deprecated <meta> tags
  // (in colibri)
  var metaTags = $('meta');
  var lookingFor = ['document', 'wiki', 'space', 'page', 'version', 'restURL', 'form_token'];
  var results = {}
  for (var i = 0; i < metaTags.length; ++i) {
    var metaTag = $(metaTags[i]);
    var name = metaTag.attr('name');
    for (var j = 0; j < lookingFor.length; ++j) {
      if (name == lookingFor[j]) {
        results[name] = metaTag.attr('content');
      }
    }
  }
  return results;
});
