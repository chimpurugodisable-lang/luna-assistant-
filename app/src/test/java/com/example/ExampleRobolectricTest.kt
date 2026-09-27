package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.intent.LunaCommandParser
import com.example.intent.LunaIntent
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class ExampleRobolectricTest {

    @Test
    fun `read string from context`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val appName = context.getString(R.string.app_name)
        assertEquals("LUNA", appName)
    }

    @Test
    fun `test command parser intents`() {
        val parser = LunaCommandParser()

        // Test app launch
        val appIntent = parser.parse("Luna, open WhatsApp Business")
        assertTrue(appIntent is LunaIntent.OpenApp)
        assertEquals("com.whatsapp.w4b", (appIntent as LunaIntent.OpenApp).suggestedPackage)

        // Test calling
        val callIntent = parser.parse("Hey Luna, call John")
        assertTrue(callIntent is LunaIntent.CallContact)
        assertEquals("John", (callIntent as LunaIntent.CallContact).query)

        // Test media control
        val mediaIntent = parser.parse("Luna, play music")
        assertTrue(mediaIntent is LunaIntent.MediaControl)
        assertEquals(LunaIntent.MediaAction.PLAY, (mediaIntent as LunaIntent.MediaControl).action)

        // Test web search
        val searchIntent = parser.parse("Luna, search for Computer Engineering scholarships in Canada")
        assertTrue(searchIntent is LunaIntent.WebSearch)
        assertEquals("Computer Engineering scholarships in Canada", (searchIntent as LunaIntent.WebSearch).query)
    }
}
