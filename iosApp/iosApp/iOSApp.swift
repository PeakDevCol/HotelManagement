import SwiftUI
import FirebaseCore
import Shared

@main
struct iOSApp: App {
    init() {
        // GoogleService-Info.plist is gitignored. Without it the app starts with Firebase disabled.
        let hasFirebaseConfig = Bundle.main.path(forResource: "GoogleService-Info", ofType: "plist") != nil
        if hasFirebaseConfig {
            FirebaseApp.configure()
        }
        KoinIosKt.doInitKoinIos(firebaseEnabled: hasFirebaseConfig)
    }

    var body: some Scene {
        WindowGroup {
            ContentView()
        }
    }
}
