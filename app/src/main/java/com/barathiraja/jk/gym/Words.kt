package com.barathiraja.jk.gym

import androidx.credentials.exceptions.GetCredentialCancellationException
import androidx.credentials.exceptions.NoCredentialException
import com.google.firebase.FirebaseNetworkException
import com.google.firebase.firestore.FirebaseFirestoreException

/** "1 workout", "3 workouts". */
fun plural(n: Int, word: String) = "$n $word${if (n == 1) "" else "s"}"

/**
 * "_S. Janarthanan_" -> "S. Janarthanan", "KOUNDAR BARATHIRAJA" -> "Koundar Barathiraja",
 * "Barathiraja K 2023-2027" -> "Barathiraja K" (college accounts add the batch years).
 */
fun tidyName(raw: String): String {
    val trimmed = raw.trim().trim('_', '.', '-', ' ').replace(Regex("\\s+"), " ")
        .replace(Regex("[\\s(\\[]*\\d{4}\\s*[-–]\\s*\\d{2,4}[)\\]]*$"), "")
        .replace(Regex("\\s+\\d+$"), "")
        .trim().trim('_', '.', '-', ' ')
    return if (trimmed.any { it.isLowerCase() }) trimmed
    else trimmed.split(' ').joinToString(" ") { w -> w.lowercase().replaceFirstChar { it.titlecase() } }
}

/** An error in words the person can act on. */
fun friendlyMessage(e: Exception): String = when {
    e is GymException -> e.message.orEmpty()
    e is PartialSave -> "Only ${e.saved} of ${e.total} were saved. Check your connection and assign the rest again."
    e is GetCredentialCancellationException -> "Sign-in cancelled"
    e is NoCredentialException -> "No Google account found on this phone"
    e is FirebaseFirestoreException && e.code == FirebaseFirestoreException.Code.PERMISSION_DENIED -> "You don't have permission to do that"
    e is FirebaseNetworkException -> "No internet connection"
    else -> e.message ?: "Something went wrong"
}
