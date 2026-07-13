package com.shejan.financebuddy.ui.pending

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.shejan.financebuddy.data.db.AccountEntity
import com.shejan.financebuddy.data.db.PendingSmsTransactionEntity
import com.shejan.financebuddy.ui.theme.*
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.*

// ─── Categories ──────────────────────────────────────────────────────────────

private val CATEGORIES = listOf(
    "Mobile Banking", "Banking", "Food & Dining", "Shopping", "Transport",
    "Utilities", "Healthcare", "Education", "Entertainment", "Salary",
    "Freelance", "Business", "Investment", "Transfer", "Other"
)

// ─── Main Screen ──────────────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PendingTransactionsScreen(
    pendingList: List<PendingSmsTransactionEntity>,
    accounts: List<AccountEntity>,
    onConfirm: (PendingSmsTransactionEntity, PendingSmsTransactionEntity) -> Unit,
    onDismiss: (PendingSmsTransactionEntity) -> Unit,
    onUpdate: (PendingSmsTransactionEntity) -> Unit,
    onDismissAll: () -> Unit,
    onBack: () -> Unit
) {
    var editTarget by remember { mutableStateOf<PendingSmsTransactionEntity?>(null) }
    var showDismissAllDialog by remember { mutableStateOf(false) }

    // Edit bottom sheet
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(BackgroundDark)
    ) {
        Column(modifier = Modifier.fillMaxSize()) {

            // ── Top Bar ──────────────────────────────────────────────────────
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        Brush.verticalGradient(listOf(CardDarker, BackgroundDark))
                    )
                    .padding(top = 48.dp, start = 16.dp, end = 16.dp, bottom = 16.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = TextPrimary
                        )
                    }
                    Spacer(modifier = Modifier.width(4.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Pending Transactions",
                            color = TextPrimary,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "${pendingList.size} detected from SMS",
                            color = TextSecondary,
                            fontSize = 12.sp
                        )
                    }

                    if (pendingList.isNotEmpty()) {
                        TextButton(onClick = { showDismissAllDialog = true }) {
                            Text("Dismiss All", color = ExpenseRed, fontSize = 12.sp)
                        }
                    }
                }
            }

            // ── Content ──────────────────────────────────────────────────────
            if (pendingList.isEmpty()) {
                EmptyState()
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(pendingList, key = { it.id }) { pending ->
                        PendingTransactionCard(
                            pending  = pending,
                            accounts = accounts,
                            onConfirm = { onConfirm(pending, it) },
                            onDismiss = { onDismiss(pending) },
                            onEdit    = { editTarget = pending }
                        )
                    }
                    item { Spacer(modifier = Modifier.height(24.dp)) }
                }
            }
        }

        // ── Edit Bottom Sheet ──────────────────────────────────────────────
        editTarget?.let { target ->
            ModalBottomSheet(
                onDismissRequest = { editTarget = null },
                sheetState       = sheetState,
                containerColor   = CardDarker,
                shape            = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
            ) {
                EditPendingSheet(
                    pending  = target,
                    accounts = accounts,
                    onSave   = { edited ->
                        onUpdate(edited)
                        editTarget = null
                    },
                    onCancel = { editTarget = null }
                )
            }
        }

        // ── Dismiss All Confirmation ───────────────────────────────────────
        if (showDismissAllDialog) {
            AlertDialog(
                onDismissRequest = { showDismissAllDialog = false },
                containerColor   = CardDarker,
                title = { Text("Dismiss All?", color = TextPrimary, fontWeight = FontWeight.Bold) },
                text  = { Text("All ${pendingList.size} pending SMS transactions will be removed without saving.", color = TextSecondary) },
                confirmButton = {
                    TextButton(onClick = {
                        onDismissAll()
                        showDismissAllDialog = false
                    }) {
                        Text("Dismiss All", color = ExpenseRed, fontWeight = FontWeight.Bold)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showDismissAllDialog = false }) {
                        Text("Cancel", color = TextSecondary)
                    }
                }
            )
        }
    }
}

// ─── Pending Card ─────────────────────────────────────────────────────────────

