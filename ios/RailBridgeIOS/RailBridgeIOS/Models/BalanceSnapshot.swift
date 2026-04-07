import Foundation

struct BalanceSnapshot: Codable, Equatable {
    let cardId: String
    let balance: Int
    let timestamp: String
}
