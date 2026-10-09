package com.barathiraja.jk.ui.gym

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.KeyboardArrowDown
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.barathiraja.jk.gym.TestGym
import com.barathiraja.jk.ui.GymViewModel
import com.barathiraja.jk.ui.theme.Jk
import com.barathiraja.jk.ui.theme.plex

/** Test mode's strip at the top: says it's test data, switches who you're looking as, and leaves test mode. */
@Composable
fun TestModeBar(gvm: GymViewModel) {
    val viewer by gvm.testViewer.collectAsStateWithLifecycle()
    var open by remember { mutableStateOf(false) }
    Row(
        Modifier.fillMaxWidth().background(Color.Black).statusBarsPadding().heightIn(min = 44.dp).padding(horizontal = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text("TEST", Modifier.clip(RoundedCornerShape(6.dp)).background(Jk.Yellow).padding(horizontal = 6.dp, vertical = 2.dp),
            style = plex(11.sp, FontWeight.Bold, tracking = 1.sp), color = Color.Black)
        Spacer(Modifier.width(10.dp))
        Box(Modifier.weight(1f)) {
            Surface(onClick = { open = true }, color = Color.Transparent, contentColor = Color.White, shape = RoundedCornerShape(8.dp)) {
                Row(Modifier.heightIn(min = 40.dp).padding(horizontal = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text("Viewing as ${viewer.label}", style = plex(14.sp, FontWeight.SemiBold), maxLines = 1, overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f, fill = false))
                    Icon(Icons.Outlined.KeyboardArrowDown, "Switch view", Modifier.size(20.dp))
                }
            }
            DropdownMenu(open, { open = false }) {
                TestGym.Viewer.entries.forEach { v ->
                    DropdownMenuItem(
                        text = { Text(v.label, style = plex(15.sp)) },
                        onClick = { open = false; gvm.viewTestGymAs(v) },
                        trailingIcon = { if (v == viewer) Icon(Icons.Outlined.Check, null) },
                    )
                }
                HorizontalDivider()
                DropdownMenuItem(text = { Text("Start over with fresh test data", style = plex(15.sp)) },
                    onClick = { open = false; gvm.setTestMode(true) })
            }
        }
        Surface(onClick = { gvm.setTestMode(false) }, color = Color.Transparent, contentColor = Jk.Yellow, shape = RoundedCornerShape(8.dp)) {
            Text("Exit", Modifier.heightIn(min = 40.dp).padding(horizontal = 10.dp, vertical = 10.dp), style = plex(14.sp, FontWeight.Bold))
        }
    }
}
