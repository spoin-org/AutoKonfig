package dev.nohus.autokonfig

import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.io.File
import java.util.*
import kotlin.reflect.jvm.isAccessible

/**
 * Created by Marcin Wisniowski (Nohus) on 05/01/2020.
 */

class AutoKonfigTest {

    private val file = File("test.conf")

    private fun String.createConfigFile() {
        file.writeText(this)
        resetDefaultAutoKonfig()
    }

    private fun String.createAutoKonfig(): AutoKonfig {
        createConfigFile()
        return AutoKonfig().withConfig(file)
    }

    private fun resetDefaultAutoKonfig() {
        DefaultAutoKonfig
            .clear()
            .withSystemProperties()
            .withEnvironmentVariables()
            .withConfigs(ConfigFileLocator().getConfigFiles())
    }

    @AfterEach
    fun tearDown() {
        file.delete()
    }

    @Test
    fun `setting can be read`() {
        val config = "setting = test".createAutoKonfig()
        val setting by config.StringSetting()
        assertEquals("test", setting)
    }

    @Test
    fun `multiple settings can be read`() {
        val config = """
            foo = abc
            bar = def
            baz = ghi
        """.trimIndent().createAutoKonfig()
        val foo by config.StringSetting()
        val baz by config.StringSetting()
        assertEquals("abc", foo)
        assertEquals("ghi", baz)
    }

    @Test
    fun `multiple settings can be read from default config`() {
        """
            foo = abc
            bar = def
            baz = ghi
        """.trimIndent().createConfigFile()
        val foo by StringSetting()
        val baz by StringSetting()
        assertEquals("abc", foo)
        assertEquals("ghi", baz)
    }

    @Test
    fun `keys are case-insensitive`() {
        """
            FOO = abc
            bar = DEF
        """.trimIndent().createConfigFile()
        val foo by StringSetting()
        val bar by StringSetting()
        val a by StringSetting(name = "foo")
        val b by StringSetting(name = "Bar")
        val c by StringSetting(name = "fOo")
        assertEquals("abc", foo)
        assertEquals("DEF", bar)
        assertEquals("abc", a)
        assertEquals("DEF", b)
        assertEquals("abc", c)
    }

    @Test
    fun `keys with different casing types are matched`() {
        """
            foo-bar = 5
            TEST_DATA = 4
        """.trimIndent().createConfigFile()
        val a by IntSetting(name = "foo-bar")
        val fooBar by IntSetting()
        val b by IntSetting(name = "TEST_DATA")
        val test_data by IntSetting()
        val testData by IntSetting()
        assertEquals(5, a)
        assertEquals(5, fooBar)
        assertEquals(4, b)
        assertEquals(4, test_data)
        assertEquals(4, testData)
    }

    @Test
    fun `keys can have custom names`() {
        """
            foo = abc
        """.trimIndent().createConfigFile()
        val bar by StringSetting(name = "foo")
        assertEquals("abc", bar)
    }

    @Test
    fun `multiple variables be delegated to the same setting`() {
        """
            foo = abc
        """.trimIndent().createConfigFile()
        val foo by StringSetting()
        val bar by StringSetting(name = "foo")
        assertEquals("abc", foo)
        assertEquals("abc", bar)
    }

    @Test
    fun `setting can be read from system properties`() {
        val key = "unit.test.system.property"
        val value = "value"
        System.setProperty(key, value)
        resetDefaultAutoKonfig()
        val setting by StringSetting(name = key)
        assertEquals(value, setting)
    }

    @Test
    fun `setting can be read from environment variables`() {
        val key = "unit.test.environment.variable"
        val value = "value"
        setEnvironmentVariable(key, value)
        resetDefaultAutoKonfig()
        val setting by StringSetting(name = key)
        assertEquals(value, setting)
    }

    private fun setEnvironmentVariable(key: String, value: String) {
        val environment = System.getenv()
        val field = environment::class.members.first { it.name == "m" }
        field.isAccessible = true
        @Suppress("UNCHECKED_CAST")
        (field.call(environment) as MutableMap<String, String>)[key] = value
    }

    @Test
    fun `setting can be read from config file in resources`() {
        val config = AutoKonfig().withResourceConfig("resource.properties")
        val setting by config.StringSetting()
        assertEquals("resource", setting)
    }

    @Test
    fun `setting can be read from config file by URL`() {
        """
            setting = test
        """.trimIndent().createConfigFile()
        AutoKonfig.clear().withURLConfig(file.toURI().toString())
        val setting by StringSetting()
        assertEquals("test", setting)
    }

