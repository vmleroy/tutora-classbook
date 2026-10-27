import { typeStyles, type TypeStyleName } from '@/ui/theme/tokens';

export { typeStyles, type TypeStyleName };

export type TypeStyle = (typeof typeStyles)[TypeStyleName];

/** Font family class per weight: Nunito ships one file per weight. */
export const weightClass: Record<TypeStyle['weight'], string> = {
  400: 'font-regular',
  600: 'font-semibold',
  700: 'font-bold',
  800: 'font-black',
};

/** Large text multiplier (spec 002 decision). Type grows, spacing stays. */
export const LARGE_TEXT_SCALE = 1.15;
