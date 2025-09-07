/**
 * Type augmentations to fix compatibility issues with third-party dependencies
 * when using strict TypeScript settings like exactOptionalPropertyTypes
 */

declare module '@react-navigation/core' {
  namespace NavigationHelpersCommon {
    interface NavigationHelpers<ParamList extends Record<string, unknown> = Record<string, unknown>> {
      preload<RouteName extends string>(
        ...args: RouteName extends unknown 
          ? [screen: RouteName, params?: Record<string, unknown> | undefined] 
          : never
      ): void;
    }
  }
}

declare module '@react-navigation/bottom-tabs' {
  interface RouteProp<
    ParamList extends Record<string, unknown>,
    RouteName extends keyof ParamList
  > {
    params?: Readonly<ParamList[RouteName]> | undefined;
  }
}

declare module '@react-navigation/native-stack' {
  interface RouteProp<
    ParamList extends Record<string, unknown>, 
    RouteName extends keyof ParamList
  > {
    params?: Readonly<ParamList[RouteName]> | undefined;
  }
}

declare module 'react-native-paper/src/types' {
  // Fix for react-native-paper's internal type imports
  export * from 'react-native-paper';
}

declare module 'react-native-paper' {
  namespace MD3Colors {
    interface Palette {
      primary: string;
      onPrimary: string;
      primaryContainer: string;
      onPrimaryContainer: string;
      surface: string;
      onSurface: string;
      surfaceVariant: string;
      onSurfaceVariant: string;
      // Add other colors as needed
    }
  }
}

// Fix for react-native-vector-icons material design icons
declare module '@react-native-vector-icons/material-design-icons' {
  const icons: Record<string, number>;
  export = icons;
}

// Remove the problematic JSX namespace override
// Let React Native handle JSX types properly

// Export to make this a module
export {};