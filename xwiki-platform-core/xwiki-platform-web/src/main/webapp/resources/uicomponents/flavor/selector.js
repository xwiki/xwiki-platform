require(['jquery'], function($) {

  /**
   * Wait until all the DOM elements we want to initialize are ready.
   * We cannot rely on $(window).load() or $(window).ready() because of the asynchronous nature of requirejs
   * (see: http://requirejs.org/docs/api.html#pageload).
   */
  var waitDOMReady = function(callback) {
    // if the dom elements that we want to initialize are not loaded
    if ($('#xwikiplatformversion').length == 0) {
      // then retry in 100ms
      setTimeout(function(){waitDOMReady(callback);}, 100);
    } else {
       callback();
    }
  }
  
  var initSelector = function() {
    $('.xwiki-flavor-select-option').click(function (event) {
      $('.xwiki-flavor-select-option-selected').removeClass('xwiki-flavor-select-option-selected');
      $($(this).find('input')).prop('checked', true);
      $(this).addClass('xwiki-flavor-select-option-selected');
    });
    
    $('.xwiki-flavor-select-option a.popup').click(function (event) {
      window.open(this.href, 'flavor-popup', config='height=600, width=700, toolbar=no, menubar=no, scrollbars=yes, resizable=yes, location=no, directories=no, status=yes');
      return false;
    });
    
    $('.xwiki-flavor-select-noflavor').click(function (event) {
      $('.xwiki-flavor-select-option-selected').removeClass('xwiki-flavor-select-option-selected');
    });
    
    $('.xwiki-flavor-select .paginationFilter a').click(function (event) {
      var select = $($(this).parents('.xwiki-flavor-select')[0]);
      select.addClass('loading');
      select.find('.xwiki-flavor-select-container').fadeOut();
      $.ajax(this.href).done(function(data) {
        select[0].innerHTML = data;
        select.removeClass('loading');
        select.find('.xwiki-flavor-select-container').hide();
        select.find('.xwiki-flavor-select-container').fadeIn();
        initSelector();
      }).fail(function(){
        new XWiki.widgets.Notification("$escapetool.javascript($services.localization.render('flavor.selector.website'))", 'error');
        select.removeClass('loading');
        select.find('.xwiki-flavor-select-container').fadeIn();
      });
      return false;
    });
  }

  waitDOMReady(initSelector);

});

