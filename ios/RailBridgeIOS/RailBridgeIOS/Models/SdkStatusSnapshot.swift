import Foundation

struct SdkStatusSnapshot: Codable, Equatable {
    let initialized: Bool
    let version: String
}
