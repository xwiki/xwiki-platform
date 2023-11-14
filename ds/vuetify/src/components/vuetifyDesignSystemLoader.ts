
import { App } from "vue";
import { VAvatar, VContainer, VRow, VCol, VImg, VTextField } from "vuetify/components";
import { DesignSystemLoader } from "@cristal/api";

import 'vuetify/styles'
import { createVuetify } from 'vuetify'
import { mdi } from 'vuetify/iconsets/mdi'
// import { aliases, fa } from 'vuetify/iconsets/fa'
import * as components from 'vuetify/components'
import * as directives from 'vuetify/directives'
import { injectable } from "inversify";
import XCard from '../vue/x-card.vue';
import XAlert from '../vue/x-alert.vue';
import XDivider from '../vue/x-divider.vue';
import XBtn from '../vue/x-btn.vue';
import XDialog from '../vue/x-dialog.vue';

@injectable()
export class VuetifyDesignSystemLoader implements DesignSystemLoader {

    loadDesignSystem(app : App) : void {
        /*
        // Manuel importing to reduce build size
        const vuetify = createVuetify({
            components : { VApp, VContainer, VRow, VCol, VAvatar },
            directives : {},
            icons: {
                defaultSet: 'fa',
                aliases,
                sets: {
                    fa,
                    mdi,
                }
          })
        */

        const vuetify = createVuetify({
            components,
            directives,
            icons: {
                defaultSet: 'mdi',
                sets: {
                    mdi,
                }
            }
        });
        app.use(vuetify);
        // Native Vuetify components
        app.component("x-avatar", VAvatar);
        app.component("x-container", VContainer);
        app.component("x-img", VImg);
        app.component("x-row", VRow);
        app.component("x-col", VCol);
        app.component("x-text-field", VTextField);

        // Custom wrapped components
        app.component("x-btn", XBtn);
        app.component("x-divider", XDivider);
        app.component("x-card", XCard);
        app.component("x-alert", XAlert);
        app.component("x-dialog", XDialog);
    }
}
 