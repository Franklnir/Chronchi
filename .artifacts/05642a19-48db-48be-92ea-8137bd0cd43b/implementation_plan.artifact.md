# Modernizing ESPBridge UI/UX

Refactor the existing "Neo-brutalism" design into a "Clean and Modern" aesthetic. This involves updating the color palette, typography, and component styling to improve hierarchy, readability, and overall visual appeal.

## User Review Required

> [!IMPORTANT]
> The "Neo-brutalism" style (heavy outlines, high contrast) will be replaced with a softer, more layered approach using elevation and subtle gradients.

## Proposed Changes

### 1. Foundation (Theme & Design System)

Update the base styles to set a modern tone.

#### [MODIFY] [Color.kt](file:///C:/Users/frank/Downloads/ESPBridge/app/src/main/java/com/irsyadlabs/espbridge/ui/theme/Color.kt)
- Introduce a refined color palette with softer primary colors and neutral grays.
- Add surface and elevation-related colors.

#### [MODIFY] [Type.kt](file:///C:/Users/frank/Downloads/ESPBridge/app/src/main/java/com/irsyadlabs/espbridge/ui/theme/Type.kt)
- Adjust font weights to reduce "visual noise". Use Medium/SemiBold instead of Black/ExtraBold for many headers.

#### [MODIFY] [Theme.kt](file:///C:/Users/frank/Downloads/ESPBridge/app/src/main/java/com/irsyadlabs/espbridge/ui/theme/Theme.kt)
- Update `MaterialTheme` configuration to use the new color scheme.

### 2. Core Components

Refactor reusable UI elements to match the new style.

#### [MODIFY] [PrimaryActionButton.kt](file:///C:/Users/frank/Downloads/ESPBridge/app/src/main/java/com/irsyadlabs/espbridge/ui/components/PrimaryActionButton.kt) (Wait, I need to find where this is)
#### [MODIFY] [PlayfulCard.kt](file:///C:/Users/frank/Downloads/ESPBridge/app/src/main/java/com/irsyadlabs/espbridge/ui/components/PlayfulCard.kt)
#### [MODIFY] [StatusPill.kt](file:///C:/Users/frank/Downloads/ESPBridge/app/src/main/java/com/irsyadlabs/espbridge/ui/components/StatusPill.kt)

### 3. Screens

Apply the new styles to the main app screens.

#### [MODIFY] [LoginScreen.kt](file:///C:/Users/frank/Downloads/ESPBridge/app/src/main/java/com/irsyadlabs/espbridge/ui/screens/auth/LoginScreen.kt)
- Soften the layout, improve the "Google" button styling, and add a more modern header.

#### [MODIFY] [HomeScreen.kt](file:///C:/Users/frank/Downloads/ESPBridge/app/src/main/java/com/irsyadlabs/espbridge/ui/screens/home/HomeScreen.kt)
- Redesign the "Phone Summary" card with a modern look (gradients/shadows).
- Clean up the `SourceCard` list.

## Verification Plan

### Automated Tests
- `gradlew :app:assembleDebug` to ensure no regressions in build logic.

### Manual Verification
- Render Compose Previews for `LoginScreen` and `HomeScreen` to verify the new visual style.
