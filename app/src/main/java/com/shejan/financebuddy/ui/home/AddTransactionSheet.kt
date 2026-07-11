package com.shejan.financebuddy.ui.home

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.SheetState
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.shejan.financebuddy.data.db.AccountEntity
import com.shejan.financebuddy.data.db.TransactionEntity
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

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun AddTransactionSheet(
    accounts: List<AccountEntity>,
    sheetState: SheetState,
    onDismiss: () -> Unit,
    onSaveTransaction: (TransactionEntity) -> Unit
) {
    if (accounts.isEmpty()) return

    var amount by remember { mutableStateOf("") }
    var selectedType by remember { mutableStateOf("EXPENSE") } // "INCOME", "EXPENSE", "TRANSFER"
    var selectedCategory by remember { mutableStateOf("") }
    var selectedFromAccount by remember { mutableStateOf(accounts.first()) }
    var selectedToAccount by remember { mutableStateOf<AccountEntity?>(null) }
    var note by remember { mutableStateOf("") }

    var fromAccountExpanded by remember { mutableStateOf(false) }
    var toAccountExpanded by remember { mutableStateOf(false) }

    val incomeCategories = listOf("Salary", "Freelance", "Investment", "Pocket Money", "Other")
    val expenseCategories = listOf("Food", "Groceries", "Rent", "Utilities", "Travel", "Shopping", "Entertainment", "Medical", "Other")
    val activeCategories = if (selectedType == "INCOME") incomeCategories else if (selectedType == "EXPENSE") expenseCategories else listOf("Transfer")

    // Reset default category if type changes
    LaunchedEffectForType(selectedType) {
        selectedCategory = if (selectedType == "TRANSFER") "Transfer" else activeCategories.first()
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState       = sheetState,
        containerColor   = SurfaceDarkColor,
        shape            = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
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
                containerColor   = CardDarker,
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

            Spacer(modifier = Modifier.height(18.dp))

            // ── Account Selector(s) ──────────────────────────────
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Source / From Account
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(12.dp))
                        .background(CardDark)
                        .clickable { fromAccountExpanded = true }
                        .padding(horizontal = 16.dp, vertical = 14.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text(
                                text  = if (selectedType == "TRANSFER") "From Account" else "Account",
                                style = androidx.compose.material3.MaterialTheme.typography.labelMedium,
                                color = TextMuted
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(text = selectedFromAccount.name, color = TextPrimary, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                        }
                        Icon(imageVector = Icons.Default.ArrowDropDown, contentDescription = null, tint = TextSecondary)
                    }

                    DropdownMenu(
                        expanded = fromAccountExpanded,
                        onDismissRequest = { fromAccountExpanded = false },
                        modifier = Modifier.background(CardDark)
                    ) {
                        accounts.forEach { account ->
                            DropdownMenuItem(
                                text = { Text(account.name, color = TextPrimary) },
                                onClick = {
                                    selectedFromAccount = account
                                    fromAccountExpanded = false
                                }
                            )
                        }
                    }
                }

                // Destination / To Account (Visible only for TRANSFER)
                if (selectedType == "TRANSFER") {
                    val destAccounts = accounts.filter { it.id != selectedFromAccount.id }
                    if (selectedToAccount == null && destAccounts.isNotEmpty()) {
                        selectedToAccount = destAccounts.first()
                    }

                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(12.dp))
                            .background(CardDark)
                            .clickable { toAccountExpanded = true }
                            .padding(horizontal = 16.dp, vertical = 14.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column {
                                Text(
                                    text  = "To Account",
                                    style = androidx.compose.material3.MaterialTheme.typography.labelMedium,
                                    color = TextMuted
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(text = selectedToAccount?.name ?: "Select", color = TextPrimary, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                            }
                            Icon(imageVector = Icons.Default.ArrowDropDown, contentDescription = null, tint = TextSecondary)
                        }

                        DropdownMenu(
                            expanded = toAccountExpanded,
                            onDismissRequest = { toAccountExpanded = false },
                            modifier = Modifier.background(CardDark)
                        ) {
                            destAccounts.forEach { account ->
                                DropdownMenuItem(
                                    text = { Text(account.name, color = TextPrimary) },
                                    onClick = {
                                        selectedToAccount = account
                                        toAccountExpanded = false
                                    }
                                )
                            }
                        }
                    }
                }
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
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(20.dp))
                                .background(if (isSelected) indicatorColor else CardDark)
                                .clickable { selectedCategory = cat }
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
                    (selectedType != "TRANSFER" || (selectedToAccount != null && selectedToAccount?.id != selectedFromAccount.id))

            Button(
                onClick = {
                    if (isValid) {
                        onSaveTransaction(
                            TransactionEntity(
                                amount        = amount.toDouble(),
                                type          = selectedType,
                                category      = if (selectedType == "TRANSFER") "Transfer" else selectedCategory,
                                timestamp     = System.currentTimeMillis(),
                                fromAccountId = selectedFromAccount.id,
                                toAccountId   = if (selectedType == "TRANSFER") selectedToAccount?.id else null,
                                note          = note
                            )
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
    }
}

// ─────────────────────────────────────────────────────────────
// Styles Helpers
// ─────────────────────────────────────────────────────────────

private val SurfaceDarkColor = Color(0xFF0F1221)

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
    focusedContainerColor  = CardDarker,
    unfocusedContainerColor = CardDarker,
    focusedLabelColor      = AccentTeal,
    unfocusedLabelColor    = TextSecondary
)

@Composable
fun LaunchedEffectForType(type: String, block: suspend () -> Unit) {
    androidx.compose.runtime.LaunchedEffect(key1 = type) {
        block()
    }
}
