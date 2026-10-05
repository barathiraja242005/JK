package com.barathiraja.jk.ui.gym

import android.content.Context
import android.content.Intent
import com.barathiraja.jk.ui.components.shareText

/** Opens WhatsApp with [text] so the owner just picks the contact; falls back to the share sheet. */
internal fun Context.whatsApp(text: String) {
    val send = Intent(Intent.ACTION_SEND).setType("text/plain").putExtra(Intent.EXTRA_TEXT, text)
    runCatching { startActivity(Intent(send).setPackage("com.whatsapp")) }
        .onFailure { shareText(text) }
}
