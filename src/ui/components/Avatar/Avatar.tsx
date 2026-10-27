import { View } from 'react-native';

import { cn, tv } from '@/ui/cn';
import { tagInk, tagSoft, type Tag } from '@/ui/components/TagDot';
import { Text } from '@/ui/components/Text';

export interface AvatarProps {
  initials: string;
  tag?: Tag;
  size?: 'sm' | 'md' | 'lg';
  /** Extra classes merged over the component's own (layout such as margins). */
  className?: string;
}

const avatar = tv({
  base: 'items-center justify-center rounded-full',
  variants: {
    size: {
      sm: 'h-avatar-sm w-avatar-sm',
      md: 'h-avatar-md w-avatar-md',
      lg: 'h-avatar-lg w-avatar-lg',
    },
  },
});

const textVariant = { sm: 'caption', md: 'body', lg: 'section' } as const;

/** Initials on the student's tag color. Decorative: the name beside it is the label. */
export function Avatar({ className, initials, tag = 'amber', size = 'md' }: AvatarProps) {
  return (
    <View
      testID={`avatar-${tag}`}
      importantForAccessibility="no-hide-descendants"
      accessibilityElementsHidden
      className={cn(avatar({ size }), tagSoft[tag], className)}
    >
      <Text variant={textVariant[size]} weight={800} className={tagInk[tag]}>
        {initials}
      </Text>
    </View>
  );
}
