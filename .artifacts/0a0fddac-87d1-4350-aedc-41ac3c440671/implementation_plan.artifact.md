# Fix Duplicate Resources Error

The project is failing to build with a "Duplicate resources" error because several drawable resources have both a PNG and an XML (vector) version in the same `res/drawable` directory. Android requires resource names to be unique within a resource type, regardless of the file extension.

## User Review Required

> [!IMPORTANT]
> I am proposing to delete the PNG versions of the following resources, keeping the XML (vector) versions. Vector drawables are generally preferred for scalability and smaller APK size. However, if the PNGs contain specific custom artwork that the XMLs do not match, we should keep the PNGs and delete the XMLs instead.

Drawables with duplicates:
- `road`
- `water`
- `sanitary`
- `security`
- `electricity`

## Proposed Changes

### app module

#### [DELETE] [road.png](file:///C:/Users/m/AndroidStudioProjects/VoiceYanga/app/src/main/res/drawable/road.png)
#### [DELETE] [water.png](file:///C:/Users/m/AndroidStudioProjects/VoiceYanga/app/src/main/res/drawable/water.png)
#### [DELETE] [sanitary.png](file:///C:/Users/m/AndroidStudioProjects/VoiceYanga/app/src/main/res/drawable/sanitary.png)
#### [DELETE] [security.png](file:///C:/Users/m/AndroidStudioProjects/VoiceYanga/app/src/main/res/drawable/security.png)
#### [DELETE] [electricity.png](file:///C:/Users/m/AndroidStudioProjects/VoiceYanga/app/src/main/res/drawable/electricity.png)

## Verification Plan

### Automated Tests
- Run `./gradlew :app:packageDebugResources` to verify that the duplicate resource error is resolved.
- Run a full build: `./gradlew assembleDebug`.

### Manual Verification
- Inspect the icons in the app (e.g., in `activity_create_complaint.xml` preview) to ensure the vector versions are acceptable.
