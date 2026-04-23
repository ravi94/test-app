package com.testapp.renttracker.model

enum class BillingMonthStatus {
    Draft,
    Finalized,
}

enum class PaymentComponent {
    Rent,
    Electricity,
    Combined,
}

enum class PaymentStatus {
    Unpaid,
    Partial,
    Paid,
}
