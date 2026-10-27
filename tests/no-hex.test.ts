import { hexColorsIn } from './noHex';
import { read, sourceFiles } from './sourceFiles';

describe('semantic colors only', () => {
  it('AC-001-10: no hex color appears in src/app or src/ui/components', () => {
    // Test files may use hex values as fixtures; shipped code may not.
    const files = [...sourceFiles('src/app'), ...sourceFiles('src/ui/components')].filter(
      (f) => !/\.test\.tsx?$/.test(f),
    );
    expect(files.length).toBeGreaterThan(0);
    const offenders = files.flatMap((file) =>
      hexColorsIn(read(file)).map((hex) => `${file}: ${hex}`),
    );
    expect(offenders).toEqual([]);
  });

  it('AC-001-10: the check catches hex colors in any form', () => {
    expect(
      hexColorsIn(`
        const a = '#2E5E4E';
        const b = "#fff";
        <View style={{ color: '#2e5e4e80' }} />
        const c = \`#abcd\`;
        // issue #12 and id="#main" are not colors
      `),
    ).toEqual(['#2E5E4E', '#fff', '#2e5e4e80', '#abcd']);
  });
});
