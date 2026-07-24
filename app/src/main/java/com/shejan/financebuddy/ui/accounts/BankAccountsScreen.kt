package com.shejan.financebuddy.ui.accounts

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.Spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.waitForUpOrCancellation
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.BorderStroke
import androidx.compose.ui.draw.blur
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.shejan.financebuddy.data.db.AccountEntity
import com.shejan.financebuddy.ui.theme.*
import kotlinx.coroutines.launch
import java.text.DecimalFormat

private val PRESET_CASH = listOf("Cash in Hand", "Petty Cash", "Wallet Cash")
private val PRESET_BANKS = listOf(
    "BRAC Bank PLC", "The City Bank PLC", "Eastern Bank PLC (EBL)",
    "Dutch-Bangla Bank PLC (DBBL)", "Prime Bank PLC", "Mutual Trust Bank PLC",
    "Islami Bank Bangladesh PLC (IBBL)", "Al-Arafah Islami Bank PLC",
    "Shahjalal Islami Bank PLC", "Sonali Bank PLC", "Janata Bank PLC",
    "Agrani Bank PLC", "Rupali Bank PLC", "Trust Bank PLC",
    "One Bank PLC", "Meghna Bank PLC", "NRB Bank PLC"
)
private val PRESET_MFS = listOf(
    "bKash", "Nagad", "Rocket", "Upay", "CellFin (IBBL)", "Ok Wallet", "MyCash"
)
private val ACCOUNT_SUBTYPES = listOf("Savings", "Current", "Salary", "Student", "Business", "Islamic", "Personal", "Merchant", "Agent", "In Hand", "Wallet", "Petty Cash", "Other")

