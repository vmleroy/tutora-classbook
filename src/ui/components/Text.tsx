import { Text as RNText, type TextProps } from 'react-native';

import { cn } from '../cn';
import { useTheme } from '../theme/ThemeProvider';
import { typeStyles, weightClass, type TypeStyleName } from '../theme/type';

type Props = TextProps & {
  variant?: TypeStyleName;
  /** Tabular figures, so digits keep their width. Money styles always have them. */
  tabular?: boolean;
  className?: string;
};

/** All app text. Sizes come from the type tokens, scaled by the Large text setting. */
export function Text({ variant = 'body', tabular, className, style, ...rest }: Props) {
  const { textScale } = useTheme();
  const type = typeStyles[variant];
  const isTabular = tabular || variant.startsWith('money');
  return (
    <RNText
      className={cn(weightClass[type.weight], 'text-ink', className)}
      style={[
        {
          fontSize: type.fontSize * textScale,
          lineHeight: type.lineHeight * textScale,
          letterSpacing: type.letterSpacing,
        },
        isTabular && { fontVariant: ['tabular-nums'] },
        style,
      ]}
      {...rest}
    />
  );
}
