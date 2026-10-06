import { definePreset } from '@primeuix/themes';
import Nora from '@primeuix/themes/nora';

/**
 * Bank brand preset built on PrimeNG's Nora theme.
 * Primary = Colosseum orange, neutral = cool slate.
 *
 * NOTE: the topbar and sidebar do NOT use these tokens — they use fixed
 * brand-* custom properties defined in styles.css so the chrome never
 * repaints between light/dark mode. This preset only drives the page
 * content area (cards, tables, tags, inputs, ...), where light mode is a
 * white surface and dark mode is a dark slate surface.
 *
 * Both surface ramps run 0 (lightest) -> 950 (darkest), which is the
 * convention Nora expects too (same direction as every PrimeUIX theme);
 * the engine itself decides which step maps to content background /
 * border / hover for each mode. Getting this direction wrong silently
 * inverts dark mode (light text on light bg).
 */
export const BankPreset = definePreset(Nora, {
  semantic: {
    primary: {
      50: '#fef6ee',
      100: '#fce8d4',
      200: '#f8cea2',
      300: '#f3ae6c',
      400: '#ed8f44',
      500: '#d9731f',
      600: '#b85c16',
      700: '#954815',
      800: '#783a16',
      900: '#623015',
      950: '#351708',
    },
    colorScheme: {
      light: {
        surface: {
          0: '#ffffff',
          50: '#f7f8f9',
          100: '#eef0f2',
          200: '#dfe3e7',
          300: '#c5cbd3',
          400: '#9aa4b0',
          500: '#747f8d',
          600: '#5a6472',
          700: '#454d59',
          800: '#303640',
          900: '#20242b',
          950: '#121418',
        },
        primary: {
          // Brand color (#d9731f = primary.500), shared with the topbar.
          // White text by design, for visual consistency with the topbar
          // chrome (intentionally below WCAG AA on the base color, ~3.3:1 —
          // accepted trade-off, do not "fix" by reverting to dark text).
          // Hover/active darken (600/700) to stay legible with white text.
          color: '#d9731f',
          contrastColor: '#ffffff',
          hoverColor: '#b85c16',
          activeColor: '#954815',
        },
        highlight: {
          background: '#fce8d4',
          focusBackground: '#f8cea2',
          color: '#783a16',
          focusColor: '#623015',
        },
      },
      dark: {
        surface: {
          0: '#ffffff',
          50: '#f1f2f4',
          100: '#dfe2e6',
          200: '#c7cbd2',
          300: '#9aa2ac',
          400: '#6e7682',
          500: '#4f5862',
          600: '#3a414a',
          700: '#2b3139',
          800: '#20242b',
          900: '#191c21',
          950: '#121418',
        },
        primary: {
          color: '#ed8f44',
          contrastColor: '#20130a',
          hoverColor: '#f3ae6c',
          activeColor: '#f8cea2',
        },
        highlight: {
          background: 'color-mix(in srgb, #ed8f44 20%, transparent)',
          focusBackground: 'color-mix(in srgb, #ed8f44 30%, transparent)',
          color: '#f8cea2',
          focusColor: '#fce8d4',
        },
      },
    },
  },
});