private val BANK_COLOR_MAP = mapOf(
    "Cash in Hand" to "#10B981",
    "Petty Cash" to "#059669",
    "Wallet Cash" to "#34D399",
    "BRAC Bank PLC" to "#0096FF",
    "The City Bank PLC" to "#007A33",
    "Eastern Bank PLC (EBL)" to "#003366",
    "Dutch-Bangla Bank PLC (DBBL)" to "#7C5CFC",
    "Prime Bank PLC" to "#FF5722",
    "Mutual Trust Bank PLC" to "#0C2340",
    "Islami Bank Bangladesh PLC (IBBL)" to "#1B5E20",
    "Al-Arafah Islami Bank PLC" to "#2E7D32",
    "Shahjalal Islami Bank PLC" to "#008080",
    "bKash" to "#FF5C7C",
    "Nagad" to "#FFBD2E",
    "Rocket" to "#00D4AA",
    "Upay" to "#FFB300",
    "CellFin (IBBL)" to "#4CAF50",
    "Ok Wallet" to "#FF5722",
    "MyCash" to "#3F51B5"
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BankAccountsScreen(
    accounts: List<AccountEntity>,
    onBack: () -> Unit,
    onAddAccount: (AccountEntity) -> Unit,
    onUpdateAccount: (AccountEntity) -> Unit,
    onDeleteAccount: (AccountEntity) -> Unit
) {
    val currencyFormat = remember { DecimalFormat("##,##,##0.00") }
    val cash  = remember(accounts) { accounts.filter { it.type == "CASH" || it.name.contains("Cash", ignoreCase = true) } }
    val banks = remember(accounts) { accounts.filter { it.type == "BANK" && !it.name.contains("Cash", ignoreCase = true) } }
    val mfs   = remember(accounts) { accounts.filter { it.type == "MFS"  && !it.name.contains("Cash", ignoreCase = true) } }
    val totalBalance = remember(accounts) { accounts.sumOf { it.balance } }
    val totalCashBalance = remember(cash) { cash.sumOf { it.balance } }
    val totalBankBalance = remember(banks) { banks.sumOf { it.balance } }
    val totalMfsBalance = remember(mfs) { mfs.sumOf { it.balance } }

    var showAddSheet by remember { mutableStateOf(false) }
    var editingAccount by remember { mutableStateOf<AccountEntity?>(null) }
    var deletingAccount by remember { mutableStateOf<AccountEntity?>(null) }
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val scope = rememberCoroutineScope()

    deletingAccount?.let { acc ->
        AlertDialog(
            onDismissRequest = { deletingAccount = null },
            containerColor   = CardDark,
            title = { Text("Delete Account?", color = TextPrimary, fontWeight = FontWeight.Bold) },
            text  = {
                Text(
                    "Are you sure you want to remove \"${acc.name}\"? Existing transactions will not be deleted.",
                    color = TextSecondary, fontSize = 14.sp
                )
            },
            confirmButton = {
                TextButton(onClick = { onDeleteAccount(acc); deletingAccount = null }) {
                    Text("Delete", color = ExpenseRed, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { deletingAccount = null }) {
                    Text("Cancel", color = TextSecondary)
                }
            }
        )
    }

    Box(modifier = Modifier.fillMaxSize().background(BackgroundDark)) {
        // Ambient background glow
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(280.dp)
                .background(
                    Brush.verticalGradient(
                        colors = listOf(AccentTeal.copy(alpha = 0.08f), Color.Transparent)
                    )
                )
        )

        Column(modifier = Modifier.fillMaxSize()) {
            // Clean Top Bar without clutter
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .padding(horizontal = 12.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = { onBack() },
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(CardDarker)
                        .border(1.dp, DividerColor, CircleShape)
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.KeyboardArrowLeft,
                        contentDescription = "Back",
                        tint = TextPrimary,
                        modifier = Modifier.size(24.dp)
                    )
                }
                Spacer(modifier = Modifier.width(14.dp))
                Column {
                    Text(
                        text = "Bank Accounts",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                    Text(
                        text = "Manage your wallets & accounts",
                        fontSize = 12.sp,
                        color = TextMuted
                    )
                }
            }

            if (accounts.isEmpty()) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Box(
                            modifier = Modifier
                                .size(80.dp)
                                .clip(CircleShape)
                                .background(CardDarker)
                                .border(1.dp, DividerColor, CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.AccountBalance,
                                contentDescription = null,
                                tint = AccentTeal.copy(alpha = 0.7f),
                                modifier = Modifier.size(36.dp)
                            )
                        }
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = "No accounts linked yet",
                            color = TextPrimary,
                            fontSize = 17.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "Tap + below to add your first Bank or MFS",
                            color = TextMuted,
                            fontSize = 13.sp
                        )
                    }
                }
            } else {
                LazyColumn(
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    // Hero Total Balance Summary Card
                    item {
                        AccountsHeroCard(
                            totalBalance = totalBalance,
                            cashBalance = totalCashBalance,
                            bankBalance = totalBankBalance,
                            mfsBalance = totalMfsBalance,
                            cashCount = cash.size,
                            bankCount = banks.size,
                            mfsCount = mfs.size,
                            currencyFormat = currencyFormat
                        )
                    }

                    if (cash.isNotEmpty()) {
                        item { SectionGroupHeader(title = "Cash in Hand", count = cash.size) }
                        items(cash, key = { it.id }) { account ->
                            AccountManageCard(
                                account = account,
                                currencyFormat = currencyFormat,
                                onEdit = { editingAccount = account; showAddSheet = true },
                                onDelete = { deletingAccount = account }
                            )
                        }
                    }

                    if (banks.isNotEmpty()) {
                        item {
                            if (cash.isNotEmpty()) Spacer(Modifier.height(4.dp))
                            SectionGroupHeader(title = "Banks", count = banks.size)
                        }
                        items(banks, key = { it.id }) { account ->
                            AccountManageCard(
                                account = account,
                                currencyFormat = currencyFormat,
                                onEdit = { editingAccount = account; showAddSheet = true },
                                onDelete = { deletingAccount = account }
                            )
                        }
                    }

                    if (mfs.isNotEmpty()) {
                        item {
                            Spacer(Modifier.height(4.dp))
                            SectionGroupHeader(title = "Mobile Financial Services", count = mfs.size)
                        }
                        items(mfs, key = { it.id }) { account ->
                            AccountManageCard(
                                account = account,
                                currencyFormat = currencyFormat,
                                onEdit = { editingAccount = account; showAddSheet = true },
                                onDelete = { deletingAccount = account }
                            )
                        }
                    }

                    item { Spacer(Modifier.height(80.dp)) }
                }
            }
        }

        // Modern Floating Add Button
        FloatingActionButton(
            onClick = { editingAccount = null; showAddSheet = true },
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .navigationBarsPadding()
                .padding(20.dp)
                .size(56.dp),
            containerColor = Color.Transparent,
            contentColor = BackgroundDark,
            shape = CircleShape
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Brush.linearGradient(listOf(AccentTeal, AccentBlue))),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Default.Add, "Add Account", tint = BackgroundDark, modifier = Modifier.size(26.dp))
            }
        }
    }

    if (showAddSheet) {
        AccountFormSheet(
            sheetState = sheetState,
            existingAccount = editingAccount,
            onDismiss = { scope.launch { sheetState.hide() }.invokeOnCompletion { showAddSheet = false; editingAccount = null } },
            onSave = { account ->
                if (editingAccount != null) onUpdateAccount(account) else onAddAccount(account)
                scope.launch { sheetState.hide() }.invokeOnCompletion { showAddSheet = false; editingAccount = null }
            }
        )
    }
}

