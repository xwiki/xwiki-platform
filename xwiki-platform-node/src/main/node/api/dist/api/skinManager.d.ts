import { Container } from 'inversify';
import { App, Component } from 'vue';
/**
 * @since 0.1
 * @beta
 */
export interface SkinManager {
    getTemplate(name: string): Component | null;
    getDefaultTemplate(name: string): Component | null;
    loadDesignSystem(app: App, container: Container): void;
    setDesignSystem(designSystem: string): void;
    getDesignSystem(): string;
}
