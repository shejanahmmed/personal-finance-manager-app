package com.shejan.financebuddy.ui.accounts

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
private val ACCOUNT_SUBTYPES = listOf("Savings", "Current", "Salary", "Student", "Business", "Islamic", "Other")

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
    val banks = remember(accounts) { accounts.filter { it.type == "BANK" } }
    val mfs   = remember(accounts) { accounts.filter { it.type == "MFS"  } }

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
                Box(
                    modifier = Modifier.size(40.dp).clip(RoundedCornerShape(10.dp))
                        .background(CardDarker).border(1.dp, DividerColor, RoundedCornerShape(10.dp))
                        .clickable { onBack() },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back", tint = TextPrimary, modifier = Modifier.size(20.dp))
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text("Bank Accounts", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                    Text("Manage your wallets & accounts", fontSize = 12.sp, color = TextMuted)
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text("Total", fontSize = 10.sp, color = TextMuted)
                    Text("?${currencyFormat.format(accounts.sumOf { it.balance })}", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = AccentTeal)
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
    Box(modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(18.dp)).background(CardDark)
        .border(1.dp, cardColor.copy(alpha = 0.25f), RoundedCornerShape(18.dp))) {
        Box(modifier = Modifier.align(Alignment.CenterStart).fillMaxHeight().width(4.dp)
            .clip(RoundedCornerShape(topStart = 18.dp, bottomStart = 18.dp)).background(cardColor))
        Row(
            modifier = Modifier.fillMaxWidth().padding(start = 16.dp, top = 14.dp, end = 12.dp, bottom = 14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(modifier = Modifier.size(44.dp).clip(CircleShape).background(cardColor.copy(alpha = 0.12f)), contentAlignment = Alignment.Center) {
                Icon(if (account.type == "MFS") Icons.Default.PhoneAndroid else Icons.Default.AccountBalance,
                    null, tint = cardColor, modifier = Modifier.size(22.dp))
            }
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(account.name, color = TextPrimary, fontSize = 14.sp, fontWeight = FontWeight.SemiBold,
                        maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f, fill = false))
                    if (account.accountSubtype.isNotBlank()) {
                        Box(modifier = Modifier.clip(RoundedCornerShape(5.dp)).background(cardColor.copy(alpha = 0.15f)).padding(horizontal = 5.dp, vertical = 2.dp)) {
                            Text(account.accountSubtype, color = cardColor, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                    if (account.isManaged) {
                        Box(modifier = Modifier.clip(RoundedCornerShape(5.dp)).background(TransferYellow.copy(alpha = 0.15f)).padding(horizontal = 5.dp, vertical = 2.dp)) {
                            Text("MANAGED", color = TransferYellow, fontSize = 9.sp, fontWeight = FontWeight.ExtraBold)
                        }
                    }
                }
                if (account.isManaged && account.holderName.isNotBlank()) {
                    Spacer(Modifier.height(3.dp))
                    Text("Holder: ${account.holderName}", color = TextMuted, fontSize = 11.sp)
                }
                if (account.accountNumber.isNotBlank()) {
                    Text("Acc: \u2022\u2022\u2022\u2022${account.accountNumber.takeLast(4)}", color = TextMuted, fontSize = 11.sp)
                }
            }
            Spacer(Modifier.width(8.dp))
            Column(horizontalAlignment = Alignment.End) {
                Text("?${currencyFormat.format(account.balance)}",
                    color = if (account.balance > 0) TextPrimary else TextMuted, fontSize = 15.sp, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    Box(modifier = Modifier.size(30.dp).clip(RoundedCornerShape(8.dp)).background(CardDarker)
                        .border(1.dp, DividerColor, RoundedCornerShape(8.dp)).clickable { onEdit() }, contentAlignment = Alignment.Center) {
                        Icon(Icons.Default.Edit, "Edit", tint = AccentTeal, modifier = Modifier.size(14.dp))
                    }
                    Box(modifier = Modifier.size(30.dp).clip(RoundedCornerShape(8.dp)).background(ExpenseRed.copy(alpha = 0.1f))
                        .border(1.dp, ExpenseRed.copy(alpha = 0.3f), RoundedCornerShape(8.dp)).clickable { onDelete() }, contentAlignment = Alignment.Center) {
                        Icon(Icons.Default.Delete, "Delete", tint = ExpenseRed, modifier = Modifier.size(14.dp))
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
    var accountName    by remember(existingAccount) { mutableStateOf(existingAccount?.name ?: "") }
    var accountType    by remember(existingAccount) { mutableStateOf(existingAccount?.type ?: "BANK") }
    var accountSubtype by remember(existingAccount) { mutableStateOf(existingAccount?.accountSubtype ?: "") }
    var initialBalance by remember(existingAccount) { mutableStateOf(if (isEditing) existingAccount!!.balance.toString() else "") }
    var isManaged      by remember(existingAccount) { mutableStateOf(existingAccount?.isManaged ?: false) }
    var holderName     by remember(existingAccount) { mutableStateOf(existingAccount?.holderName ?: "") }
    var accountNumber  by remember(existingAccount) { mutableStateOf(existingAccount?.accountNumber ?: "") }

    var nameExpanded    by remember { mutableStateOf(false) }
    var subtypeExpanded by remember { mutableStateOf(false) }

    val presetList = if (accountType == "BANK") PRESET_BANKS else PRESET_MFS
    val filteredPresets = if (accountName.isBlank()) presetList else presetList.filter { it.contains(accountName, ignoreCase = true) }

    val isValid = accountName.trim().isNotBlank() && (!isManaged || holderName.trim().isNotBlank())

    ModalBottomSheet(
        onDismissRequest = onDismiss, sheetState = sheetState,
        containerColor = CardDark, shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
    ) {
        Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp).verticalScroll(rememberScrollState()).navigationBarsPadding()) {
            Row(modifier = Modifier.fillMaxWidth().padding(bottom = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(if (isEditing) "Edit Account" else "Add New Account", color = TextPrimary, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                    Text(if (isEditing) "Update account details" else "Link a bank or MFS to track your money", color = TextMuted, fontSize = 12.sp)
                }
                Box(modifier = Modifier.size(34.dp).clip(RoundedCornerShape(8.dp)).background(CardDarker)
                    .border(1.dp, DividerColor, RoundedCornerShape(8.dp)).clickable { onDismiss() }, contentAlignment = Alignment.Center) {
                    Icon(Icons.Default.Close, null, tint = TextPrimary, modifier = Modifier.size(18.dp))
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
                        .clickable { accountType = t; accountName = ""; nameExpanded = false }.padding(vertical = 10.dp),
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
                    label = { Text(if (accountType == "BANK") "Bank Name" else "MFS Name", color = TextSecondary) },
                    placeholder = { Text("Type or select\u2026", color = TextMuted) },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = nameExpanded) },
                    singleLine = true, shape = RoundedCornerShape(12.dp), colors = formTextFieldColors(),
                    modifier = Modifier.fillMaxWidth().menuAnchor(ExposedDropdownMenuAnchorType.PrimaryEditable)
                )
                if (filteredPresets.isNotEmpty()) {
                    ExposedDropdownMenu(expanded = nameExpanded, onDismissRequest = { nameExpanded = false },
                        modifier = Modifier.background(CardDarker)) {
                        filteredPresets.forEach { preset ->
                            DropdownMenuItem(text = { Text(preset, color = TextPrimary, fontSize = 13.sp) },
                                onClick = { accountName = preset; nameExpanded = false })
                        }
                    }
                }
            }

            Spacer(Modifier.height(12.dp))

            // Account subtype
            ExposedDropdownMenuBox(expanded = subtypeExpanded, onExpandedChange = { subtypeExpanded = it }) {
                OutlinedTextField(
                    value = accountSubtype, onValueChange = {},
                    label = { Text("Account Type", color = TextSecondary) },
                    placeholder = { Text("e.g. Savings, Current\u2026", color = TextMuted) },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = subtypeExpanded) },
                    readOnly = true, singleLine = true, shape = RoundedCornerShape(12.dp), colors = formTextFieldColors(),
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

            // Initial balance (new accounts only)
            if (!isEditing) {
                OutlinedTextField(
                    value = initialBalance, onValueChange = { if (it.isEmpty() || it.toDoubleOrNull() != null) initialBalance = it },
                    label = { Text("Initial Balance (\u09f3)", color = TextSecondary) },
                    prefix = { Text("\u09f3 ", color = AccentTeal, fontWeight = FontWeight.Bold) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true, shape = RoundedCornerShape(12.dp), colors = formTextFieldColors(),
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(12.dp))
            }

            // Managed toggle
            Box(modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp)).background(CardDarker)
                .border(1.dp, if (isManaged) TransferYellow.copy(alpha = 0.4f) else DividerColor, RoundedCornerShape(14.dp))
                .padding(horizontal = 16.dp, vertical = 12.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Managing for someone else?", color = TextPrimary, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                        Text("Enable if you manage this account for another person", color = TextMuted, fontSize = 11.sp)
                    }
                    Switch(checked = isManaged, onCheckedChange = { isManaged = it },
                        colors = SwitchDefaults.colors(checkedThumbColor = BackgroundDark, checkedTrackColor = TransferYellow))
                }
            }

            // Managed holder fields
            AnimatedVisibility(visible = isManaged, enter = fadeIn(), exit = fadeOut()) {
                Column(modifier = Modifier.animateContentSize()) {
                    Spacer(Modifier.height(12.dp))
                    OutlinedTextField(
                        value = holderName, onValueChange = { holderName = it },
                        label = { Text("Account Holder Name *", color = TextSecondary) },
                        placeholder = { Text("e.g. Father, Mother\u2026", color = TextMuted) },
                        singleLine = true, keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words),
                        shape = RoundedCornerShape(12.dp), colors = formTextFieldColors(), modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(Modifier.height(10.dp))
                    OutlinedTextField(
                        value = accountNumber, onValueChange = { accountNumber = it },
                        label = { Text("Account Number (optional)", color = TextSecondary) },
                        placeholder = { Text("For reference only", color = TextMuted) },
                        singleLine = true, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        shape = RoundedCornerShape(12.dp), colors = formTextFieldColors(), modifier = Modifier.fillMaxWidth()
                    )
                }
            }

            Spacer(Modifier.height(24.dp))

            Button(
                onClick = {
                    val colorHex = BANK_COLOR_MAP[accountName] ?: if (accountType == "MFS") "#FF5C7C" else "#0096FF"
                    val saved = if (isEditing) {
                        existingAccount!!.copy(name = accountName.trim(), type = accountType, accountSubtype = accountSubtype,
                            isManaged = isManaged, holderName = if (isManaged) holderName.trim() else "",
                            accountNumber = if (isManaged) accountNumber.trim() else "", colorHex = colorHex)
                    } else {
                        AccountEntity(name = accountName.trim(), type = accountType,
                            balance = initialBalance.toDoubleOrNull() ?: 0.0, colorHex = colorHex,
                            accountSubtype = accountSubtype, isManaged = isManaged,
                            holderName = if (isManaged) holderName.trim() else "",
                            accountNumber = if (isManaged) accountNumber.trim() else "")
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun formTextFieldColors() = OutlinedTextFieldDefaults.colors(
    focusedBorderColor = AccentTeal, unfocusedBorderColor = DividerColor,
    focusedLabelColor = AccentTeal, unfocusedLabelColor = TextSecondary,
    cursorColor = AccentTeal, focusedTextColor = TextPrimary, unfocusedTextColor = TextPrimary,
    focusedContainerColor = CardDarker, unfocusedContainerColor = CardDarker
)
