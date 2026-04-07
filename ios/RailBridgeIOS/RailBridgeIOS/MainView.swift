import SwiftUI

struct MainView: View {
    var body: some View {
        NavigationStack {
            VStack(spacing: 24) {
                Spacer()

                VStack(spacing: 12) {
                    Text("RailBridge iOS Parity Demo")
                        .font(.system(size: 30, weight: .bold, design: .rounded))
                        .multilineTextAlignment(.center)

                    Text("WKWebView bridge, deterministic SDK presets, and Android-aligned diagnostics for hybrid stabilization demos.")
                        .font(.system(size: 16, weight: .medium))
                        .foregroundStyle(.secondary)
                        .multilineTextAlignment(.center)
                        .padding(.horizontal, 20)
                }

                NavigationLink {
                    WebViewDemoView()
                } label: {
                    Text("Start demo")
                        .font(.headline)
                        .frame(maxWidth: .infinity)
                        .padding(.vertical, 16)
                        .foregroundStyle(.white)
                        .background(
                            LinearGradient(
                                colors: [Color(red: 0.07, green: 0.62, blue: 0.53), Color(red: 0.26, green: 0.84, blue: 0.56)],
                                startPoint: .leading,
                                endPoint: .trailing
                            )
                        )
                        .clipShape(RoundedRectangle(cornerRadius: 18, style: .continuous))
                        .shadow(color: Color.black.opacity(0.16), radius: 18, y: 10)
                }
                .padding(.horizontal, 24)

                VStack(alignment: .leading, spacing: 10) {
                    Label("Four business actions match Android bridge names", systemImage: "arrow.triangle.branch")
                    Label("Scenario presets mirror timeout, callback loss, and duplicate callback flows", systemImage: "exclamationmark.triangle")
                    Label("Diagnostics export stays comparable by correlationId", systemImage: "doc.text.magnifyingglass")
                }
                .font(.system(size: 14, weight: .medium))
                .foregroundStyle(.secondary)
                .padding(20)
                .frame(maxWidth: .infinity, alignment: .leading)
                .background(Color(.secondarySystemBackground))
                .clipShape(RoundedRectangle(cornerRadius: 18, style: .continuous))
                .padding(.horizontal, 24)

                Spacer()
            }
            .padding(.vertical, 24)
            .navigationTitle("RailBridge")
        }
    }
}

#Preview {
    MainView()
}
