package app.tutora.adapters.export

import io.nayuki.qrcodegen.QrCode

/** The QR code of [text] as rows of dark (true) and light modules, error correction M (AC-020-10). */
fun qrMatrix(text: String): List<BooleanArray> {
    val qr = QrCode.encodeText(text, QrCode.Ecc.MEDIUM)
    return List(qr.size) { y -> BooleanArray(qr.size) { x -> qr.getModule(x, y) } }
}