@Composable
private fun PendingTransactionCard(
    pending: PendingSmsTransactionEntity,
    accounts: List<AccountEntity>,
    onConfirm: (PendingSmsTransactionEntity) -> Unit,
    onDismiss: () -> Unit,
    onEdit: () -> Unit
) {
    val matchedAccount = accounts.firstOrNull { it.id == pending.fromAccountId }
    val typeColor = if (pending.type == "INCOME") IncomeGreen else ExpenseRed
    val typeLabel = when (pending.type) {
        "INCOME"   -> "Income"
        "EXPENSE"  -> "Expense"
        "TRANSFER" -> "Transfer"
        else       -> pending.type
    }
    val typeIcon = when (pending.type) {
        "INCOME"   -> Icons.Default.KeyboardArrowDown
        "EXPENSE"  -> Icons.Default.KeyboardArrowUp
        else       -> Icons.Default.SwapHoriz
    }

    Card(
        modifier  = Modifier.fillMaxWidth(),
        shape     = RoundedCornerShape(16.dp),
        colors    = CardDefaults.cardColors(containerColor = CardDark),
        elevation = CardDefaults.cardElevation(0.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {

            // ── Header row ───────────────────────────────────────────────────
            Row(verticalAlignment = Alignment.CenterVertically) {
                // Type icon badge
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(typeColor.copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = typeIcon,
                        contentDescription = null,
                        tint = typeColor,
                        modifier = Modifier.size(22.dp)
                    )
                }

                Spacer(modifier = Modifier.width(12.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = pending.detectedAccountName,
                        color = TextPrimary,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = formatTimestamp(pending.receivedAt),
                        color = TextMuted,
                        fontSize = 11.sp
                    )
                }

                // Amount
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = "৳${formatAmount(pending.amount)}",
                        color = typeColor,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    )
                    // Type badge
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(typeColor.copy(alpha = 0.12f))
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(typeLabel, color = typeColor, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }

            // ── Details row ──────────────────────────────────────────────────
            if (pending.note.isNotBlank() || matchedAccount != null || pending.category.isNotBlank()) {
                Spacer(modifier = Modifier.height(10.dp))
                HorizontalDivider(color = DividerColor)
                Spacer(modifier = Modifier.height(10.dp))

                if (pending.note.isNotBlank()) {
                    DetailRow(label = "Note", value = pending.note)
                }
                if (pending.category.isNotBlank()) {
                    DetailRow(label = "Category", value = pending.category)
                }
                if (matchedAccount != null) {
                    DetailRow(label = "Account", value = matchedAccount.name)
                } else {
                    DetailRow(label = "Account", value = "⚠ Not matched — please select", valueColor = TransferYellow)
                }
            }

            // ── SMS snippet ──────────────────────────────────────────────────
            Spacer(modifier = Modifier.height(10.dp))
            Text(
                text = "\"${pending.rawSmsBody.take(120)}${if (pending.rawSmsBody.length > 120) "…" else ""}\"",
                color = TextMuted,
                fontSize = 10.sp,
                lineHeight = 14.sp,
                fontStyle = androidx.compose.ui.text.font.FontStyle.Italic
            )

            // ── Action buttons ───────────────────────────────────────────────
            Spacer(modifier = Modifier.height(14.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Dismiss
                OutlinedButton(
                    onClick = onDismiss,
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = ExpenseRed),
                    border = BorderStroke(1.dp, ExpenseRed.copy(alpha = 0.4f))
                ) {
                    Icon(Icons.Default.Close, contentDescription = null, modifier = Modifier.size(14.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Dismiss", fontSize = 12.sp)
                }

                // Edit
                OutlinedButton(
                    onClick = onEdit,
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = TextSecondary),
                    border = BorderStroke(1.dp, DividerColor)
                ) {
                    Icon(Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(14.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Edit", fontSize = 12.sp)
                }

                // Confirm
                Button(
                    onClick = { onConfirm(pending) },
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = IncomeGreen),
                    enabled = pending.fromAccountId != -1
                ) {
                    Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(14.dp), tint = BackgroundDark)
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Confirm", fontSize = 12.sp, color = BackgroundDark, fontWeight = FontWeight.Bold)
                }
            }

            if (pending.fromAccountId == -1) {
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "⚠ Tap Edit to select an account before confirming",
                    color = TransferYellow,
                    fontSize = 10.sp,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}

// ─── Edit Sheet ───────────────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun EditPendingSheet(
    pending: PendingSmsTransactionEntity,
    accounts: List<AccountEntity>,
    onSave: (PendingSmsTransactionEntity) -> Unit,
    onCancel: () -> Unit
) {
    var amount       by remember { mutableStateOf(pending.amount.toString()) }
    var type         by remember { mutableStateOf(pending.type) }
    var category     by remember { mutableStateOf(pending.category) }
    var note         by remember { mutableStateOf(pending.note) }
    var fromAccountId by remember { mutableStateOf(pending.fromAccountId) }
    var toAccountId  by remember { mutableStateOf(pending.toAccountId) }

    var showCategoryDropdown by remember { mutableStateOf(false) }
    var showFromAccountDropdown by remember { mutableStateOf(false) }
    var showToAccountDropdown by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 8.dp)
            .verticalScroll(rememberScrollState())
    ) {
        // Handle
        Box(
            modifier = Modifier
                .width(40.dp)
                .height(4.dp)
                .clip(CircleShape)
                .background(DividerColor)
                .align(Alignment.CenterHorizontally)
        )

        Spacer(modifier = Modifier.height(20.dp))

        Text(
            text = "Edit Transaction",
            color = TextPrimary,
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold
        )
        Text(
            text = "from ${pending.senderAddress}",
            color = TextSecondary,
            fontSize = 12.sp
        )

        Spacer(modifier = Modifier.height(20.dp))

        // ── Amount ──────────────────────────────────────────────────────────
        SheetLabel("Amount (৳)")
        OutlinedTextField(
            value = amount,
            onValueChange = { amount = it },
            modifier = Modifier.fillMaxWidth(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
            shape = RoundedCornerShape(12.dp),
            colors = outlinedTextFieldColors(),
            singleLine = true,
            leadingIcon = { Text("৳", color = AccentTeal, fontWeight = FontWeight.Bold, modifier = Modifier.padding(start = 8.dp)) }
        )

        Spacer(modifier = Modifier.height(14.dp))

        // ── Type ─────────────────────────────────────────────────────────────
        SheetLabel("Type")
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            listOf("INCOME", "EXPENSE", "TRANSFER").forEach { t ->
                val selected = type == t
                val color = when (t) {
                    "INCOME"   -> IncomeGreen
                    "EXPENSE"  -> ExpenseRed
                    else       -> TransferYellow
                }
                FilterChip(
                    selected = selected,
                    onClick  = { type = t },
                    label    = { Text(t, fontSize = 11.sp, fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal) },
                    colors   = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = color.copy(alpha = 0.2f),
                        selectedLabelColor     = color,
                        labelColor             = TextSecondary,
                        containerColor         = CardDark
                    ),
                    border = FilterChipDefaults.filterChipBorder(
                        enabled = true, selected = selected,
                        selectedBorderColor = color, borderColor = DividerColor
                    )
                )
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // ── Category ─────────────────────────────────────────────────────────
        SheetLabel("Category")
        ExposedDropdownMenuBox(
            expanded = showCategoryDropdown,
            onExpandedChange = { showCategoryDropdown = it }
        ) {
            OutlinedTextField(
                value = category,
                onValueChange = {},
                readOnly = true,
                modifier = Modifier.fillMaxWidth().menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable),
                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = showCategoryDropdown) },
                shape = RoundedCornerShape(12.dp),
                colors = outlinedTextFieldColors()
            )
            ExposedDropdownMenu(
                expanded = showCategoryDropdown,
                onDismissRequest = { showCategoryDropdown = false },
                modifier = Modifier.background(CardDarker)
            ) {
                CATEGORIES.forEach { cat ->
                    DropdownMenuItem(
                        text = { Text(cat, color = TextPrimary, fontSize = 13.sp) },
                        onClick = { category = cat; showCategoryDropdown = false }
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // ── Note ─────────────────────────────────────────────────────────────
        SheetLabel("Note / Merchant")
        OutlinedTextField(
            value = note,
            onValueChange = { note = it },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            colors = outlinedTextFieldColors(),
            maxLines = 2,
            placeholder = { Text("e.g. Shajahan Restaurant", color = TextMuted) }
        )

        Spacer(modifier = Modifier.height(14.dp))

        // ── From Account ──────────────────────────────────────────────────────
        SheetLabel("Account")
        ExposedDropdownMenuBox(
            expanded = showFromAccountDropdown,
            onExpandedChange = { showFromAccountDropdown = it }
        ) {
            OutlinedTextField(
                value = accounts.firstOrNull { it.id == fromAccountId }?.name ?: "Select account",
                onValueChange = {},
                readOnly = true,
                modifier = Modifier.fillMaxWidth().menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable),
                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = showFromAccountDropdown) },
                shape = RoundedCornerShape(12.dp),
                colors = outlinedTextFieldColors()
            )
            ExposedDropdownMenu(
                expanded = showFromAccountDropdown,
                onDismissRequest = { showFromAccountDropdown = false },
                modifier = Modifier.background(CardDarker)
            ) {
                accounts.forEach { acc ->
                    DropdownMenuItem(
                        text = { Text(acc.name, color = TextPrimary, fontSize = 13.sp) },
                        onClick = { fromAccountId = acc.id; showFromAccountDropdown = false }
                    )
                }
            }
        }

        // ── To Account (Transfer only) ─────────────────────────────────────
        if (type == "TRANSFER") {
            Spacer(modifier = Modifier.height(14.dp))
            SheetLabel("Transfer To Account")
            ExposedDropdownMenuBox(
                expanded = showToAccountDropdown,
                onExpandedChange = { showToAccountDropdown = it }
            ) {
                OutlinedTextField(
                    value = accounts.firstOrNull { it.id == toAccountId }?.name ?: "Select destination",
                    onValueChange = {},
                    readOnly = true,
                    modifier = Modifier.fillMaxWidth().menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable),
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = showToAccountDropdown) },
                    shape = RoundedCornerShape(12.dp),
                    colors = outlinedTextFieldColors()
                )
                ExposedDropdownMenu(
                    expanded = showToAccountDropdown,
                    onDismissRequest = { showToAccountDropdown = false },
                    modifier = Modifier.background(CardDarker)
                ) {
                    accounts.filter { it.id != fromAccountId }.forEach { acc ->
                        DropdownMenuItem(
                            text = { Text(acc.name, color = TextPrimary, fontSize = 13.sp) },
                            onClick = { toAccountId = acc.id; showToAccountDropdown = false }
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // ── Save button ───────────────────────────────────────────────────────
        Button(
            onClick = {
                val parsedAmount = amount.toDoubleOrNull() ?: pending.amount
                onSave(
                    pending.copy(
                        amount        = parsedAmount,
                        type          = type,
                        category      = category,
                        note          = note,
                        fromAccountId = fromAccountId,
                        toAccountId   = if (type == "TRANSFER") toAccountId else null
                    )
                )
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(50.dp),
            shape = RoundedCornerShape(14.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = AccentTeal
            )
        ) {
            Icon(Icons.Default.Check, contentDescription = null, tint = BackgroundDark)
            Spacer(modifier = Modifier.width(8.dp))
            Text("Save Changes", color = BackgroundDark, fontWeight = FontWeight.Bold)
        }

        Spacer(modifier = Modifier.height(8.dp))

        OutlinedButton(
            onClick = onCancel,
            modifier = Modifier
                .fillMaxWidth()
                .height(46.dp),
            shape = RoundedCornerShape(14.dp),
            colors = ButtonDefaults.outlinedButtonColors(contentColor = TextSecondary),
            border = BorderStroke(1.dp, DividerColor)
        ) {
            Text("Cancel")
        }

        Spacer(modifier = Modifier.height(32.dp))
    }
}

// ─── Empty State ─────────────────────────────────────────────────────────────

@Composable
private fun EmptyState() {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Box(
                modifier = Modifier
                    .size(80.dp)
                    .clip(CircleShape)
                    .background(CardDark),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.CheckCircle,
                    contentDescription = null,
                    tint = IncomeGreen.copy(alpha = 0.7f),
                    modifier = Modifier.size(40.dp)
                )
            }
            Spacer(modifier = Modifier.height(16.dp))
            Text("All caught up!", color = TextPrimary, fontSize = 18.sp, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = "No pending SMS transactions.\nNew bank/MFS messages will appear here automatically.",
                color = TextSecondary,
                fontSize = 13.sp,
                textAlign = TextAlign.Center,
                lineHeight = 20.sp
            )
        }
    }
}