    @Test
    fun `setting can be read from command line arguments`() {
        val config = AutoKonfig().withCommandLineArguments(arrayOf("-a", "b", "-c"))
        val a by config.StringSetting()
        val c by config.BooleanSetting()
        assertEquals("b", a)
        assertTrue(c)
    }

    @Test
    fun `boolean settings can be read`() {
        """
            a = true
            b = yes
            c = on
            d = 1
            e = false
            f = no
            g = off
            h = 0
        """.trimIndent().createConfigFile()
        val a by BooleanSetting()
        val b by BooleanSetting()
        val c by BooleanSetting()
        val d by BooleanSetting()
        val e by BooleanSetting()
        val f by BooleanSetting()
        val g by BooleanSetting()
        val h by BooleanSetting()
        assertTrue(a)
        assertTrue(b)
        assertTrue(c)
        assertTrue(d)
        assertFalse(e)
        assertFalse(f)
        assertFalse(g)
        assertFalse(h)
    }

    @Test
    fun `flag settings are false by default`() {
        """
            a = true
        """.trimIndent().createConfigFile()
        val a by FlagSetting()
        val b by FlagSetting()
        assertTrue(a)
        assertFalse(b)
    }

    @Test
    fun `settings of all types can be read`() {
        """
            string = hello
            int = 10
            long = 3000000000
            float = 3.14
            double = 3.1415
            boolean = false
            flag = true
        """.trimIndent().createConfigFile()
        val string by StringSetting()
        val int by IntSetting()
        val long by LongSetting()
        val float by FloatSetting()
        val double by DoubleSetting()
        val boolean by BooleanSetting()
        val flag by FlagSetting()
        assertEquals("hello", string)
        assertEquals(10, int)
        assertEquals(3000000000, long)
        assertEquals(3.14f, float)
        assertEquals(3.1415, double)
        assertFalse(boolean)
        assertTrue(flag)
    }

    object TypesGroup : Group() {
        val string by StringSetting()
        val int by IntSetting()
        val long by LongSetting()
        val float by FloatSetting()
        val double by DoubleSetting()
        val boolean by BooleanSetting()
        val flag by FlagSetting()
    }

    @Test
    fun `settings of all types can be read in a group`() {
        """
            typesGroup.string = hello
            typesGroup.int = 10
            typesGroup.long = 3000000000
            typesGroup.float = 3.14
            typesGroup.double = 3.1415
            typesGroup.boolean = false
            typesGroup.flag = true
        """.trimIndent().createConfigFile()
        assertEquals("hello", TypesGroup.string)
        assertEquals(10, TypesGroup.int)
        assertEquals(3000000000, TypesGroup.long)
        assertEquals(3.14f, TypesGroup.float)
        assertEquals(3.1415, TypesGroup.double)
        assertFalse(TypesGroup.boolean)
        assertTrue(TypesGroup.flag)
    }

    @Test
    fun `settings of all types can be read directly`() {
        """
            string = hello
            int = 10
            long = 3000000000
            float = 3.14
            double = 3.1415
            boolean = false
            flag = true
        """.trimIndent().createConfigFile()
        assertEquals("hello", AutoKonfig.getString("string"))
        assertEquals(10, AutoKonfig.getInt("int"))
        assertEquals(3000000000, AutoKonfig.getLong("long"))
        assertEquals(3.14f, AutoKonfig.getFloat("float"))
        assertEquals(3.1415, AutoKonfig.getDouble("double"))
        assertFalse(AutoKonfig.getBoolean("boolean"))
        assertTrue(AutoKonfig.getFlag("flag"))
    }

    @Test
    fun `nonexistent setting throws an exception`() {
        "".createConfigFile()
        val exception = assertThrows<AutoKonfigException> {
            val nonexistent by StringSetting()
        }
        assertEquals("Required key \"nonexistent\" is missing", exception.message)
    }

    @Test
    fun `nonexistent config file throws an exception`() {
        val file = File("nonexistent")
        val exception = assertThrows<AutoKonfigException> {
            AutoKonfig().withConfig(file)
        }
        assertEquals("Failed to read file: ${file.normalize().absolutePath}", exception.message)
    }

    @Test
    fun `nonexistent resources config file throws an exception`() {
        val exception = assertThrows<AutoKonfigException> {
            AutoKonfig().withResourceConfig("nonexistent")
        }
        assertEquals("Failed to read resource: nonexistent", exception.message)
    }

