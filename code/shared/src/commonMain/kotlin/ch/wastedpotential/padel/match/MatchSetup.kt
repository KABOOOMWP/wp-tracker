package ch.wastedpotential.padel.match

import ch.wastedpotential.padel.model.Config
import ch.wastedpotential.padel.model.PlayMode
import ch.wastedpotential.padel.model.Player
import ch.wastedpotential.padel.model.RuleMode
import ch.wastedpotential.padel.model.Team

public object MatchSetup {

    /**
     * The match [Config] for the setup answers. [serverOnLeft] picks the first server in doubles and is
     * ignored in singles. In doubles the opponents' first server is a placeholder until the game-2 serve pick.
     */
    public fun config(playMode: PlayMode, bestOf: Int, ruleMode: RuleMode, servingTeam: Team, serverOnLeft: Boolean): Config {
        val first = Player.of(servingTeam, leftSide = playMode == PlayMode.DOUBLES && serverOnLeft)
        val receiver = Player.of(servingTeam.other, leftSide = false)
        val order = when (playMode) {
            PlayMode.SINGLES -> listOf(first, receiver)
            PlayMode.DOUBLES -> listOf(first, receiver, first.partner, receiver.partner)
        }
        return Config(bestOf = bestOf, ruleMode = ruleMode, playMode = playMode, serveOrder = order)
    }
}
