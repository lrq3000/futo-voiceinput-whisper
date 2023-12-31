package org.futo.voiceinput.settings

import android.content.Context
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Alignment.Companion.Center
import androidx.compose.ui.Alignment.Companion.CenterHorizontally
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.datastore.preferences.core.edit
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import androidx.navigation.NavHostController
import androidx.navigation.NavOptions
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import org.futo.voiceinput.BuildConfig
import org.futo.voiceinput.FORCE_SHOW_NOTICE
import org.futo.voiceinput.HAS_SEEN_PAID_NOTICE
import org.futo.voiceinput.IS_ALREADY_PAID
import org.futo.voiceinput.IS_PAYMENT_PENDING
import org.futo.voiceinput.NOTICE_REMINDER_TIME
import org.futo.voiceinput.R
import org.futo.voiceinput.Screen
import org.futo.voiceinput.dataStore
import org.futo.voiceinput.payments.BillingManager
import org.futo.voiceinput.startAppActivity
import org.futo.voiceinput.ui.theme.Slate200
import org.futo.voiceinput.ui.theme.Typography
import kotlin.coroutines.coroutineContext
import kotlin.math.absoluteValue

@Composable
fun ParagraphText(it: String) {
    Text(
        it,
        modifier = Modifier.padding(8.dp),
        style = Typography.bodyMedium,
        color = MaterialTheme.colorScheme.onBackground
    )
}

@Composable
fun PaymentText() {
    val numDaysInstalled = useNumberOfDaysInstalled()

    // Doesn't make sense to say "You've been using for ... days" if it's less than seven days
    if(numDaysInstalled.value >= 7) {
        ParagraphText(stringResource(R.string.payment_text_1, numDaysInstalled.value))
    } else {
        ParagraphText(stringResource(R.string.payment_text_1_alt))
    }

    ParagraphText(stringResource(R.string.payment_text_2))
}

suspend fun pushNoticeReminderTime(context: Context, days: Float) {
    // If the user types in a crazy high number, the long can't store such a large value and it won't suppress the reminder
    // 21x the age of the universe ought to be enough for a payment notice reminder
    // Also take the absolute value in the case of a negative number
    val clampedDays = if (days.absoluteValue >= 1.06751991E14f) {
        1.06751991E14f
    } else {
        days.absoluteValue
    }

    context.dataStore.edit { preferences ->
        preferences[NOTICE_REMINDER_TIME] =
            System.currentTimeMillis() / 1000L + (clampedDays * 60.0 * 60.0 * 24.0).toLong()
    }
}

const val TRIAL_PERIOD_DAYS = 30

@Composable
fun UnpaidNoticeCondition(
    force: Boolean = LocalInspectionMode.current,
    showOnlyIfReminder: Boolean = false,
    inner: @Composable () -> Unit
) {
    val numDaysInstalled = useNumberOfDaysInstalled()
    val forceShowNotice = useDataStore(FORCE_SHOW_NOTICE, default = false)
    val isAlreadyPaid = useDataStore(IS_ALREADY_PAID, default = false)
    val pushReminderTime = useDataStore(NOTICE_REMINDER_TIME, default = 0L)
    val currentTime = System.currentTimeMillis() / 1000L

    val reminderTimeIsUp = (currentTime >= pushReminderTime.value)

    val displayCondition = if(showOnlyIfReminder) {
        // Either the reminder time is not up, or we're not past the trial period
        (!isAlreadyPaid.value) && ((!reminderTimeIsUp) || (!forceShowNotice.value && numDaysInstalled.value < TRIAL_PERIOD_DAYS))
    } else {
        // The trial period time is over
        (forceShowNotice.value || (numDaysInstalled.value >= TRIAL_PERIOD_DAYS))
                // and the current time is past the reminder time
                && reminderTimeIsUp
                // and we have not already paid
                && (!isAlreadyPaid.value)
    }
    if (force || displayCondition) {
        inner()
    }
}

@Composable
@Preview
fun ConditionalUnpaidNoticeInVoiceInputWindow(onClose: (() -> Unit)? = null) {
    val context = LocalContext.current

    UnpaidNoticeCondition {
        TextButton(onClick = {
            context.startAppActivity(PaymentActivity::class.java)
            if (onClose != null) onClose()
        }) {
            Text(stringResource(R.string.unpaid_indicator), color = Slate200)
        }
    }
}


