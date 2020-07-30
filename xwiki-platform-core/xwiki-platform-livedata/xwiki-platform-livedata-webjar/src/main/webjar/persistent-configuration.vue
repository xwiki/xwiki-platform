<template>
  <div :v-show="false"></div>
</template>


<script>
/*
  * See the NOTICE file distributed with this work for additional
  * information regarding copyright ownership.
  *
  * This is free software; you can redistribute it and/or modify it
  * under the terms of the GNU Lesser General Public License as
  * published by the Free Software Foundation; either version 2.1 of
  * the License, or (at your option) any later version.
  *
  * This software is distributed in the hope that it will be useful,
  * but WITHOUT ANY WARRANTY; without even the implied warranty of
  * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
  * Lesser General Public License for more details.
  *
  * You should have received a copy of the GNU Lesser General Public
  * License along with this software; if not, write to the Free
  * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
  * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
  */
define([
  "Vue",
  "u-node",
  "LZString",
], function (
  Vue,
  u,
  LZString
) {
  Vue.component("persistent-configuration", {

    name: "persistent-configuration",

    template: template,


    props: {
      logic: Object,
      urlHash: {
        type: Boolean,
        default: true,
      },
      localStorage: {
        type: Boolean,
        default: false,
      },
    },


    computed: {
      data: function () { return this.logic.data; },

      encodingSpecsProperties: function () {
        return ["oneOf"].concat(this.logic.getPropertyIds());
      },

      encodingSpecsFilterOperators: function () {
        var self = this;
        var operators = [];
        this.data.meta.filters.forEach(function (filterDescriptor) {
          (filterDescriptor.operators || []).forEach(function (operator) {
            if (operators.indexOf(operator.id) === -1) {
              operators.push(operator.id);
            }
          });
        });
        return ["oneOf"].concat(operators);
      },

      encodingSpecsCurrentLayoutId: function () {
        return ["oneOf"].concat(this.logic.getLayoutIds());
      },

      encodingSpecsV1: function () {
        return {
          filters: ["array", {
            "property": this.encodingSpecsProperties,
            "matchAll": ["boolean"],
            "constrains": ["array", {
              operator: this.encodingSpecsFilterOperators,
              value: ["varchar"],
            }],
          }],
          sort: ["array", {
            property: this.encodingSpecsProperties,
            descending: ["boolean"],
          }],
          offset: ["integer"],
          limit: ["integer"],
          currentLayoutId: this.encodingSpecsCurrentLayoutId,
          hiddenProperties: ["array", this.encodingSpecsProperties],
        };
      },

      coders: function () {
        return [
          u.fromJson(1, this.encodingSpecsV1),
        ];
      },

      dataToSave: function () {
        var dataToSave = {};
        for (var key in this.encodingSpecsV1) {
          dataToSave[key] = this.getDataParentObject(key)[key];
        }
        return dataToSave;
      },

      encodedConfig: function () {
        return this.encodeConfig(this.dataToSave);
      },

    },

    watch: {
      encodedConfig: function () {
        if (this.encodedConfig) {
          this.saveConfig();
        } else {
          this.deleteConfig();
        }
      },
    },


    methods: {

      getDataParentObject: function (key) {
        if (this.data.query.hasOwnProperty(key)) {
          return this.data.query;
        } else {
          return this.logic;
        }
      },

      encodeConfig: function (config) {
        try {
          var encoded = u.encode(this.coders[this.coders.length -1], config);
          var compressed = LZString.compressToEncodedURIComponent(encoded);
          return compressed;
        } catch (err) {
          console.warn(err);
          return "";
        }
      },

      decodeConfig: function (encodedConfig) {
        if (!encodedConfig) { return; }
        var uncompressed = LZString.decompressFromEncodedURIComponent(encodedConfig);
        if (!uncompressed) { return; }
        try {
          var decoded = u.decode(this.coders, uncompressed);
          return decoded;
        } catch (err) {
          console.error(err);
        }
      },

      saveConfig: function () {
        // url hash
        if (this.urlHash) {
          var url = new URL(window.location);
          url.searchParams.set("livedata-config", this.encodedConfig);
          history.replaceState(null, "", url.href);
        }
        // local storage
        if (this.localStorage) {
          window.localStorage.setItem("livedata-config", this.encodedConfig);
        }
      },

      getConfig: function () {
        var config = "";
        // url hash
        if (!config && this.urlHash) {
          config = (new URLSearchParams(window.location.search)).get("livedata-config");
        }
        // local storage
        if (!config && this.localStorage) {
          config = window.localStorage.getItem("livedata-config");
        }
        return config;
      },

      deleteConfig: function () {
        // url hash
        if (this.urlHash) {
          var url = new URL(window.location);
          url.searchParams.delete("livedata-config");
          history.replaceState(null, "", url.href);
        }
        // local storage
        if (this.localStorage) {
          window.localStorage.removeItem("livedata-config");
        }
      },
    },


    mounted: function () {
      var self = this;
      var config = this.decodeConfig(this.getConfig());
      if (!config) {
        this.deleteConfig();
        new XWiki.widgets.Notification("Bad LiveData config given, fall back to default");
        return
      }

      Object.keys(this.dataToSave).forEach(function (key) {
        self.getDataParentObject(key)[key] = config[key];
      });
    },


  });
});
</script>


<style>


</style>
