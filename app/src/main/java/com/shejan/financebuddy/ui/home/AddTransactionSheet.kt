package com.shejan.financebuddy.ui.home

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import android.content.Context
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.border
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material3.Surface
import androidx.compose.material3.TextButton
import androidx.compose.ui.platform.LocalContext
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.ExposedDropdownMenuAnchorType
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.SheetState
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.shejan.financebuddy.data.db.AccountEntity
import com.shejan.financebuddy.data.db.TransactionEntity
import com.shejan.financebuddy.data.db.PayeeEntity
import com.shejan.financebuddy.data.db.PayeeAccountEntity
import com.shejan.financebuddy.ui.theme.AccentBlue
import com.shejan.financebuddy.ui.theme.AccentTeal
import com.shejan.financebuddy.ui.theme.BackgroundDark
import com.shejan.financebuddy.ui.theme.CardDark
import com.shejan.financebuddy.ui.theme.CardDarker
import com.shejan.financebuddy.ui.theme.DividerColor
import com.shejan.financebuddy.ui.theme.ExpenseRed
import com.shejan.financebuddy.ui.theme.GradientEnd
import com.shejan.financebuddy.ui.theme.GradientStart
import com.shejan.financebuddy.ui.theme.IncomeGreen
import com.shejan.financebuddy.ui.theme.TextMuted
import com.shejan.financebuddy.ui.theme.TextPrimary
import com.shejan.financebuddy.ui.theme.TextSecondary
import com.shejan.financebuddy.ui.theme.TransferYellow

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class, ExperimentalFoundationApi::class)
@Composable
fun AddTransactionSheet(
    accounts: List<AccountEntity>,
    sheetState: SheetState,
    onDismiss: () -> Unit,
    onSaveTransaction: (TransactionEntity, AccountEntity?, AccountEntity?) -> Unit,
    payees: List<PayeeEntity> = emptyList(),
    payeeAccounts: List<PayeeAccountEntity> = emptyList(),
    onSavePayee: (String, String, String, String) -> Unit = { _, _, _, _ -> }
) {

    var amount by remember { mutableStateOf("") }
    var selectedType by remember { mutableStateOf("EXPENSE") } // "INCOME", "EXPENSE", "TRANSFER"
    var selectedCategory by remember { mutableStateOf("") }
    var selectedFromAccount by remember { mutableStateOf<AccountEntity?>(null) }
    var selectedToAccount by remember { mutableStateOf<AccountEntity?>(null) }
    var note by remember { mutableStateOf("") }

    var fromAccountExpanded by remember { mutableStateOf(false) }
    var toAccountExpanded by remember { mutableStateOf(false) }

    var fromAccountSearchText by remember(selectedFromAccount) {
        mutableStateOf(
            TextFieldValue(
                text = selectedFromAccount?.name ?: "",
                selection = TextRange((selectedFromAccount?.name ?: "").length)
            )
        )
    }
    var toAccountSearchText by remember(selectedToAccount) {
        mutableStateOf(
            TextFieldValue(
                text = selectedToAccount?.name ?: "",
                selection = TextRange((selectedToAccount?.name ?: "").length)
            )
        )
    }

    var isOwnAccount by remember { mutableStateOf(true) }
    var recipientName by remember { mutableStateOf("") }
    var recipientAccountNumber by remember { mutableStateOf("") }
    var saveToPayees by remember { mutableStateOf(false) }

    var selectedPayee by remember { mutableStateOf<PayeeEntity?>(null) }
    var selectedPayeeAccount by remember { mutableStateOf<PayeeAccountEntity?>(null) }

    var payeeExpanded by remember { mutableStateOf(false) }
    var payeeAccountExpanded by remember { mutableStateOf(false) }

    val isFromAccountNew = remember(selectedFromAccount, fromAccountSearchText, accounts) {
        selectedFromAccount == null && fromAccountSearchText.text.trim().isNotEmpty() &&
                accounts.none { it.name.equals(fromAccountSearchText.text.trim(), ignoreCase = true) }
    }
    val isToAccountNew = remember(selectedToAccount, toAccountSearchText, accounts) {
        selectedToAccount == null && toAccountSearchText.text.trim().isNotEmpty() &&
                accounts.none { it.name.equals(toAccountSearchText.text.trim(), ignoreCase = true) }
    }

    val selectedBalance = selectedFromAccount?.balance ?: 0.0
    val parsedAmount = amount.toDoubleOrNull() ?: 0.0
    val isInsufficient = (selectedType == "EXPENSE" || selectedType == "TRANSFER") &&
            ((selectedFromAccount != null && parsedAmount > selectedBalance) || (isFromAccountNew && parsedAmount > 0.0))

    val context = LocalContext.current
    val sharedPreferences = remember { context.getSharedPreferences("finance_buddy_prefs", Context.MODE_PRIVATE) }

    var customIncomeCategories by remember {
        mutableStateOf(
            sharedPreferences.getString("custom_income_categories", "")
                ?.split("|")
                ?.filter { it.isNotEmpty() }
                ?: emptyList()
        )
    }
    var customExpenseCategories by remember {
        mutableStateOf(
            sharedPreferences.getString("custom_expense_categories", "")
                ?.split("|")
                ?.filter { it.isNotEmpty() }
                ?: emptyList()
        )
    }

    val defaultIncomeCategories = listOf("Salary", "Freelance", "Investment", "Pocket Money", "Other")
    val defaultExpenseCategories = listOf("Food", "Groceries", "Rent", "Utilities", "Travel", "Shopping", "Entertainment", "Medical", "Other")

    val activeCategories = remember(selectedType, customIncomeCategories, customExpenseCategories) {
        if (selectedType == "INCOME") {
            defaultIncomeCategories + customIncomeCategories
        } else if (selectedType == "EXPENSE") {
            defaultExpenseCategories + customExpenseCategories
        } else {
            listOf("Transfer")
        }
    }

    var showAddCategoryDialog by remember { mutableStateOf(false) }
    var showDeleteDialog by remember { mutableStateOf(false) }
    var categoryToDelete by remember { mutableStateOf("") }
    var newCategoryName by remember { mutableStateOf("") }

    // Reset default category if type changes
    LaunchedEffectForType(selectedType) {
        selectedCategory = if (selectedType == "TRANSFER") "Transfer" else activeCategories.first()
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState       = sheetState,
        containerColor   = CardDarker,
        shape            = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp)
                .padding(bottom = 40.dp)
        ) {
            // Title
            Text(
                text      = "Add Transaction",
                style     = androidx.compose.material3.MaterialTheme.typography.titleLarge,
                color     = TextPrimary,
                modifier  = Modifier.fillMaxWidth(),
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(20.dp))

            // ── Tab Selector ────────────────────────────────────
            val types = listOf("EXPENSE", "INCOME", "TRANSFER")
            val selectedTabIndex = types.indexOf(selectedType)
            val indicatorColor = when (selectedType) {
                "INCOME" -> IncomeGreen
                "EXPENSE" -> ExpenseRed
                else -> TransferYellow
            }

            TabRow(
                selectedTabIndex = selectedTabIndex,
                containerColor   = CardDark,
                modifier         = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp)),
                indicator        = { tabPositions ->
                    TabRowDefaults.SecondaryIndicator(
                        modifier = Modifier.tabIndicatorOffset(tabPositions[selectedTabIndex]),
                        color    = indicatorColor
                    )
                },
                divider = {}
            ) {
                types.forEachIndexed { index, type ->
                    Tab(
                        selected = selectedType == type,
                        onClick  = { selectedType = type },
                        text     = {
                            Text(
                                text       = type.lowercase().replaceFirstChar { it.uppercase() },
                                color      = if (selectedType == type) TextPrimary else TextSecondary,
                                fontWeight = if (selectedType == type) FontWeight.SemiBold else FontWeight.Normal,
                                fontSize   = 14.sp
                            )
                        }
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // ── Amount Input (Typographic ৳ Field) ──────────────
            OutlinedTextField(
                value         = amount,
                onValueChange = { if (it.isEmpty() || it.toDoubleOrNull() != null) amount = it },
                label         = { Text("Amount in BDT (৳)", color = TextSecondary) },
                prefix        = { Text("৳ ", color = indicatorColor, fontWeight = FontWeight.Bold, fontSize = 18.sp) },
                textStyle     = TextStyleForAmount(indicatorColor),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                singleLine    = true,
                modifier      = Modifier.fillMaxWidth(),
                colors        = TextFieldColors()
            )

            if (isInsufficient) {
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "Warning: Insufficient balance in ${selectedFromAccount?.name ?: fromAccountSearchText} (Available: ৳${String.format("%,.2f", selectedBalance)})",
                    color = ExpenseRed,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier.padding(horizontal = 4.dp)
                )
            }

            Spacer(modifier = Modifier.height(18.dp))

            // ── Account Selector(s) ──────────────────────────────
            if (selectedType == "TRANSFER") {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)
                ) {
                    Text(text = "Transfer to", style = androidx.compose.material3.MaterialTheme.typography.labelMedium, color = TextSecondary)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf("Own Account", "Other's Account").forEach { opt ->
                            val selected = (opt == "Own Account" && isOwnAccount) || (opt == "Other's Account" && !isOwnAccount)
                            val color = TransferYellow
                            androidx.compose.material3.FilterChip(
                                selected = selected,
                                onClick  = { isOwnAccount = (opt == "Own Account") },
                                label    = { Text(opt, fontSize = 10.sp, fontWeight = FontWeight.Bold) },
                                colors   = androidx.compose.material3.FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = color.copy(alpha = 0.15f),
                                    selectedLabelColor     = color,
                                    labelColor             = TextSecondary,
                                    containerColor         = CardDark
                                ),
                                border = androidx.compose.material3.FilterChipDefaults.filterChipBorder(
                                    enabled = true, selected = selected,
                                    selectedBorderColor = color, borderColor = DividerColor
                                ),
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.height(28.dp)
                            )
                        }
                    }
                }
            }

            // Source / From Account
            ExposedDropdownMenuBox(
                expanded = fromAccountExpanded,
                onExpandedChange = {
                    fromAccountExpanded = it
                    if (it) {
                        fromAccountSearchText = TextFieldValue("")
                    }
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                OutlinedTextField(
                    value = fromAccountSearchText,
                    onValueChange = {
                        fromAccountSearchText = it
                        fromAccountExpanded = true
                    },
                    label = { Text(if (selectedType == "TRANSFER") "From Account" else "Account", color = TextSecondary) },
                    placeholder = { Text("Select account", color = TextMuted) },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = fromAccountExpanded) },
                    shape = RoundedCornerShape(12.dp),
                    colors = TextFieldColors(),
                    modifier = Modifier
                        .fillMaxWidth()
                        .menuAnchor(ExposedDropdownMenuAnchorType.PrimaryEditable)
                )

                ExposedDropdownMenu(
                    expanded = fromAccountExpanded,
                    onDismissRequest = {
                        fromAccountExpanded = false
                        fromAccountSearchText = TextFieldValue(
                            text = selectedFromAccount?.name ?: "",
                            selection = TextRange((selectedFromAccount?.name ?: "").length)
                        )
                    },
                    modifier = Modifier.background(CardDarker)
                ) {
                    AccountDropdownItems(
                        searchText = fromAccountSearchText.text,
                        accountsList = accounts,
                        onSelectExisting = { account ->
                            selectedFromAccount = account
                            fromAccountSearchText = TextFieldValue(
                                text = account.name,
                                selection = TextRange(account.name.length)
                            )
                            fromAccountExpanded = false
                        },
                        onSelectNew = { name ->
                            selectedFromAccount = null
                            fromAccountSearchText = TextFieldValue(
                                text = name,
                                selection = TextRange(name.length)
                            )
                            fromAccountExpanded = false
                        }
                    )
                }
            }

            // Destination / To Account (Visible only for TRANSFER and isOwnAccount)
            if (selectedType == "TRANSFER" && isOwnAccount) {
                Spacer(modifier = Modifier.height(12.dp))
                val destAccounts = if (isOwnAccount) {
                    accounts.filter { it.id != (selectedFromAccount?.id ?: -1) }
                } else {
                    accounts
                }

                ExposedDropdownMenuBox(
                    expanded = toAccountExpanded,
                    onExpandedChange = {
                        toAccountExpanded = it
                        if (it) {
                            toAccountSearchText = TextFieldValue("")
                        }
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    OutlinedTextField(
                        value = toAccountSearchText,
                        onValueChange = {
                            toAccountSearchText = it
                            toAccountExpanded = true
                        },
                        label = { Text(if (isOwnAccount) "To Account" else "To Bank/MFS", color = TextSecondary) },
                        placeholder = { Text(if (isOwnAccount) "Select destination" else "Select bank", color = TextMuted) },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = toAccountExpanded) },
                        shape = RoundedCornerShape(12.dp),
                        colors = TextFieldColors(),
                        modifier = Modifier
                            .fillMaxWidth()
                            .menuAnchor(ExposedDropdownMenuAnchorType.PrimaryEditable)
                    )

                    ExposedDropdownMenu(
                        expanded = toAccountExpanded,
                        onDismissRequest = {
                            toAccountExpanded = false
                            toAccountSearchText = TextFieldValue(
                                text = selectedToAccount?.name ?: "",
                                selection = TextRange((selectedToAccount?.name ?: "").length)
                            )
                        },
                        modifier = Modifier.background(CardDarker)
                    ) {
                        AccountDropdownItems(
                            searchText = toAccountSearchText.text,
                            accountsList = destAccounts,
                            onSelectExisting = { account ->
                                selectedToAccount = account
                                toAccountSearchText = TextFieldValue(
                                    text = account.name,
                                    selection = TextRange(account.name.length)
                                )
                                toAccountExpanded = false
                            },
                            onSelectNew = { name ->
                                selectedToAccount = null
                                toAccountSearchText = TextFieldValue(
                                    text = name,
                                    selection = TextRange(name.length)
                                )
                                toAccountExpanded = false
                            }
                        )
                    }
                }
            }

            if (selectedType == "TRANSFER" && !isOwnAccount) {
                Spacer(modifier = Modifier.height(14.dp))

                // Editable Recipient Name Field
                OutlinedTextField(
                    value = recipientName,
                    onValueChange = {
                        recipientName = it
                    },
                    label = { Text("Recipient Name *", color = TextSecondary) },
                    placeholder = { Text("Enter recipient name", color = TextMuted) },
                    shape = RoundedCornerShape(12.dp),
                    colors = TextFieldColors(),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Editable Recipient Account Number Field
                OutlinedTextField(
                    value = recipientAccountNumber,
                    onValueChange = {
                        recipientAccountNumber = it
                    },
                    label = { Text("Recipient Account/Mobile Number *", color = TextSecondary) },
                    placeholder = { Text("Enter account or phone number", color = TextMuted) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    shape = RoundedCornerShape(12.dp),
                    colors = TextFieldColors(),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }

            Spacer(modifier = Modifier.height(18.dp))

            // ── Category Selector (Category Chips Grid) ─────────
            if (selectedType != "TRANSFER") {
                Text(text = "Category", style = androidx.compose.material3.MaterialTheme.typography.labelMedium, color = TextSecondary)
                Spacer(modifier = Modifier.height(8.dp))

                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    activeCategories.forEach { cat ->
                        val isSelected = selectedCategory == cat
                        val isCustom = if (selectedType == "INCOME") cat in customIncomeCategories else cat in customExpenseCategories
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(20.dp))
                                .background(if (isSelected) indicatorColor else CardDark)
                                .combinedClickable(
                                    onClick = { selectedCategory = cat },
                                    onLongClick = {
                                        if (isCustom) {
                                            categoryToDelete = cat
                                            showDeleteDialog = true
                                        }
                                    }
                                )
                                .padding(horizontal = 14.dp, vertical = 8.dp)
                        ) {
                            Text(
                                text       = cat,
                                color      = if (isSelected) BackgroundDark else TextPrimary,
                                fontSize   = 12.sp,
                                fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal
                            )
                        }
                    }

                    // Add Custom Category Chip
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(20.dp))
                            .background(CardDark.copy(alpha = 0.6f))
                            .border(1.dp, AccentTeal.copy(alpha = 0.5f), RoundedCornerShape(20.dp))
                            .clickable { showAddCategoryDialog = true }
                            .padding(horizontal = 14.dp, vertical = 8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Add,
                                contentDescription = "Add Category",
                                tint = AccentTeal,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "New",
                                color = AccentTeal,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(18.dp))
            }

            // ── Notes ──────────────────────────────────────────
            OutlinedTextField(
                value         = note,
                onValueChange = { note = it },
                label         = { Text("Add Note (Optional)", color = TextSecondary) },
                singleLine    = true,
                modifier      = Modifier.fillMaxWidth(),
                colors        = TextFieldColors()
            )

            Spacer(modifier = Modifier.height(28.dp))

            // ── Save Button ────────────────────────────────────
            val isValid = amount.isNotEmpty() && amount.toDoubleOrNull() != null && amount.toDouble() > 0 &&
                    (selectedFromAccount != null || isFromAccountNew) && !isInsufficient &&
                    (selectedType != "TRANSFER" || 
                        ((isOwnAccount && (selectedToAccount != null || isToAccountNew) && 
                          (selectedFromAccount?.id != selectedToAccount?.id || fromAccountSearchText.text.trim().lowercase() != toAccountSearchText.text.trim().lowercase())) ||
                         (!isOwnAccount && recipientName.trim().isNotEmpty() && recipientAccountNumber.trim().isNotEmpty())))

            Button(
                onClick = {
                    if (isValid) {
                        val finalNote = if (selectedType == "TRANSFER" && !isOwnAccount) {
                            "To: ${recipientName.trim()} (${recipientAccountNumber.trim()})" + (if (note.trim().isNotEmpty()) " - ${note.trim()}" else "")
                        } else {
                            note
                        }
                        if (selectedType == "TRANSFER" && !isOwnAccount && saveToPayees) {
                            onSavePayee(
                                recipientName.trim(),
                                "",
                                recipientAccountNumber.trim(),
                                "BANK"
                            )
                        }
                        
                        val newFromAcc = if (isFromAccountNew) createNewAccountEntity(fromAccountSearchText.text.trim()) else null
                        val newToAcc = if (selectedType == "TRANSFER" && isOwnAccount && isToAccountNew) {
                            createNewAccountEntity(toAccountSearchText.text.trim())
                        } else null

                        onSaveTransaction(
                            TransactionEntity(
                                amount        = amount.toDouble(),
                                type          = selectedType,
                                category      = if (selectedType == "TRANSFER") "Transfer" else selectedCategory,
                                timestamp     = System.currentTimeMillis(),
                                fromAccountId = selectedFromAccount?.id ?: 0,
                                toAccountId   = if (selectedType == "TRANSFER" && isOwnAccount) (selectedToAccount?.id ?: 0) else null,
                                note          = finalNote
                            ),
                            newFromAcc,
                            newToAcc
                        )
                        onDismiss()
                    }
                },
                enabled  = isValid,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                shape    = RoundedCornerShape(16.dp),
                colors   = ButtonDefaults.buttonColors(
                    containerColor = Color.Transparent,
                    disabledContainerColor = CardDarker
                ),
                contentPadding = androidx.compose.foundation.layout.PaddingValues(0.dp)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            if (isValid) {
                                Brush.horizontalGradient(colors = listOf(GradientStart, GradientEnd))
                            } else {
                                Brush.horizontalGradient(colors = listOf(CardDark, CardDark))
                            }
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text  = "Save Transaction",
                        style = androidx.compose.material3.MaterialTheme.typography.titleMedium,
                        color = if (isValid) BackgroundDark else TextMuted
                    )
                }
            }
        }

        // ── Custom Category Dialogs ──────────────────────────────
        if (showAddCategoryDialog) {
            androidx.compose.ui.window.Dialog(onDismissRequest = { showAddCategoryDialog = false }) {
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .wrapContentHeight()
                        .padding(24.dp),
                    shape = RoundedCornerShape(24.dp),
                    color = CardDark
                ) {
                    Column(
                        modifier = Modifier.padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "Add Custom Category",
                            style = androidx.compose.material3.MaterialTheme.typography.titleMedium,
                            color = TextPrimary,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        OutlinedTextField(
                            value = newCategoryName,
                            onValueChange = { newCategoryName = it },
                            label = { Text("Category Name", color = TextSecondary) },
                            singleLine = true,
                            colors = TextFieldColors(),
                            modifier = Modifier.fillMaxWidth()
                        )
                        Spacer(modifier = Modifier.height(24.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.End
                        ) {
                            TextButton(onClick = {
                                showAddCategoryDialog = false
                                newCategoryName = ""
                            }) {
                                Text("Cancel", color = TextSecondary)
                            }
                            Spacer(modifier = Modifier.width(16.dp))
                            Button(
                                onClick = {
                                    val trimmed = newCategoryName.trim()
                                    if (trimmed.isNotEmpty()) {
                                        if (selectedType == "INCOME") {
                                            val updated = customIncomeCategories + trimmed
                                            customIncomeCategories = updated
                                            sharedPreferences.edit().putString("custom_income_categories", updated.joinToString("|")).apply()
                                            selectedCategory = trimmed
                                        } else if (selectedType == "EXPENSE") {
                                            val updated = customExpenseCategories + trimmed
                                            customExpenseCategories = updated
                                            sharedPreferences.edit().putString("custom_expense_categories", updated.joinToString("|")).apply()
                                            selectedCategory = trimmed
                                        }
                                    }
                                    showAddCategoryDialog = false
                                    newCategoryName = ""
                                },
                                enabled = newCategoryName.trim().isNotEmpty(),
                                colors = ButtonDefaults.buttonColors(containerColor = AccentTeal)
                            ) {
                                Text("Add", color = BackgroundDark, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        }

        if (showDeleteDialog) {
            androidx.compose.ui.window.Dialog(onDismissRequest = { showDeleteDialog = false }) {
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .wrapContentHeight()
                        .padding(24.dp),
                    shape = RoundedCornerShape(24.dp),
                    color = CardDark
                ) {
                    Column(
                        modifier = Modifier.padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "Delete Category?",
                            style = androidx.compose.material3.MaterialTheme.typography.titleMedium,
                            color = TextPrimary,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = "Are you sure you want to delete the category \"$categoryToDelete\"?",
                            color = TextSecondary,
                            fontSize = 14.sp,
                            textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(24.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.End
                        ) {
                            TextButton(onClick = { showDeleteDialog = false }) {
                                Text("Cancel", color = TextSecondary)
                            }
                            Spacer(modifier = Modifier.width(16.dp))
                            Button(
                                onClick = {
                                    if (selectedType == "INCOME") {
                                        val updated = customIncomeCategories.filter { it != categoryToDelete }
                                        customIncomeCategories = updated
                                        sharedPreferences.edit().putString("custom_income_categories", updated.joinToString("|")).apply()
                                        if (selectedCategory == categoryToDelete) {
                                            selectedCategory = (defaultIncomeCategories + updated).first()
                                        }
                                    } else if (selectedType == "EXPENSE") {
                                        val updated = customExpenseCategories.filter { it != categoryToDelete }
                                        customExpenseCategories = updated
                                        sharedPreferences.edit().putString("custom_expense_categories", updated.joinToString("|")).apply()
                                        if (selectedCategory == categoryToDelete) {
                                            selectedCategory = (defaultExpenseCategories + updated).first()
                                        }
                                    }
                                    showDeleteDialog = false
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = ExpenseRed)
                            ) {
                                Text("Delete", color = Color.White, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────
// Styles Helpers
// ─────────────────────────────────────────────────────────────

@Composable
private fun TextStyleForAmount(color: Color) = androidx.compose.ui.text.TextStyle(
    color      = color,
    fontSize   = 22.sp,
    fontWeight = FontWeight.Bold
)

@Composable
private fun TextFieldColors() = OutlinedTextFieldDefaults.colors(
    focusedTextColor       = TextPrimary,
    unfocusedTextColor     = TextPrimary,
    focusedBorderColor     = AccentTeal,
    unfocusedBorderColor   = DividerColor,
    focusedContainerColor  = CardDark,
    unfocusedContainerColor = CardDark,
    focusedLabelColor      = AccentTeal,
    unfocusedLabelColor    = TextSecondary
)

@Composable
fun LaunchedEffectForType(type: String, block: suspend () -> Unit) {
    androidx.compose.runtime.LaunchedEffect(key1 = type) {
        block()
    }
}

private val PRESET_BANKS = listOf(
    "BRAC Bank PLC", "The City Bank PLC", "Eastern Bank PLC (EBL)",
    "Dutch-Bangla Bank PLC (DBBL)", "Prime Bank PLC", "Mutual Trust Bank PLC",
    "Islami Bank Bangladesh PLC (IBBL)", "Al-Arafah Islami Bank PLC", "Shahjalal Islami Bank PLC"
)
private val PRESET_MFS = listOf(
    "bKash", "Nagad", "Rocket", "Upay", "CellFin (IBBL)", "Ok Wallet", "MyCash"
)

private fun createNewAccountEntity(name: String): AccountEntity {
    val presetMfs = listOf("bKash", "Nagad", "Rocket", "Upay", "CellFin (IBBL)", "Ok Wallet", "MyCash")
    val isMfs = presetMfs.any { name.contains(it, ignoreCase = true) }
    val type = if (isMfs) "MFS" else "BANK"
    val subtype = if (isMfs) "Personal" else "Savings"
    val colorHex = when {
        name.contains("BRAC", ignoreCase = true) -> "#0096FF"
        name.contains("City", ignoreCase = true) -> "#1A365D"
        name.contains("Eastern", ignoreCase = true) -> "#004B87"
        name.contains("Dutch-Bangla", ignoreCase = true) || name.contains("DBBL", ignoreCase = true) -> "#00875A"
        name.contains("Prime", ignoreCase = true) -> "#1E3A8A"
        name.contains("Mutual Trust", ignoreCase = true) || name.contains("MTB", ignoreCase = true) -> "#A21CAF"
        name.contains("Islami", ignoreCase = true) || name.contains("IBBL", ignoreCase = true) -> "#15803D"
        name.contains("Al-Arafah", ignoreCase = true) -> "#0F766E"
        name.contains("Shahjalal", ignoreCase = true) -> "#0369A1"
        name.contains("bKash", ignoreCase = true) -> "#E2136E"
        name.contains("Nagad", ignoreCase = true) -> "#F04A24"
        name.contains("Rocket", ignoreCase = true) -> "#8C2D19"
        name.contains("Upay", ignoreCase = true) -> "#0052CC"
        name.contains("CellFin", ignoreCase = true) -> "#15803D"
        isMfs -> "#FF5C7C"
        else -> "#0096FF"
    }
    return AccountEntity(
        name = name,
        type = type,
        balance = 0.0,
        colorHex = colorHex,
        accountSubtype = subtype
    )
}

@Composable
private fun androidx.compose.foundation.layout.ColumnScope.AccountDropdownItems(
    searchText: String,
    accountsList: List<AccountEntity>,
    onSelectExisting: (AccountEntity) -> Unit,
    onSelectNew: (String) -> Unit
) {
    val matchingExistingBanks = accountsList.filter { it.type == "BANK" && it.name.contains(searchText, ignoreCase = true) }
    val matchingExistingMfs = accountsList.filter { it.type == "MFS" && it.name.contains(searchText, ignoreCase = true) }
    val existingNames = accountsList.map { it.name.lowercase() }
    val matchingPresetBanks = PRESET_BANKS.filter {
        !existingNames.contains(it.lowercase()) && it.contains(searchText, ignoreCase = true)
    }
    val matchingPresetMfs = PRESET_MFS.filter {
        !existingNames.contains(it.lowercase()) && it.contains(searchText, ignoreCase = true)
    }

    if (matchingExistingBanks.isNotEmpty()) {
        DropdownMenuItem(
            text = { Text("Your Banks", color = AccentTeal, fontWeight = FontWeight.Bold, fontSize = 11.sp) },
            onClick = {},
            enabled = false
        )
        matchingExistingBanks.forEach { account ->
            DropdownMenuItem(
                text = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(account.name, color = TextPrimary, fontWeight = FontWeight.Medium)
                        
                        if (account.accountNumber.isNotBlank()) {
                            val accLast4 = account.accountNumber.takeLast(4)
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(DividerColor.copy(alpha = 0.3f))
                                    .border(1.dp, DividerColor.copy(alpha = 0.5f), RoundedCornerShape(6.dp))
                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                            ) {
                                Text(
                                    text = "*******$accLast4",
                                    color = TextSecondary,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                        
                        if (account.showAs.isNotBlank()) {
                            val tagColor = remember(account.colorHex) {
                                try { Color(android.graphics.Color.parseColor(account.colorHex)) } catch (e: Exception) { AccentTeal }
                            }
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(tagColor.copy(alpha = 0.12f))
                                    .border(1.dp, tagColor.copy(alpha = 0.25f), RoundedCornerShape(6.dp))
                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                            ) {
                                Text(
                                    text = account.showAs,
                                    color = tagColor,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                },
                onClick = { onSelectExisting(account) }
            )
        }
    }

    if (matchingPresetBanks.isNotEmpty()) {
        DropdownMenuItem(
            text = { Text("Link Bank Account", color = AccentTeal, fontWeight = FontWeight.Bold, fontSize = 11.sp) },
            onClick = {},
            enabled = false
        )
        matchingPresetBanks.forEach { preset ->
            DropdownMenuItem(
                text = { Text("+ Link $preset", color = TextPrimary) },
                onClick = { onSelectNew(preset) }
            )
        }
    }

    if (matchingExistingMfs.isNotEmpty()) {
        if (matchingExistingBanks.isNotEmpty() || matchingPresetBanks.isNotEmpty()) {
            androidx.compose.material3.HorizontalDivider(color = DividerColor.copy(alpha = 0.5f))
        }
        DropdownMenuItem(
            text = { Text("Your Mobile Wallets", color = AccentTeal, fontWeight = FontWeight.Bold, fontSize = 11.sp) },
            onClick = {},
            enabled = false
        )
        matchingExistingMfs.forEach { account ->
            DropdownMenuItem(
                text = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(account.name, color = TextPrimary, fontWeight = FontWeight.Medium)
                        
                        if (account.accountNumber.isNotBlank()) {
                            val accLast4 = account.accountNumber.takeLast(4)
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(DividerColor.copy(alpha = 0.3f))
                                    .border(1.dp, DividerColor.copy(alpha = 0.5f), RoundedCornerShape(6.dp))
                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                            ) {
                                Text(
                                    text = "*******$accLast4",
                                    color = TextSecondary,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                        
                        if (account.showAs.isNotBlank()) {
                            val tagColor = remember(account.colorHex) {
                                try { Color(android.graphics.Color.parseColor(account.colorHex)) } catch (e: Exception) { AccentTeal }
                            }
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(tagColor.copy(alpha = 0.12f))
                                    .border(1.dp, tagColor.copy(alpha = 0.25f), RoundedCornerShape(6.dp))
                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                            ) {
                                Text(
                                    text = account.showAs,
                                    color = tagColor,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                },
                onClick = { onSelectExisting(account) }
            )
        }
    }

    if (matchingPresetMfs.isNotEmpty()) {
        if (matchingExistingBanks.isNotEmpty() || matchingPresetBanks.isNotEmpty() || matchingExistingMfs.isNotEmpty()) {
            androidx.compose.material3.HorizontalDivider(color = DividerColor.copy(alpha = 0.5f))
        }
        DropdownMenuItem(
            text = { Text("Link Mobile Wallet", color = AccentTeal, fontWeight = FontWeight.Bold, fontSize = 11.sp) },
            onClick = {},
            enabled = false
        )
        matchingPresetMfs.forEach { preset ->
            DropdownMenuItem(
                text = { Text("+ Link $preset", color = TextPrimary) },
                onClick = { onSelectNew(preset) }
            )
        }
    }

    val typedTrimmed = searchText.trim()
    if (typedTrimmed.isNotEmpty() &&
        !accountsList.any { it.name.equals(typedTrimmed, ignoreCase = true) } &&
        !PRESET_BANKS.any { it.equals(typedTrimmed, ignoreCase = true) } &&
        !PRESET_MFS.any { it.equals(typedTrimmed, ignoreCase = true) }
    ) {
        DropdownMenuItem(
            text = { Text("+ Create custom account: \"$typedTrimmed\"", color = TextPrimary) },
            onClick = { onSelectNew(typedTrimmed) }
        )
    }
}
