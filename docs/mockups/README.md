# Mockups

The 27 screens of Tutora v1, also viewable (and clickable) on the [Tutora app screens](https://claude.ai/artifact/B8YKPsZff8TMJ7qLQd1JJY) canvas.

These files are design references, not app code. Each one is plain HTML with inline styles for a 390 × 844 phone, plus a small `Component` class at the bottom with the sample data and interactions. They need the canvas runtime (`support.js`) to render, so open the canvas link to see them; read the source here to get exact copy, layout order, sample values and states.

Colors in the mockups are hex values from the Quadro-negro light theme. In the app, never copy a hex: use the matching token from [`docs/design-system/`](../design-system/) (for example `#2E5E4E` is `primary`, `#FAF7F0` is `background`, `#5C6A70` is `ink-muted`).

| File | Screen |
| --- | --- |
| [NavMap.dc.html](NavMap.dc.html) | Mapa de navegação. Out of date since 005: Alunos and Locais are sub-pages of the Cadastro tab, and Ajustes is the fifth tab (no gear on Hoje) |
| [Palette.dc.html](Palette.dc.html) | Paleta e tipografia |
| [Themes.dc.html](Themes.dc.html) | Temas · claro e escuro |
| [Main.dc.html](Main.dc.html) | Hoje · aba 1 |
| [Schedule.dc.html](Schedule.dc.html) | Agenda · aba 2 |
| [LessonDetail.dc.html](LessonDetail.dc.html) | Hoje ou Agenda › tocar numa aula › Detalhe da aula |
| [EditLesson.dc.html](EditLesson.dc.html) | Detalhe da aula › Editar |
| [AddLesson.dc.html](AddLesson.dc.html) | Hoje ou Agenda › + › Nova aula / Registrar |
| [Live.dc.html](Live.dc.html) | Iniciar aula › Aula ao vivo |
| [Students.dc.html](Students.dc.html) | Alunos · aba 3 |
| [StudentDetail.dc.html](StudentDetail.dc.html) | Alunos › tocar num aluno › Detalhe do aluno |
| [StudentEdit.dc.html](StudentEdit.dc.html) | Detalhe do aluno › Editar |
| [Places.dc.html](Places.dc.html) | Locais · aba 4 |
| [PlaceDetail.dc.html](PlaceDetail.dc.html) | Locais › tocar num local › Detalhe do local |
| [PlaceEdit.dc.html](PlaceEdit.dc.html) | Detalhe do local › Editar |
| [Earnings.dc.html](Earnings.dc.html) | Ganhos · aba 5 (toque em Relatório) |
| [EarningsPdf.dc.html](EarningsPdf.dc.html) | Ganhos › Relatório › PDF de ganhos |
| [Report.dc.html](Report.dc.html) | Detalhe do aluno ou local › Relatório |
| [ReportPdf.dc.html](ReportPdf.dc.html) | Relatório › PDF |
| [Settings.dc.html](Settings.dc.html) | Hoje › engrenagem › Ajustes |
| [Theme.dc.html](Theme.dc.html) | Ajustes › Aparência |
| [ColorPicker.dc.html](ColorPicker.dc.html) | Ajustes › Aparência › Suas cores |
| [Calendar.dc.html](Calendar.dc.html) | Ajustes › Sincronizar calendário |
| [Backup.dc.html](Backup.dc.html) | Ajustes › Backup e planilha |
| [Fees.dc.html](Fees.dc.html) | Ajustes › Cancelamentos e faltas |
| [Setup.dc.html](Setup.dc.html) | Primeira abertura › Configuração inicial (4 passos) |
| [Currency.dc.html](Currency.dc.html) | Out of v1 (reais only, constitution rule 9). The Moeda and Cotação rows in Settings.dc.html are out too |
| [DirA.dc.html](DirA.dc.html) | 2026 look (spec 016) · Hoje |
| [A_Agenda.dc.html](A_Agenda.dc.html) | 2026 look · Agenda, Dia |
| [A_AgendaMes.dc.html](A_AgendaMes.dc.html) | 2026 look · Agenda › Mês |
| [A_AgendaAno.dc.html](A_AgendaAno.dc.html) | 2026 look · Agenda › tocar no mês e ano › escolher mês |
| [A_Aluno.dc.html](A_Aluno.dc.html) | 2026 look · Detalhe do aluno |
| [A_Ganhos.dc.html](A_Ganhos.dc.html) | 2026 look · Ganhos |
| [A_Ajustes.dc.html](A_Ajustes.dc.html) | 2026 look · Ajustes |
| [A_Estilo.dc.html](A_Estilo.dc.html) | 2026 look · color roles, Suas cores parts, type, shapes |

The 2026 boards take every color from CSS variables named after the theme tokens, with Tema and Modo tweaks, so they carry no hex in their markup.
