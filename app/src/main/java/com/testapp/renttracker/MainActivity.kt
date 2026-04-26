package com.testapp.renttracker

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.room.Room
import com.testapp.renttracker.data.room.RentTrackerDatabase
import com.testapp.renttracker.data.room.migration.Migrations
import com.testapp.renttracker.presentation.billing.MonthlyBillingViewModel
import com.testapp.renttracker.presentation.common.AppGraph
import com.testapp.renttracker.presentation.dashboard.DashboardViewModel
import com.testapp.renttracker.presentation.onboarding.TenantOnboardingViewModel
import com.testapp.renttracker.presentation.payment.PaymentViewModel
import com.testapp.renttracker.ui.RentTrackerApp

class MainActivity : ComponentActivity() {
    private val appGraph: AppGraph by lazy {
        val db = Room.databaseBuilder(
            applicationContext,
            RentTrackerDatabase::class.java,
            "rent-tracker.db",
        )
            .addMigrations(
                Migrations.MIGRATION_1_2,
                Migrations.MIGRATION_2_3,
                Migrations.MIGRATION_3_4,
                Migrations.MIGRATION_4_5,
            )
            .build()

        AppGraph(db)
    }

    private val billingViewModel: MonthlyBillingViewModel by viewModels {
        appGraph.billingViewModelFactory()
    }

    private val paymentViewModel: PaymentViewModel by viewModels {
        appGraph.paymentViewModelFactory()
    }

    private val dashboardViewModel: DashboardViewModel by viewModels {
        appGraph.dashboardViewModelFactory()
    }
    private val tenantOnboardingViewModel: TenantOnboardingViewModel by viewModels {
        appGraph.tenantOnboardingViewModelFactory()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        appGraph.seedDemoDataIfEmpty()

        setContent {
            MaterialTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background,
                ) {
                    RentTrackerApp(
                        tenantOnboardingViewModel = tenantOnboardingViewModel,
                        billingViewModel = billingViewModel,
                        paymentViewModel = paymentViewModel,
                        dashboardViewModel = dashboardViewModel,
                    )
                }
            }
        }
    }
}
