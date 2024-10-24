package io.piano.flutter.piano_analytics

import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkObject
import io.mockk.slot
import io.mockk.verify
import io.piano.android.analytics.Configuration
import io.piano.android.analytics.PianoAnalytics
import io.piano.android.analytics.model.Event
import io.piano.android.analytics.model.PrivacyMode
import io.piano.android.analytics.model.PrivacyStorageFeature
import io.piano.android.analytics.model.Property
import io.piano.android.analytics.model.PropertyName
import io.piano.android.analytics.model.VisitorIDType
import java.util.Date
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals

internal class PianoAnalyticsPluginTest: BasePluginTest() {

    @BeforeTest
    fun setUp() {
        mockkObject(PianoAnalytics.Companion)
    }

    @Test
    fun `Check init`() {
        every { PianoAnalytics.Companion.init(any(), any(), any(), any()) } returns mockk()

        call("init", mapOf(
          "site" to 123456789,
          "collectDomain" to "xxxxxxx.pa-cd.com",
          "visitorIDType" to "UUID"
        ))

        val slot = slot<Configuration>()
        verify { PianoAnalytics.init(any(), capture(slot), any(), any()) }

        val configuration = slot.captured
        assertEquals(123456789, configuration.site)
        assertEquals("xxxxxxx.pa-cd.com", configuration.collectDomain)
        assertEquals(VisitorIDType.UUID, configuration.visitorIDType)
    }

    @Test
    fun `Check send`() {
        val pianoAnalytics: PianoAnalytics = mockk()
        every { PianoAnalytics.Companion.getInstance() } returns pianoAnalytics
        every { pianoAnalytics.sendEvents(any()) } returns Unit

        call("send", mapOf(
            "events" to listOf(
                mapOf(
                    "name" to "page.display",
                    "data" to mapOf(
                        "bool" to mapOf("value" to true),
                        "bool_force" to mapOf("value" to "true", "forceType" to "b"),
                        "int" to mapOf("value" to 1),
                        "int_force" to mapOf("value" to "1", "forceType" to "n"),
                        "long" to mapOf("value" to 1L),
                        "double" to mapOf("value" to 1.0),
                        "double_force" to mapOf("value" to "1.0", "forceType" to "f"),
                        "string" to mapOf("value" to "value"),
                        "string_force" to mapOf("value" to 1, "forceType" to "s"),
                        "date" to mapOf("value" to Date(0)),
                        "intArray" to mapOf("value" to listOf(1, 2, 3)),
                        "intArray_force" to mapOf("value" to listOf("1", "2", "3"), "forceType" to "a:n"),
                        "doubleArray" to mapOf("value" to listOf(1.0, 2.0, 3.0)),
                        "doubleArray_force" to mapOf("value" to listOf("1.0", "2.0", "3.0"), "forceType" to "a:f"),
                        "stringArray" to mapOf("value" to listOf("a", "b", "c")),
                        "stringArray_force" to mapOf("value" to listOf(1, 2, 3), "forceType" to "a:s")
                    )
                )
            )
        ))

        val slot = slot<Event>()
        verify { pianoAnalytics.sendEvents(capture(slot)) }

        val event = slot.captured
        assertEquals("page.display", event.name)
        assertEquals(true, event.properties.valueOf("bool"))
        assertEquals(1, event.properties.valueOf("int"))
        assertEquals(1L, event.properties.valueOf("long"))
        assertEquals(1.0, event.properties.valueOf("double"))
        assertEquals("value", event.properties.valueOf("string"))

        assertEquals(
            Property(PropertyName("date"), Date(0)).value,
            event.properties.valueOf("date")
        )

        assertEquals(setOf(1, 2, 3), event.properties.setOf("intArray"))
        assertEquals(setOf(1.0, 2.0, 3.0), event.properties.setOf("doubleArray"))
        assertEquals(setOf("a", "b", "c"), event.properties.setOf("stringArray"))

        assertEquals("true", event.properties.valueOf("bool_force"))
        assertEquals(Property.Type.BOOLEAN, event.properties.propertyOf("bool_force")?.forceType)

        assertEquals("1", event.properties.valueOf("int_force"))
        assertEquals(Property.Type.INTEGER, event.properties.propertyOf("int_force")?.forceType)

        assertEquals("1.0", event.properties.valueOf("double_force"))
        assertEquals(Property.Type.FLOAT, event.properties.propertyOf("double_force")?.forceType)

        assertEquals(1, event.properties.valueOf("string_force"))
        assertEquals(Property.Type.STRING, event.properties.propertyOf("string_force")?.forceType)

        assertEquals(setOf("1", "2", "3"), event.properties.setOf("intArray_force"))
        assertEquals(Property.Type.INTEGER_ARRAY, event.properties.propertyOf("intArray_force")?.forceType)

        assertEquals(setOf("1.0", "2.0", "3.0"), event.properties.setOf("doubleArray_force"))
        assertEquals(Property.Type.FLOAT_ARRAY, event.properties.propertyOf("doubleArray_force")?.forceType)

        assertEquals(setOf(1, 2, 3), event.properties.setOf("stringArray_force"))
        assertEquals(Property.Type.STRING_ARRAY, event.properties.propertyOf("stringArray_force")?.forceType)
    }

