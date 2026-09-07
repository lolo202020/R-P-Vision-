package com.example.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.data.dao.AppDao
import com.example.data.model.SalaryRecord
import com.example.data.model.AuditLog
import com.example.data.model.Site
import com.example.data.model.StaffAttendance
import com.example.data.model.StaffMember
import com.example.data.model.TransactionEntry
import com.example.data.model.User
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

@Database(
    entities = [User::class, Site::class, TransactionEntry::class, StaffMember::class, StaffAttendance::class, SalaryRecord::class, AuditLog::class],
    version = 8,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {

    abstract fun appDao(): AppDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context, scope: CoroutineScope): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "construction_khata_db"
                )
                    .fallbackToDestructiveMigration()
                    .addCallback(DatabaseCallback(scope))
                    .build()
                INSTANCE = instance
                instance
            }
        }

        private class DatabaseCallback(
            private val scope: CoroutineScope
        ) : RoomDatabase.Callback() {
            override fun onCreate(db: SupportSQLiteDatabase) {
                super.onCreate(db)
                INSTANCE?.let { database ->
                    scope.launch(Dispatchers.IO) {
                        populateInitialData(database.appDao())
                    }
                }
            }
        }

        suspend fun populateInitialData(dao: AppDao) {
            // Seed Active Project Sites
            val sites = listOf(
                Site(
                    id = 1,
                    name = "Metro City Tower",
                    code = "PRJ-A",
                    location = "Sector 62, Noida",
                    clientName = "Apex Realty Ltd.",
                    budget = 8500000.0,
                    inchargeName = "Ramesh Kumar",
                    mobile = "9876500002",
                    password = "123"
                ),
                Site(
                    id = 2,
                    name = "NH-48 Highway Bypass",
                    code = "PRJ-B",
                    location = "Gurugram Expressway",
                    clientName = "National Highways Authority",
                    budget = 14500000.0,
                    inchargeName = "Suresh Patel",
                    mobile = "9876500003",
                    password = "123"
                ),
                Site(
                    id = 3,
                    name = "Riverfront Commercial Hub",
                    code = "PRJ-C",
                    location = "Civil Lines, Kanpur",
                    clientName = "Riverview Infotech",
                    budget = 6200000.0,
                    inchargeName = "Amit Sharma",
                    mobile = "9876500004",
                    password = "123"
                )
            )
            dao.insertSites(sites)

            // Seed Authorized System Personnel (Admin + Site Incharges)
            val users = listOf(
                User(
                    id = 1,
                    name = "Admin (Director)",
                    mobile = "9621803006",
                    password = "80808080",
                    role = "ADMIN",
                    assignedSiteId = null,
                    assignedSiteName = null,
                    designation = "Managing Director / Admin"
                ),
                User(
                    id = 2,
                    name = "Ramesh Kumar",
                    mobile = "9876500002",
                    password = "123",
                    role = "SITE_INCHARGE",
                    assignedSiteId = 1,
                    assignedSiteName = "Metro City Tower",
                    designation = "Senior Site Engineer"
                ),
                User(
                    id = 3,
                    name = "Suresh Patel",
                    mobile = "9876500003",
                    password = "123",
                    role = "SITE_INCHARGE",
                    assignedSiteId = 2,
                    assignedSiteName = "NH-48 Highway Bypass",
                    designation = "Project Incharge"
                ),
                User(
                    id = 4,
                    name = "Amit Sharma",
                    mobile = "9876500004",
                    password = "123",
                    role = "SITE_INCHARGE",
                    assignedSiteId = 3,
                    assignedSiteName = "Riverfront Commercial Hub",
                    designation = "Site Supervisor"
                )
            )
            dao.insertUsers(users)

            // Clean Production Setup:
            // No dummy transactions seeded.
            // No dummy staff attendance seeded.
            // No dummy staff members seeded.
            // App is ready for live operational entries.
        }
    }
}