@Composable
private fun AccountsHeroCard(
    totalBalance: Double,
    cashBalance: Double,
    bankBalance: Double,
    mfsBalance: Double,
    cashCount: Int,
    bankCount: Int,
    mfsCount: Int,
    currencyFormat: DecimalFormat
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = CardDark),
        border = BorderStroke(
            1.dp,
            Brush.horizontalGradient(
                listOf(AccentTeal.copy(alpha = 0.35f), AccentBlue.copy(alpha = 0.15f))
            )
        )
    ) {
        Box(modifier = Modifier.fillMaxWidth()) {
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .background(
                        Brush.linearGradient(
                            colors = listOf(
                                AccentTeal.copy(alpha = 0.08f),
                                AccentPurple.copy(alpha = 0.04f),
                                Color.Transparent
                            )
                        )
                    )
            )

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(28.dp)
                                .clip(CircleShape)
                                .background(AccentTeal.copy(alpha = 0.15f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.AccountBalanceWallet,
                                contentDescription = null,
                                tint = AccentTeal,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "TOTAL NET BALANCE",
                            color = TextSecondary,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 0.8.sp
                        )
                    }

                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(20.dp))
                            .background(CardDarker)
                            .border(1.dp, DividerColor, RoundedCornerShape(20.dp))
                            .padding(horizontal = 10.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = "${cashCount + bankCount + mfsCount} Linked",
                            color = TextSecondary,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                Text(
                    text = "৳${currencyFormat.format(totalBalance)}",
                    fontSize = 28.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )

                Spacer(modifier = Modifier.height(16.dp))
                HorizontalDivider(color = DividerColor.copy(alpha = 0.6f))
                Spacer(modifier = Modifier.height(14.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Cash Pill
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.weight(1f)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(28.dp)
                                .clip(CircleShape)
                                .background(IncomeGreen.copy(alpha = 0.12f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Payments,
                                contentDescription = null,
                                tint = IncomeGreen,
                                modifier = Modifier.size(15.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(6.dp))
                        Column {
                            Text("Cash ($cashCount)", fontSize = 10.sp, color = TextMuted)
                            Text("৳${currencyFormat.format(cashBalance)}", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                        }
                    }

                    Box(modifier = Modifier.width(1.dp).height(24.dp).background(DividerColor))
                    Spacer(modifier = Modifier.width(6.dp))

                    // Banks Pill
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.weight(1f)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(28.dp)
                                .clip(CircleShape)
                                .background(AccentBlue.copy(alpha = 0.12f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.AccountBalance,
                                contentDescription = null,
                                tint = AccentBlue,
                                modifier = Modifier.size(15.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(6.dp))
                        Column {
                            Text("Banks ($bankCount)", fontSize = 10.sp, color = TextMuted)
                            Text("৳${currencyFormat.format(bankBalance)}", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                        }
                    }

                    Box(modifier = Modifier.width(1.dp).height(24.dp).background(DividerColor))
                    Spacer(modifier = Modifier.width(6.dp))

                    // MFS Pill
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.weight(1f)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(28.dp)
                                .clip(CircleShape)
                                .background(ExpenseRed.copy(alpha = 0.12f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.PhoneAndroid,
                                contentDescription = null,
                                tint = ExpenseRed,
                                modifier = Modifier.size(15.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(6.dp))
                        Column {
                            Text("MFS ($mfsCount)", fontSize = 10.sp, color = TextMuted)
                            Text("৳${currencyFormat.format(mfsBalance)}", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SectionGroupHeader(title: String, count: Int) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.padding(top = 8.dp, bottom = 4.dp)
    ) {
        Box(
            modifier = Modifier
                .size(4.dp, 16.dp)
                .clip(RoundedCornerShape(2.dp))
                .background(AccentTeal)
        )
        Spacer(Modifier.width(10.dp))
        Text(
            text = title,
            color = TextSecondary,
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 0.5.sp
        )
        Spacer(Modifier.width(8.dp))
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(6.dp))
                .background(AccentTeal.copy(alpha = 0.12f))
                .padding(horizontal = 7.dp, vertical = 2.dp)
        ) {
            Text(
                text = count.toString(),
                color = AccentTeal,
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun AccountManageCard(
    account: AccountEntity,
    currencyFormat: DecimalFormat,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    val cardColor = remember(account.colorHex) {
        try { Color(android.graphics.Color.parseColor(account.colorHex)) } catch (e: Exception) { AccentTeal }
    }
    var showActions by remember { mutableStateOf(false) }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(IntrinsicSize.Min)
            .clip(RoundedCornerShape(16.dp))
            .background(CardDark)
            .border(1.dp, cardColor.copy(alpha = 0.20f), RoundedCornerShape(16.dp))
            .combinedClickable(
                onClick = { if (showActions) showActions = false },
                onLongClick = { showActions = true }
            )
    ) {
        val blurRadius = if (showActions) 8.dp else 0.dp
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .blur(blurRadius)
                .padding(start = 14.dp, top = 14.dp, end = 12.dp, bottom = 14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(cardColor.copy(alpha = 0.12f))
                    .border(1.dp, cardColor.copy(alpha = 0.25f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = if (account.type == "CASH" || account.name.contains("Cash", ignoreCase = true)) Icons.Default.Payments else if (account.type == "MFS") Icons.Default.PhoneAndroid else Icons.Default.AccountBalance,
                    contentDescription = null,
                    tint = cardColor,
                    modifier = Modifier.size(22.dp)
                )
            }

            Spacer(Modifier.width(14.dp))

            Column(modifier = Modifier.weight(1f)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = account.name,
                        color = TextPrimary,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f, fill = false)
                    )

                    if (account.accountSubtype.isNotBlank()) {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(cardColor.copy(alpha = 0.14f))
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = account.accountSubtype,
                                color = cardColor,
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }

                Spacer(Modifier.height(4.dp))

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "Balance: ",
                        color = TextMuted,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium
                    )
                    Text(
                        text = "৳${currencyFormat.format(account.balance)}",
                        color = if (account.balance > 0) TextPrimary else TextMuted,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                if (account.accountNumber.isNotBlank() || account.showAs.isNotBlank()) {
                    Spacer(Modifier.height(3.dp))
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        if (account.accountNumber.isNotBlank()) {
                            val displayNum = if (account.accountNumber.length > 4) {
                                "•••• ${account.accountNumber.takeLast(4)}"
                            } else {
                                account.accountNumber
                            }
                            Text(
                                text = "Acc: $displayNum",
                                color = TextSecondary,
                                fontSize = 11.sp
                            )
                        }
                        if (account.accountNumber.isNotBlank() && account.showAs.isNotBlank()) {
                            Text(
                                text = "•",
                                color = TextMuted,
                                fontSize = 11.sp
                            )
                        }
                        if (account.showAs.isNotBlank()) {
                            Text(
                                text = "Show as: ${account.showAs}",
                                color = TextSecondary,
                                fontSize = 11.sp
                            )
                        }
                    }
                }
            }

            // Quick Menu Button
            IconButton(
                onClick = { showActions = true },
                modifier = Modifier.size(32.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.MoreVert,
                    contentDescription = "Account Options",
                    tint = TextMuted,
                    modifier = Modifier.size(18.dp)
                )
            }
        }

        // Animated action overlay for Edit & Delete
        AnimatedVisibility(
            visible = showActions,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier.fillMaxSize()
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(BackgroundDark.copy(alpha = 0.90f))
                    .clickable { showActions = false },
                contentAlignment = Alignment.Center
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Button(
                        onClick = {
                            showActions = false
                            onEdit()
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = CardDarker
                        ),
                        shape = RoundedCornerShape(10.dp),
                        border = BorderStroke(1.dp, DividerColor),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
                        modifier = Modifier.height(38.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Edit,
                            contentDescription = "Edit",
                            tint = AccentTeal,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Edit", color = TextPrimary, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }

                    Button(
                        onClick = {
                            showActions = false
                            onDelete()
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = ExpenseRed.copy(alpha = 0.15f)
                        ),
                        shape = RoundedCornerShape(10.dp),
                        border = BorderStroke(1.dp, ExpenseRed.copy(alpha = 0.35f)),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
                        modifier = Modifier.height(38.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = "Delete",
                            tint = ExpenseRed,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Delete", color = ExpenseRed, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AccountFormSheet(
    sheetState: SheetState,
    existingAccount: AccountEntity?,
    onDismiss: () -> Unit,
    onSave: (AccountEntity) -> Unit
) {
    val isEditing = existingAccount != null
    var accountName    by remember(existingAccount) {
        mutableStateOf(
            TextFieldValue(
                text = existingAccount?.name ?: "",
                selection = TextRange((existingAccount?.name ?: "").length)
            )
        )
    }
    var accountType    by remember(existingAccount) { mutableStateOf(existingAccount?.type ?: "BANK") }
    var accountSubtype by remember(existingAccount) { mutableStateOf(existingAccount?.accountSubtype ?: "") }
    var initialBalance by remember(existingAccount) { mutableStateOf(if (isEditing) existingAccount!!.balance.toString() else "") }
    var accountNumber  by remember(existingAccount) { mutableStateOf(existingAccount?.accountNumber ?: "") }
    var showAs         by remember(existingAccount) { mutableStateOf(existingAccount?.showAs ?: "") }
    var nameExpanded    by remember { mutableStateOf(false) }
    var subtypeExpanded by remember { mutableStateOf(false) }

    val presetList = if (accountType == "BANK") PRESET_BANKS else PRESET_MFS
    val filteredPresets = if (accountName.text.isBlank()) presetList else presetList.filter { it.contains(accountName.text, ignoreCase = true) }

    val isValid = accountName.text.trim().isNotBlank() &&
            (accountType != "BANK" || accountSubtype.isNotBlank()) &&
            (isEditing || initialBalance.trim().isNotBlank())

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = CardDark,
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
        dragHandle = { BottomSheetDefaults.DragHandle(color = DividerColor) }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .verticalScroll(rememberScrollState())
                .navigationBarsPadding()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(CircleShape)
                        .background(AccentTeal.copy(alpha = 0.15f))
                        .border(1.dp, AccentTeal.copy(alpha = 0.3f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = if (isEditing) Icons.Default.Edit else Icons.Default.AddCard,
                        contentDescription = null,
                        tint = AccentTeal,
                        modifier = Modifier.size(22.dp)
                    )
                }

                Spacer(modifier = Modifier.width(14.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = if (isEditing) "Edit Account" else "Add New Account",
                        color = TextPrimary,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = if (isEditing) "Update account details" else "Link a bank or MFS to track your money",
                        color = TextMuted,
                        fontSize = 12.sp
                    )
                }
            }

            Spacer(Modifier.height(18.dp))

            // Type toggle
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .background(CardDarker)
                    .border(1.dp, DividerColor, RoundedCornerShape(14.dp))
                    .padding(4.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                listOf("CASH", "BANK", "MFS").forEach { t ->
                    val selected = accountType == t
                    val itemColor = if (selected) BackgroundDark else TextPrimary
                    val icon = when (t) {
                        "CASH" -> Icons.Default.Payments
                        "MFS"  -> Icons.Default.PhoneAndroid
                        else   -> Icons.Default.AccountBalance
                    }
                    val label = when (t) {
                        "CASH" -> "Cash"
                        "MFS"  -> "MFS"
                        else   -> "Bank"
                    }

                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(10.dp))
                            .background(if (selected) AccentTeal else Color.Transparent)
                            .clickable {
                                accountType = t
                                accountName = TextFieldValue("")
                                nameExpanded = false
                            }
                            .padding(vertical = 11.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Icon(
                                imageVector = icon,
                                contentDescription = null,
                                tint = itemColor,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = label,
                                color = itemColor,
                                fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                                fontSize = 13.sp
                            )
                        }
                    }
                }
            }

            Spacer(Modifier.height(16.dp))

            // Account name autocomplete (Opens keyboard + suggestions on tap)
            ExposedDropdownMenuBox(
                expanded = nameExpanded,
                onExpandedChange = { nameExpanded = it }
            ) {
                OutlinedTextField(
                    value = accountName,
                    onValueChange = {
                        accountName = it
                        nameExpanded = true
                    },
                    label = { Text(if (accountType == "CASH") "Cash Account Name" else if (accountType == "BANK") "Bank Name" else "MFS Name") },
                    placeholder = { Text("Type or select\u2026", color = TextMuted) },
                    leadingIcon = {
                        Icon(
                            imageVector = when (accountType) {
                                "CASH" -> Icons.Default.Payments
                                "MFS"  -> Icons.Default.PhoneAndroid
                                else   -> Icons.Default.AccountBalance
                            },
                            contentDescription = null,
                            tint = AccentTeal,
                            modifier = Modifier.size(20.dp)
                        )
                    },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = nameExpanded) },
                    singleLine = true,
                    shape = RoundedCornerShape(14.dp),
                    colors = formTextFieldColors(accountName.text.isEmpty()),
                    modifier = Modifier
                        .fillMaxWidth()
                        .menuAnchor(
                            type = ExposedDropdownMenuAnchorType.PrimaryEditable,
                            enabled = true
                        )
                )
                if (filteredPresets.isNotEmpty()) {
                    ExposedDropdownMenu(
                        expanded = nameExpanded,
                        onDismissRequest = { nameExpanded = false },
                        modifier = Modifier
                            .background(CardDarker)
                            .border(1.dp, DividerColor, RoundedCornerShape(12.dp))
                    ) {
                        filteredPresets.forEach { preset ->
                            DropdownMenuItem(
                                text = { Text(preset, color = TextPrimary, fontSize = 13.sp) },
                                onClick = {
                                    accountName = TextFieldValue(
                                        text = preset,
                                        selection = TextRange(preset.length)
                                    )
                                    nameExpanded = false
                                }
                            )
                        }
                    }
                }
            }

            Spacer(Modifier.height(14.dp))

            // Account subtype (Banks only)
            if (accountType == "BANK") {
                ExposedDropdownMenuBox(
                    expanded = subtypeExpanded,
                    onExpandedChange = { subtypeExpanded = it }
                ) {
                    OutlinedTextField(
                        value = accountSubtype,
                        onValueChange = {},
                        label = { Text("Account Type") },
                        placeholder = { Text("e.g. Savings, Current\u2026", color = TextMuted) },
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Default.Tune,
                                contentDescription = null,
                                tint = AccentTeal,
                                modifier = Modifier.size(20.dp)
                            )
                        },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = subtypeExpanded) },
                        readOnly = true,
                        singleLine = true,
                        shape = RoundedCornerShape(14.dp),
                        colors = formTextFieldColors(accountSubtype.isEmpty()),
                        modifier = Modifier
                            .fillMaxWidth()
                            .menuAnchor(ExposedDropdownMenuAnchorType.PrimaryEditable)
                    )
                    ExposedDropdownMenu(
                        expanded = subtypeExpanded,
                        onDismissRequest = { subtypeExpanded = false },
                        modifier = Modifier
                            .background(CardDarker)
                            .border(1.dp, DividerColor, RoundedCornerShape(12.dp))
                    ) {
                        ACCOUNT_SUBTYPES.forEach { sub ->
                            DropdownMenuItem(
                                text = { Text(sub, color = TextPrimary, fontSize = 13.sp) },
                                onClick = { accountSubtype = sub; subtypeExpanded = false }
                            )
                        }
                    }
                }
                Spacer(Modifier.height(14.dp))
            }

            // Initial balance (new accounts only)
            if (!isEditing) {
                OutlinedTextField(
                    value = initialBalance,
                    onValueChange = { if (it.isEmpty() || it.toDoubleOrNull() != null) initialBalance = it },
                    label = { Text("Initial Balance (\u09f3)") },
                    prefix = { Text("\u09f3 ", color = AccentTeal, fontWeight = FontWeight.Bold) },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.AccountBalanceWallet,
                            contentDescription = null,
                            tint = AccentTeal,
                            modifier = Modifier.size(20.dp)
                        )
                    },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true,
                    shape = RoundedCornerShape(14.dp),
                    colors = formTextFieldColors(initialBalance.isEmpty()),
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(14.dp))
            }

            // Account Number (digits only)
            OutlinedTextField(
                value = accountNumber,
                onValueChange = { input ->
                    if (input.all { it.isDigit() }) {
                        accountNumber = input
                    }
                },
                label = { Text("Account Number") },
                placeholder = { Text("Digits only", color = TextMuted) },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Default.CreditCard,
                        contentDescription = null,
                        tint = AccentTeal,
                        modifier = Modifier.size(20.dp)
                    )
                },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                singleLine = true,
                shape = RoundedCornerShape(14.dp),
                colors = formTextFieldColors(accountNumber.isEmpty()),
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(Modifier.height(14.dp))

            // Nickname (max 20 chars)
            OutlinedTextField(
                value = showAs,
                onValueChange = { input ->
                    if (input.length <= 20) {
                        showAs = input
                    }
                },
                label = { Text("Nickname") },
                placeholder = { Text("Nickname (max 20 letters)", color = TextMuted) },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Default.Badge,
                        contentDescription = null,
                        tint = AccentTeal,
                        modifier = Modifier.size(20.dp)
                    )
                },
                singleLine = true,
                shape = RoundedCornerShape(14.dp),
                colors = formTextFieldColors(showAs.isEmpty()),
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(Modifier.height(24.dp))

            Button(
                onClick = {
                    val colorHex = BANK_COLOR_MAP[accountName.text] ?: if (accountType == "CASH") "#10B981" else if (accountType == "MFS") "#FF5C7C" else "#0096FF"
                    val saved = if (isEditing) {
                        existingAccount!!.copy(
                            name = accountName.text.trim(),
                            type = accountType,
                            accountSubtype = if (accountType == "BANK" || accountType == "CASH") accountSubtype else "",
                            isManaged = false,
                            holderName = "",
                            accountNumber = accountNumber.trim(),
                            showAs = showAs.trim(),
                            colorHex = colorHex
                        )
                    } else {
                        AccountEntity(
                            name = accountName.text.trim(),
                            type = accountType,
                            balance = initialBalance.toDoubleOrNull() ?: 0.0,
                            colorHex = colorHex,
                            accountSubtype = if (accountType == "BANK" || accountType == "CASH") accountSubtype else "",
                            isManaged = false,
                            holderName = "",
                            accountNumber = accountNumber.trim(),
                            showAs = showAs.trim()
                        )
                    }
                    onSave(saved)
                },
                enabled = isValid,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color.Transparent,
                    disabledContainerColor = CardDarker
                ),
                contentPadding = PaddingValues(0.dp)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            if (isValid) Brush.linearGradient(listOf(AccentTeal, AccentBlue))
                            else Brush.linearGradient(listOf(CardDarker, CardDarker))
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = if (isEditing) Icons.Default.Check else Icons.Default.Add,
                            contentDescription = null,
                            tint = if (isValid) BackgroundDark else TextMuted,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(
                            text = if (isEditing) "Save Changes" else "Add Account",
                            color = if (isValid) BackgroundDark else TextMuted,
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp
                        )
                    }
                }
            }

            Spacer(Modifier.height(20.dp))
        }
    }
}

@Composable
private fun formTextFieldColors(isEmpty: Boolean) = OutlinedTextFieldDefaults.colors(
    focusedBorderColor = AccentTeal, unfocusedBorderColor = DividerColor,
    focusedLabelColor = AccentTeal, unfocusedLabelColor = if (isEmpty) TextMuted else TextSecondary,
    cursorColor = AccentTeal, focusedTextColor = TextPrimary, unfocusedTextColor = TextPrimary,
    focusedContainerColor = CardDarker, unfocusedContainerColor = CardDarker,
    focusedPlaceholderColor = TextMuted, unfocusedPlaceholderColor = TextMuted
)
