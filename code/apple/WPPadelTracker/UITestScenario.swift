#if DEBUG
import Foundation
import Shared

/// Matches the watch UI tests open on (-UITestMatch <name>), played through the real session
/// so a test can start where it needs to instead of tapping its way there.
enum UITestScenario: String {
    /// 6-0 for you: the change of sides is asked.
    case setWon
    /// Doubles, you serving from the right: 6-0 for you after picking the left opponent; the change of sides is asked.
    case doublesSetWon
    /// 6-0 6-0 for you, on the summary.
    case won
    /// 6-0 6-0 for the opponent, on the summary.
    case lost
    /// Golden Point rules: you win game 1 on the golden point, then 6-0 6-0; on the summary.
    case goldenWon
    /// 6-0 5-0 40-0 for you: match point.
    case matchPoint
    /// 6-6 in the first set: the tie-break starts.
    case tieBreak

    private enum Step {
        case points(Team, Int)
        case answer
        case end
    }

    private static let pointsPerGame = 4
    private static let loveSet = 6 * pointsPerGame

    private var ruleMode: RuleMode { self == .goldenWon ? .golden : .standard }
    private var playMode: PlayMode { self == .doublesSetWon ? .doubles : .singles }

    private var steps: [Step] {
        let you = Team.you, opp = Team.opp
        switch self {
        case .setWon:         return [.points(you, Self.loveSet)]
        case .doublesSetWon:  return [.points(you, Self.pointsPerGame), .answer, .points(you, Self.loveSet - Self.pointsPerGame)]
        case .won:            return [.points(you, Self.loveSet), .answer, .points(you, Self.loveSet), .end]
        case .lost:           return [.points(opp, Self.loveSet), .answer, .points(opp, Self.loveSet), .end]
        case .goldenWon:
            return [
                .points(you, 3), .points(opp, 3), .points(you, 1), .points(you, Self.loveSet - Self.pointsPerGame),
                .answer, .points(you, Self.loveSet), .end
            ]
        case .matchPoint:     return [.points(you, Self.loveSet), .answer, .points(you, Self.loveSet - 1)]
        case .tieBreak:       return Array(repeating: [Step.points(you, Self.pointsPerGame), .points(opp, Self.pointsPerGame)], count: 6).flatMap { $0 }
        }
    }

    /// Plays the scenario into `store`, where the watch's MatchStore then finds it.
    func play(into store: TextStore) {
        let session = MatchSession(store: store, phone: nil)
        session.start(config: MatchSetup.shared.config(playMode: playMode, bestOf: 3, ruleMode: ruleMode,
                                                       servingTeam: .you, serverOnLeft: false))
        for step in steps {
            switch step {
            case .points(let team, let count): for _ in 0..<count { _ = session.score(team: team) }
            case .answer: _ = session.answerPrompt(choice: 0)
            case .end: session.end()
            }
        }
    }
}

extension MatchStore {
    /// In memory, with no phone, no Health and no haptics; optionally opened on a scenario.
    static func uiTesting(scenario name: String?) -> MatchStore {
        let store = MemoryTextStore(data: nil)
        if let name {
            guard let scenario = UITestScenario(rawValue: name) else { fatalError("Unknown -UITestMatch \(name)") }
            scenario.play(into: store)
        }
        return MatchStore(store: store, names: NamesModel(store: MemoryTextStore(data: nil)),
                          phone: nil, workout: nil, haptics: nil)
    }
}
#endif