    @Test
    fun `settings can be read from multiple files`() {
        val config = AutoKonfig().withConfigs(
            File("src/test/resources/test/multiple/5.properties"),
            File("src/test/resources/test/multiple/6.conf")
        )
        val foo by config.StringSetting()
        val bar by config.StringSetting()
        assertEquals("abc", foo)
        assertEquals("def", bar)
    }

    object groupA : Group() {
        object subgroup : Group() {
            val setting by StringSetting("")
        }
    }

    @Test
    fun `setting can be read in a group`() {
        """
            groupA.subgroup.setting = test
        """.trimIndent().createConfigFile()
        assertEquals("test", groupA.subgroup.setting)
    }

    object groupB : Group("outer") {
        object subgroup : Group("inner") {
            val setting by StringSetting(name = "key")
        }
    }

    @Test
    fun `setting can be read in a group with custom names`() {
        """
            outer.inner.key = test
        """.trimIndent().createConfigFile()
        assertEquals("test", groupB.subgroup.setting)
    }

    object groupC : Group("outer") {
        object subgroup : Group() {
            val setting by StringSetting()
        }
    }

    @Test
    fun `setting can be read in a group with some custom names`() {
        """
            outer.subgroup.setting = test
        """.trimIndent().createConfigFile()
        assertEquals("test", groupC.subgroup.setting)
    }

    @Test
    fun `wrong type of setting for value throws an exception`() {
        """
            foo = test
        """.trimIndent().createConfigFile()
        val exception = assertThrows<AutoKonfigException> {
            val a by IntSetting(name = "foo")
        }
        assertEquals("Failed to parse setting \"foo\", the value is \"test\", but must be an Int number", exception.message)
    }

    @Test
    fun `getAll returns all settings`() {
        """
            a = 1
            b = 2
        """.trimIndent().createConfigFile()
        AutoKonfig.clear().withConfig(file)
        assertEquals(mapOf(
            "a" to "1",
            "b" to "2"
        ), AutoKonfig.getAll())
    }

    @Test
    fun `setting can be traced to a file`() {
        """
            foo = 2
        """.trimIndent().createConfigFile()
        AutoKonfig.clear().withConfig(file)
        assertEquals("Key \"foo\" was read from config file at \"${file.normalize().absolutePath}\"", AutoKonfig.getKeySource("foo"))
    }

    @Test
    fun `setting with a fuzzy matched key can be traced to a file`() {
        """
            SERVER_PORT = 2
        """.trimIndent().createConfigFile()
        AutoKonfig.clear().withConfig(file)
        assertEquals("Key \"serverPort\" was read as \"SERVER_PORT\" from config file at \"${file.normalize().absolutePath}\"",
            AutoKonfig.getKeySource("serverPort"))
    }

    @Test
    fun `setting can be traced to a resource file`() {
        val config = AutoKonfig.clear().withResourceConfig("resource.properties")
        assertEquals("Key \"setting\" was read from config file resource at \"resource.properties\"", config.getKeySource("setting"))
    }

    @Test
    fun `setting can be traced to environment variables`() {
        val key = "unit.test.environment.variable"
        val value = "value"
        setEnvironmentVariable(key, value)
        resetDefaultAutoKonfig()
        assertEquals("Key \"unit.test.environment.variable\" was read from environment variables", AutoKonfig.getKeySource(key))
    }

    @Test
    fun `setting can be traced to system properties`() {
        val key = "unit.test.system.property"
        val value = "value"
        System.setProperty(key, value)
        resetDefaultAutoKonfig()
        assertEquals("Key \"unit.test.system.property\" was read from system properties", AutoKonfig.getKeySource(key))
    }

    @Test
    fun `setting can be traced to command line arguments`() {
        AutoKonfig.clear().withCommandLineArguments(arrayOf("-a", "b"))
        assertEquals("Key \"a\" was read from command line parameters", AutoKonfig.getKeySource("a"))
    }

    @Test
    fun `setting can be traced to manually inserted properties`() {
        AutoKonfig.clear().withProperties(Properties().apply { put("a", "b") })
        assertTrue(AutoKonfig.getKeySource("a").startsWith("Key \"a\" was read from properties inserted by org.junit"))
    }

    @Test
    fun `setting can be traced to manually inserted map`() {
        AutoKonfig.clear().withMap(mapOf("a" to "b"))
        assertTrue(AutoKonfig.getKeySource("a").startsWith("Key \"a\" was read from a map inserted by org.junit"))
    }

