# Icon

Line icons at 24-unit grid, 2px round strokes, drawn in the current text color.

## Use
- 22px by default; 18px inside buttons, rows and fields; 26px on the FAB.
- Icons always sit beside a word, or the control carries an `aria-label` / `accessibilityLabel`.
- In the app use `lucide-react-native` (same stroke style as the mockups). Names here map to Lucide: home → `House`, calendar → `Calendar`, users → `Users`, pin → `MapPin`, wallet → `Wallet`, alert → `CircleAlert`, file → `FileText`, cloud → `CloudUpload`, gear → `Settings`, play → `Play`, star → `Star`, search → `Search`, archive → `Archive`, pencil → `Pencil` (edit).

## Do / Don't
- Do color icons with text tokens (`text-ink`, `text-primary`, `text-on-primary`).
- Don't use emoji as icons or mix filled and outline icons, except `play`, which is filled.

## Props

```ts
interface IconProps {
  name: IconName;
  size?: number;
  strokeWidth?: number;
  label?: string;
  /** Color token. React Native has no currentColor, so the caller names the text color. Default `ink`. */
  tone?: TokenName;
}
```
