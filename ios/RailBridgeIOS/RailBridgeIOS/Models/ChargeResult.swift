import Foundation

struct ChargeResult: Codable, Equatable {
    let transactionId: String
    let amount: Int
    let balance: Int
    let timestamp: String
}
