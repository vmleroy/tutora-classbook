// Every class name is a design-system token name; values come from src/ui/theme/tokens.json.
const tokens = require('./src/ui/theme/tokens.json');

const v = (name) => `rgb(var(--${name}) / <alpha-value>)`;
const px = (value) => value;

const colors = { transparent: 'transparent' };
for (const { name } of tokens.color.tokens) colors[name] = v(name);
// Custom-color parts (docs/design-system/Theming.md): default to primary, repainted on their own.
for (const part of ['btn-primary', 'on-btn-primary', 'tab-active', 'tab-active-soft'])
  colors[part] = v(part);

const spacing = {};
for (const { name, value } of tokens.spacing.tokens)
  spacing[name.replace('space-', '')] = px(value);
for (const { name, value } of tokens.size.tokens) spacing[name.replace('size-', '')] = px(value);

const borderRadius = {};
for (const { name, value } of tokens.radius.tokens)
  borderRadius[name.replace('radius-', '')] = value;

const fontSize = {};
for (const group of tokens.type.groups) {
  for (const s of group.styles) {
    fontSize[s.name] = [
      s.fontSize,
      { lineHeight: s.lineHeight, letterSpacing: s.letterSpacing ?? '0px' },
    ];
  }
}

/** @type {import('tailwindcss').Config} */
module.exports = {
  content: ['./src/**/*.{ts,tsx}'],
  presets: [require('nativewind/preset')],
  // Nunito ships one file per weight; the weight is chosen by family, never by fontWeight.
  // Regular is `font-regular`, not `font-sans`: NativeWind's native preset extends `sans`
  // with the system font, which would win over ours on Android.
  corePlugins: { fontWeight: false },
  theme: {
    colors,
    fontFamily: {
      regular: ['Nunito_400Regular'],
      semibold: ['Nunito_600SemiBold'],
      bold: ['Nunito_700Bold'],
      black: ['Nunito_800ExtraBold'],
    },
    borderRadius,
    extend: { spacing, fontSize },
  },
};
