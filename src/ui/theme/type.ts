import tokens from '../../../design-system/tokens.json';

export type TypeStyleName =
  | 'money-display'
  | 'money-xl'
  | 'money-lg'
  | 'money-md'
  | 'title-xl'
  | 'title-lg'
  | 'title-md'
  | 'section'
  | 'body-strong'
  | 'body'
  | 'row-title'
  | 'caption'
  | 'meta'
  | 'button'
  | 'field-label'
  | 'badge'
  | 'overline'
  | 'tab';

export type TypeStyle = Readonly<{
  fontSize: number;
  lineHeight: number;
  letterSpacing: number;
  weight: 400 | 600 | 700 | 800;
}>;

const px = (value: string | undefined) => (value ? parseFloat(value) : 0);

export const typeStyles = Object.fromEntries(
  tokens.type.groups.flatMap((group) =>
    group.styles.map((s) => [
      s.name,
      {
        fontSize: px(s.fontSize),
        lineHeight: px(s.lineHeight),
        letterSpacing: px('letterSpacing' in s ? s.letterSpacing : undefined),
        weight: s.fontWeight as TypeStyle['weight'],
      },
    ]),
  ),
) as Record<TypeStyleName, TypeStyle>;

/** Font family class per weight: Nunito ships one file per weight. */
export const weightClass: Record<TypeStyle['weight'], string> = {
  400: 'font-sans',
  600: 'font-semibold',
  700: 'font-bold',
  800: 'font-black',
};

/** Large text multiplier (spec 002 decision). Type grows, spacing stays. */
export const LARGE_TEXT_SCALE = 1.15;
