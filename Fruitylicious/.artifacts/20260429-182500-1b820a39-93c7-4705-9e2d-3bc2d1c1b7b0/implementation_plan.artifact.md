# Implementation Plan - Restrict Branch Picker to Admin/Owner

This plan outlines the changes to hide the branch picker from users with the "staff" role across various screens. Only users with the "admin" (Owner) role will be able to see and use the branch picker.

## User Review Required

> [!IMPORTANT]
> For staff users, the screens will default to showing data for their assigned branch. Since they won't have the "All" option anymore, this change also enforces branch-level data isolation in the UI.

## Proposed Changes

### Shared Components & Models

#### [SharedScreenMode.kt](file:///C:/Users/pritongisda/AndroidStudioProjects/Fruitylicious/Fruitylicious/app/src/main/java/com/example/fruitylicious/ui/shared/SharedScreenMode.kt)
- No changes needed (already has ADMIN and STAFF).

---

### ViewModels Update
I will update the following ViewModels to include `isAdmin` and `userBranchId` in their `UiState`.

1. [AdminDashboardViewModel.kt](file:///C:/Users/pritongisda/AndroidStudioProjects/Fruitylicious/Fruitylicious/app/src/main/java/com/example/fruitylicious/ui/admin/dashboard/AdminDashboardViewModel.kt)
2. [SalesSummaryViewModel.kt](file:///C:/Users/pritongisda/AndroidStudioProjects/Fruitylicious/Fruitylicious/app/src/main/java/com/example/fruitylicious/ui/admin/reports/SalesSummaryViewModel.kt)
3. [StaffLogViewModel.kt](file:///C:/Users/pritongisda/AndroidStudioProjects/Fruitylicious/Fruitylicious/app/src/main/java/com/example/fruitylicious/ui/admin/staffmanagement/StaffLogViewModel.kt)
4. [AuditLogViewModel.kt](file:///C:/Users/pritongisda/AndroidStudioProjects/Fruitylicious/Fruitylicious/app/src/main/java/com/example/fruitylicious/ui/admin/system/AuditLogViewModel.kt)
5. [ManageIngredientsViewModel.kt](file:///C:/Users/pritongisda/AndroidStudioProjects/Fruitylicious/Fruitylicious/app/src/main/java/com/example/fruitylicious/ui/admin/ingredients/ManageIngredientsViewModel.kt)
6. [ManageProductsViewModel.kt](file:///C:/Users/pritongisda/AndroidStudioProjects/Fruitylicious/Fruitylicious/app/src/main/java/com/example/fruitylicious/ui/admin/products/ManageProductsViewModel.kt)
7. [InventoryMonitoringViewModel.kt](file:///C:/Users/pritongisda/AndroidStudioProjects/Fruitylicious/Fruitylicious/app/src/main/java/com/example/fruitylicious/ui/shared/inventory/InventoryMonitoringViewModel.kt)
8. [NotificationsViewModel.kt](file:///C:/Users/pritongisda/AndroidStudioProjects/Fruitylicious/Fruitylicious/app/src/main/java/com/example/fruitylicious/ui/shared/notifications/NotificationsViewModel.kt)
9. [RestockViewModel.kt](file:///C:/Users/pritongisda/AndroidStudioProjects/Fruitylicious/Fruitylicious/app/src/main/java/com/example/fruitylicious/ui/shared/restock/RestockViewModel.kt)
10. [TransactionHistoryViewModel.kt](file:///C:/Users/pritongisda/AndroidStudioProjects/Fruitylicious/Fruitylicious/app/src/main/java/com/example/fruitylicious/ui/shared/transaction/TransactionHistoryViewModel.kt)
11. [WasteManagementViewModel.kt](file:///C:/Users/pritongisda/AndroidStudioProjects/Fruitylicious/Fruitylicious/app/src/main/java/com/example/fruitylicious/ui/shared/waste/WasteManagementViewModel.kt)

---

### Screens Update
For each of the following screens, I will:
- Update the `selectedBranch` initialization to respect the user's role.
- Conditionally render the branch picker in the `Header` or top section.

#### [TransactionHistoryScreen.kt](file:///C:/Users/pritongisda/AndroidStudioProjects/Fruitylicious/Fruitylicious/app/src/main/java/com/example/fruitylicious/ui/shared/transaction/TransactionHistoryScreen.kt)
- Add `isAdmin` check in `Header`.
- Initialize `selectedBranch` to user's branch if not admin.

#### [InventoryMonitoringScreen.kt](file:///C:/Users/pritongisda/AndroidStudioProjects/Fruitylicious/Fruitylicious/app/src/main/java/com/example/fruitylicious/ui/shared/inventory/InventoryMonitoringScreen.kt)
- Similar changes as above.

#### [RestockScreen.kt](file:///C:/Users/pritongisda/AndroidStudioProjects/Fruitylicious/Fruitylicious/app/src/main/java/com/example/fruitylicious/ui/shared/restock/RestockScreen.kt)
- Similar changes as above.

#### [WasteManagementScreen.kt](file:///C:/Users/pritongisda/AndroidStudioProjects/Fruitylicious/Fruitylicious/app/src/main/java/com/example/fruitylicious/ui/shared/waste/WasteManagementScreen.kt)
- Similar changes as above.

#### [NotificationsScreen.kt](file:///C:/Users/pritongisda/AndroidStudioProjects/Fruitylicious/Fruitylicious/app/src/main/java/com/example/fruitylicious/ui/shared/notifications/NotificationsScreen.kt)
- Similar changes as above.

#### [AdminDashboard.kt](file:///C:/Users/pritongisda/AndroidStudioProjects/Fruitylicious/Fruitylicious/app/src/main/java/com/example/fruitylicious/ui/admin/dashboard/AdminDashboard.kt)
- Update `DashboardHeader` to conditionally show branch picker.

#### [ReportsScreen.kt](file:///C:/Users/pritongisda/AndroidStudioProjects/Fruitylicious/Fruitylicious/app/src/main/java/com/example/fruitylicious/ui/admin/reports/ReportsScreen.kt)
- Since it has no ViewModel, I'll use a `hiltViewModel()` to get role info or pass it from `NavGraph`. (Actually, I'll add a simple ViewModel or pass it from `NavGraph`).

#### [SalesSummaryScreen.kt](file:///C:/Users/pritongisda/AndroidStudioProjects/Fruitylicious/Fruitylicious/app/src/main/java/com/example/fruitylicious/ui/admin/reports/SalesSummaryScreen.kt)
- Update `Header` to hide branch picker for staff.

#### [StaffLogScreen.kt](file:///C:/Users/pritongisda/AndroidStudioProjects/Fruitylicious/Fruitylicious/app/src/main/java/com/example/fruitylicious/ui/admin/staffmanagement/StaffLogScreen.kt)
- Update `Header` to hide branch picker for staff.

#### [AuditLogScreen.kt](file:///C:/Users/pritongisda/AndroidStudioProjects/Fruitylicious/Fruitylicious/app/src/main/java/com/example/fruitylicious/ui/admin/system/AuditLogScreen.kt)
- Update `Header` to hide branch picker for staff.

#### [ManageIngredientScreen.kt](file:///C:/Users/pritongisda/AndroidStudioProjects/Fruitylicious/Fruitylicious/app/src/main/java/com/example/fruitylicious/ui/admin/ingredient/ManageIngredientScreen.kt)
- Update `MiHeader` to hide branch picker for staff.

#### [ManageProductsScreen.kt](file:///C:/Users/pritongisda/AndroidStudioProjects/Fruitylicious/Fruitylicious/app/src/main/java/com/example/fruitylicious/ui/admin/products/ManageProductsScreen.kt)
- Update `Header` to hide branch picker for staff.

## Verification Plan

### Manual Verification
- Log in as an **Admin/Owner**.
    - Verify that the branch picker (B1, B2, All) is visible on all 12 screens.
    - Verify that switching branches updates the displayed data.
- Log in as a **Staff**.
    - Verify that the branch picker is **NOT visible** on any of the screens (especially shared ones like Inventory, Transaction History).
    - Verify that the data shown is restricted to the staff's assigned branch.
