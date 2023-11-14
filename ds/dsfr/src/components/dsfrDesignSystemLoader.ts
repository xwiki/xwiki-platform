
import { App } from "vue";
import { DesignSystemLoader } from "@cristal/api";

import { injectable } from "inversify";

import XBtn from "../vue/x-btn.vue";
import XCard from "../vue/x-card.vue";
import XAlert from "../vue/x-alert.vue";
import XDivider from "../vue/x-divider.vue";
import XDialog from "../vue/x-dialog.vue";
import XContainer from "../vue/x-container.vue";
import XCol from "../vue/x-col.vue";
import XRow from "../vue/x-row.vue";
import XImg from "../vue/x-img.vue";
import XTextField from "../vue/x-textfield.vue";
import XAvatar from '../vue/x-avatar.vue'

import VueDsfr from '@gouvminint/vue-dsfr'
import '@gouvfr/dsfr/dist/dsfr.min.css'

@injectable()
export class DSFRDesignSystemLoader implements DesignSystemLoader {

    loadDesignSystem(app : App) : void {
        // Loading specific components from DSFR
        app.use(VueDsfr);
        app.component("x-btn", XBtn);
        app.component("x-card", XCard);
        app.component("x-alert", XAlert);
        app.component("x-divider", XDivider);
        app.component("x-dialog", XDialog);
        app.component("x-avatar", XAvatar);
        app.component("x-container", XContainer);
        app.component("x-img", XImg);
        app.component("x-row", XRow);
        app.component("x-col", XCol);
        app.component("x-text-field", XTextField);
    }
}
 