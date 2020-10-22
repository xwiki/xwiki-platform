define([
  "Vue",
  "Logic",
  "xwiki-livedata",
  //"polyfills"
], function (
  Vue,
  Logic,
  XWikiLivedata
) {


  class Livedata {

    /**
     * @param {Object} parameters
     * @param {HTMLElement} parameters.element The element where to mount the Livedata component
     * @param {Object} parameters.config The initial config of the Livedata
     */
    constructor ({ element, config }) {

      this.config = config;
      this.element = element;
      this.logic = new Logic({ element, config });

      // Create Livedata instance
      this.vm = new Vue({
        el: element,
        components: {
          "XWikiLivedata": XWikiLivedata,
        },
        template: "<XWikiLivedata :logic='logic'></XWikiLivedata>",
        data: {
          logic: this.logic,
        },
      });

    }

  }

  return Livedata;

});
