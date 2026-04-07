import Foundation

final class ScenarioController {
    private let queue = DispatchQueue(label: "com.demo.railbridge.ios.scenario")
    private var activePresetStorage: ScenarioPreset = .normal

    var activePreset: ScenarioPreset {
        queue.sync { activePresetStorage }
    }

    func setActivePreset(_ rawValue: String) {
        let preset = ScenarioPreset.from(rawValue)
        queue.sync {
            activePresetStorage = preset
        }
    }

    func reset() {
        setActivePreset(ScenarioPreset.normal.rawValue)
    }
}
