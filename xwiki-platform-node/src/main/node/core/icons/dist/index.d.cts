import { Size } from './size';
/**
 * @since 0.7
 * @beta
 */
declare const CIconAnnotated: import('vue').DefineComponent<{
    name: string;
    size?: Size;
}, {}, {}, {}, {}, import('vue').ComponentOptionsMixin, import('vue').ComponentOptionsMixin, {
    click: (event: MouseEvent) => any;
}, string, import('vue').PublicProps, Readonly<{
    name: string;
    size?: Size;
}> & Readonly<{
    onClick?: ((event: MouseEvent) => any) | undefined;
}>, {
    size: Size;
}, {}, {}, {}, string, import('vue').ComponentProvideOptions, false, {}, HTMLSpanElement>;
export { CIconAnnotated as CIcon, Size };
