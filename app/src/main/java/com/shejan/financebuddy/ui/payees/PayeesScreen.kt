package com.shejan.financebuddy.ui.payees

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.filled.*
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.shejan.financebuddy.data.db.PayeeAccountEntity
import com.shejan.financebuddy.data.db.PayeeEntity
import com.shejan.financebuddy.ui.theme.*
import kotlinx.coroutines.launch
import java.util.Locale
import java.util.UUID

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PayeesScreen(
    payees: List<PayeeEntity>,
    payeeAccounts: List<PayeeAccountEntity>,
    onBack: () -> Unit,
    onPayeeClick: (PayeeEntity) -> Unit,
    onAddPayee: (PayeeEntity, List<PayeeAccountEntity>) -> Unit
) {
    var searchQuery by remember { mutableStateOf("") }
    var showAddSheet by remember { mutableStateOf(false) }
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val scope = rememberCoroutineScope()

    val filteredPayees = remember(payees, searchQuery) {
        if (searchQuery.isBlank()) {
            payees
        } else {
            payees.filter { it.name.contains(searchQuery, ignoreCase = true) }
        }
    }

    val accountsCountMap = remember(payeeAccounts) {
        payeeAccounts.groupBy { it.payeeId }.mapValues { it.value.size }
    }

    Box(modifier = Modifier.fillMaxSize().background(BackgroundDark)) {
        // Ambient glow
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(250.dp)
                .background(Brush.verticalGradient(colors = listOf(AccentBlue.copy(alpha = 0.06f), Color.Transparent)))
        )

        Column(modifier = Modifier.fillMaxSize()) {
            // -- Top Bar ------------------------------------------
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
                Column(modifier = Modifier.weight(1f)) {
                    Text("Recipient Profiles", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                    Text("Manage contacts and payment accounts", fontSize = 12.sp, color = TextMuted)
                }
            }

            // -- Search Bar ---------------------------------------
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                placeholder = { Text("Search by name...", color = TextMuted) },
                leadingIcon = { Icon(Icons.Default.Search, null, tint = AccentBlue, modifier = Modifier.size(20.dp)) },
                trailingIcon = {
                    if (searchQuery.isNotEmpty()) {
                        IconButton(onClick = { searchQuery = "" }) {
                            Icon(Icons.Default.Close, null, tint = TextMuted, modifier = Modifier.size(18.dp))
                        }
                    }
                },
                singleLine = true,
                colors = SearchTextFieldColors(),
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp)
            )

            // -- Payees List --------------------------------------
            if (filteredPayees.isEmpty()) {
                Box(modifier = Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Box(
                            modifier = Modifier
                                .size(72.dp)
                                .clip(CircleShape)
                                .background(CardDarker)
                                .border(1.dp, DividerColor, CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.People, null, tint = AccentBlue.copy(alpha = 0.7f), modifier = Modifier.size(34.dp))
                        }
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            if (searchQuery.isEmpty()) "No recipients yet" else "No matching profiles",
                            color = TextPrimary, fontSize = 16.sp, fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            if (searchQuery.isEmpty()) "Create profiles to quickly transfer funds" else "Try searching for another name",
                            color = TextMuted, fontSize = 13.sp
                        )
                    }
                }
            } else {
                LazyColumn(
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    items(filteredPayees, key = { it.id }) { payee ->
                        val count = accountsCountMap[payee.id] ?: 0
                        PayeeCard(
                            payee = payee,
                            accountCount = count,
                            onClick = { onPayeeClick(payee) }
                        )
                    }
                    item { Spacer(Modifier.height(80.dp)) }
                }
            }
        }

        // -- FAB --------------------------------------------------
        FloatingActionButton(
            onClick = { showAddSheet = true },
            modifier = Modifier.align(Alignment.BottomEnd).navigationBarsPadding().padding(20.dp).size(56.dp),
            containerColor = Color.Transparent, contentColor = BackgroundDark, shape = CircleShape
        ) {
            Box(
                modifier = Modifier.fillMaxSize().background(Brush.linearGradient(listOf(AccentBlue, AccentTeal))),
                contentAlignment = Alignment.Center
            ) { Icon(Icons.Default.Add, "Add Recipient", tint = BackgroundDark, modifier = Modifier.size(26.dp)) }
        }
    }

    if (showAddSheet) {
        AddPayeeSheet(
            sheetState = sheetState,
            onDismiss = { scope.launch { sheetState.hide() }.invokeOnCompletion { showAddSheet = false } },
            onSave = { name, accountsList ->
                val uniqueId = "PAY-" + UUID.randomUUID().toString().take(4).uppercase(Locale.ROOT)
                onAddPayee(PayeeEntity(name = name.trim(), uniqueId = uniqueId), accountsList)
                scope.launch { sheetState.hide() }.invokeOnCompletion { showAddSheet = false }
            }
        )
    }
}

