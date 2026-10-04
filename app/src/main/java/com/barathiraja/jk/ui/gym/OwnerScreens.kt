package com.barathiraja.jk.ui.gym

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.heightIn
import androidx.compose.material.icons.automirrored.outlined.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.outlined.Send
import androidx.compose.material.icons.outlined.EmojiEvents
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.sp
import com.barathiraja.jk.gym.Person
import com.barathiraja.jk.ui.theme.CodeFont
import com.barathiraja.jk.ui.theme.HeroBlue
import com.barathiraja.jk.ui.theme.Leaf
import android.content.Context
import android.content.Intent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.TextButton
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import com.barathiraja.jk.gym.OwnerStats
import com.barathiraja.jk.ui.components.shareText
import com.barathiraja.jk.ui.theme.Ember
import com.barathiraja.jk.ui.theme.Alert
import com.barathiraja.jk.ui.theme.Sun
import java.time.LocalDate
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.foundation.clickable
import java.util.Locale
import java.time.format.TextStyle
import com.barathiraja.jk.ui.components.ProgressLine
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.Icons
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavHostController
import com.barathiraja.jk.gym.PersonStatus
import com.barathiraja.jk.gym.Role
import com.barathiraja.jk.ui.GymViewModel
import com.barathiraja.jk.ui.Routes
import com.barathiraja.jk.ui.components.JkCard
import com.barathiraja.jk.ui.components.Avatar
import com.barathiraja.jk.ui.components.BackScreen

/** Body text for the owner's screens: a step above the app's 15sp body, since the owner is an older reader. */
@Composable
internal fun ownerBody() = MaterialTheme.typography.bodyLarge.copy(fontSize = 17.sp, lineHeight = 25.sp)

@Composable
private fun buttonText() = MaterialTheme.typography.titleMedium

/** Section heading with one plain sentence under it. */
@Composable
internal fun Section(title: String, explain: String? = null) {
    Column(Modifier.padding(start = 4.dp, end = 4.dp, top = 20.dp)) {
        Text(title, style = MaterialTheme.typography.headlineSmall)
        if (explain != null) Text(explain, style = ownerBody(), color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 4.dp))
    }
}

/** A section heading followed by its card; used by the owner's Me screen. */
@Composable
internal fun OwnerCard(title: String, count: Int = 0, explain: String? = null, content: @Composable ColumnScope.() -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Section(if (count > 0) "$title ($count)" else title, explain)
        JkCard(Modifier.fillMaxWidth(), content = content)
    }
}

/** Opens WhatsApp with [text] so the owner just picks the contact; falls back to the share sheet. */
internal fun Context.whatsApp(text: String) {
    val send = Intent(Intent.ACTION_SEND).setType("text/plain").putExtra(Intent.EXTRA_TEXT, text)
    runCatching { startActivity(Intent(send).setPackage("com.whatsapp")) }
        .onFailure { shareText(text) }
}
