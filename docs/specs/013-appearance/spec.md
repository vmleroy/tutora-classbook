# Spec 013: Appearance

Status: draft
Depends on: 002 Design system (4 themes × light and dark, the seven Suas cores parts, Large text, contrast rules), 016 Visual refresh (the look and the part names: Topo, Botões principais, Botão de destaque, Números de ganhos, Fundo, Cartões, Menu ativo)
Mockups: docs/mockups/Theme.dc.html (Aparência), docs/mockups/ColorPicker.dc.html (Suas cores), docs/mockups/Settings.dc.html (the "Aparência" row)

## Goal

Everything Tutora needs to repaint itself already exists since spec 002: four themes, light and dark, seven parts the teacher can paint, and Large text. What's missing is the screens. After this spec the teacher can open Ajustes › Aparência, see a live preview of Hoje, and choose the mode, the theme, quick main and highlight colors and the text size. From there, Suas cores paints any of the seven parts with any color, by its hex code or with hue, intensity and brightness, and Tutora keeps every text readable and says so. Nothing leaves the phone, and no choice ever changes an amount or a class.

## User stories

- **US-1.** As a teacher, I make Tutora dark at night, or follow my phone.
- **US-2.** As a teacher, I pick the Biblioteca theme because I like the wine and gold.
- **US-3.** As a teacher, I paint the buttons in my school's color and still read every label.
- **US-4.** As a teacher who needs bigger letters, I turn on Grande and nothing gets cut off.
- **US-5.** As a teacher who went too far, I go back to the theme's colors in one tap.

## Acceptance criteria

### Where it lives

- **AC-013-1.** Ajustes › Geral has "Aparência", whose line says the theme, the mode and whether there are custom colors or Large text ("Quadro-negro · Claro", "Biblioteca · Sistema · cores e texto"). It opens the Aparência page laid out as its mockup: the live preview, Modo, Tema, Personalizar, Tamanho do texto and Cores avançadas.

### The preview

- **AC-013-2.** The top of Aparência shows a small Hoje (the date, the greeting with the teacher's name when set, "Ganho hoje" and an Iniciar aula button) painted with the current choices. It changes as soon as any choice changes, before leaving the page.

### Mode and theme

- **AC-013-3.** Modo is Claro, Escuro or Sistema (Sistema at first); Sistema follows the phone and changes with it while the app is open.
- **AC-013-4.** Tema lists the four themes (Quadro-negro, Caderno azul, Biblioteca, Giz pastel), each with its main and highlight colors in the current mode, and the chosen one outlined. Choosing one repaints the whole app at once, with no restart and no flash.

### Quick colors

- **AC-013-5.** Personalizar has "Cor principal" and "Cor de destaque", each a row of swatches: "Do tema" first, then presets (Verde quadro, Azul, Vinho, Turquesa, Roxo, Grafite, Terracota for the main color; Amarelo lápis, Laranja, Coral, Menta, Rosa, Azul claro, Dourado for the highlight), and a + that opens Suas cores on that color. Each swatch is read aloud by its name.
- **AC-013-6.** "Cor principal" paints Topo, Botões principais and Menu ativo; "Cor de destaque" paints Botão de destaque. "Do tema" takes those parts back to the theme's colors.
- **AC-013-7.** "Restaurar tema" appears when there is any custom color and removes all of them, back to the theme's colors (AC-002-9).

### Text size

- **AC-013-8.** Tamanho do texto is Normal or Grande. Grande makes every text larger at once, in every screen, and nothing is cut off at 360 dp wide (AC-002-16).

### Suas cores

- **AC-013-9.** "Cores avançadas" opens Suas cores, laid out as its mockup: a preview (the Hoje top, Salvar aula and Iniciar aula buttons, the tab bar) painted with the colors being chosen, "Escolha o que pintar" with the seven parts and their current colors, then the chosen part's hex code, Matiz, Intensidade and Brilho.
- **AC-013-10.** Typing a valid hex code ("#7A2E3B" or "7a2e3b") sets the color; an invalid one is shown as typed and doesn't change anything until it's valid. Moving Matiz, Intensidade or Brilho changes the color, and the hex code follows.
- **AC-013-11.** Under the sliders, the screen says which text goes on the color and how well it reads: "Texto branco escolhido · contraste 7,2:1 · bom", with a green, yellow or red dot (green from 4,5:1, yellow from 3:1).
- **AC-013-12.** In dark mode, the preview and the app show a custom color lightened as the design system says (AC-002-7), and the screen says so ("No modo escuro, esta cor fica mais clara").
- **AC-013-13.** Leaving Suas cores with any part below 4,5:1 asks first: "Algum texto pode ficar difícil de ler. Salvar assim?" with "Salvar" and "Ajustar" (AC-002-8).
- **AC-013-14.** "Restaurar" in Suas cores takes every part back to the theme's colors.
- **AC-013-15.** Red and green for owed and paid never change, whatever the colors chosen (AC-002-6).

### Language

- **AC-013-16.** Both pages follow the app language; numbers in the contrast line are written for it ("7,2:1" / "7.2:1"), and theme and color names are translated.

## Out of scope

- New themes or a theme editor beyond the seven parts.
- Choosing a font, or more text sizes than Normal and Grande.
- Sharing a color set with another teacher.
- First-run choice of theme (spec 014 may offer it).

## Open questions

For Victor. The assumptions above are marked here so they are easy to change.

- **What "Cor principal" paints:** assumed Topo, Botões principais and Menu ativo, and "Cor de destaque" only Botão de destaque. Should the main color also paint Números de ganhos?
- **Choosing a theme with custom colors:** the mockup clears custom colors when a theme is chosen. Clear them (asking first), or keep them over the new theme?
- **Suas cores applies when:** assumed each change shows at once in the preview and in the app, and the low-contrast question comes only when leaving. Or keep the changes in the preview until a "Salvar" button?
- **Modo at first:** assumed Sistema. Or Claro?
