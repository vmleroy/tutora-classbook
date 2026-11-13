# Spec 021: Comprovantes

Status: draft
Depends on: 008 Payments and packages (payments, the payment detail), 010 Reports and 020 Fatura and detailed report (what is sent), 011 Backup and restore (the full backup), 015 App lock (what is private)
Mockups: none yet; the plan adds them for the payment sheet, the payment detail and the receipt viewer.

## Goal

When a student, a parent or a place pays by Pix or transfer, the teacher gets a comprovante: a screenshot or a PDF from the bank. Today it lives in WhatsApp or the gallery, apart from the payment it proves. After this spec the teacher keeps each comprovante on its payment in Tutora: added from the camera, the gallery, a file or straight from the bank app's Compartilhar. It shows wherever the payment shows, and it can go along with a Fatura or a Relatório detalhado, so a place or a parent sees the proof next to the classes it paid. Everything stays on the phone, in the backup and behind the app lock.

## User stories

- **US-1.** As a teacher, when I record Ana's Pix I attach the screenshot her mother sent, so I never search WhatsApp for it again.
- **US-2.** As a teacher, from my bank app I tap Compartilhar › Tutora on the comprovante, and Tutora asks which payment (or which payer, to record a new one) it belongs to.
- **US-3.** As a teacher, the salinha asks for proof of what they paid me this month; I send the Relatório detalhado with the comprovantes at the end.
- **US-4.** As a teacher, I change phones and the comprovantes come back with my backup.

## Acceptance criteria

### Adding a comprovante

- **AC-021-1.** Registrar pagamento and Editar pagamento have "Comprovante": Tirar foto, Escolher da galeria, or Escolher arquivo (an image or a PDF). One payment holds up to 3 comprovantes.
- **AC-021-2.** Tutora keeps its own copy of each one in its private storage, so deleting the original from the gallery, Downloads or WhatsApp doesn't lose it. A photo larger than 2000 px on its longer side is reduced to that before it is kept; a PDF is kept as it is, up to 10 MB.
- **AC-021-3.** Sharing an image or a PDF to Tutora from another app (Compartilhar › Tutora) opens "Anexar comprovante": the payments of the last 60 days without a comprovante, newest first, each with payer, date, method and amount, and "Registrar novo pagamento", which opens Registrar pagamento with it attached. With the app lock on, Tutora asks to unlock first (015).

### Seeing it

- **AC-021-4.** A payment with a comprovante shows a small mark wherever the payment is listed (the payer's Pagamentos, Hoje's recent payments) and its thumbnail on the payment detail.
- **AC-021-5.** Tapping the thumbnail opens the comprovante full screen: an image with pinch to zoom, a PDF page by page. From there: Compartilhar, Substituir and Remover (which asks first, constitution rule 3).
- **AC-021-6.** Removing a payment removes its comprovantes with it, after the same confirmation; Desfazer brings both back.

### Sending them

- **AC-021-7.** The Fatura and Relatório detalhado sheets (AC-020-1) have "Incluir comprovantes", off at first and shown only when a payment in the period has one. On, the PDF ends with a "Comprovantes" section: one per page, each headed by its payment ("16/10/2026 · Pix · R$ 540,00"), an image fitted to the page, a PDF's pages after it.
- **AC-021-8.** The Excel files (AC-010-10, AC-020-19) get a "Comprovante" column in Pagamentos: Sim or Não. The files themselves never go into the spreadsheet.
- **AC-021-9.** Copiar como texto never carries a comprovante.

### Keeping them safe

- **AC-021-10.** The full backup (011) carries every comprovante, and restoring it brings them back on their payments. A backup made before this spec restores as before, with no comprovantes.
- **AC-021-11.** Comprovantes never leave the phone except when the teacher shares them, sends a document that includes them, or backs up. Tutora's card in recent apps stays blank while locked (AC-015), the viewer included.
- **AC-021-12.** Ajustes › Backup e planilha shows how much space the comprovantes take ("12 comprovantes · 8,4 MB").

### Language

- **AC-021-13.** Every new word is in Português and English.

## Out of scope

- Reading the amount or date off a comprovante (OCR) to fill the payment.
- Comprovantes on anything but payments (a class, a place's contract).
- Sending a comprovante back to the payer as a receipt from the teacher (a "recibo" made by Tutora).

## Open questions

For Victor, before this is marked ready (each with the assumption written above):

1. **Sending.** Should the comprovantes go inside the PDF, at its end (AC-021-7), or as separate files shared with it (the PDF plus the images)?
2. **Backup format.** The full backup is one .json file today. With comprovantes it becomes a .zip holding that .json and the files. Is that fine, or should images stay out of the backup (lighter, but lost with the phone)?
3. **Compartilhar › Tutora.** Is receiving a comprovante from the bank app in scope (AC-021-3), or only adding it from inside Tutora?
4. **How many.** Up to 3 per payment (AC-021-1), and PDFs as well as images?
5. **On the Fatura.** May a Fatura carry comprovantes (it is mostly about what is still to pay), or only the Relatório detalhado?