    private enum class Letters {
        Alpha, Beta
    }

    private object EnumGroup : Group() {
        val setting by EnumSetting(Letters::class)
        val settingJava by EnumSetting(Letters::class.java, name = "setting")
    }

    @Test
    fun `enum settings can be read`() {
        """
            EnumGroup.setting = Alpha
        """.trimIndent().createConfigFile()
        assertEquals(Letters.Alpha, EnumGroup.setting)
        assertEquals(Letters.Alpha, EnumGroup.settingJava)
    }

    @Test
    fun `enum settings can be read directly`() {
        """
            setting = Alpha
        """.trimIndent().createConfigFile()
        assertEquals(Letters.Alpha, AutoKonfig.getEnum(Letters::class, "setting"))
        assertEquals(Letters.Alpha, AutoKonfig.getEnum(Letters::class.java, "setting"))
    }

    @Test
    fun `enum settings are case-insensitive`() {
        """
            EnumGroup.setting = beTA
        """.trimIndent().createConfigFile()
        assertEquals(Letters.Beta, EnumGroup.setting)
        assertEquals(Letters.Beta, EnumGroup.settingJava)
    }

    @Test
    fun `invalid enum value throws an exception`() {
        """
            setting = Gamma
        """.trimIndent().createConfigFile()
        val exception = assertThrows<AutoKonfigException> {
            val setting by EnumSetting(Letters::class)
        }
        val exceptionJava = assertThrows<AutoKonfigException> {
            val settingJava by EnumSetting(Letters::class.java, name = "setting")
        }
        assertEquals("Failed to parse setting \"setting\", the value is \"Gamma\", but possible values are [Alpha, Beta]", exception.message)
        assertEquals("Failed to parse setting \"setting\", the value is \"Gamma\", but possible values are [Alpha, Beta]", exceptionJava.message)
    }

    private object Temporal : Group() {
        val instant by InstantSetting()
        val duration by DurationSetting()
        val localTime by LocalTimeSetting()
        val localDate by LocalDateSetting()
        val localDateTime by LocalDateTimeSetting()
    }

    @Test
    fun `temporal settings can be read in a group`() {
        """
            temporal.instant = 2011-12-03T10:15:30Z
            temporal.duration = PT20.345S
            temporal.local-time = 10:15:30
            temporal.local-date = 2020-01-09
            temporal.local-date-time = 2020-01-09T10:15:30
        """.trimIndent().createConfigFile()
        assertEquals("2011-12-03T10:15:30Z", Temporal.instant.toString())
        assertEquals("PT20.345S", Temporal.duration.toString())
        assertEquals("10:15:30", Temporal.localTime.toString())
        assertEquals("2020-01-09", Temporal.localDate.toString())
        assertEquals("2020-01-09T10:15:30", Temporal.localDateTime.toString())
    }

    @Test
    fun `temporal settings can be read`() {
        """
            instant = 2011-12-03T10:15:30Z
            duration = PT20.345S
            local-time = 10:15:30
            local-date = 2020-01-09
            local-date-time = 2020-01-09T10:15:30
        """.trimIndent().createConfigFile()
        val instant by InstantSetting()
        val duration by DurationSetting()
        val localTime by LocalTimeSetting()
        val localDate by LocalDateSetting()
        val localDateTime by LocalDateTimeSetting()
        assertEquals("2011-12-03T10:15:30Z", instant.toString())
        assertEquals("PT20.345S", duration.toString())
        assertEquals("10:15:30", localTime.toString())
        assertEquals("2020-01-09", localDate.toString())
        assertEquals("2020-01-09T10:15:30", localDateTime.toString())
    }

    @Test
    fun `temporal settings can be read directly`() {
        """
            instant = 2011-12-03T10:15:30Z
            duration = PT20.345S
            local-time = 10:15:30
            local-date = 2020-01-09
            local-date-time = 2020-01-09T10:15:30
        """.trimIndent().createConfigFile()
        assertEquals("2011-12-03T10:15:30Z", AutoKonfig.getInstant("instant").toString())
        assertEquals("PT20.345S", AutoKonfig.getDuration("duration").toString())
        assertEquals("10:15:30", AutoKonfig.getLocalTime("local-time").toString())
        assertEquals("2020-01-09", AutoKonfig.getLocalDate("local-date").toString())
        assertEquals("2020-01-09T10:15:30", AutoKonfig.getLocalDateTime("local-date-time").toString())
    }

