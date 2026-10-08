package io.github.fate_grand_automata.ui.battle_config_list

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFails
import kotlin.test.assertTrue

class ClipboardBattleConfigImporterTest {
    @Test
    fun `accepts short skill command`() {
        val config = ClipboardBattleConfigImporter.parse("a4,#,b4,#,c4")
        assertEquals("a4,#,b4,#,c4", config["autoskill_cmd"])
        assertEquals("Pasted skills", config["autoskill_name"])
    }

    @Test
    fun `accepts prefixed full configuration without losing settings`() {
        val config = ClipboardBattleConfigImporter.parse(
            """FGA1:{"autoskill_name":"Test","autoskill_cmd":"a4,#,b4,#,c4","autoskill_party":"-1","auto_choose_target":true}"""
        )
        assertEquals("Test", config["autoskill_name"])
        assertEquals("-1", config["autoskill_party"])
        assertEquals(true, config["auto_choose_target"])
    }

    @Test
    fun `accepts unprefixed fga json`() {
        val config = ClipboardBattleConfigImporter.parse(
            """{"autoskill_name":"Test","autoskill_cmd":"a4,#,b4,#,c4"}"""
        )
        assertEquals("a4,#,b4,#,c4", config["autoskill_cmd"])
    }

    @Test
    fun `adds fallback name to unnamed configuration`() {
        val config = ClipboardBattleConfigImporter.parse("""FGA1:{"autoskill_cmd":"a4"}""")
        assertEquals("Pasted config", config["autoskill_name"])
    }

    @Test
    fun `rejects invalid commands and unrelated clipboard text`() {
        listOf(
            "",
            " ",
            "hello world",
            "FGA1:not-json",
            "{}",
            """{"autoskill_name":"Test","autoskill_cmd":"not-a-command"}""",
            """{"autoskill_name":14,"autoskill_cmd":"a4"}"""
        ).forEach {
            assertFails("Should reject: $it") { ClipboardBattleConfigImporter.parse(it) }
        }
    }

    @Test
    fun `rejects oversized input`() {
        assertFails { ClipboardBattleConfigImporter.parse("a".repeat(65537)) }
    }

    @Test
    fun `accepts an empty skill command in an otherwise named configuration`() {
        val config = ClipboardBattleConfigImporter.parse("""{"autoskill_name":"Normal attack","autoskill_cmd":""}""")
        assertTrue(config["autoskill_cmd"] == "")
    }
}
