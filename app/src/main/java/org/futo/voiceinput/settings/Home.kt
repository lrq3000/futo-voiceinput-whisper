package org.futo.voiceinput.settings

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Send
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.rememberNavController
import org.futo.voiceinput.ENABLE_MULTILINGUAL
import org.futo.voiceinput.ENABLE_SOUND
import org.futo.voiceinput.ENGLISH_MODEL_INDEX
import org.futo.voiceinput.ENGLISH_MODEL_INDEX_DEFAULT
import org.futo.voiceinput.HAS_SEEN_PAID_NOTICE
import org.futo.voiceinput.IS_ALREADY_PAID
import org.futo.voiceinput.MULTILINGUAL_MODEL_INDEX
import org.futo.voiceinput.MULTILINGUAL_MODEL_INDEX_DEFAULT
import org.futo.voiceinput.R
import org.futo.voiceinput.Screen
import org.futo.voiceinput.openURI
import org.futo.voiceinput.ui.theme.Typography
import org.futo.voiceinput.BuildConfig


@Composable
fun ShareFeedbackOption(title: String = stringResource(R.string.send_feedback)) {
    val context = LocalContext.current
    val mailUri = "mailto:${stringResource(R.string.support_email)}"
    SettingItem(title = title, onClick = {
        context.openURI(mailUri)
    }) {
        Icon(Icons.Default.Send, contentDescription = stringResource(R.string.go))
    }
}

@Composable
fun IssueTrackerOption(title: String = stringResource(R.string.issue_tracker)) {
    val context = LocalContext.current
    val mailUri = "https://github.com/futo-org/voice-input/issues"
    SettingItem(title = title, onClick = {
        context.openURI(mailUri)
    }) {
        Icon(Icons.Default.ArrowForward, contentDescription = stringResource(R.string.go))
    }
}

@Composable
fun SettingsSeparator(text: String) {
    Spacer(modifier = Modifier.height(16.dp))
    Text(text, style = Typography.labelMedium)
}

@Composable
@Preview
fun HomeScreen(
    settingsViewModel: SettingsViewModel = viewModel(),
    navController: NavHostController = rememberNavController()
) {
    val isAlreadyPaid = useDataStore(IS_ALREADY_PAID, default = false)

    val (multilingual, _) = useDataStore(key = ENABLE_MULTILINGUAL, default = false)
    val multilingualSubtitle = if (multilingual) {
        stringResource(R.string.multilingual_enabled_english_will_be_slower)
    } else {
        null
    }

    Screen(stringResource(R.string.futo_voice_input_settings)) {
        ScrollableList {
            Text(
                "v${BuildConfig.VERSION_NAME}",
                style = Typography.labelSmall,
                modifier = Modifier.fillMaxWidth(),
                textAlign = TextAlign.End
            )

            ConditionalUnpaidNoticeWithNav(navController)
            ConditionalUpdate()

            SettingsSeparator(stringResource(R.string.options))
            SettingItem(
                title = stringResource(R.string.languages),
                onClick = { navController.navigate("languages") },
                subtitle = multilingualSubtitle
            ) {
                Icon(Icons.Default.ArrowForward, contentDescription = stringResource(R.string.go))
            }
            SettingItem(
                title = stringResource(R.string.models),
                onClick = { navController.navigate("models") },
                subtitle = modelsSubtitle()
            ) {
                Icon(Icons.Default.ArrowForward, contentDescription = stringResource(R.string.go))
            }
            SettingItem(
                title = stringResource(R.string.input_options),
                onClick = { navController.navigate("input") }
            ) {
                Icon(Icons.Default.ArrowForward, contentDescription = stringResource(R.string.go))
            }

            SettingsSeparator(stringResource(R.string.miscellaneous))
            SettingItem(
                title = stringResource(R.string.testing_menu),
                subtitle = stringResource(R.string.try_out_voice_input),
                onClick = { navController.navigate("testing") }
            ) {
                Icon(Icons.Default.ArrowForward, contentDescription = stringResource(R.string.go))
            }
            UnpaidNoticeCondition(showOnlyIfReminder = true) {
                SettingItem(
                    title = stringResource(R.string.payment),
                    onClick = { navController.navigate("pleasePay") }) {
                    Icon(
                        Icons.Default.ArrowForward,
                        contentDescription = stringResource(R.string.go)
                    )
                }
            }
            SettingItem(title = stringResource(R.string.advanced), onClick = { navController.navigate("advanced") }) {
                Icon(Icons.Default.ArrowForward, contentDescription = stringResource(R.string.go))
            }

            SettingsSeparator(stringResource(R.string.about))
            SettingItem(
                title = stringResource(R.string.help),
                onClick = { navController.navigate("help") }) {
                Icon(Icons.Default.ArrowForward, contentDescription = stringResource(R.string.go))
            }
            SettingItem(
                title = stringResource(R.string.credits),
                onClick = { navController.navigate("credits") }) {
                Icon(Icons.Default.ArrowForward, contentDescription = stringResource(R.string.go))
            }
            ShareFeedbackOption()
            IssueTrackerOption()

            if (isAlreadyPaid.value) {
                Text(
                    stringResource(R.string.thank_you_for_using_the_paid_version_of_futo_voice_input),
                    style = Typography.bodyMedium,
                    modifier = Modifier.fillMaxWidth(),
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}
