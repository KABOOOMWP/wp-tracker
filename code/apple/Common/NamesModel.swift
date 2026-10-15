import Foundation
import Shared

/// Publishes the shared NamesBook to SwiftUI: edited on the phone, received by the watch.
@MainActor
final class NamesModel: ObservableObject {

    @Published private(set) var names: TeamNames
    /// True once names were saved on this device: typed here on a phone, received on a watch.
    private(set) var isSaved: Bool
    private let book: NamesBook

    init(store: TextStore) {
        book = NamesBook(store: store)
        names = book.names
        isSaved = store.load() != nil
    }

    /// The names kept in Application Support.
    static func onDisk() -> NamesModel { NamesModel(store: FileTextStore(fileName: "names.json")) }

    /// Names from the other device; returns true when they changed.
    @discardableResult
    func receive(_ data: String) -> Bool {
        guard book.receive(data: data) else { return false }
        update()
        return true
    }

    /// A name-editor edit; returns the names to send, or nil when nothing changed.
    func edit(you: String, opp: String) -> TeamNames? {
        guard let edited = book.edit(you: you, opp: opp) else { return nil }
        update()
        return edited
    }

    private func update() {
        names = book.names
        isSaved = true
    }
}
