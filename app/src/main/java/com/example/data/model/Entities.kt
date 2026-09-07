package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "users")
data class User(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String,
    val mobile: String,
    val password: String = "123456",
    val role: String, // "ADMIN" or "SITE_INCHARGE"
    val assignedSiteId: Long? = null,
    val assignedSiteName: String? = null,
    val designation: String = "Site Incharge"
)

@Entity(tableName = "sites")
data class Site(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String,
    val code: String, // e.g. "PRJ-A"
    val location: String,
    val clientName: String,
    val budget: Double,
    val inchargeName: String,
    val mobile: String = "",
    val password: String = "123456"
)

@Entity(tableName = "transactions")
data class TransactionEntry(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val type: String, // "INCOME" or "EXPENSE"
    val dateMillis: Long,
    val dateFormatted: String, // "YYYY-MM-DD"
    val siteId: Long,
    val siteName: String,
    val userId: Long,
    val userName: String,
    val userMobile: String,
    val category: String,
    val subCategory: String,
    val partyName: String, // Vendor or Source
    val description: String,
    val amount: Double,
    val paymentMode: String, // "Cash", "Bank", "UPI", "Other"
    val receiptPhotoUri: String? = null,
    val remarks: String? = null,
    val createdAt: Long = System.currentTimeMillis()
)

data class CategoryMaster(
    val mainCategory: String,
    val subCategories: List<String>
)

object CategoryConstants {
    val INCOME_CATEGORIES = listOf(
        CategoryMaster(
            mainCategory = "Head Office",
            subCategories = listOf("HO se received amount", "Additional Budget Allocation", "Special Project Advance")
        ),
        CategoryMaster(
            mainCategory = "Loan Received",
            subCategories = listOf("Bank Loan", "Person Loan / Private Lender", "Director / Partner Advance")
        ),
        CategoryMaster(
            mainCategory = "Income – Machinery & Site",
            subCategories = listOf("Machinery hire income", "Site work income", "Scrap / Material Sale", "Other income")
        )
    )

    val EXPENSE_CATEGORIES = listOf(
        CategoryMaster(
            mainCategory = "Material Purchase",
            subCategories = listOf("Cement", "Sand", "Aggregate", "Bricks", "Steel", "Pipes", "Electrical Material")
        ),
        CategoryMaster(
            mainCategory = "Site Labour",
            subCategories = listOf("Mason", "Helper", "Carpenter", "Electrician", "Welder", "Labour Contractor")
        ),
        CategoryMaster(
            mainCategory = "Machinery & Equipment",
            subCategories = listOf(
                "JCB",
                "Excavator",
                "Backhoe Loader",
                "Bulldozer",
                "Wheel Loader",
                "Road Roller",
                "Vibratory Roller",
                "Motor Grader",
                "Crane",
                "Mobile Crane",
                "Tower Crane",
                "Hydra Crane",
                "Tractor",
                "Tractor Trolley",
                "Dumper",
                "Tipper",
                "Truck",
                "Mini Truck",
                "Pickup / Bolero Pickup",
                "Trailer",
                "Water Tanker",
                "Concrete Mixer",
                "Transit Mixer",
                "Concrete Pump",
                "Boom Pump",
                "Batching Plant",
                "Asphalt / Hot Mix Plant",
                "Paver Machine",
                "Bitumen Sprayer",
                "Stone Crusher",
                "Screening Machine",
                "DG Generator",
                "Air Compressor",
                "Welding Machine",
                "Cutting Machine",
                "Drilling Machine",
                "Borewell Machine",
                "Compactor",
                "Plate Compactor",
                "Power Trowel",
                "Bar Bending Machine",
                "Bar Cutting Machine",
                "Rebar Threading Machine",
                "Concrete Cutter",
                "Core Cutting Machine",
                "Vibrator Machine",
                "Dewatering Pump",
                "Submersible Pump",
                "Lift / Material Hoist",
                "Scaffolding Equipment",
                "Forklift",
                "Telehandler",
                "Manlift / Boom Lift",
                "Road Sweeper",
                "Crane Truck",
                "Garbage / Debris Vehicle",
                "Service Vehicle",
                "Company Vehicle",
                "Staff Transport Vehicle",
                "Other Construction Machinery"
            )
        ),

        CategoryMaster(
            mainCategory = "Transportation",
            subCategories = listOf("Truck", "Dumper", "Tractor", "Loading", "Unloading", "Freight")
        ),
        CategoryMaster(
            mainCategory = "Site Expenses",
            subCategories = listOf("Water", "Electricity", "Site Office", "Cleaning", "Security")
        ),
        CategoryMaster(
            mainCategory = "Tools & Consumables",
            subCategories = listOf("Welding Rod", "Cutting Disc", "Drill Bit", "Safety Items", "Small Tools")
        ),
        CategoryMaster(
            mainCategory = "Sub-Contractor",
            subCategories = listOf("Civil Work", "Electrical Work", "Road Work", "RCC Work", "Plumbing")
        ),
        CategoryMaster(
            mainCategory = "Staff Expenses",
            subCategories = listOf("Salary", "Advance Salary", "Travelling", "Food", "Accommodation")
        ),
        CategoryMaster(
            mainCategory = "Office Expenses",
            subCategories = listOf("Stationery", "Printing", "Telephone", "Internet", "Courier")
        ),
        CategoryMaster(
            mainCategory = "Mess",
            subCategories = listOf("Grocery", "Vegetables", "Gas", "Electricity", "Breakfast", "Lunch", "Dinner")
        ),
        CategoryMaster(
            mainCategory = "Miscellaneous",
            subCategories = listOf("Other Site Expenses")
        ),
        CategoryMaster(
            mainCategory = "Loan Return",
            subCategories = listOf("Bank / Person")
        )
    )

