import { describe, it, expect, beforeEach } from 'vitest';
import { useThemeStore } from './themeStore';

describe('themeStore', () => {
  beforeEach(() => {
    // Reset the store state
    useThemeStore.setState({ isDarkMode: false });
    document.documentElement.classList.remove('dark');
  });

  it('should have initial state as light mode', () => {
    const { isDarkMode } = useThemeStore.getState();
    expect(isDarkMode).toBe(false);
  });

  it('should toggle dark mode', () => {
    const { toggleDarkMode } = useThemeStore.getState();

    toggleDarkMode();

    const { isDarkMode } = useThemeStore.getState();
    expect(isDarkMode).toBe(true);
    expect(document.documentElement.classList.contains('dark')).toBe(true);
  });

  it('should toggle back to light mode', () => {
    const store = useThemeStore.getState();

    store.toggleDarkMode(); // to dark
    store.toggleDarkMode(); // back to light

    const { isDarkMode } = useThemeStore.getState();
    expect(isDarkMode).toBe(false);
    expect(document.documentElement.classList.contains('dark')).toBe(false);
  });

  it('should set dark mode directly', () => {
    const { setDarkMode } = useThemeStore.getState();

    setDarkMode(true);

    const { isDarkMode } = useThemeStore.getState();
    expect(isDarkMode).toBe(true);
    expect(document.documentElement.classList.contains('dark')).toBe(true);
  });

  it('should set light mode directly', () => {
    const { setDarkMode } = useThemeStore.getState();

    setDarkMode(true);
    setDarkMode(false);

    const { isDarkMode } = useThemeStore.getState();
    expect(isDarkMode).toBe(false);
    expect(document.documentElement.classList.contains('dark')).toBe(false);
  });
});
