import Foundation
import R2Navigator
import R2Shared
import UIKit

final class PDFModule: ReaderFormatModule {
    weak var delegate: ReaderFormatModuleDelegate?

    init(delegate: ReaderFormatModuleDelegate?) {
        self.delegate = delegate
    }

    var publicationFormats: [Publication.Format] {
        return [.pdf]
    }

    func makeReaderViewController(for publication: Publication, locator: Locator?, bookId: Book.Id, resourcesServer: ResourcesServer) throws -> ReaderViewController {
        let viewController = try await PDFViewController(
            publication: publication,
            locator: locator,
            bookId: bookId,
            resourcesServer: resourcesServer
        )
        viewController.moduleDelegate = delegate
        return viewController
    }

}
