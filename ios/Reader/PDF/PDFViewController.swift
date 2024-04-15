import Foundation
import R2Navigator
import R2Shared
import ReadiumAdapterGCDWebServer
import SwiftUI
import UIKit

final class PDFViewController: ReaderViewController {

    init(
        publication: Publication,
        locator: Locator?,
        bookId: Book.Id,
        resourcesServer: ResourcesServer
    ) throws {
        self.preferencesStore = preferencesStore

        let navigator = try PDFNavigatorViewController(
            publication: publication,
            initialLocation: locator,
            resourcesServer: resourcesServer
        )

        super.init(navigator: navigator, publication: publication, bookId: bookId)

        navigator.delegate = self
    }

}

extension PDFViewController: PDFNavigatorDelegate {}