    val MACHINERY_EXPENSE_TYPES = listOf("Fuel", "Repairing", "Rent")

    val MACHINERY_LIST = listOf(
        "JCB",
        "Excavator",
        "Backhoe Loader",
        "Bulldozer",
        "Wheel Loader",
        "Road Roller",
        "Vibratory Roller",
        "Motor Grader",
        "Crane",
        "Mobile Crane",
        "Tower Crane",
        "Hydra Crane",
        "Tractor",
        "Tractor Trolley",
        "Dumper",
        "Tipper",
        "Truck",
        "Mini Truck",
        "Pickup / Bolero Pickup",
        "Trailer",
        "Water Tanker",
        "Concrete Mixer",
        "Transit Mixer",
        "Concrete Pump",
        "Boom Pump",
        "Batching Plant",
        "Asphalt / Hot Mix Plant",
        "Paver Machine",
        "Bitumen Sprayer",
        "Stone Crusher",
        "Screening Machine",
        "DG Generator",
        "Air Compressor",
        "Welding Machine",
        "Cutting Machine",
        "Drilling Machine",
        "Borewell Machine",
        "Compactor",
        "Plate Compactor",
        "Power Trowel",
        "Bar Bending Machine",
        "Bar Cutting Machine",
        "Rebar Threading Machine",
        "Concrete Cutter",
        "Core Cutting Machine",
        "Vibrator Machine",
        "Dewatering Pump",
        "Submersible Pump",
        "Lift / Material Hoist",
        "Scaffolding Equipment",
        "Forklift",
        "Telehandler",
        "Manlift / Boom Lift",
        "Road Sweeper",
        "Crane Truck",
        "Garbage / Debris Vehicle",
        "Service Vehicle",
        "Company Vehicle",
        "Staff Transport Vehicle",
        "Other Construction Machinery"
    )

    val PAYMENT_MODES = listOf("Cash", "Bank", "UPI", "Other")
}

@Entity(tableName = "staff_members")
data class StaffMember(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String,
    val mobile: String,
    val designation: String, // "Mason", "Helper", "Carpenter", "Supervisor", "Driver", "Electrician"
    val siteId: Long,
    val siteName: String,
    val dailyWage: Double,
    val paymentType: String = "DAILY" // "DAILY" or "MONTHLY"
)

@Entity(
    tableName = "staff_attendance",
    indices = [androidx.room.Index(value = ["staffId", "dateFormatted", "siteId"], unique = true)]
)
data class StaffAttendance(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val staffId: Long,
    val staffName: String,
    val siteId: Long,
    val siteName: String,
    val dateMillis: Long,
    val dateFormatted: String, // "yyyy-MM-dd"
    val status: String, // "PRESENT", "ABSENT", "HALF_DAY", "LEAVE"
    val wageAmount: Double,
    val overtimeHours: Double = 0.0,
    val checkInTime: String? = null,
    val checkOutTime: String? = null,
    val remarks: String? = null
)

@Entity(tableName = "salary_records")
data class SalaryRecord(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val staffId: Long,
    val staffName: String,
    val siteId: Long,
    val siteName: String,
    val monthYear: String, // "YYYY-MM"
    val basicSalary: Double,
    val presentDays: Int,
    val absentDays: Int,
    val halfDays: Int,
    val leaveDays: Int,
    val overtimeHours: Double,
    val overtimeAmount: Double,
    val bonus: Double,
    val advance: Double,
    val deduction: Double,
    val netSalary: Double,
    val paymentStatus: String, // "PENDING", "PARTIAL", "PAID", "APPROVED"
    val paymentMode: String = "Cash", // "Cash", "Bank", "UPI"
    val paidDate: String? = null,
    val remarks: String? = null
)

@Entity(tableName = "audit_logs")
data class AuditLog(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val timestamp: Long = System.currentTimeMillis(),
    val userName: String,
    val action: String,
    val details: String,
    val siteId: Long = 0
)


