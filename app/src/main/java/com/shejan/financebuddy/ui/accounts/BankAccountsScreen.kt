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
private val ACCOUNT_SUBTYPES = listOf("Savings", "Current", "Salary", "Student", "Business", "Islamic", "Personal", "Merchant", "Agent", "Other")

private val BANK_COLOR_MAP = mapOf(
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
    val banks = remember(accounts) { accounts.filter { it.type == "BANK" && it.accountSubtype.isNotBlank() } }
    val mfs   = remember(accounts) { accounts.filter { it.type == "MFS"  && it.accountSubtype.isNotBlank() } }

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
        Box(
            modifier = Modifier.fillMaxWidth().height(250.dp)
                .background(Brush.verticalGradient(colors = listOf(AccentTeal.copy(alpha = 0.07f), Color.Transparent)))
        )

        Column(modifier = Modifier.fillMaxSize()) {
            Row(
                modifier = Modifier.fillMaxWidth().statusBarsPadding()
                    .padding(horizontal = 8.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = { onBack() },
                    modifier = Modifier.size(36.dp)
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.KeyboardArrowLeft,
                        contentDescription = "Back",
                        tint = TextPrimary,
                        modifier = Modifier.size(22.dp)
                    )
                }
                Spacer(modifier = Modifier.width(8.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text("Bank Accounts", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                    Text("Manage your wallets & accounts", fontSize = 12.sp, color = TextMuted)
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text("Total", fontSize = 10.sp, color = TextMuted)
                    Text("৳${currencyFormat.format(accounts.sumOf { it.balance })}", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = AccentTeal)
                }
                Spacer(modifier = Modifier.width(8.dp))
            }

            HorizontalDivider(color = DividerColor, modifier = Modifier.padding(horizontal = 16.dp))

            if (accounts.isEmpty()) {
                Box(modifier = Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(Icons.Default.AccountBalance, null, tint = TextMuted, modifier = Modifier.size(56.dp))
                        Spacer(modifier = Modifier.height(16.dp))
                        Text("No accounts yet", color = TextMuted, fontSize = 16.sp, fontWeight = FontWeight.Medium)
                        Spacer(modifier = Modifier.height(6.dp))
                        Text("Tap + to add your first account", color = TextMuted.copy(alpha = 0.6f), fontSize = 13.sp)
                    }
                }
            } else {
                LazyColumn(
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    if (banks.isNotEmpty()) {
                        item { SectionGroupHeader(title = "Banks", count = banks.size) }
                        items(banks, key = { it.id }) { account ->
                            AccountManageCard(account, currencyFormat,
                                onEdit = { editingAccount = account; showAddSheet = true },
                                onDelete = { deletingAccount = account })
                        }
                    }
                    if (mfs.isNotEmpty()) {
                        item { Spacer(Modifier.height(4.dp)); SectionGroupHeader("Mobile Financial Services (MFS)", mfs.size) }
                        items(mfs, key = { it.id }) { account ->
                            AccountManageCard(account, currencyFormat,
                                onEdit = { editingAccount = account; showAddSheet = true },
                                onDelete = { deletingAccount = account })
                        }
                    }
                    item { Spacer(Modifier.height(80.dp)) }
                }
            }
        }

        FloatingActionButton(
            onClick = { editingAccount = null; showAddSheet = true },
            modifier = Modifier.align(Alignment.BottomEnd).navigationBarsPadding().padding(20.dp).size(56.dp),
            containerColor = Color.Transparent, contentColor = BackgroundDark, shape = CircleShape
        ) {
            Box(
                modifier = Modifier.fillMaxSize().background(Brush.linearGradient(listOf(AccentTeal, AccentBlue))),
                contentAlignment = Alignment.Center
            ) { Icon(Icons.Default.Add, "Add Account", tint = BackgroundDark) }
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
private fun SectionGroupHeader(title: String, count: Int) {
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(vertical = 6.dp)) {
        Box(modifier = Modifier.size(4.dp, 16.dp).clip(RoundedCornerShape(2.dp)).background(AccentTeal))
        Spacer(Modifier.width(10.dp))
        Text(title, color = TextSecondary, fontSize = 12.sp, fontWeight = FontWeight.Bold, letterSpacing = 0.5.sp)
        Spacer(Modifier.width(8.dp))
        Box(modifier = Modifier.clip(RoundedCornerShape(6.dp)).background(AccentTeal.copy(alpha = 0.12f)).padding(horizontal = 6.dp, vertical = 2.dp)) {
            Text(count.toString(), color = AccentTeal, fontSize = 10.sp, fontWeight = FontWeight.Bold)
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
            .height(IntrinsicSize.Min) // Force minimum intrinsic height of content
            .clip(RoundedCornerShape(16.dp))
            .background(CardDark)
            .border(1.dp, cardColor.copy(alpha = 0.15f), RoundedCornerShape(16.dp))
            .combinedClickable(
                onClick = { if (showActions) showActions = false },
                onLongClick = { showActions = true }
            )
    ) {
        // Content container that gets blurred/dimmed
        val blurRadius = if (showActions) 8.dp else 0.dp
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .blur(blurRadius)
                .padding(start = 16.dp, top = 12.dp, end = 12.dp, bottom = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(42.dp)
                    .clip(CircleShape)
                    .background(cardColor.copy(alpha = 0.08f))
                    .border(1.dp, cardColor.copy(alpha = 0.15f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = if (account.type == "MFS") Icons.Default.PhoneAndroid else Icons.Default.AccountBalance,
                    contentDescription = null,
                    tint = cardColor,
                    modifier = Modifier.size(20.dp)
                )
            }
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = account.name,
                    color = TextPrimary,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
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
                    Spacer(Modifier.height(2.dp))
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
            
            // Subtype Tag aligned to the far right corner
            if (account.accountSubtype.isNotBlank()) {
                Spacer(Modifier.width(12.dp))
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(5.dp))
                        .background(cardColor.copy(alpha = 0.12f))
                        .padding(horizontal = 5.dp, vertical = 2.dp)
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

        // Animated overlay for Edit & Delete actions
        AnimatedVisibility(
            visible = showActions,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier.fillMaxSize()
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(BackgroundDark.copy(alpha = 0.88f))
                    .clickable { showActions = false },
                contentAlignment = Alignment.Center
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
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
                        contentPadding = PaddingValues(horizontal = 8.dp),
                        modifier = Modifier.width(110.dp).height(40.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Edit,
                            contentDescription = "Edit",
                            tint = AccentTeal,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Edit", color = TextPrimary, fontSize = 13.sp, fontWeight = FontWeight.Bold, maxLines = 1)
                    }

                    Button(
                        onClick = {
                            showActions = false
                            onDelete()
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = ExpenseRed.copy(alpha = 0.12f)
                        ),
                        shape = RoundedCornerShape(10.dp),
                        border = BorderStroke(1.dp, ExpenseRed.copy(alpha = 0.3f)),
                        contentPadding = PaddingValues(horizontal = 8.dp),
                        modifier = Modifier.width(110.dp).height(40.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = "Delete",
                            tint = ExpenseRed,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Delete", color = ExpenseRed, fontSize = 13.sp, fontWeight = FontWeight.Bold, maxLines = 1)
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
        onDismissRequest = onDismiss, sheetState = sheetState,
        containerColor = CardDark, shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
        dragHandle = {
            var isPressed by remember { mutableStateOf(false) }
            val density = LocalDensity.current
            val targetOffsetPx = remember(density) { with(density) { 4.dp.toPx() } }
            val arrowOffsetPx by animateFloatAsState(
                targetValue = if (isPressed) targetOffsetPx else 0f,
                animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
                label = "arrowOffset"
            )

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 12.dp)
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null
                    ) {}
                    .pointerInput(Unit) {
                        awaitPointerEventScope {
                            while (true) {
                                val down = awaitFirstDown(requireUnconsumed = false)
                                isPressed = true
                                waitForUpOrCancellation()
                                isPressed = false
                            }
                        }
                    },
                contentAlignment = Alignment.Center
            ) {
                Canvas(modifier = Modifier.size(width = 36.dp, height = 10.dp)) {
                    val width = size.width
                    val height = size.height
                    val centerY = height / 2f
                    val strokeWidth = 4.dp.toPx()

                    val path = Path().apply {
                        moveTo(0f, centerY - arrowOffsetPx)
                        lineTo(width / 2f, centerY + arrowOffsetPx)
                        lineTo(width, centerY - arrowOffsetPx)
                    }

                    drawPath(
                        path = path,
                        color = DividerColor.copy(alpha = 0.8f),
                        style = Stroke(
                            width = strokeWidth,
                            cap = StrokeCap.Round,
                            join = StrokeJoin.Round
                        )
                    )
                }
            }
        }
    ) {
        Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp).verticalScroll(rememberScrollState()).navigationBarsPadding()) {
            Row(modifier = Modifier.fillMaxWidth().padding(bottom = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(if (isEditing) "Edit Account" else "Add New Account", color = TextPrimary, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                    Text(if (isEditing) "Update account details" else "Link a bank or MFS to track your money", color = TextMuted, fontSize = 12.sp)
                }
            }

            Spacer(Modifier.height(20.dp))

            // Type toggle
            Row(modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).background(CardDarker).padding(4.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                listOf("BANK", "MFS").forEach { t ->
                    val selected = accountType == t
                    Box(modifier = Modifier.weight(1f).clip(RoundedCornerShape(10.dp))
                        .background(if (selected) AccentTeal else Color.Transparent)
                        .clickable { accountType = t; accountName = TextFieldValue(""); nameExpanded = false }.padding(vertical = 10.dp),
                        contentAlignment = Alignment.Center) {
                        Text(if (t == "BANK") "\uD83C\uDFE6  Bank" else "\uD83D\uDCF1  MFS",
                            color = if (selected) BackgroundDark else TextSecondary,
                            fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal, fontSize = 13.sp)
                    }
                }
            }

            Spacer(Modifier.height(16.dp))

            // Account name autocomplete
            ExposedDropdownMenuBox(expanded = nameExpanded, onExpandedChange = { nameExpanded = it }) {
                OutlinedTextField(
                    value = accountName, onValueChange = { accountName = it; nameExpanded = true },
                    label = { Text(if (accountType == "BANK") "Bank Name" else "MFS Name") },
                    placeholder = { Text("Type or select\u2026", color = TextMuted) },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = nameExpanded) },
                    singleLine = true, shape = RoundedCornerShape(12.dp), colors = formTextFieldColors(accountName.text.isEmpty()),
                    modifier = Modifier.fillMaxWidth().menuAnchor(ExposedDropdownMenuAnchorType.PrimaryEditable)
                )
                if (filteredPresets.isNotEmpty()) {
                    ExposedDropdownMenu(expanded = nameExpanded, onDismissRequest = { nameExpanded = false },
                        modifier = Modifier.background(CardDarker)) {
                        filteredPresets.forEach { preset ->
                            DropdownMenuItem(text = { Text(preset, color = TextPrimary, fontSize = 13.sp) },
                                onClick = {
                                    accountName = TextFieldValue(
                                        text = preset,
                                        selection = TextRange(preset.length)
                                    )
                                    nameExpanded = false
                                })
                        }
                    }
                }
            }

            Spacer(Modifier.height(12.dp))

            // Account subtype (Banks only)
            if (accountType == "BANK") {
                ExposedDropdownMenuBox(expanded = subtypeExpanded, onExpandedChange = { subtypeExpanded = it }) {
                    OutlinedTextField(
                        value = accountSubtype, onValueChange = {},
                        label = { Text("Account Type") },
                        placeholder = { Text("e.g. Savings, Current\u2026", color = TextMuted) },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = subtypeExpanded) },
                        readOnly = true, singleLine = true, shape = RoundedCornerShape(12.dp), colors = formTextFieldColors(accountSubtype.isEmpty()),
                        modifier = Modifier.fillMaxWidth().menuAnchor(ExposedDropdownMenuAnchorType.PrimaryEditable)
                    )
                    ExposedDropdownMenu(expanded = subtypeExpanded, onDismissRequest = { subtypeExpanded = false },
                        modifier = Modifier.background(CardDarker)) {
                        ACCOUNT_SUBTYPES.forEach { sub ->
                            DropdownMenuItem(text = { Text(sub, color = TextPrimary, fontSize = 13.sp) },
                                onClick = { accountSubtype = sub; subtypeExpanded = false })
                        }
                    }
                }
                Spacer(Modifier.height(12.dp))
            }

            // Initial balance (new accounts only)
            if (!isEditing) {
                OutlinedTextField(
                    value = initialBalance, onValueChange = { if (it.isEmpty() || it.toDoubleOrNull() != null) initialBalance = it },
                    label = { Text("Initial Balance (\u09f3)") },
                    prefix = { Text("\u09f3 ", color = AccentTeal, fontWeight = FontWeight.Bold) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true, shape = RoundedCornerShape(12.dp), colors = formTextFieldColors(initialBalance.isEmpty()),
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(12.dp))
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
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                singleLine = true,
                shape = RoundedCornerShape(12.dp),
                colors = formTextFieldColors(accountNumber.isEmpty()),
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(Modifier.height(12.dp))

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
                singleLine = true,
                shape = RoundedCornerShape(12.dp),
                colors = formTextFieldColors(showAs.isEmpty()),
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(Modifier.height(24.dp))

            Button(
                onClick = {
                    val colorHex = BANK_COLOR_MAP[accountName.text] ?: if (accountType == "MFS") "#FF5C7C" else "#0096FF"
                    val saved = if (isEditing) {
                        existingAccount!!.copy(name = accountName.text.trim(), type = accountType, 
                            accountSubtype = if (accountType == "BANK") accountSubtype else "",
                            isManaged = false, holderName = "",
                            accountNumber = accountNumber.trim(), showAs = showAs.trim(), colorHex = colorHex)
                    } else {
                        AccountEntity(name = accountName.text.trim(), type = accountType,
                            balance = initialBalance.toDoubleOrNull() ?: 0.0, colorHex = colorHex,
                            accountSubtype = if (accountType == "BANK") accountSubtype else "", isManaged = false,
                            holderName = "",
                            accountNumber = accountNumber.trim(), showAs = showAs.trim())
                    }
                    onSave(saved)
                },
                enabled = isValid, modifier = Modifier.fillMaxWidth().height(52.dp),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(containerColor = AccentTeal, disabledContainerColor = CardDarker)
            ) {
                Icon(if (isEditing) Icons.Default.Check else Icons.Default.Add, null,
                    tint = if (isValid) BackgroundDark else TextMuted)
                Spacer(Modifier.width(8.dp))
                Text(if (isEditing) "Save Changes" else "Add Account",
                    color = if (isValid) BackgroundDark else TextMuted, fontWeight = FontWeight.Bold)
            }

            Spacer(Modifier.height(16.dp))
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
