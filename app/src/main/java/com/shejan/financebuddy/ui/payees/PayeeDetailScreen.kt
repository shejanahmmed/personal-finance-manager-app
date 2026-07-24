package com.shejan.financebuddy.ui.payees

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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.shejan.financebuddy.data.db.PayeeAccountEntity
import com.shejan.financebuddy.data.db.PayeeEntity
import com.shejan.financebuddy.ui.theme.*
import kotlinx.coroutines.launch
import java.util.Locale

private val PRESET_BANKS = listOf(
    "BRAC Bank PLC", "The City Bank PLC", "Eastern Bank PLC (EBL)",
    "Dutch-Bangla Bank PLC (DBBL)", "Prime Bank PLC", "Mutual Trust Bank PLC",
    "Islami Bank Bangladesh PLC (IBBL)", "Al-Arafah Islami Bank PLC",
    "Shahjalal Islami Bank PLC", "Sonali Bank PLC", "Janata Bank PLC",
    "Agrani Bank PLC", "Rupali Bank PLC", "Trust Bank PLC"
)
private val PRESET_MFS = listOf(
    "bKash", "Nagad", "Rocket", "Upay", "CellFin (IBBL)", "Ok Wallet", "MyCash"
)

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
fun PayeeDetailScreen(
    payee: PayeeEntity?,
    accounts: List<PayeeAccountEntity>,
    onBack: () -> Unit,
    onDeletePayee: () -> Unit,
    onAddAccount: (PayeeAccountEntity) -> Unit,
    onUpdateAccount: (PayeeAccountEntity) -> Unit,
    onDeleteAccount: (PayeeAccountEntity) -> Unit
) {
    if (payee == null) {
        Box(modifier = Modifier.fillMaxSize().background(BackgroundDark), contentAlignment = Alignment.Center) {
            CircularProgressIndicator(color = AccentBlue)
        }
        return
    }

    var showAddSheet by remember { mutableStateOf(false) }
    var editingAccount by remember { mutableStateOf<PayeeAccountEntity?>(null) }
    var showDeleteConfirm by remember { mutableStateOf(false) }
    var accountToDelete by remember { mutableStateOf<PayeeAccountEntity?>(null) }

    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val scope = rememberCoroutineScope()

    val avatarBg = remember(payee.name) {
        val hash = payee.name.hashCode()
        val colors = listOf(AccentTeal, AccentBlue, TransferYellow, IncomeGreen, Color(0xFF9C27B0), Color(0xFFE91E63))
        colors[Math.abs(hash) % colors.size]
    }

    // Delete Payee Confirmation
    if (showDeleteConfirm) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            containerColor = CardDark,
            title = { Text("Delete Recipient Profile?", color = TextPrimary, fontWeight = FontWeight.Bold) },
            text = { Text("This will permanently delete \"${payee.name}\" and all their saved accounts.", color = TextSecondary) },
            confirmButton = {
                TextButton(onClick = {
                    onDeletePayee()
                    showDeleteConfirm = false
                }) { Text("Delete", color = ExpenseRed, fontWeight = FontWeight.Bold) }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirm = false }) { Text("Cancel", color = TextSecondary) }
            }
        )
    }

    // Delete Account Confirmation
    accountToDelete?.let { acc ->
        AlertDialog(
            onDismissRequest = { accountToDelete = null },
            containerColor = CardDark,
            title = { Text("Remove Saved Account?", color = TextPrimary, fontWeight = FontWeight.Bold) },
            text = { Text("Are you sure you want to remove this ${acc.bankName} account?", color = TextSecondary) },
            confirmButton = {
                TextButton(onClick = {
                    onDeleteAccount(acc)
                    accountToDelete = null
                }) { Text("Remove", color = ExpenseRed, fontWeight = FontWeight.Bold) }
            },
            dismissButton = {
                TextButton(onClick = { accountToDelete = null }) { Text("Cancel", color = TextSecondary) }
            }
        )
    }

    Box(modifier = Modifier.fillMaxSize().background(BackgroundDark)) {
        // Ambient background glow
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(300.dp)
                .background(Brush.verticalGradient(colors = listOf(avatarBg.copy(alpha = 0.08f), Color.Transparent)))
        )

        Column(modifier = Modifier.fillMaxSize()) {
            // -- Top Action Bar ------------------------------------
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
                Spacer(modifier = Modifier.weight(1f))
                IconButton(
                    onClick = { showDeleteConfirm = true },
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(ExpenseRed.copy(alpha = 0.12f))
                        .border(1.dp, ExpenseRed.copy(alpha = 0.25f), CircleShape)
                ) {
                    Icon(Icons.Default.Delete, "Delete Payee", tint = ExpenseRed, modifier = Modifier.size(20.dp))
                }
            }

            // -- Header Profile Card ------------------------------
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 8.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Box(
                    modifier = Modifier
                        .size(80.dp)
                        .clip(CircleShape)
                        .background(avatarBg.copy(alpha = 0.14f))
                        .border(2.dp, avatarBg.copy(alpha = 0.35f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Text(payee.name.take(1).uppercase(Locale.ROOT), color = avatarBg, fontSize = 34.sp, fontWeight = FontWeight.Bold)
                }
                Spacer(Modifier.height(12.dp))
                Text(payee.name, color = TextPrimary, fontSize = 22.sp, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(6.dp))
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(CardDarker)
                        .padding(horizontal = 10.dp, vertical = 4.dp)
                        .border(1.dp, DividerColor, RoundedCornerShape(8.dp))
                ) {
                    Text(payee.uniqueId, color = TextMuted, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }
            }

            Spacer(Modifier.height(16.dp))
            HorizontalDivider(color = DividerColor, modifier = Modifier.padding(horizontal = 20.dp))

            // -- Accounts Header ----------------------------------
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 20.dp, end = 20.dp, top = 16.dp, bottom = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Saved Accounts", color = TextSecondary, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(10.dp))
                        .background(AccentBlue.copy(alpha = 0.12f))
                        .clickable { editingAccount = null; showAddSheet = true }
                        .padding(horizontal = 10.dp, vertical = 5.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Add, null, tint = AccentBlue, modifier = Modifier.size(14.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("Add Account", color = AccentBlue, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }

            // -- Linked Accounts List -----------------------------
            if (accounts.isEmpty()) {
                Box(modifier = Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Box(
                            modifier = Modifier
                                .size(64.dp)
                                .clip(CircleShape)
                                .background(CardDarker)
                                .border(1.dp, DividerColor, CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.AccountBalanceWallet, null, tint = TextMuted.copy(alpha = 0.6f), modifier = Modifier.size(30.dp))
                        }
                        Spacer(modifier = Modifier.height(12.dp))
                        Text("No saved accounts for this recipient", color = TextMuted, fontSize = 13.sp)
                    }
                }
            } else {
                LazyColumn(
                    contentPadding = PaddingValues(horizontal = 20.dp, vertical = 4.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    items(accounts, key = { it.id }) { acc ->
                        PayeeAccountCard(
                            account = acc,
                            onEdit = { editingAccount = acc; showAddSheet = true },
                            onDelete = { accountToDelete = acc }
                        )
                    }
                }
            }
        }
    }

    if (showAddSheet) {
        PayeeAccountFormSheet(
            sheetState = sheetState,
            payeeName = payee.name,
            existingAccount = editingAccount,
            onDismiss = { scope.launch { sheetState.hide() }.invokeOnCompletion { showAddSheet = false; editingAccount = null } },
            onSave = { acc ->
                if (editingAccount != null) {
                    onUpdateAccount(acc.copy(id = editingAccount!!.id, payeeId = payee.id))
                } else {
                    onAddAccount(acc.copy(payeeId = payee.id))
                }
                scope.launch { sheetState.hide() }.invokeOnCompletion { showAddSheet = false; editingAccount = null }
            }
        )
    }
}

