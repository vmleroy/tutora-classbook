# Thumbnail

A comprovante's small picture (spec 021): in the Comprovante part of Registrar / Editar pagamento, on the payment's detail, and on top of Anexar comprovante. `AddThumbnail` is the dashed tile after them that adds another. Mockups: `PaymentSheet.dc.html`, `PaymentDetail.dc.html`, `Receipt.dc.html`.

## Consumer provides
A spoken `label` ("Comprovante 1 de 3, imagem"), the `image` once it is loaded (null shows the file icon), `kindLabel` ("PDF") for a file without a picture, `size` (`MD` 72 in sheets and lists, `LG` 96 on the detail), `onClick` to open the viewer, and `onRemove` only while the payment is being edited.

## Rules
- Corners `radius-xl`; a picture fills the tile from the top, cropped, on `surface-sunken`; a PDF or a picture still loading shows the file icon on `secondary-soft` with `secondary-ink`.
- The × is drawn small (`icon-lg`, `ink` with a `surface` cross) on the top-right corner, but its press area is a full `touch` target. It only takes the comprovante off the form being edited; removing a saved one is the viewer's Remover, which asks first.
- `AddThumbnail` is the same size, a `border-strong` dashed-looking outline in `line-strong`, the plus and its word in `primary`.
- Thumbnails sit in a row with `s3` between them and scroll sideways when there are more than fit; there is no limit on how many.
