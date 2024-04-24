import RNFS from 'react-native-fs';
import type { Locator } from 'react-native-readium';

export * from './DEFAULT_SETTINGS';

// export const EPUB_URL = `https://filebin.net/tswgizhlz526ceu7/epubTest.lcpl`;
// export const EPUB_PATH = `${RNFS.DocumentDirectoryPath}/epubTest.lcpl`;
export const EPUB_URL = `https://filebin.net/iyy11qz0dsq3qwu2/pdfTest.lcpl`;
export const EPUB_PATH = `${RNFS.DocumentDirectoryPath}/pdfTest.lcpl`;
export const INITIAL_LOCATION: Locator = {
  href: '/OPS/main3.xml',
  title: 'Chapter 2 - The Carpet-Bag',
  type: 'application/xhtml+xml',
  target: 27,
  locations: {
    position: 24,
    progression: 0,
    totalProgression: 0.03392330383480826
  },
};

//PDF
// export const EPUB_URL = `https://tcpdf.org/files/examples/example_045.pdf`;
// export const EPUB_PATH = `${RNFS.DocumentDirectoryPath}/moby-dick-2.pdf`;
// export const INITIAL_LOCATION: Locator = {
//   href: '/moby-dick-2.pdf',
//   title: 'Chapter 2 - The Carpet-Bag',
//   target: 27,
//   type: 'application/pdf',
//   // locations: {fragments: ["page=10"], "position": 10, "progression": 0.5625, "totalProgression": 0.5625},
//
// };