@Composable
@Preview
fun UnpaidNotice(onPay: () -> Unit = { }, onAlreadyPaid: () -> Unit = { }) {
    Surface(
        color = MaterialTheme.colorScheme.surfaceVariant, modifier = Modifier
            .fillMaxWidth()
            .padding(8.dp), shape = RoundedCornerShape(4.dp)
    ) {
        Column(modifier = Modifier.padding(8.dp, 0.dp)) {
            Text(
                "Unpaid FUTO Voice Input",
                modifier = Modifier.padding(8.dp),
                style = Typography.titleMedium,
                color = MaterialTheme.colorScheme.onBackground
            )

            PaymentText()

            Row(
                modifier = Modifier
                    .padding(8.dp)
                    .align(CenterHorizontally)
            ) {

                Box(modifier = Modifier.weight(1.0f)) {
                    Button(onClick = onPay, modifier = Modifier.align(Center)) {
                        Text(stringResource(R.string.pay_now))
                    }
                }

                Box(modifier = Modifier.weight(1.0f)) {
                    Button(
                        onClick = onAlreadyPaid, colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.secondary,
                            contentColor = MaterialTheme.colorScheme.onSecondary
                        ), modifier = Modifier.align(Center)
                    ) {
                        Text(stringResource(R.string.i_already_paid))
                    }
                }
            }
        }
    }
}


@Composable
@Preview
fun ConditionalUnpaidNoticeWithNav(navController: NavController = rememberNavController()) {
    val isAlreadyPaid = useDataStore(IS_ALREADY_PAID, default = false)

    UnpaidNoticeCondition {
        UnpaidNotice(onPay = {
            navController.navigate("pleasePay")
        }, onAlreadyPaid = {
            isAlreadyPaid.setValue(true)
        })
    }
}

@Composable
@Preview
fun PaymentThankYouScreen(onExit: () -> Unit = { }) {
    val hasSeenPaidNotice = useDataStore(HAS_SEEN_PAID_NOTICE, default = false)
    val isPending = useDataStore(IS_PAYMENT_PENDING, default = false)

    Screen(
        if (isPending.value) {
            stringResource(R.string.payment_pending)
        } else {
            stringResource(R.string.thank_you)
        }
    ) {
        ScrollableList {
            ParagraphText(stringResource(R.string.thank_you_for_purchasing_voice_input))
            if (isPending.value) {
                ParagraphText(stringResource(R.string.payment_pending_body))
            }
            ParagraphText(stringResource(R.string.purchase_will_help_body))

            Box(modifier = Modifier.fillMaxWidth()) {
                Button(
                    onClick = {
                        hasSeenPaidNotice.setValue(true)
                        onExit()
                    },
                    modifier = Modifier.align(Center)
                ) {
                    Text(stringResource(R.string.continue_))
                }
            }
        }
    }
}

@Composable
@Preview
fun PaymentFailedScreen(onExit: () -> Unit = { }) {
    val hasSeenPaidNotice = useDataStore(HAS_SEEN_PAID_NOTICE, default = true)

    val context = LocalContext.current

    Screen(stringResource(R.string.payment_error)) {
        ScrollableList {
            @Suppress("KotlinConstantConditions")
            ParagraphText( when(BuildConfig.FLAVOR) {
                "fDroid" -> stringResource(R.string.payment_failed_body_ex)
                "dev" -> stringResource(R.string.payment_failed_body_ex)
                "standalone" -> stringResource(R.string.payment_failed_body_ex)
                else -> stringResource(R.string.payment_failed_body)
            })
            ShareFeedbackOption(title = stringResource(R.string.contact_support))
            Box(modifier = Modifier.fillMaxWidth()) {
                val coroutineScope = rememberCoroutineScope()
                Button(
                    onClick = {
                        // It would be rude to immediately annoy the user again about paying, so delay the notice forever
                        coroutineScope.launch {
                            pushNoticeReminderTime(context, Float.MAX_VALUE)
                        }

                        hasSeenPaidNotice.setValue(false)
                        onExit()
                    },
                    modifier = Modifier.align(Center)
                ) {
                    Text(stringResource(R.string.continue_))
                }
            }
        }
    }
}