// ─── Helper composables ───────────────────────────────────────────────────────

@Composable
private fun SheetLabel(text: String) {
    Text(text, color = TextSecondary, fontSize = 12.sp, fontWeight = FontWeight.Medium)
    Spacer(modifier = Modifier.height(6.dp))
}

@Composable
private fun DetailRow(label: String, value: String, valueColor: Color = TextSecondary) {
    Row(modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp)) {
        Text(label, color = TextMuted, fontSize = 12.sp, modifier = Modifier.width(80.dp))
        Text(value, color = valueColor, fontSize = 12.sp, modifier = Modifier.weight(1f), maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}

@Composable
private fun outlinedTextFieldColors() = OutlinedTextFieldDefaults.colors(
    focusedBorderColor   = AccentTeal,
    unfocusedBorderColor = DividerColor,
    focusedTextColor     = TextPrimary,
    unfocusedTextColor   = TextPrimary,
    cursorColor          = AccentTeal,
    focusedContainerColor   = CardDark,
    unfocusedContainerColor = CardDark
)

// ─── Format helpers ───────────────────────────────────────────────────────────

private fun formatAmount(amount: Double): String {
    val nf = NumberFormat.getNumberInstance()
    nf.maximumFractionDigits = 2
    nf.minimumFractionDigits = 0
    return nf.format(amount)
}

private fun formatTimestamp(ms: Long): String {
    val sdf = SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.getDefault())
    return sdf.format(Date(ms))
}
