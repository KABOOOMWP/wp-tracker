import Foundation
import Shared

/// Sends the team names to the watch.
@MainActor
protocol NamesSender: AnyObject {
    func send(_ names: TeamNames)
}

/// Publishes the shared PhoneBoard (newest scoreboard from the watch) and sends name edits to the watch.
@MainActor
final class ScoreboardModel: ObservableObject {

    @Published private(set) var board: Scoreboard?
    let names: NamesModel
    var watch: NamesSender?
    private let phoneBoard = PhoneBoard()

    init(names: NamesModel) {
        self.names = names
    }

    /// The app's model: names on disk, linked to the watch.
    static func live() -> ScoreboardModel {
        #if DEBUG
        if Launch.isUITesting { return uiTesting(board: Launch.sampleBoard) }
        #endif
        let model = ScoreboardModel(names: .onDisk())
        model.watch = WatchLink(model: model)
        return model
    }

    #if DEBUG
    /// No watch link and names in memory, optionally showing a shared sample board (-UITestBoard match|summary).
    static func uiTesting(board name: String?) -> ScoreboardModel {
        let model = ScoreboardModel(names: NamesModel(store: MemoryTextStore(data: nil)))
        if let name {
            guard let sample = SampleBoards.shared.make(name: name) else { fatalError("Unknown -UITestBoard \(name)") }
            model.receive(sample.encode())
        }
        return model
    }
    #endif

    /// The live match, the summary, or neither (the waiting board): the shared PhoneBoard decides.
    var match: MatchDisplay? { phoneBoard.match }
    var summary: SummaryStats? { phoneBoard.summary }
    var showsNameEditor: Bool { phoneBoard.showsNameEditor }

    /// Accepts a scoreboard from the watch; unreadable ones and older revisions of the same epoch are ignored. Returns true when shown.
    @discardableResult
    func receive(_ data: String) -> Bool {
        guard phoneBoard.receive(data: data) else { return false }
        board = phoneBoard.board
        return true
    }

    /// Saves an edit from the name editor (blank fields keep the current name) and sends it to the watch.
    func editNames(you: String, opp: String) {
        if let edited = names.edit(you: you, opp: opp) { watch?.send(edited) }
    }

    /// When the link comes up: only names typed on this phone, so a reinstall never resets the watch to the defaults.
    func sendSavedNames() {
        guard names.isSaved else { return }
        watch?.send(names.names)
    }
}
