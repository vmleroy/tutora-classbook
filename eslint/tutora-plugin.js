// Local ESLint rules for Tutora (AC-002-14): components and screens use tokens, never raw
// colors, font sizes or spacing. Fixed geometry (width, height, borderRadius) is allowed.

const HEX = /#(?:[0-9a-f]{8}|[0-9a-f]{6}|[0-9a-f]{3,4})\b/gi;
const TYPE_KEYS = new Set(['fontSize', 'lineHeight', 'letterSpacing']);
const SPACING_KEY =
  /^(padding|margin)(Top|Bottom|Left|Right|Horizontal|Vertical|Start|End)?$|^(gap|rowGap|columnGap)$/;
// An arbitrary Tailwind value with a unit or a color, e.g. p-[13px], text-[1rem], bg-[#fff].
const ARBITRARY = /(?:^|\s)([a-z-]+-\[[^\]]*(?:px|rem|em|%|#)[^\]]*\])/g;

function keyName(property) {
  if (property.key.type === 'Identifier') return property.key.name;
  if (property.key.type === 'Literal') return String(property.key.value);
  return null;
}

function isNumber(node) {
  if (node.type === 'Literal' && typeof node.value === 'number') return node.value;
  if (node.type === 'UnaryExpression' && node.operator === '-' && node.argument.type === 'Literal')
    return -node.argument.value;
  return null;
}

const noRawDesignValues = {
  meta: {
    type: 'problem',
    docs: { description: 'Use design tokens instead of raw colors, font sizes and spacing.' },
    schema: [],
  },
  create(context) {
    function checkString(node, text) {
      for (const match of text.matchAll(HEX)) {
        context.report({ node, message: `Raw hex color ${match[0]}: use a color token class.` });
      }
      for (const match of text.matchAll(ARBITRARY)) {
        context.report({ node, message: `Arbitrary value ${match[1]}: use a token class.` });
      }
    }
    return {
      Literal(node) {
        if (typeof node.value === 'string') checkString(node, node.value);
      },
      TemplateElement(node) {
        checkString(node, node.value.raw);
      },
      Property(node) {
        const key = keyName(node);
        if (!key) return;
        const value = isNumber(node.value);
        if (value === null) return;
        if (TYPE_KEYS.has(key)) {
          context.report({
            node,
            message: `Raw ${key} ${value}: use a type style (Text variant).`,
          });
        } else if (SPACING_KEY.test(key) && value !== 0) {
          context.report({ node, message: `Raw ${key} ${value}: use a spacing class or token.` });
        }
      },
    };
  },
};

module.exports = { rules: { 'no-raw-design-values': noRawDesignValues } };
