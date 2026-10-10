import SwiftUI
import CodeTrail

@main
struct iOSApp: App {
    var body: some Scene {
        WindowGroup {
            ComposeView()
                .ignoresSafeArea()
                .persistentSystemOverlays(.hidden)
        }
    }
}

/// Hosts the shared Compose UI; everything the player sees lives in the Kotlin module.
struct ComposeView: UIViewControllerRepresentable {
    func makeUIViewController(context: Context) -> UIViewController {
        MainViewControllerKt.MainViewController()
    }

    func updateUIViewController(_ uiViewController: UIViewController, context: Context) {}
}
