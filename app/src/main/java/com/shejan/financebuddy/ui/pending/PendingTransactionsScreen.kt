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
import androidx.compose.material.icons.automirrored.filled.Notes
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.filled.Category
import androidx.compose.material.icons.filled.CreditCard
import androidx.compose.material.icons.filled.Sms
import androidx.compose.material.icons.filled.History
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import kotlinx.coroutines.launch
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
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
    database: com.shejan.financebuddy.data.db.FinanceDatabase,
    onConfirm: (PendingSmsTransactionEntity, PendingSmsTransactionEntity) -> Unit,
    onDismiss: (PendingSmsTransactionEntity) -> Unit,
    onUpdate: (PendingSmsTransactionEntity) -> Unit,
    onDismissAll: () -> Unit,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var isScanning by remember { mutableStateOf(false) }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            isScanning = true
            scope.launch {
                val count = com.shejan.financebuddy.sms.SmsSyncHelper.syncPreviousSms(context, database)
                isScanning = false
                android.widget.Toast.makeText(
                    context,
                    if (count > 0) "Imported $count transaction messages!" else "No new transaction messages found.",
                    android.widget.Toast.LENGTH_LONG
                ).show()
            }
        } else {
            android.widget.Toast.makeText(
                context,
                "Permission denied. Cannot scan SMS history.",
                android.widget.Toast.LENGTH_SHORT
            ).show()
        }
    }

    fun triggerHistoryScan() {
        val hasReadPermission = androidx.core.content.ContextCompat.checkSelfPermission(
            context,
            android.Manifest.permission.READ_SMS
        ) == android.content.pm.PackageManager.PERMISSION_GRANTED
        if (hasReadPermission) {
            isScanning = true
            scope.launch {
                val count = com.shejan.financebuddy.sms.SmsSyncHelper.syncPreviousSms(context, database)
                isScanning = false
                android.widget.Toast.makeText(
                    context,
                    if (count > 0) "Imported $count transaction messages!" else "No new transaction messages found.",
                    android.widget.Toast.LENGTH_LONG
                ).show()
            }
        } else {
            permissionLauncher.launch(android.Manifest.permission.READ_SMS)
        }
    }

    var editTarget by remember { mutableStateOf<PendingSmsTransactionEntity?>(null) }
    var showDismissAllDialog by remember { mutableStateOf(false) }

    // Edit bottom sheet state
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(BackgroundDark)
    ) {
        Column(modifier = Modifier.fillMaxSize()) {

            // ── Premium Top Bar ──────────────────────────────────────────────
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(CardDarker, BackgroundDark.copy(alpha = 0.95f))
                        )
                    )
                    .border(width = 1.dp, color = DividerColor.copy(alpha = 0.5f), shape = RoundedCornerShape(0.dp))
                    .padding(top = 44.dp, start = 8.dp, end = 16.dp, bottom = 16.dp)
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
                            text = "Transaction Inbox",
                            color = TextPrimary,
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = (-0.5).sp
                        )
                        Text(
                            text = if (pendingList.isEmpty()) "No unreviewed transactions" else "${pendingList.size} unreviewed transactions",
                            color = TextSecondary,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }

                    IconButton(
                        onClick = { triggerHistoryScan() },
                        enabled = !isScanning,
                        modifier = Modifier.padding(end = 4.dp)
                    ) {
                        if (isScanning) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(20.dp),
                                strokeWidth = 2.dp,
                                color = AccentTeal
                            )
                        } else {
                            Icon(
                                imageVector = Icons.Default.History,
                                contentDescription = "Scan SMS History",
                                tint = AccentTeal
                            )
                        }
                    }

                    if (pendingList.isNotEmpty()) {
                        Button(
                            onClick = { showDismissAllDialog = true },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = ExpenseRed.copy(alpha = 0.12f),
                                contentColor = ExpenseRed
                            ),
                            shape = RoundedCornerShape(10.dp),
                            contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp),
                            modifier = Modifier.height(34.dp)
                        ) {
                            Text("Dismiss All", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }

            // ── Scrollable list with entrance animations ──────────────────────
            if (pendingList.isEmpty()) {
                EmptyState(
                    isScanning = isScanning,
                    onScanHistory = { triggerHistoryScan() }
                )
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    items(pendingList, key = { it.id }) { pending ->
                        AnimatedVisibility(
                            visible = true,
                            enter = fadeIn() + expandVertically(),
                            exit = fadeOut() + shrinkVertically()
                        ) {
                            PendingTransactionCard(
                                pending   = pending,
                                accounts  = accounts,
                                onConfirm = { onConfirm(pending, it) },
                                onDismiss = { onDismiss(pending) },
                                onEdit    = { editTarget = pending }
                            )
                        }
                    }
                    item { Spacer(modifier = Modifier.height(32.dp)) }
                }
            }
        }

        // ── Edit Bottom Sheet ──────────────────────────────────────────────
        editTarget?.let { target ->
            ModalBottomSheet(
                onDismissRequest = { editTarget = null },
                sheetState       = sheetState,
                containerColor   = CardDarker,
                shape            = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
                tonalElevation   = 8.dp,
                dragHandle = {
                    Box(
                        modifier = Modifier
                            .padding(vertical = 12.dp)
                            .width(36.dp)
                            .height(4.dp)
                            .clip(CircleShape)
                            .background(DividerColor)
                    )
                }
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

        // ── Dismiss All Dialog ─────────────────────────────────────────────
        if (showDismissAllDialog) {
            AlertDialog(
                onDismissRequest = { showDismissAllDialog = false },
                containerColor   = CardDarker,
                title = {
                    Text(
                        "Dismiss All transactions?",
                        color = TextPrimary,
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp
                    )
                },
                text = {
                    Text(
                        "All ${pendingList.size} unconfirmed SMS detections will be permanently cleared from the queue.",
                        color = TextSecondary,
                        fontSize = 14.sp
                    )
                },
                confirmButton = {
                    Button(
                        onClick = {
                            onDismissAll()
                            showDismissAllDialog = false
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = ExpenseRed),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Text("Dismiss All", color = Color.White, fontWeight = FontWeight.Bold)
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

// ─── Pending Card Composable ──────────────────────────────────────────────────

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

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(CardDark)
            .border(
                border = BorderStroke(1.dp, DividerColor.copy(alpha = 0.7f)),
                shape = RoundedCornerShape(20.dp)
            )
    ) {
        // Left side type indicator bar
        Box(
            modifier = Modifier
                .align(Alignment.CenterStart)
                .fillMaxHeight()
                .width(4.dp)
                .background(typeColor)
        )

        Column(
            modifier = Modifier
                .padding(start = 16.dp, top = 16.dp, end = 16.dp, bottom = 16.dp)
        ) {
            // Header Row (Origin & Amount)
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                // Circle Badge for Institution Icon
                Box(
                    modifier = Modifier
                        .size(42.dp)
                        .clip(CircleShape)
                        .background(typeColor.copy(alpha = 0.12f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = typeIcon,
                        contentDescription = null,
                        tint = typeColor,
                        modifier = Modifier.size(24.dp)
                    )
                }

                Spacer(modifier = Modifier.width(12.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = pending.detectedAccountName,
                        color = TextPrimary,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = formatTimestamp(pending.receivedAt),
                        color = TextMuted,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium
                    )
                }

                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = "৳${formatAmount(pending.amount)}",
                        color = typeColor,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.ExtraBold,
                        letterSpacing = (-0.5).sp
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(typeColor.copy(alpha = 0.12f))
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = typeLabel,
                            color = typeColor,
                            fontSize = 9.sp,
                            fontWeight = FontWeight.ExtraBold
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Unmatched Warning Banner
            if (pending.fromAccountId == -1) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(TransferYellow.copy(alpha = 0.08f), RoundedCornerShape(12.dp))
                        .border(1.dp, TransferYellow.copy(alpha = 0.25f), RoundedCornerShape(12.dp))
                        .padding(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Warning,
                        contentDescription = null,
                        tint = TransferYellow,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Unknown bank/MFS source. Please link an account.",
                        color = TransferYellow,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
                Spacer(modifier = Modifier.height(12.dp))
            }

            // Metadata card section
            if (pending.note.isNotBlank() || matchedAccount != null || pending.category.isNotBlank()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(CardDarker.copy(alpha = 0.6f), RoundedCornerShape(12.dp))
                        .border(1.dp, DividerColor.copy(alpha = 0.5f), RoundedCornerShape(12.dp))
                        .padding(12.dp)
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        if (pending.note.isNotBlank()) {
                            DetailRowItem(
                                icon = Icons.AutoMirrored.Filled.Notes,
                                label = "Note",
                                value = pending.note
                            )
                        }
                        if (pending.category.isNotBlank()) {
                            DetailRowItem(
                                icon = Icons.Default.Category,
                                label = "Category",
                                value = pending.category
                            )
                        }
                        DetailRowItem(
                            icon = Icons.Default.CreditCard,
                            label = "Account",
                            value = matchedAccount?.name ?: "Tap Edit to choose account",
                            valueColor = if (matchedAccount != null) TextSecondary else TransferYellow
                        )
                    }
                }
                Spacer(modifier = Modifier.height(12.dp))
            }

            // Raw SMS Message bubble
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(CardDarker, RoundedCornerShape(12.dp))
                    .padding(12.dp)
            ) {
                Column {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(
                            imageVector = Icons.Default.Sms,
                            contentDescription = null,
                            tint = AccentTeal.copy(alpha = 0.7f),
                            modifier = Modifier.size(12.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "SMS SENDER: ${pending.senderAddress}",
                            color = TextMuted,
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 0.5.sp
                        )
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = pending.rawSmsBody,
                        color = TextSecondary,
                        fontSize = 11.sp,
                        lineHeight = 16.sp,
                        fontWeight = FontWeight.Normal
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Action Buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Dismiss Button
                OutlinedButton(
                    onClick = onDismiss,
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = ExpenseRed,
                        containerColor = Color.Transparent
                    ),
                    border = BorderStroke(1.dp, ExpenseRed.copy(alpha = 0.3f))
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = null,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Dismiss", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }

                // Edit Button
                OutlinedButton(
                    onClick = onEdit,
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = TextSecondary,
                        containerColor = Color.Transparent
                    ),
                    border = BorderStroke(1.dp, DividerColor)
                ) {
                    Icon(
                        imageVector = Icons.Default.Edit,
                        contentDescription = null,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Edit", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }

                // Confirm Button
                val canConfirm = pending.fromAccountId != -1
                Button(
                    onClick = { onConfirm(pending) },
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = IncomeGreen,
                        disabledContainerColor = IncomeGreen.copy(alpha = 0.15f),
                        disabledContentColor = TextMuted
                    ),
                    enabled = canConfirm
                ) {
                    Icon(
                        imageVector = Icons.Default.Check,
                        contentDescription = null,
                        modifier = Modifier.size(14.dp),
                        tint = if (canConfirm) BackgroundDark else TextMuted
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "Confirm",
                        fontSize = 12.sp,
                        color = if (canConfirm) BackgroundDark else TextMuted,
                        fontWeight = FontWeight.ExtraBold
                    )
                }
            }
        }
    }
}

// ─── Edit Bottom Sheet Composable ─────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun EditPendingSheet(
    pending: PendingSmsTransactionEntity,
    accounts: List<AccountEntity>,
    onSave: (PendingSmsTransactionEntity) -> Unit,
    onCancel: () -> Unit
) {
    var amount        by remember { mutableStateOf(pending.amount.toString()) }
    var type          by remember { mutableStateOf(pending.type) }
    var category      by remember { mutableStateOf(pending.category) }
    var note          by remember { mutableStateOf(pending.note) }
    var fromAccountId by remember { mutableStateOf(pending.fromAccountId) }
    var toAccountId   by remember { mutableStateOf(pending.toAccountId) }

    var fromAccountSearchText by remember(fromAccountId) {
        mutableStateOf(accounts.find { it.id == fromAccountId }?.name ?: "")
    }
    var toAccountSearchText by remember(toAccountId) {
        mutableStateOf(accounts.find { it.id == toAccountId }?.name ?: "")
    }

    var showCategoryDropdown    by remember { mutableStateOf(false) }
    var showFromAccountDropdown by remember { mutableStateOf(false) }
    var showToAccountDropdown   by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 8.dp)
            .verticalScroll(rememberScrollState())
    ) {
        Text(
            text = "Fine-tune Transaction",
            color = TextPrimary,
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = (-0.5).sp
        )
        Text(
            text = "From message: \"${pending.senderAddress}\"",
            color = TextSecondary,
            fontSize = 12.sp,
            fontWeight = FontWeight.Medium
        )

        Spacer(modifier = Modifier.height(20.dp))

        // ── Amount ──────────────────────────────────────────────────────────
        SheetLabel("Transaction Amount (৳)")
        OutlinedTextField(
            value = amount,
            onValueChange = { amount = it },
            modifier = Modifier.fillMaxWidth(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
            shape = RoundedCornerShape(14.dp),
            colors = outlinedTextFieldColors(),
            singleLine = true,
            leadingIcon = { Text("৳", color = AccentTeal, fontWeight = FontWeight.Bold, fontSize = 16.sp, modifier = Modifier.padding(start = 12.dp)) }
        )

        Spacer(modifier = Modifier.height(14.dp))

        // ── Type Chips ───────────────────────────────────────────────────────
        SheetLabel("Transaction Type")
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
                    label    = { Text(t, fontSize = 11.sp, fontWeight = FontWeight.ExtraBold) },
                    colors   = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = color.copy(alpha = 0.15f),
                        selectedLabelColor     = color,
                        labelColor             = TextSecondary,
                        containerColor         = CardDark
                    ),
                    border = FilterChipDefaults.filterChipBorder(
                        enabled = true, selected = selected,
                        selectedBorderColor = color, borderColor = DividerColor
                    ),
                    shape = RoundedCornerShape(10.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // ── Category Dropdown ────────────────────────────────────────────────
        SheetLabel("Category")
        ExposedDropdownMenuBox(
            expanded = showCategoryDropdown,
            onExpandedChange = { showCategoryDropdown = it }
        ) {
            OutlinedTextField(
                value = category,
                onValueChange = {},
                readOnly = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable),
                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = showCategoryDropdown) },
                shape = RoundedCornerShape(14.dp),
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

        // ── Note / Merchant ──────────────────────────────────────────────────
        SheetLabel("Merchant / Description")
        OutlinedTextField(
            value = note,
            onValueChange = { note = it },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(14.dp),
            colors = outlinedTextFieldColors(),
            maxLines = 2,
            placeholder = { Text("e.g. bKash cash out fee, Restora...", color = TextMuted) }
        )

        Spacer(modifier = Modifier.height(14.dp))

        // ── Source Account Dropdown ──────────────────────────────────────────
        SheetLabel(if (type == "TRANSFER") "From Account" else "Account")
        ExposedDropdownMenuBox(
            expanded = showFromAccountDropdown,
            onExpandedChange = {
                showFromAccountDropdown = it
                if (it) {
                    fromAccountSearchText = ""
                }
            }
        ) {
            OutlinedTextField(
                value = fromAccountSearchText,
                onValueChange = {
                    fromAccountSearchText = it
                    showFromAccountDropdown = true
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .menuAnchor(ExposedDropdownMenuAnchorType.PrimaryEditable),
                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = showFromAccountDropdown) },
                shape = RoundedCornerShape(14.dp),
                colors = outlinedTextFieldColors()
            )
            ExposedDropdownMenu(
                expanded = showFromAccountDropdown,
                onDismissRequest = {
                    showFromAccountDropdown = false
                    fromAccountSearchText = accounts.find { it.id == fromAccountId }?.name ?: ""
                },
                modifier = Modifier.background(CardDarker)
            ) {
                val filteredBanks = accounts.filter { it.type == "BANK" && it.name.contains(fromAccountSearchText, ignoreCase = true) }
                val filteredMfs = accounts.filter { it.type == "MFS" && it.name.contains(fromAccountSearchText, ignoreCase = true) }

                if (filteredBanks.isNotEmpty()) {
                    DropdownMenuItem(
                        text = { Text("Banks", color = AccentTeal, fontWeight = FontWeight.Bold, fontSize = 11.sp) },
                        onClick = {},
                        enabled = false
                    )
                    filteredBanks.forEach { acc ->
                        DropdownMenuItem(
                            text = { Text(acc.name, color = TextPrimary, fontSize = 13.sp) },
                            onClick = {
                                fromAccountId = acc.id
                                fromAccountSearchText = acc.name
                                showFromAccountDropdown = false
                            }
                        )
                    }
                }

                if (filteredMfs.isNotEmpty()) {
                    if (filteredBanks.isNotEmpty()) {
                        androidx.compose.material3.HorizontalDivider(color = DividerColor.copy(alpha = 0.5f))
                    }
                    DropdownMenuItem(
                        text = { Text("Mobile Financial Services (MFS)", color = AccentTeal, fontWeight = FontWeight.Bold, fontSize = 11.sp) },
                        onClick = {},
                        enabled = false
                    )
                    filteredMfs.forEach { acc ->
                        DropdownMenuItem(
                            text = { Text(acc.name, color = TextPrimary, fontSize = 13.sp) },
                            onClick = {
                                fromAccountId = acc.id
                                fromAccountSearchText = acc.name
                                showFromAccountDropdown = false
                            }
                        )
                    }
                }
            }
        }

        // ── Destination Account Dropdown (TRANSFER only) ──────────────────────
        if (type == "TRANSFER") {
            Spacer(modifier = Modifier.height(14.dp))
            SheetLabel("To Account")
            ExposedDropdownMenuBox(
                expanded = showToAccountDropdown,
                onExpandedChange = {
                    showToAccountDropdown = it
                    if (it) {
                        toAccountSearchText = ""
                    }
                }
            ) {
                OutlinedTextField(
                    value = toAccountSearchText,
                    onValueChange = {
                        toAccountSearchText = it
                        showToAccountDropdown = true
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .menuAnchor(ExposedDropdownMenuAnchorType.PrimaryEditable),
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = showToAccountDropdown) },
                    shape = RoundedCornerShape(14.dp),
                    colors = outlinedTextFieldColors()
                )
                ExposedDropdownMenu(
                    expanded = showToAccountDropdown,
                    onDismissRequest = {
                        showToAccountDropdown = false
                        toAccountSearchText = accounts.find { it.id == toAccountId }?.name ?: ""
                    },
                    modifier = Modifier.background(CardDarker)
                ) {
                    val destAccounts = accounts.filter { it.id != fromAccountId }
                    val filteredBanks = destAccounts.filter { it.type == "BANK" && it.name.contains(toAccountSearchText, ignoreCase = true) }
                    val filteredMfs = destAccounts.filter { it.type == "MFS" && it.name.contains(toAccountSearchText, ignoreCase = true) }

                    if (filteredBanks.isNotEmpty()) {
                        DropdownMenuItem(
                            text = { Text("Banks", color = AccentTeal, fontWeight = FontWeight.Bold, fontSize = 11.sp) },
                            onClick = {},
                            enabled = false
                        )
                        filteredBanks.forEach { acc ->
                            DropdownMenuItem(
                                text = { Text(acc.name, color = TextPrimary, fontSize = 13.sp) },
                                onClick = {
                                    toAccountId = acc.id
                                    toAccountSearchText = acc.name
                                    showToAccountDropdown = false
                                }
                            )
                        }
                    }

                    if (filteredMfs.isNotEmpty()) {
                        if (filteredBanks.isNotEmpty()) {
                            androidx.compose.material3.HorizontalDivider(color = DividerColor.copy(alpha = 0.5f))
                        }
                        DropdownMenuItem(
                            text = { Text("Mobile Financial Services (MFS)", color = AccentTeal, fontWeight = FontWeight.Bold, fontSize = 11.sp) },
                            onClick = {},
                            enabled = false
                        )
                        filteredMfs.forEach { acc ->
                            DropdownMenuItem(
                                text = { Text(acc.name, color = TextPrimary, fontSize = 13.sp) },
                                onClick = {
                                    toAccountId = acc.id
                                    toAccountSearchText = acc.name
                                    showToAccountDropdown = false
                                }
                            )
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(28.dp))

        // ── Save and Cancel Buttons ──────────────────────────────────────────
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
                .height(52.dp),
            shape = RoundedCornerShape(14.dp),
            colors = ButtonDefaults.buttonColors(containerColor = AccentTeal)
        ) {
            Icon(Icons.Default.Check, contentDescription = null, tint = BackgroundDark)
            Spacer(modifier = Modifier.width(8.dp))
            Text("Update & Save Details", color = BackgroundDark, fontWeight = FontWeight.Bold)
        }

        Spacer(modifier = Modifier.height(10.dp))

        OutlinedButton(
            onClick = onCancel,
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp),
            shape = RoundedCornerShape(14.dp),
            colors = ButtonDefaults.outlinedButtonColors(contentColor = TextSecondary),
            border = BorderStroke(1.dp, DividerColor)
        ) {
            Text("Go Back")
        }

        Spacer(modifier = Modifier.height(32.dp))
    }
}

// ─── Empty State Composable ──────────────────────────────────────────────────

@Composable
private fun EmptyState(
    isScanning: Boolean,
    onScanHistory: () -> Unit
) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(horizontal = 32.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(90.dp)
                    .clip(CircleShape)
                    .background(CardDark)
                    .border(1.dp, DividerColor, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.CheckCircle,
                    contentDescription = null,
                    tint = IncomeGreen,
                    modifier = Modifier.size(46.dp)
                )
            }
            Spacer(modifier = Modifier.height(20.dp))
            Text(
                text = "Perfect Sync!",
                color = TextPrimary,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "All detected bank and MFS SMS messages have been processed. New notifications will appear here instantly.",
                color = TextSecondary,
                fontSize = 13.sp,
                textAlign = TextAlign.Center,
                lineHeight = 20.sp,
                fontWeight = FontWeight.Medium
            )
            Spacer(modifier = Modifier.height(24.dp))
            OutlinedButton(
                onClick = onScanHistory,
                enabled = !isScanning,
                colors = ButtonDefaults.outlinedButtonColors(contentColor = AccentTeal),
                border = BorderStroke(1.dp, AccentTeal),
                shape = RoundedCornerShape(12.dp)
            ) {
                if (isScanning) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(16.dp),
                        strokeWidth = 2.dp,
                        color = AccentTeal
                    )
                } else {
                    Icon(
                        imageVector = Icons.Default.History,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                        tint = AccentTeal
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Scan Past 30 Days SMS", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

// ─── Helper Custom Row Composable ─────────────────────────────────────────────

@Composable
private fun DetailRowItem(
    icon: ImageVector,
    label: String,
    value: String,
    valueColor: Color = TextSecondary
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = TextMuted,
            modifier = Modifier.size(16.dp)
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = label,
            color = TextMuted,
            fontSize = 12.sp,
            fontWeight = FontWeight.Medium,
            modifier = Modifier.width(72.dp)
        )
        Text(
            text = value,
            color = valueColor,
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f)
        )
    }
}

// ─── Helper Styles ────────────────────────────────────────────────────────────

@Composable
private fun SheetLabel(text: String) {
    Text(
        text = text,
        color = TextSecondary,
        fontSize = 12.sp,
        fontWeight = FontWeight.Bold
    )
    Spacer(modifier = Modifier.height(6.dp))
}

@Composable
private fun outlinedTextFieldColors() = OutlinedTextFieldDefaults.colors(
    focusedBorderColor   = AccentTeal,
    unfocusedBorderColor = DividerColor,
    focusedTextColor     = TextPrimary,
    unfocusedTextColor   = TextPrimary,
    cursorColor          = AccentTeal,
    focusedContainerColor   = CardDarker,
    unfocusedContainerColor = CardDarker
)

// ─── Format Helpers ───────────────────────────────────────────────────────────

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

