package dev.reassembly.commands.impl.config

import dev.reassembly.commands.BaseCommand
import dev.reassembly.database.DatabaseHandler
import dev.reassembly.models.Server
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import net.dv8tion.jda.api.Permission
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel
import net.dv8tion.jda.api.entities.channel.concrete.ThreadChannel
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent

object ConfigCommand: BaseCommand("config") {

    /**
     * Represents an option one can select when changing the game channel
     *
     * @param runChannelSetter The function to run when assigning the new channel
     * @param breakingMessage The message used for that game channel to act as its breakpoint. Can be null.
     */
    private data class GameChannelSelection (
        val runChannelSetter: (server: Server, channelId: String) -> Unit,
        val breakingMessage: String? = null,
    )

    private val gameChannelSelectionMap: Map<String, GameChannelSelection> = mapOf(
        "change" to GameChannelSelection({server, channelId -> server.changeChannel = channelId}),
        "letter" to GameChannelSelection({server, channelId -> server.letterChannel = channelId}, "Start your letter here:"),
        "pattern" to GameChannelSelection({server, channelId -> server.patternChannel = channelId}, "Start pattern game here:"),
        "freezetag" to GameChannelSelection({server, channelId -> server.freezeTagChannel = channelId}),
        "lateforwork" to GameChannelSelection({server, channelId -> server.lateForWorkChannel = channelId}),
        "partyquirks" to GameChannelSelection({server, channelId -> server.partyQuirksChannel = channelId}),
        "bedtimestories" to GameChannelSelection({server, channelId -> server.bedtimeStoriesChannel = channelId}),
        "timewarp" to GameChannelSelection({server, channelId -> server.timeWarpChannel = channelId}),
        "guessinggame" to GameChannelSelection({server, channelId -> server.guessingGameChannel = channelId}),
        "seventhings" to GameChannelSelection({server, channelId -> server.sevenThingsChannel = channelId}),
        "book" to GameChannelSelection({server, channelId -> server.bookChannel = channelId}),
        "longform" to GameChannelSelection({server, channelId -> server.longFormChannel = channelId}),
        "genre" to GameChannelSelection({server, channelId -> server.genreChannel = channelId}),
        "alphabet" to GameChannelSelection({server, channelId -> server.alphabetChannel = channelId}),
        "flurry" to GameChannelSelection({server, channelId -> server.flurryChannel = channelId}),
        "pillars" to GameChannelSelection({server, channelId -> server.pillarsChannel = channelId}),
        "chain" to GameChannelSelection({server, channelId -> server.chainChannel = channelId}, "Start chain game here:")
    )

    override suspend fun execute(event: SlashCommandInteractionEvent) {

        val server = DatabaseHandler.getServer(event.guild!!.id)
        if (server == null) {
            event.reply("This server has not been registered").setEphemeral(true).queue()
            return
        }

        if (!event.member!!.hasPermission(Permission.MANAGE_SERVER)) {
            event.reply("You need the ``Manage Server`` permission to run this command!")
        }

        when (event.subcommandName) {
            "channels" -> {

                val channel = event.getOption("channel")!!.asChannel
                val textChannel = channel as? TextChannel ?: channel as? ThreadChannel

                val gameOption = event.getOption("game")!!.asString

                // Now Playing is a special case since we store the message ID too, handling this separately
                if (gameOption == "nowplaying") {
                    server.currentlyPlayingMessageChannel = channel.id
                    if (channel is TextChannel || channel is ThreadChannel) {
                        val nowPlayingMessage = withContext(Dispatchers.IO) {
                            channel.sendMessage("Currently Active Games").complete()
                        }
                        server.currentlyPlayingMessageId = nowPlayingMessage.id
                    }
                    event.reply("Set Now Playing channel to " + channel.asMention).queue()
                }
                val game = gameChannelSelectionMap[gameOption] ?: run {
                    event.reply("Could not find game").setEphemeral(true).queue()
                    return
                }
                game.runChannelSetter(server, channel.id)
                if (game.breakingMessage != null) textChannel?.sendMessage(game.breakingMessage)?.queue()

                event.reply("Set $gameOption channel to " + channel.asMention).queue()

                server.save()
            }
        }
    }
}