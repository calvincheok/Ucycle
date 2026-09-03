# How to open this project in Android Studio Otter 3 (2025.2.3)

There are two ways. **Method A is the safer one** and is what I recommend,
because Android Studio generates the Gradle wrapper and build files that exactly
match your installed version, so you avoid version-mismatch errors entirely.

---------------------------------------------------------------------------
## METHOD A (recommended): create a fresh project, then copy the source in
---------------------------------------------------------------------------

### Step 1 - Create the project
1. Android Studio -> **New Project**
2. Choose **Empty Views Activity** (NOT "Empty Activity" - that one is Compose/Kotlin)
3. Fill in:
   - Name: `Ucycle`
   - Package name: `com.utar.ucycle`   <-- must match exactly
   - Language: **Java**
   - Minimum SDK: **API 26**
   - Build configuration language: Kotlin DSL (build.gradle.kts)
4. Finish, and let it finish the first Gradle sync.

### Step 2 - Delete what the template generated
In the Project view (Android mode), delete:
- `app/src/main/java/com/utar/ucycle/MainActivity.java`
- `app/src/main/res/layout/activity_main.xml`

### Step 3 - Copy my files in
From this zip, copy into your new project:

| From this zip                       | Into your project                  |
|-------------------------------------|------------------------------------|
| `app/src/main/java/com/utar/ucycle/` | same path (all .java + subfolders) |
| `app/src/main/res/layout/`           | same path (all 16 layouts)         |
| `app/src/main/res/drawable/`         | same path (7 shape XMLs)           |
| `app/src/main/res/menu/`             | same path (bottom_nav.xml)         |
| `app/src/main/res/values/colors.xml` | overwrite                          |
| `app/src/main/res/values/themes.xml` | overwrite                          |
| `app/src/main/res/values/strings.xml`| overwrite                          |
| `app/src/main/AndroidManifest.xml`   | overwrite                          |

Easiest way: open both folders in Windows File Explorer and drag/replace.

### Step 4 - Add ViewBinding + dependencies
Open `app/build.gradle.kts` in your NEW project and make these edits.

Inside `android { ... }` add:
```kotlin
    buildFeatures { viewBinding = true }
```

Inside `dependencies { ... }` add:
```kotlin
    implementation("com.google.android.material:material:1.12.0")
    implementation("androidx.constraintlayout:constraintlayout:2.1.4")
    implementation("androidx.recyclerview:recyclerview:1.3.2")
    implementation("androidx.fragment:fragment:1.8.3")
    implementation("androidx.activity:activity:1.9.2")

    implementation(platform("com.google.firebase:firebase-bom:33.3.0"))
    implementation("com.google.firebase:firebase-auth")
    implementation("com.google.firebase:firebase-firestore")
    implementation("com.google.firebase:firebase-storage")
    implementation("com.google.firebase:firebase-messaging")

    implementation("com.github.bumptech.glide:glide:4.16.0")
```

At the TOP of `app/build.gradle.kts`, inside `plugins { ... }`, add:
```kotlin
    id("com.google.gms.google-services")
```

And in the PROJECT-level `build.gradle.kts` (the one in the root folder),
inside `plugins { ... }`, add:
```kotlin
    id("com.google.gms.google-services") version "4.4.2" apply false
```

### Step 5 - Firebase (required, app won't run without it)
1. https://console.firebase.google.com -> Add project -> name it "Ucycle"
2. Add an **Android** app, package name exactly `com.utar.ucycle`
3. Download `google-services.json`, put it in the `app/` folder
4. In the console enable:
   - **Authentication** -> Sign-in method -> Email/Password -> Enable
   - **Firestore Database** -> Create database -> start in **test mode**
   - **Storage** -> Get started -> **test mode**

### Step 6 - Sync and run
Click **Sync Now**, then Run on an emulator (API 26+) or your phone.

---------------------------------------------------------------------------
## METHOD B: open this folder directly
---------------------------------------------------------------------------
File -> Open -> select the `UcycleJava` folder.

This zip has no `gradle-wrapper.jar` (binary files can't be included), so
Android Studio will either regenerate it or show a wrapper error. If it does,
go to **File -> Sync Project with Gradle Files**, or just use Method A instead.

The build files here are set to AGP 8.9.1 / Gradle 8.11.1 / compileSdk 36,
which Otter 3 accepts. If Otter offers an **AGP Upgrade Assistant** prompt,
accepting it is fine.

---------------------------------------------------------------------------
## First-run: Firestore will ask for indexes
---------------------------------------------------------------------------
Some screens use a filter + sort together, which Firestore needs an index for.
The first time you open Home / Borrowing / Chats, **Logcat** will print an error
containing a clickable link like:

    ...The query requires an index. You can create it here: https://console.firebase...

Click each link, press **Create Index**, wait ~1 minute, reopen the screen.
Expect about 5 of them. This is normal, not a bug in the code.

---------------------------------------------------------------------------
## Common errors and fixes
---------------------------------------------------------------------------
**"File google-services.json is missing"**
You skipped Step 5. The file must sit directly inside `app/`, not in the root.

**"Default FirebaseApp is not initialized"**
The google-services plugin line is missing from one of the two build.gradle.kts
files. Re-check Step 4.

**"PERMISSION_DENIED" in Logcat**
Firestore/Storage rules are still locked. Use test mode while developing, and
before your demo switch to:

    rules_version = '2';
    service cloud.firestore {
      match /databases/{database}/documents {
        match /{document=**} {
          allow read, write: if request.auth != null;
        }
      }
    }

**Login says "verify your email first"**
That is intended. Open the verification link sent to the UTAR inbox, then log in
again. For testing you can use any real email you control - the UTAR domain check
only runs on the text you type, so use a real @1utar.my address you can open.