@Composable
fun PaymentScreen(
    settingsViewModel: SettingsViewModel = viewModel(),
    navController: NavHostController = rememberNavController(),
    onExit: () -> Unit = { },
    billing: BillingManager
) {
    val isAlreadyPaid = useDataStore(IS_ALREADY_PAID, default = false)
    val pushReminderTime = useDataStore(NOTICE_REMINDER_TIME, default = 0L)
    val numDaysInstalled = useNumberOfDaysInstalled()
    val forceShowNotice = useDataStore(FORCE_SHOW_NOTICE, default = false)
    val currentTime = System.currentTimeMillis() / 1000L

    val reminderTimeIsUp = (currentTime >= pushReminderTime.value) && ((numDaysInstalled.value >= TRIAL_PERIOD_DAYS) || forceShowNotice.value)

    val onAlreadyPaid = {
        isAlreadyPaid.setValue(true)
        navController.popBackStack()
        navController.navigate("paid", NavOptions.Builder().setLaunchSingleTop(true).build())
    }

    LaunchedEffect(Unit) {
        billing.checkAlreadyOwnsProduct()
    }

    Screen(stringResource(R.string.payment_title)) {
        ScrollableList {
            PaymentText()

            val context = LocalContext.current
            Column(modifier = Modifier.fillMaxWidth()) {
                Column(
                    modifier = Modifier
                        .padding(8.dp)
                        .align(CenterHorizontally)
                ) {
                    billing.getBillings().forEach {
                        Button(
                            onClick = {
                                it.launchBillingFlow()
                            }, modifier = Modifier
                                .padding(8.dp)
                                .align(CenterHorizontally)
                        ) {
                            val name = it.getName()
                            val text = if(name.isEmpty()) {
                                stringResource(R.string.pay)
                            } else {
                                stringResource(R.string.pay_via_x, name)
                            }

                            Text(text)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(64.dp))

                val counter = remember { mutableStateOf(0) }
                Button(
                    onClick = {
                        counter.value += 1
                        if(counter.value == 2) {
                            onAlreadyPaid()
                        }
                    }, colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.secondary,
                        contentColor = MaterialTheme.colorScheme.onSecondary
                    ), modifier = Modifier.align(CenterHorizontally)
                ) {
                    Text(stringResource(
                        when(counter.value) {
                            0 -> R.string.i_already_paid
                            else -> R.string.i_already_paid_2
                        })
                    )
                }

                if (reminderTimeIsUp) {
                    val lastValidRemindValue = remember { mutableStateOf(5.0f) }
                    val remindDays = remember { mutableStateOf("5") }
                    Row(
                        modifier = Modifier
                            .align(CenterHorizontally)
                            .padding(16.dp)
                    ) {
                        val coroutineScope = rememberCoroutineScope()
                        Button(
                            onClick = {
                                coroutineScope.launch {
                                    pushNoticeReminderTime(context, lastValidRemindValue.value)
                                }
                                onExit()
                            }, colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.secondary,
                                contentColor = MaterialTheme.colorScheme.onSecondary
                            )
                        ) {
                            Text(stringResource(R.string.remind_me_in_x))
                            Surface(color = MaterialTheme.colorScheme.surface) {
                                BasicTextField(
                                    value = remindDays.value,
                                    onValueChange = {
                                        remindDays.value = it

                                        it.toFloatOrNull()?.let { lastValidRemindValue.value = it }
                                    },
                                    modifier = Modifier
                                        .width(32.dp)
                                        .background(MaterialTheme.colorScheme.surface),
                                    textStyle = Typography.bodyMedium.copy(color = MaterialTheme.colorScheme.onSurface),
                                    cursorBrush = SolidColor(MaterialTheme.colorScheme.onSurface),
                                    keyboardOptions = KeyboardOptions.Default.copy(keyboardType = KeyboardType.Number)
                                )
                            }
                            Text(stringResource(R.string.in_x_days))
                        }
                    }
                }

                @Suppress("KotlinConstantConditions")
                if (BuildConfig.FLAVOR == "dev") {
                    Text(
                        stringResource(R.string.developer_mode_payment_methods),
                        style = Typography.labelSmall,
                        modifier = Modifier.padding(8.dp)
                    )
                }
            }
        }
    }
}


@Composable
fun PaymentScreenSwitch(
    settingsViewModel: SettingsViewModel = viewModel(),
    navController: NavHostController = rememberNavController(),
    onExit: () -> Unit = { },
    billing: BillingManager,
    startDestination: String = "pleasePay"
) {
    val isAlreadyPaid = useDataStore(IS_ALREADY_PAID, default = false)
    val hasSeenNotice = useDataStore(HAS_SEEN_PAID_NOTICE, default = false)
    val paymentDest = if (!isAlreadyPaid.value && hasSeenNotice.value) {
        "error"
    } else if (isAlreadyPaid.value && !hasSeenNotice.value) {
        "paid"
    } else {
        "pleasePay"
    }

    LaunchedEffect(paymentDest) {
        if (paymentDest != "pleasePay") {
            navController.navigate(
                paymentDest,
                NavOptions.Builder().setLaunchSingleTop(true).build()
            )
        }
    }

    NavHost(
        navController = navController,
        startDestination = startDestination
    ) {
        composable("pleasePay") {
            PaymentScreen(settingsViewModel, navController, onExit, billing)
        }

        composable("paid") {
            PaymentThankYouScreen(onExit)
        }

        composable("error") {
            PaymentFailedScreen(onExit)
        }
    }
}