@Composable
private fun PayeeCard(
    payee: PayeeEntity,
    accountCount: Int,
    onClick: () -> Unit
) {
    val initial = payee.name.trim().take(1).uppercase(Locale.ROOT)
    val avatarBg = remember(payee.name) {
        val hash = payee.name.hashCode()
        val colors = listOf(AccentTeal, AccentBlue, TransferYellow, IncomeGreen, Color(0xFF9C27B0), Color(0xFFE91E63))
        colors[Math.abs(hash) % colors.size]
    }

    Card(
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = CardDark),
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .border(1.dp, DividerColor, RoundedCornerShape(18.dp))
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Profile initial avatar
            Box(
                modifier = Modifier
                    .size(46.dp)
                    .clip(CircleShape)
                    .background(avatarBg.copy(alpha = 0.12f))
                    .border(1.dp, avatarBg.copy(alpha = 0.25f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = initial,
                    color = avatarBg,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(Modifier.width(14.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = payee.name,
                    color = TextPrimary,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(Modifier.height(3.dp))
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(CardDarker)
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = payee.uniqueId,
                            color = TextMuted,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Text(
                        text = "•",
                        color = TextMuted,
                        fontSize = 11.sp
                    )
                    Text(
                        text = if (accountCount == 1) "1 account" else "$accountCount accounts",
                        color = AccentBlue,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }

            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .background(AccentBlue.copy(alpha = 0.12f))
                    .padding(horizontal = 10.dp, vertical = 6.dp)
            ) {
                Text("View Profile", color = AccentBlue, fontSize = 11.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AddPayeeSheet(
    sheetState: SheetState,
    onDismiss: () -> Unit,
    onSave: (String, List<PayeeAccountEntity>) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var accountsList by remember { mutableStateOf(listOf<PayeeAccountDraft>()) }

    val isMainNameValid = name.trim().isNotBlank()
    val areAccountsValid = accountsList.all { it.bankName.trim().isNotBlank() && it.accountNumber.trim().isNotBlank() }
    val isValid = isMainNameValid && areAccountsValid

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
                        imageVector = Icons.Default.PersonAdd,
                        contentDescription = null,
                        tint = AccentBlue,
                        modifier = Modifier.size(22.dp)
                    )
                }

                Spacer(modifier = Modifier.width(14.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text("Add Recipient", color = TextPrimary, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                    Text("Create a new recipient profile", color = TextMuted, fontSize = 12.sp)
                }
            }

            Spacer(Modifier.height(18.dp))

            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text("Recipient Name *", color = TextSecondary) },
                placeholder = { Text("e.g. Shejan Ahmmed", color = TextMuted) },
                leadingIcon = {
                    Icon(Icons.Default.Person, null, tint = AccentBlue, modifier = Modifier.size(20.dp))
                },
                singleLine = true,
                shape = RoundedCornerShape(14.dp),
                colors = formTextFieldColors(),
                modifier = Modifier.fillMaxWidth()
            )

            // List of associated accounts
            accountsList.forEachIndexed { index, account ->
                Spacer(Modifier.height(16.dp))
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(CardDarker)
                        .border(1.dp, DividerColor, RoundedCornerShape(16.dp))
                        .padding(14.dp)
                ) {
                    Text("Account #${index + 1}", color = AccentBlue, fontSize = 13.sp, fontWeight = FontWeight.Bold)

                    Spacer(Modifier.height(10.dp))

                    // Type Toggle (Bank / MFS with vector icons)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(CardDark)
                            .border(1.dp, DividerColor, RoundedCornerShape(12.dp))
                            .padding(4.dp),
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        listOf("CASH", "BANK", "MFS").forEach { t ->
                            val selected = account.type == t
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
                                    .clickable {
                                        accountsList = accountsList.toMutableList().apply {
                                            this[index] = account.copy(type = t, bankName = "")
                                        }
                                    }
                                    .padding(vertical = 9.dp),
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
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = label,
                                        color = itemColor,
                                        fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                                        fontSize = 12.sp
                                    )
                                }
                            }
                        }
                    }

                    Spacer(Modifier.height(12.dp))

                    // Bank Name Autocomplete
                    var nameExpanded by remember { mutableStateOf(false) }
                    val presetList = if (account.type == "BANK") PRESET_BANKS else PRESET_MFS
                    val filteredPresets = if (account.bankName.isBlank()) presetList else presetList.filter { it.contains(account.bankName, ignoreCase = true) }

                    ExposedDropdownMenuBox(
                        expanded = nameExpanded,
                        onExpandedChange = { nameExpanded = it }
                    ) {
                        OutlinedTextField(
                            value = account.bankName,
                            onValueChange = { valText ->
                                accountsList = accountsList.toMutableList().apply {
                                    this[index] = account.copy(bankName = valText)
                                }
                                nameExpanded = true
                            },
                            label = { Text(if (account.type == "BANK") "Bank Name *" else "MFS Name *", color = TextSecondary) },
                            placeholder = { Text("Type or select…", color = TextMuted) },
                            leadingIcon = {
                                Icon(
                                    imageVector = if (account.type == "BANK") Icons.Default.AccountBalance else Icons.Default.PhoneAndroid,
                                    contentDescription = null,
                                    tint = AccentBlue,
                                    modifier = Modifier.size(18.dp)
                                )
                            },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = nameExpanded) },
                            singleLine = true,
                            shape = RoundedCornerShape(12.dp),
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
                                        onClick = {
                                            accountsList = accountsList.toMutableList().apply {
                                                this[index] = account.copy(bankName = preset)
                                            }
                                            nameExpanded = false
                                        }
                                    )
                                }
                            }
                        }
                    }

                    Spacer(Modifier.height(10.dp))

                    // Account Number / Wallet Number
                    OutlinedTextField(
                        value = account.accountNumber,
                        onValueChange = { valText ->
                            accountsList = accountsList.toMutableList().apply {
                                this[index] = account.copy(accountNumber = valText)
                            }
                        },
                        label = { Text(if (account.type == "BANK") "Account Number *" else "Mobile Number *", color = TextSecondary) },
                        placeholder = { Text(if (account.type == "BANK") "e.g. 120409..." else "e.g. 017...", color = TextMuted) },
                        leadingIcon = {
                            Icon(Icons.Default.CreditCard, null, tint = AccentBlue, modifier = Modifier.size(18.dp))
                        },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        shape = RoundedCornerShape(12.dp),
                        colors = formTextFieldColors(),
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(Modifier.height(10.dp))

                    // Nickname / Alias
                    OutlinedTextField(
                        value = account.nickname,
                        onValueChange = { valText ->
                            if (valText.length <= 20) {
                                accountsList = accountsList.toMutableList().apply {
                                    this[index] = account.copy(nickname = valText)
                                }
                            }
                        },
                        label = { Text("Nickname (Optional)", color = TextSecondary) },
                        placeholder = { Text("e.g. Personal, Salary", color = TextMuted) },
                        leadingIcon = {
                            Icon(Icons.Default.Badge, null, tint = AccentBlue, modifier = Modifier.size(18.dp))
                        },
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        colors = formTextFieldColors(),
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(Modifier.height(10.dp))

                    // Account Holder Name (Defaults to main name)
                    OutlinedTextField(
                        value = if (account.recipientName.isBlank()) name else account.recipientName,
                        onValueChange = { valText ->
                            accountsList = accountsList.toMutableList().apply {
                                this[index] = account.copy(recipientName = valText)
                            }
                        },
                        label = { Text("Account Holder Name", color = TextSecondary) },
                        leadingIcon = {
                            Icon(Icons.Default.PersonOutline, null, tint = AccentBlue, modifier = Modifier.size(18.dp))
                        },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words),
                        shape = RoundedCornerShape(12.dp),
                        colors = formTextFieldColors(),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }

            Spacer(Modifier.height(16.dp))

            // Add Account Button Row with Square Red Delete Button on right side
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedButton(
                    onClick = {
                        accountsList = accountsList + PayeeAccountDraft()
                    },
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp),
                    shape = RoundedCornerShape(14.dp),
                    border = BorderStroke(1.dp, AccentBlue.copy(alpha = 0.5f)),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = AccentBlue)
                ) {
                    Icon(Icons.Default.Add, null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("Add Bank/MFS Account", fontWeight = FontWeight.Bold)
                }

                if (accountsList.isNotEmpty()) {
                    Box(
                        modifier = Modifier
                            .size(48.dp)
                            .clip(RoundedCornerShape(14.dp))
                            .background(ExpenseRed.copy(alpha = 0.12f))
                            .border(1.dp, ExpenseRed.copy(alpha = 0.35f), RoundedCornerShape(14.dp))
                            .clickable {
                                if (accountsList.isNotEmpty()) {
                                    accountsList = accountsList.toMutableList().apply { removeAt(lastIndex) }
                                }
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = "Remove Account",
                            tint = ExpenseRed,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }

            Spacer(Modifier.height(24.dp))

            Button(
                onClick = {
                    val accountsToSave = accountsList.map { draft ->
                        PayeeAccountEntity(
                            payeeId = 0,
                            bankName = draft.bankName.trim(),
                            accountNumber = draft.accountNumber.trim(),
                            recipientName = if (draft.recipientName.isBlank()) name.trim() else draft.recipientName.trim(),
                            type = draft.type,
                            nickname = draft.nickname.trim()
                        )
                    }
                    onSave(name, accountsToSave)
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
                            imageVector = Icons.Default.Add,
                            contentDescription = null,
                            tint = if (isValid) BackgroundDark else TextMuted,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(
                            text = "Create Profile",
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
private fun SearchTextFieldColors() = OutlinedTextFieldDefaults.colors(
    focusedBorderColor = AccentBlue.copy(alpha = 0.6f),
    unfocusedBorderColor = DividerColor,
    focusedContainerColor = CardDark,
    unfocusedContainerColor = CardDark,
    focusedTextColor = TextPrimary,
    unfocusedTextColor = TextPrimary
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun formTextFieldColors() = OutlinedTextFieldDefaults.colors(
    focusedBorderColor = AccentBlue, unfocusedBorderColor = DividerColor,
    focusedLabelColor = AccentBlue, unfocusedLabelColor = TextSecondary,
    cursorColor = AccentBlue, focusedTextColor = TextPrimary, unfocusedTextColor = TextPrimary,
    focusedContainerColor = CardDarker, unfocusedContainerColor = CardDarker
)

private data class PayeeAccountDraft(
    val type: String = "BANK",
    val bankName: String = "",
    val accountNumber: String = "",
    val nickname: String = "",
    val recipientName: String = ""
)

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
