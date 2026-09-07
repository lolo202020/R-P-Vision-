package com.example

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onRoot
import com.example.data.AppDatabase
import com.example.data.repository.ConstructionRepository
import com.example.ui.screens.auth.LoginScreen
import com.example.ui.theme.SiteKhataTheme
import com.example.ui.viewmodel.ConstructionViewModel
import com.github.takahirom.roborazzi.RobolectricDeviceQualifiers
import com.github.takahirom.roborazzi.captureRoboImage
import kotlinx.coroutines.test.TestScope
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = RobolectricDeviceQualifiers.Pixel8, sdk = [34])
class GreetingScreenshotTest {

    @get:Rule val composeTestRule = createComposeRule()

    @Test
    fun login_screen_screenshot() {
        val testScope = TestScope()
        val context = androidx.test.core.app.ApplicationProvider.getApplicationContext<android.content.Context>()
        val db = AppDatabase.getDatabase(context, testScope)
        val repo = ConstructionRepository(db.appDao())
        val viewModel = ConstructionViewModel(repo)

        composeTestRule.setContent {
            SiteKhataTheme {
                LoginScreen(
                    viewModel = viewModel,
                    onLoginSuccess = {}
                )
            }
        }

        composeTestRule.onRoot().captureRoboImage(filePath = "src/test/screenshots/login_screen.png")
    }
}
