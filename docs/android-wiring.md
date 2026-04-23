# Android Wiring Notes

This repository now includes an `app` Android module with Room artifacts and Compose-friendly ViewModels.

## Included
- Room entities: `app/src/main/kotlin/com/testapp/renttracker/data/room/entity`
- DAO interfaces: `app/src/main/kotlin/com/testapp/renttracker/data/room/dao`
- `RoomDatabase`: `app/src/main/kotlin/com/testapp/renttracker/data/room/RentTrackerDatabase.kt`
- Migrations: `app/src/main/kotlin/com/testapp/renttracker/data/room/migration/Migrations.kt`
- Room-backed repositories: `app/src/main/kotlin/com/testapp/renttracker/data/repo/RoomRepositories.kt`
- ViewModels:
  - `MonthlyBillingViewModel`
  - `PaymentViewModel`
  - `DashboardViewModel`
- Graph/factories: `app/src/main/kotlin/com/testapp/renttracker/presentation/common/AppGraph.kt`

## App-level DB bootstrap
Already wired in `app/src/main/java/com/testapp/renttracker/MainActivity.kt`.
Reference setup:

```kotlin
val db = Room.databaseBuilder(
    context,
    RentTrackerDatabase::class.java,
    "rent-tracker.db"
)
    .addMigrations(
        Migrations.MIGRATION_1_2,
        Migrations.MIGRATION_2_3,
    )
    .build()

val appGraph = AppGraph(db)
```

## Compose usage pattern
```kotlin
val vm: MonthlyBillingViewModel = viewModel(factory = appGraph.billingViewModelFactory())
val uiState by vm.state.collectAsState()
```

## Important note
This repo is currently scaffolded as Kotlin/JVM; Android runtime integration (AGP module, manifest, Compose screens) should be added in an Android app module to execute on device/emulator.
