# ZoomImage

A picture that fills the viewer and zooms (spec 021): the comprovante's image, or a page of a PDF comprovante. Mockup: `Receipt.dc.html`.

## Consumer provides
The `image` already decoded at the screen's size and a `contentDescription` ("Comprovante 1 de 2").

## Rules
- Fits its box at first, whole and centered, on the screen's `surface-sunken`.
- Pinch zooms up to 5×; a drag moves it, never past its edges; a double tap zooms to 2.5× or back to fit.
- Nothing else on the page moves with it: the header and its actions stay put.