    @Test
    fun `invalid temporal values throw exceptions`() {
        """
            instant = invalid
            duration = invalid
            local-time = invalid
            local-date = invalid
            local-date-time = invalid
        """.trimIndent().createConfigFile()
        var exception: AutoKonfigException = assertThrows {
            val instant by InstantSetting()
        }
        assertEquals("Failed to parse setting \"instant\", the value is \"invalid\", but must be an Instant", exception.message)
        exception = assertThrows {
            val duration by DurationSetting()
        }
        assertEquals("Failed to parse setting \"duration\", the value is \"invalid\", but must be a Duration", exception.message)
        exception = assertThrows {
            val localTime by LocalTimeSetting()
        }
        assertEquals("Failed to parse setting \"localTime\", the value is \"invalid\", but must be a LocalTime", exception.message)
        exception = assertThrows {
            val localDate by LocalDateSetting()
        }
        assertEquals("Failed to parse setting \"localDate\", the value is \"invalid\", but must be a LocalDate", exception.message)
        exception = assertThrows {
            val localDateTime by LocalDateTimeSetting()
        }
        assertEquals("Failed to parse setting \"localDateTime\", the value is \"invalid\", but must be a LocalDateTime", exception.message)
    }

    object Collections : Group() {
        val strings by ListSetting(StringSettingType)
        val strings2 by ListSetting(StringSettingType, ",", name = "strings")
        val strings3 by ListSetting(StringSettingType, Regex(","), name = "strings")
        val numbers by SetSetting(IntSettingType)
        val numbers2 by SetSetting(IntSettingType, ",", name = "numbers")
        val numbers3 by SetSetting(IntSettingType, Regex(","), name = "numbers")
    }

    @Test
    fun `list settings can be read in a group`() {
        """
            collections.strings = a,b,c
            collections.numbers = 1,2,3,2,1
        """.trimIndent().createConfigFile()
        assertEquals(listOf("a", "b", "c"), Collections.strings)
        assertEquals(listOf("a", "b", "c"), Collections.strings2)
        assertEquals(listOf("a", "b", "c"), Collections.strings3)
        assertEquals(setOf(1, 2, 3), Collections.numbers)
        assertEquals(setOf(1, 2, 3), Collections.numbers2)
        assertEquals(setOf(1, 2, 3), Collections.numbers3)
    }

    @Test
    fun `list settings can be read`() {
        """
            strings = a,b,c
            numbers = 1,2,3
        """.trimIndent().createConfigFile()
        val strings by ListSetting(StringSettingType)
        val numbers by SetSetting(IntSettingType)
        assertEquals(listOf("a", "b", "c"), strings)
        assertEquals(setOf(1, 2, 3), numbers)
    }

    @Test
    fun `list settings can be read directly`() {
        """
            strings = a,b,c
            numbers = 1,2,3
        """.trimIndent().createConfigFile()
        assertEquals(listOf("a", "b", "c"), AutoKonfig.getList(StringSettingType, "strings"))
        assertEquals(listOf("a", "b", "c"), AutoKonfig.getList(StringSettingType, ",", "strings"))
        assertEquals(listOf("a", "b", "c"), AutoKonfig.getList(StringSettingType, Regex(","), "strings"))
        assertEquals(setOf(1, 2, 3), AutoKonfig.getSet(IntSettingType, "numbers"))
        assertEquals(setOf(1, 2, 3), AutoKonfig.getSet(IntSettingType, ",", "numbers"))
        assertEquals(setOf(1, 2, 3), AutoKonfig.getSet(IntSettingType, Regex(","),"numbers"))
    }

    @Test
    fun `list settings with custom separators can be read`() {
        """
            commas = 1,2,3
            commasAndWhitespace = 1,  2,      3
            dots = 1.2.3
            complex = 1AbC2teSt3
        """.trimIndent().createConfigFile()
        val commas by ListSetting(IntSettingType, ",")
        val commasAndWhitespace by ListSetting(IntSettingType, Regex(",\\s+"))
        val dots by SetSetting(IntSettingType, ".")
        val complex by SetSetting(IntSettingType, Regex("[A-z]+"))
        assertArrayEquals(listOf(1, 2, 3).toTypedArray(), commas.toTypedArray())
        assertArrayEquals(listOf(1, 2, 3).toTypedArray(), commasAndWhitespace.toTypedArray())
        assertArrayEquals(setOf(1, 2, 3).toTypedArray(), dots.toTypedArray())
        assertArrayEquals(setOf(1, 2, 3).toTypedArray(), complex.toTypedArray())
    }
}
