import SwiftUI
import Shared

@main
struct iOSApp: App {
    init() {
        IosKoinInitializerKt.doInitKoin()
    }

    var body: some Scene {
        WindowGroup {
            ContentView()
        }
    }
}