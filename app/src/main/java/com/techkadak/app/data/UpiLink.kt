package com.techkadak.app.data

/**
 * v1.5: Rs 50 "Support Tech Kadak" UPI link. Pure Kotlin (no Android types) so the URI shape is unit-tested.
 * Payee ID comes from BuildConfig.UPI_ID (build-time, from state/secrets). The UI reads the result; it never claims a payment succeeded.
 */
object UpiLink {
    const val AMOUNT_RS = "50"
    const val PAYEE_NAME = "Rohit Saini"
    const val NOTE = "Tech Kadak support"
    private val ID_RE = Regex("^[A-Za-z0-9._-]{2,256}@[A-Za-z]{2,64}$")

    /** UPI ID sahi shape ka hai ya nahi. Galat / khaali = support button chhupa rahega. */
    fun isValidId(pa: String): Boolean = ID_RE.matches(pa)

    /** Encode like a URL query value, but a space is %20 (not +). */
    fun enc(s: String): String = java.net.URLEncoder.encode(s, "UTF-8").replace("+", "%20")

    /** upi://pay?pa=<id>&pn=Rohit%20Saini&am=50&cu=INR&tn=Tech%20Kadak%20support */
    fun payUri(pa: String, pn: String = PAYEE_NAME, am: String = AMOUNT_RS, tn: String = NOTE): String {
        require(isValidId(pa)) { "bad UPI id" }
        return "upi://pay?pa=$pa&pn=${enc(pn)}&am=${enc(am)}&cu=INR&tn=${enc(tn)}"
    }
}
