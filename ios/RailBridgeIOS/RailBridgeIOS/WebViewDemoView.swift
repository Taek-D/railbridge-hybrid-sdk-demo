import SwiftUI
import WebKit

struct WebViewDemoView: View {
    @StateObject private var host = IOSWebViewHost()

    var body: some View {
        RailBridgeWebView(bridge: host.bridge)
            .navigationTitle("RailBridge Demo")
            .navigationBarTitleDisplayMode(.inline)
            .onDisappear {
                host.bridge.destroy()
            }
    }
}

private final class IOSWebViewHost: ObservableObject {
    let bridge: IOSNativeBridge

    init() {
        let scenarioController = ScenarioController()
        let errorLogger = ErrorLogger(maxTimelines: 50)
        let adapter = MockRailSdkAdapter(scenarioController: scenarioController)
        self.bridge = IOSNativeBridge(
            adapter: adapter,
            errorLogger: errorLogger,
            scenarioController: scenarioController
        )
        adapter.initialize { _ in }
    }
}

private struct RailBridgeWebView: UIViewRepresentable {
    let bridge: IOSNativeBridge

    func makeUIView(context: Context) -> WKWebView {
        let webView = bridge.makeWebView()
        loadDiagnosticsPage(into: webView)
        return webView
    }

    func updateUIView(_ uiView: WKWebView, context: Context) {
    }

    private func loadDiagnosticsPage(into webView: WKWebView) {
        let bundle = Bundle.main
        let candidates = [
            bundle.url(forResource: "index", withExtension: "html"),
            bundle.url(forResource: "index", withExtension: "html", subdirectory: "webview"),
            bundle.url(forResource: "index", withExtension: "html", subdirectory: "Resources/webview")
        ]

        guard let fileURL = candidates.compactMap({ $0 }).first else {
            return
        }

        webView.loadFileURL(fileURL, allowingReadAccessTo: fileURL.deletingLastPathComponent())
    }
}
