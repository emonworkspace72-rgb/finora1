package com.example.data.local

import com.example.data.local.entity.AccountEntity
import com.example.data.local.entity.CategoryEntity

object DefaultData {

    val DEFAULT_ACCOUNTS = listOf(
        AccountEntity(
            id = "acc_cash_main",
            name = "Cash Wallet",
            type = "CASH",
            currency = "BDT",
            openingBalanceCents = 0L, // Fresh account starts with 0
            colorHex = "#B7F345"
        ),
        AccountEntity(
            id = "acc_bank_checking",
            name = "Main Bank Account",
            type = "BANK",
            currency = "BDT",
            openingBalanceCents = 0L, // Fresh account starts with 0
            colorHex = "#38BDF8"
        ),
        AccountEntity(
            id = "acc_credit_visa",
            name = "Credit Card",
            type = "CREDIT_CARD",
            currency = "BDT",
            openingBalanceCents = 0L, // Fresh account starts with 0
            colorHex = "#F43F5E"
        )
    )

    val DEFAULT_CATEGORIES = listOf(
        // Expenses
        CategoryEntity(
            id = "cat_groceries",
            name = "Groceries",
            type = "EXPENSE",
            iconName = "ShoppingBag",
            colorHex = "#F87171",
            sortOrder = 1,
            isDefault = true
        ),
        CategoryEntity(
            id = "cat_dining",
            name = "Restaurants & Coffee",
            type = "EXPENSE",
            iconName = "Restaurant",
            colorHex = "#FB923C",
            sortOrder = 2,
            isDefault = true
        ),
        CategoryEntity(
            id = "cat_transport",
            name = "Transport & Fuel",
            type = "EXPENSE",
            iconName = "DirectionsCar",
            colorHex = "#FBBF24",
            sortOrder = 3,
            isDefault = true
        ),
        CategoryEntity(
            id = "cat_housing",
            name = "Rent & Housing",
            type = "EXPENSE",
            iconName = "Home",
            colorHex = "#34D399",
            sortOrder = 4,
            isDefault = true
        ),
        CategoryEntity(
            id = "cat_utilities",
            name = "Utilities & Bills",
            type = "EXPENSE",
            iconName = "Bolt",
            colorHex = "#38BDF8",
            sortOrder = 5,
            isDefault = true
        ),
        CategoryEntity(
            id = "cat_shopping",
            name = "Shopping & Clothes",
            type = "EXPENSE",
            iconName = "Checkroom",
            colorHex = "#818CF8",
            sortOrder = 6,
            isDefault = true
        ),
        CategoryEntity(
            id = "cat_entertainment",
            name = "Entertainment & Subs",
            type = "EXPENSE",
            iconName = "Movie",
            colorHex = "#C084FC",
            sortOrder = 7,
            isDefault = true
        ),
        CategoryEntity(
            id = "cat_health",
            name = "Health & Pharmacy",
            type = "EXPENSE",
            iconName = "LocalHospital",
            colorHex = "#F472B6",
            sortOrder = 8,
            isDefault = true
        ),
        CategoryEntity(
            id = "cat_travel",
            name = "Travel & Leisure",
            type = "EXPENSE",
            iconName = "Flight",
            colorHex = "#A3E635",
            sortOrder = 9,
            isDefault = true
        ),
        CategoryEntity(
            id = "cat_other_expense",
            name = "Other Expenses",
            type = "EXPENSE",
            iconName = "Category",
            colorHex = "#94A3B8",
            sortOrder = 10,
            isDefault = true
        ),

        // Income
        CategoryEntity(
            id = "cat_salary",
            name = "Salary",
            type = "INCOME",
            iconName = "AccountBalanceWallet",
            colorHex = "#B7F345",
            sortOrder = 11,
            isDefault = true
        ),
        CategoryEntity(
            id = "cat_freelance",
            name = "Freelance & Side Gig",
            type = "INCOME",
            iconName = "Laptop",
            colorHex = "#4ADE80",
            sortOrder = 12,
            isDefault = true
        ),
        CategoryEntity(
            id = "cat_investments",
            name = "Investments & Dividends",
            type = "INCOME",
            iconName = "TrendingUp",
            colorHex = "#2DD4BF",
            sortOrder = 13,
            isDefault = true
        ),
        CategoryEntity(
            id = "cat_gifts",
            name = "Gifts & Grants",
            type = "INCOME",
            iconName = "CardGiftcard",
            colorHex = "#60A5FA",
            sortOrder = 14,
            isDefault = true
        )
    )
}