@Composable
private fun PayeeAccountCard(
    account: PayeeAccountEntity,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    val cardColor = remember(account.bankName) {
        try { Color(android.graphics.Color.parseColor(BANK_COLOR_MAP[account.bankName] ?: "#0096FF")) }
        catch (e: Exception) { AccentTeal }
    }

    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = CardDark),
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, cardColor.copy(alpha = 0.20f), RoundedCornerShape(16.dp))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
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
                    imageVector = if (account.type == "MFS") Icons.Default.PhoneAndroid else Icons.Default.AccountBalance,
                    contentDescription = null,
                    tint = cardColor,
                    modifier = Modifier.size(22.dp)
                )
            }
            Spacer(Modifier.width(14.dp))
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        text = account.bankName,
                        color = TextPrimary,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    if (account.nickname.isNotBlank()) {
                        Box(modifier = Modifier.clip(RoundedCornerShape(6.dp)).background(AccentTeal.copy(alpha = 0.14f)).padding(horizontal = 6.dp, vertical = 2.dp)) {
                            Text(account.nickname, color = AccentTeal, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                    Box(modifier = Modifier.clip(RoundedCornerShape(6.dp)).background(cardColor.copy(alpha = 0.14f)).padding(horizontal = 6.dp, vertical = 2.dp)) {
                        Text(account.type, color = cardColor, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                    }
                }
                Spacer(Modifier.height(3.dp))
                val maskedNumber = if (account.accountNumber.length > 4) {
                    val last4 = account.accountNumber.takeLast(4)
                    when (account.accountNumber.length) {
                        16 -> "•••• •••• •••• $last4"
                        13 -> "•••• •••• • $last4"
                        11 -> "•••• ••• $last4"
                        else -> "•".repeat(account.accountNumber.length - 4) + " $last4"
                    }
                } else {
                    account.accountNumber
                }
                Text("Number: $maskedNumber", color = TextSecondary, fontSize = 12.sp)
                if (account.recipientName.isNotBlank()) {
                    Text("Name: ${account.recipientName}", color = TextMuted, fontSize = 11.sp)
                }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(CardDarker)
                        .border(1.dp, DividerColor, RoundedCornerShape(8.dp))
                        .clickable { onEdit() },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Default.Edit, "Edit", tint = AccentBlue, modifier = Modifier.size(14.dp))
                }
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(ExpenseRed.copy(alpha = 0.12f))
                        .border(1.dp, ExpenseRed.copy(alpha = 0.3f), RoundedCornerShape(8.dp))
                        .clickable { onDelete() },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Default.Delete, "Delete", tint = ExpenseRed, modifier = Modifier.size(14.dp))
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PayeeAccountFormSheet(
    sheetState: SheetState,
    payeeName: String,
    existingAccount: PayeeAccountEntity?,
    onDismiss: () -> Unit,
    onSave: (PayeeAccountEntity) -> Unit
) {
    val isEditing = existingAccount != null

    var type by remember(existingAccount) { mutableStateOf(existingAccount?.type ?: "BANK") }
    var bankName by remember(existingAccount) { mutableStateOf(existingAccount?.bankName ?: "") }
    var accountNumber by remember(existingAccount) { mutableStateOf(existingAccount?.accountNumber ?: "") }
    var recipientName by remember(existingAccount) { mutableStateOf(existingAccount?.recipientName ?: payeeName) }
    var nickname by remember(existingAccount) { mutableStateOf(existingAccount?.nickname ?: "") }

    var nameExpanded by remember { mutableStateOf(false) }

    val presetList = if (type == "BANK") PRESET_BANKS else PRESET_MFS
    val filteredPresets = if (bankName.isBlank()) presetList else presetList.filter { it.contains(bankName, ignoreCase = true) }

    val isValid = bankName.trim().isNotBlank() && accountNumber.trim().isNotBlank()

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
                        .background(AccentBlue.copy(alpha = 0.15f))
                        .border(1.dp, AccentBlue.copy(alpha = 0.3f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = if (isEditing) Icons.Default.Edit else Icons.Default.AddCard,
                        contentDescription = null,
                        tint = AccentBlue,
                        modifier = Modifier.size(22.dp)
                    )
                }

                Spacer(modifier = Modifier.width(14.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = if (isEditing) "Edit Account" else "Add Saved Account",
                        color = TextPrimary,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = if (isEditing) "Update bank details" else "Link banking details to this profile",
                        color = TextMuted,
                        fontSize = 12.sp
                    )
                }
            }

            Spacer(Modifier.height(18.dp))

            // Type Toggle (Bank / MFS with vector icons)
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
                    val selected = type == t
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
                            .background(if (selected) AccentBlue else Color.Transparent)
                            .clickable { type = t; bankName = ""; nameExpanded = false }
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
                            Spacer(modifier = Modifier.width(8.dp))
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

            // Bank Name Autocomplete
            ExposedDropdownMenuBox(
                expanded = nameExpanded,
                onExpandedChange = { nameExpanded = it }
            ) {
                OutlinedTextField(
                    value = bankName,
                    onValueChange = { bankName = it; nameExpanded = true },
                    label = { Text(if (type == "BANK") "Bank Name" else "MFS Name", color = TextSecondary) },
                    placeholder = { Text("Type or select\u2026", color = TextMuted) },
                    leadingIcon = {
                        Icon(
                            imageVector = if (type == "BANK") Icons.Default.AccountBalance else Icons.Default.PhoneAndroid,
                            contentDescription = null,
                            tint = AccentBlue,
                            modifier = Modifier.size(20.dp)
                        )
                    },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = nameExpanded) },
                    singleLine = true,
                    shape = RoundedCornerShape(14.dp),
                    colors = formTextFieldColors(),
                    modifier = Modifier
                        .fillMaxWidth()
                        .menuAnchor(ExposedDropdownMenuAnchorType.PrimaryEditable)
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
                                onClick = { bankName = preset; nameExpanded = false }
                            )
                        }
                    }
                }
            }

            Spacer(Modifier.height(14.dp))

            // Account Number / Wallet Number
            OutlinedTextField(
                value = accountNumber,
                onValueChange = { accountNumber = it },
                label = { Text(if (type == "BANK") "Account Number" else "Mobile Number", color = TextSecondary) },
                placeholder = { Text(if (type == "BANK") "e.g. 12040921..." else "e.g. 01712...", color = TextMuted) },
                leadingIcon = {
                    Icon(Icons.Default.CreditCard, null, tint = AccentBlue, modifier = Modifier.size(20.dp))
                },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                shape = RoundedCornerShape(14.dp),
                colors = formTextFieldColors(),
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(Modifier.height(14.dp))

            // Recipient Name on the account
            OutlinedTextField(
                value = recipientName,
                onValueChange = { recipientName = it },
                label = { Text("Account Holder Name", color = TextSecondary) },
                leadingIcon = {
                    Icon(Icons.Default.PersonOutline, null, tint = AccentBlue, modifier = Modifier.size(20.dp))
                },
                singleLine = true,
                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words),
                shape = RoundedCornerShape(14.dp),
                colors = formTextFieldColors(),
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(Modifier.height(14.dp))

            // Nickname / Alias
            OutlinedTextField(
                value = nickname,
                onValueChange = { if (it.length <= 20) nickname = it },
                label = { Text("Nickname (Optional)", color = TextSecondary) },
                placeholder = { Text("e.g. Personal, Business", color = TextMuted) },
                leadingIcon = {
                    Icon(Icons.Default.Badge, null, tint = AccentBlue, modifier = Modifier.size(20.dp))
                },
                singleLine = true,
                shape = RoundedCornerShape(14.dp),
                colors = formTextFieldColors(),
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(Modifier.height(24.dp))

            Button(
                onClick = {
                    onSave(
                        PayeeAccountEntity(
                            payeeId = 0,
                            bankName = bankName.trim(),
                            accountNumber = accountNumber.trim(),
                            recipientName = recipientName.trim(),
                            type = type,
                            nickname = nickname.trim()
                        )
                    )
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
                            if (isValid) Brush.linearGradient(listOf(AccentBlue, AccentTeal))
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun formTextFieldColors() = OutlinedTextFieldDefaults.colors(
    focusedBorderColor = AccentBlue, unfocusedBorderColor = DividerColor,
    focusedLabelColor = AccentBlue, unfocusedLabelColor = TextSecondary,
    cursorColor = AccentBlue, focusedTextColor = TextPrimary, unfocusedTextColor = TextPrimary,
    focusedContainerColor = CardDarker, unfocusedContainerColor = CardDarker
)
