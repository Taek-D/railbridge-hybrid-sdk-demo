import XCTest
@testable import RailBridgeIOS

final class MockRailSdkAdapterTests: XCTestCase {
    func testCallbackLossDoesNotInvokeCompletion() {
        let scenarioController = ScenarioController()
        scenarioController.setActivePreset(ScenarioPreset.callbackLoss.rawValue)
        let adapter = MockRailSdkAdapter(scenarioController: scenarioController)

        let inverted = expectation(description: "callback should not arrive")
        inverted.isInverted = true

        adapter.requestCharge(cardId: "CARD_001", amount: 1000) { _ in
            inverted.fulfill()
        }

        wait(for: [inverted], timeout: 0.35)
    }

    func testDuplicateCallbackEmitsTwoSuccessCallbacks() {
        let scenarioController = ScenarioController()
        scenarioController.setActivePreset(ScenarioPreset.duplicateCallback.rawValue)
        let adapter = MockRailSdkAdapter(scenarioController: scenarioController)

        let expectation = expectation(description: "two callbacks")
        expectation.expectedFulfillmentCount = 2

        adapter.getBalance(cardId: "CARD_001") { result in
            if case .success = result {
                expectation.fulfill()
            }
        }

        wait(for: [expectation], timeout: 1.0)
    }
}
