package io.github.fate_grand_automata.ui.battle_config_list

import io.github.fate_grand_automata.prefs.BattleConfigFile
import io.github.fate_grand_automata.scripts.models.AutoSkillCommand

/**
 * Converts text explicitly pasted by the user into a new battle configuration.
 *
 * Supports a plain skill command, a .fga JSON object, or FGA1: followed by
 * the same JSON object. FGA1 is an inline format, not a remote lookup code.
 */
internal object ClipboardBattleConfigImporter {
    private const val PREFIX = "FGA1:"
    private const val MAX_CHARACTERS = 64 * 1024

    fun parse(text: String): Map<String, Any> {
        val input = text.trim()
        require(input.isNotEmpty()) { "Clipboard is empty" }
        require(input.length <= MAX_CHARACTERS) { "Clipboard text is too long" }

        val payload = input.removePrefix(PREFIX).trim()
        if (payload.startsWith("{")) {
            val config = BattleConfigFile.decode(payload)
            val name = config["autoskill_name"]
            val command = config["autoskill_cmd"]
            require(name == null || name is String) { "Invalid config name" }
            require(command == null || command is String) { "Invalid skill command type" }
            require(name is String || command is String) { "Not an FGA battle config" }

            if (command is String && command.isNotBlank()) {
                AutoSkillCommand.parse(command)
            }

            return if (name is String && name.isNotBlank()) config
            else config + ("autoskill_name" to "Pasted config")
        }

        require(!input.startsWith(PREFIX)) { "FGA1: must be followed by JSON" }
        // Match the exact syntax and validation used by the manual command editor.
        AutoSkillCommand.parse(input)
        return mapOf(
            "autoskill_name" to "Pasted skills",
            "autoskill_cmd" to input
        )
    }
}
