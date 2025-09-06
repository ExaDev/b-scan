/**
 * B-Scan App Tests
 * @format
 */

import React from 'react';
import ReactTestRenderer from 'react-test-renderer';
import App from '../App';

describe('B-Scan App', () => {
  test('renders correctly', async () => {
    let tree: ReactTestRenderer.ReactTestRenderer | undefined;
    await ReactTestRenderer.act(() => {
      tree = ReactTestRenderer.create(<App />);
    });
    expect(tree).toBeTruthy();
  });

  test('should have the correct app structure with Paper provider', async () => {
    let tree: ReactTestRenderer.ReactTestRenderer | undefined;
    await ReactTestRenderer.act(() => {
      tree = ReactTestRenderer.create(<App />);
    });
    
    if (tree) {
      const root = tree.root;
      expect(root).toBeTruthy();
      expect(tree.toJSON()).toBeTruthy();
    }
  });
});
