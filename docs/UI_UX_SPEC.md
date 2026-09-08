# UI/UX specification

## Design language
**Playful Utility Neo-Brutal**: strong blue/yellow/red accents, paper-white background, black ink outlines, rounded mobile cards, generous spacing and small decorative accents. Technical areas (permissions, credentials, security) stay visually restrained.

## Global tokens
- page horizontal padding: 16 dp (24 dp on authentication)
- section gap: 24 dp
- card gap: 12 dp
- main card radius: 20 dp
- input/button radius: 16 dp
- primary button: 52 dp high
- minimum interactive target: 48 dp
- bottom navigation: Home / Setup / Settings, same order everywhere

## Color semantics
- Blue: primary actions, connected/info, selected navigation
- Yellow: highlights, weather, secondary CTA, mild warning
- Red: destructive actions/error/attention
- Paper white: main background
- Ink black: typography/borders

## Hierarchy
1. Screen title / device state
2. High-level status card
3. Section title + short helper line
4. Task cards/toggles
5. Destructive actions separated at the end

## Error language
User-facing copy explains what can be done. Raw GATT/internal codes should be kept for diagnostics rather than the normal UI.
