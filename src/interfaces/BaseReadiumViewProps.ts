import type { ViewStyle } from 'react-native';

import type { Settings } from './Settings';
import type { Link } from './Link';
import type { Locator } from './Locator';
import type { File } from './File';

export type BaseReadiumViewProps = {
  file: File;
  location?: Locator | Link;
  settings?: Partial<Settings>;
  style?: ViewStyle;
  onLocationChange?: (locator: Locator) => void;
  onTableOfContents?: (toc: Link[] | null) => void;
  ref?: any;
  height?: number;
  width?: number;
  searchQuery?: string;
  onSearch?: (result: Locator[]) => void;
};
