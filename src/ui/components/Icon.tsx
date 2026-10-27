import {
  Calendar,
  CalendarCheck,
  Check,
  ChevronLeft,
  ChevronRight,
  CircleAlert,
  Clock,
  CloudUpload,
  FileText,
  House,
  MapPin,
  Minus,
  Play,
  Plus,
  Settings,
  UserX,
  Users,
  Wallet,
  X,
  type LucideIcon,
} from 'lucide-react-native';

import type { PaletteToken as TokenName } from '@/domain/theme/palette';

import { size as sizes } from '../theme/metrics';
import { useTheme } from '../theme/ThemeProvider';

export type IconName =
  | 'plus'
  | 'minus'
  | 'check'
  | 'close'
  | 'chevron-left'
  | 'chevron-right'
  | 'home'
  | 'calendar'
  | 'users'
  | 'pin'
  | 'wallet'
  | 'clock'
  | 'alert'
  | 'file'
  | 'cloud'
  | 'gear'
  | 'calendar-check'
  | 'no-show'
  | 'play';

const icons: Record<IconName, LucideIcon> = {
  plus: Plus,
  minus: Minus,
  check: Check,
  close: X,
  'chevron-left': ChevronLeft,
  'chevron-right': ChevronRight,
  home: House,
  calendar: Calendar,
  users: Users,
  pin: MapPin,
  wallet: Wallet,
  clock: Clock,
  alert: CircleAlert,
  file: FileText,
  cloud: CloudUpload,
  gear: Settings,
  'calendar-check': CalendarCheck,
  'no-show': UserX,
  play: Play,
};

export interface IconProps {
  name: IconName;
  size?: number;
  strokeWidth?: number;
  label?: string;
  /** Color token; React Native has no currentColor, so the caller names the text color. */
  tone?: TokenName;
}

export function Icon({ name, size = sizes.icon, strokeWidth = 2, label, tone = 'ink' }: IconProps) {
  const { palette } = useTheme();
  const Glyph = icons[name];
  const color = palette[tone];
  return (
    <Glyph
      size={size}
      strokeWidth={strokeWidth}
      color={color}
      fill={name === 'play' ? color : 'none'}
      accessible={!!label}
      accessibilityRole={label ? 'image' : undefined}
      accessibilityLabel={label}
      importantForAccessibility={label ? 'yes' : 'no-hide-descendants'}
    />
  );
}