    @Test
    fun `Check privacyChangeStorageFeatures`() {
        mockkObject(PrivacyMode.EXEMPT)
        mockkObject(PrivacyMode.CUSTOM)

        val allowedStorageFeatures = mutableSetOf<PrivacyStorageFeature>()
        val forbiddenStorageFeatures = mutableSetOf<PrivacyStorageFeature>()
        every { PrivacyMode.EXEMPT.allowedStorageFeatures } returns allowedStorageFeatures
        every { PrivacyMode.CUSTOM.forbiddenStorageFeatures } returns forbiddenStorageFeatures

        call("privacyIncludeStorageFeatures", mapOf(
            "features" to listOf("CRASH"),
            "modes" to listOf("exempt")
        ))

        assertEquals(setOf(PrivacyStorageFeature.CRASH), allowedStorageFeatures)

        call("privacyExcludeStorageFeatures", mapOf(
            "features" to listOf("LIFECYCLE"),
            "modes" to listOf("custom")
        ))

        assertEquals(setOf(PrivacyStorageFeature.LIFECYCLE), forbiddenStorageFeatures)
    }

    @Test
    fun `Check privacyChangeProperty`() {
        mockkObject(PrivacyMode.EXEMPT)
        mockkObject(PrivacyMode.CUSTOM)

        val allowedPropertyKeys = mutableMapOf(
            Event.PAGE_DISPLAY to mutableSetOf(
                PropertyName("allowed_property_1"),
                PropertyName("allowed_property_3"),
            )
        )
        val forbiddenPropertyKeys = mutableMapOf(
            Event.ANY to mutableSetOf(
                PropertyName("forbidden_property_1"),
                PropertyName("forbidden_property_3"),
            )
        )
        every { PrivacyMode.EXEMPT.allowedPropertyKeys } returns allowedPropertyKeys
        every { PrivacyMode.CUSTOM.forbiddenPropertyKeys } returns forbiddenPropertyKeys

        call("privacyIncludeProperties", mapOf(
            "propertyNames" to listOf("allowed_property_1", "allowed_property_2"),
            "modes" to listOf("exempt"),
            "eventNames" to listOf(Event.PAGE_DISPLAY)
        ))

        assertEquals(1, allowedPropertyKeys.count())
        assertEquals(
            mutableSetOf(
                PropertyName("allowed_property_1"),
                PropertyName("allowed_property_2"),
                PropertyName("allowed_property_3"),
            ),
            allowedPropertyKeys[Event.PAGE_DISPLAY]
        )

        call("privacyExcludeProperties", mapOf(
            "propertyNames" to listOf("forbidden_property_1", "forbidden_property_2"),
            "modes" to listOf("custom")
        ))

        assertEquals(1, forbiddenPropertyKeys.count())
        assertEquals(
            mutableSetOf(
                PropertyName("forbidden_property_1"),
                PropertyName("forbidden_property_2"),
                PropertyName("forbidden_property_3"),
            ),
            forbiddenPropertyKeys[Event.ANY]
        )
    }

    @Test
    fun `Check privacyChangeEvents`() {
        mockkObject(PrivacyMode.EXEMPT)
        mockkObject(PrivacyMode.CUSTOM)

        val allowedEventNames = mutableSetOf(Event.PAGE_DISPLAY, Event.ANY)
        val forbiddenEventNames = mutableSetOf(Event.PAGE_DISPLAY, Event.ANY)
        every { PrivacyMode.EXEMPT.allowedEventNames } returns allowedEventNames
        every { PrivacyMode.CUSTOM.forbiddenEventNames } returns forbiddenEventNames

        call("privacyIncludeEvents", mapOf(
            "eventNames" to listOf(Event.PAGE_DISPLAY, Event.CLICK_ACTION),
            "modes" to listOf("exempt")
        ))

        assertEquals(3, allowedEventNames.count())
        assertEquals(allowedEventNames, setOf(Event.PAGE_DISPLAY, Event.ANY, Event.CLICK_ACTION))

        call("privacyExcludeEvents", mapOf(
            "eventNames" to listOf(Event.PAGE_DISPLAY, Event.CLICK_ACTION),
            "modes" to listOf("custom")
        ))

        assertEquals(3, forbiddenEventNames.count())
        assertEquals(forbiddenEventNames, setOf(Event.PAGE_DISPLAY, Event.ANY, Event.CLICK_ACTION))
    }

    private fun Set<Property>.propertyOf(name: String) = this.firstOrNull { it.name.key == name }
    private fun Set<Property>.valueOf(name: String) = propertyOf(name)?.value
    private fun Set<Property>.setOf(name: String) = (valueOf(name) as? Array<*>)?.toSet()

    private fun call(method: String, parameters: Map<String, Any>? = null) {
        return call(method, parameters) { PianoAnalyticsPlugin() }
    }
}
