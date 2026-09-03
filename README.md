# Ucycle (Java) - UTAR Sell & Borrow Platform
UCCD3223 Mobile Applications Development - Group Assignment

Built with **Java + XML layouts + Firebase**. No Kotlin.

## Features
- Sign up / login with any email (Firebase Auth) + email verification link.
  The UTAR-only restriction is a one-line switch in `Config.java` - see below.
- Real-time listing feed with search and All / Sell / Borrow filter
- Post a listing: photo upload (Firebase Storage), title, category, description,
  Sell / Borrow / Both toggle, condition slider (1-10), price or max borrow days
- Item detail: Request to borrow, or chat with the seller/owner
- Borrowing screen with two tabs:
  - "My borrowing" - items you requested/borrowed
  - "Requests to me" - approve (due date = owner's max borrow days), reject,
    or mark as returned
  - Status chips: Pending / On Time / Due Soon / Overdue / Returned
- Real-time in-app chat (message bubbles, per-item threads)
- Profile: listing count, rating, completed exchanges, "items saved from
  landfill" counter, my listings, logout
- FCM service + notification channel wired up

## Project structure
app/src/main/java/com/utar/ucycle/
  LoginActivity.java, SignUpActivity.java   - UTAR email auth
  MainActivity.java                          - bottom nav host
  ItemDetailActivity.java                    - item page + borrow request + chat
  CreateListingActivity.java                 - post a listing
  ChatActivity.java                          - conversation screen
  UcycleApplication.java                     - notification channel
  UcycleMessagingService.java                - FCM receiver
  model/    - Listing, BorrowRecord, ChatThread, ChatMessage, UserProfile
  ui/       - HomeFragment, BorrowingFragment, ChatListFragment, ProfileFragment
  adapter/  - ListingAdapter, BorrowAdapter, ChatThreadAdapter,
              MessageAdapter, MyListingAdapter

## What changed in v2.1 (versionCode 12)
- **Ratings are colour coded**: red for low, amber for middling, green for high.
  Thresholds live in Ratings.colorRes() - under 2.5 red, under 4.0 amber, else green.
  Applied to the stars on each comment, the score chip on a profile, and the
  rating bar itself while you are choosing, which now also shows a word
  (Very poor / Poor / Okay / Good / Excellent) in the matching colour.
  A profile still showing "New user" stays grey, so a person with too few
  ratings is never coloured red as though they were rated badly.
- The rating bar used an indicator style that is not meant to be tapped; it now
  uses the interactive one.

## What changed in v2.0 (versionCode 11)
- **Category is now a dropdown** on both the create and edit screens, with a
  fixed list (Books & Notes, Electronics, Stationery, Lab Equipment, Clothing,
  Sports & Outdoor, Furniture, Kitchen & Appliances, Bags & Accessories) and
  **Others** at the end. Choosing Others reveals a text field so people can name
  their own category, and that name is what gets saved.
  Editing an item whose category is not in the list reopens it under Others with
  the old name filled in, so nothing is lost.
  The list lives in res/values/strings.xml (string-array categories) - add
  entries there, keeping Others last.
- **The keyboard no longer covers what you are typing.** This was a bug in
  InsetsHelper: it consumed the window insets without ever reading the keyboard
  inset, so windowSoftInputMode=adjustResize could not lift the content. The
  bottom padding now grows to whichever is taller, the navigation bar or the
  keyboard. The rating screen also became scrollable for the same reason.

## What changed in v1.9 (versionCode 10)
- **App icon** is now the Ucycle logo, generated at all five densities plus an
  adaptive icon (mipmap-anydpi-v26) so it renders correctly on modern launchers
  whatever mask the phone applies. Only the U symbol is used for the icon, since
  the wordmark and tagline would be unreadable at 48dp.
- The **login screen** now shows the full logo instead of plain text.

## What changed in v1.8 (versionCode 9)
- The **item page** now opens the poster's profile when you tap their name.
  It was meant to work in v1.5 but the wiring never took effect, so the name
  was plain text.

Profiles can now be opened from: the home feed, an item page, a deal, inside a
chat, and the chat list.

## What changed in v1.7 (versionCode 8)
- **Fixed duplicated deals.** The Deals screen runs two queries (borrows and
  sales) and merges them. Refreshing while an earlier pair was still loading let
  the stale replies append a second copy of the same deal, which is why rows
  doubled after changing a status. Each refresh now carries a token, late replies
  are discarded, and rows are de-duplicated by id as a safety net.

## What changed in v1.6 (versionCode 7)
- You can now open someone's public profile from three more places:
  - the **home feed** - tap the poster's name under any card
  - inside a **chat** - tap their name in the header
  - the **chat list** - tap their avatar
  Useful for checking someone's trust score before agreeing to a deal.

## What changed in v1.5 (versionCode 6) - Step 2
- **Request posts.** Post type is now Sell / Borrow / Request. A request says
  what you are looking for and whether you want to **borrow** or **buy** it.
- **Offers.** Anyone who has the item taps "I have this" on a request and fills
  in their item as normal. That offer is private: it never appears on the home
  feed, only inside the requester's offers screen.
- **Accepting an offer.** Several people can answer the same request, so the
  requester compares the offers (photo, condition, price, who is offering) and
  accepts one. That starts a real borrow or sale, withdraws the other offers and
  closes the request. The requester can also cancel their own request.
- **Public profiles.** Tapping someone's name on an item or a deal opens their
  profile: trust score, bio and every rating left for them.
- **Comments have replies and reports.** A comment can only be deleted by the
  person who wrote it, the person being rated can reply to it once, and anyone
  else can report it.
- New "Requests" filter chip on the home feed.

## What changed in v1.4 (versionCode 5) - Step 1 of the big update
- **"Both" removed.** A listing is now either Sell or Borrow. (Request posts
  arrive in the next step.)
- **Deals screen** (was "Borrowing"): now shows borrows *and* purchases in one
  list, every row is tappable, with tabs "My deals" and "On my items".
- **New transaction detail screen** with the whole lifecycle:
  - Borrow: owner approves and picks the due date -> borrower taps "I have
    returned this" -> owner confirms -> returned date recorded and compared
    against the due date (on time / late).
  - The owner can change the due date at any point after agreeing in chat; the
    screen shows when a date has been changed.
  - Past the grace period the owner can close an overdue borrow without the
    borrower's confirmation, so nothing can deadlock.
  - Sale: buyer requests -> seller accepts -> they meet -> BOTH confirm the
    handover -> completed. One-sided confirmation is not enough, which is what
    stops a no-show counting as a sale.
- **Ratings.** After a completed borrow or sale both sides can rate each other:
  stars plus an optional comment, and it can be skipped. One combined trust
  score per person. A score stays hidden as "New user" until 3 ratings exist
  (see Config.MIN_RATINGS_TO_SHOW_SCORE).
- **Profile listings are now bordered cards** with an Edit chevron, so it is
  obvious they can be tapped.
- Chat can now be opened directly from a deal, not only from an item page.

### Config switches worth knowing
    Config.OVERDUE_GRACE_DAYS = 0        // 0 for demos, 3 for real use
    Config.MIN_RATINGS_TO_SHOW_SCORE = 3

## What changed in v1.3 (versionCode 4)
- Fixed "NOT_FOUND: No document to update" when saving a profile. Firestore's
  update() fails if the document does not exist, so every write to a user
  document now uses set(..., merge), which creates it when missing.
- Logging in also rebuilds a minimal user document if it is missing, so accounts
  created before profiles existed (or after the collection was cleared) still work.

## What changed in v1.2 (versionCode 3)
- Users can edit their own profile: Profile -> **Edit profile**.
  Name is required; profile picture, faculty, "about me" and a contact detail
  are all optional and simply stay hidden when left blank.
- The profile picture is stored as Base64 in the user document, same approach as
  listing photos, so still no Firebase Storage needed.
- Renaming yourself also refreshes the owner name shown on your existing
  listings, so the feed does not keep showing your old name.

## What changed in v1.1 (versionCode 2)
- Content no longer sits under the status bar (Android 15+ forces edge-to-edge,
  so every screen now applies window insets).
- Photos are stored as compressed Base64 **inside the Firestore document**.
  Firebase Storage is no longer used at all, because since Feb 2026 Cloud Storage
  requires a Blaze billing account. No credit card needed now.
- A listing is saved even if its photo cannot be processed. Previously a failed
  photo upload silently aborted the whole post - this is why posts were not
  appearing for other people.
- Queries no longer combine a filter with a sort, so **no composite Firestore
  indexes are needed**. Filtering and sorting happen in the app instead.
- Query failures now show an error message on screen instead of a blank list.
- Owners can edit or delete their listings: Profile -> tap one of "My listings".
- Email verification is temporarily OFF so the group can test with made-up
  addresses. See Config.java.

**Note:** any listing posted before v1.1 has its photo in Firebase Storage and
will show no image. Clear the `listings` collection in the Firebase console and
re-post a couple of test items.

## Setup (REQUIRED - app will not run without this)
1. Open the folder in Android Studio (Ladybug or newer).
2. Go to https://console.firebase.google.com and create a project named "Ucycle".
3. Add an **Android** app with package name exactly: `com.utar.ucycle`
4. Download `google-services.json` and put it inside the `app/` folder
   (same level as app/build.gradle.kts).
5. In the Firebase Console enable:
   - Authentication -> Sign-in method -> **Email/Password**
   - Firestore Database -> Create database (test mode while developing)
   - Storage -> Get started (test mode while developing)
6. Sync Gradle and run on an emulator or phone (min SDK 26).

## Firestore indexes
None needed. Every query uses a single field, which Firestore indexes
automatically, and the app does its own filtering and sorting. If you ever add a
query that combines whereEqualTo with orderBy, Firestore will demand a composite
index and print a clickable link in Logcat.

## Security rules (replace test mode before the demo)
rules_version = '2';
service cloud.firestore {
  match /databases/{database}/documents {
    match /{document=**} {
      allow read, write: if request.auth != null;
    }
  }
}

## Email restriction switch (Config.java)
`app/src/main/java/com/utar/ucycle/Config.java` holds two flags:

    RESTRICT_TO_UTAR_EMAIL = false;   // any valid email can register
    REQUIRE_EMAIL_VERIFICATION = true;

Right now it is set to **false** so the group can build and test with normal
Gmail/Outlook accounts while we have no UTAR email access. Everything else
(verification link, listings, borrowing, chat) works exactly the same.

**Before the final demo / submission, change it to `true`.** That single line
restores the @1utar.my / @utar.edu.my check described in the proposal, and the
field labels and error messages update automatically.

`REQUIRE_EMAIL_VERIFICATION` can be set to false if you need a very fast demo
with throwaway accounts, but leave it true normally since it is the feature the
proposal calls "OTP verification".

## Notes for the report
- The proposal says "OTP". Firebase has no built-in email OTP, so verification is
  done with Firebase's **email verification link** sent to the user's inbox. It
  proves the same thing (the user owns that email account). If the lecturer wants
  a numeric OTP, that needs Cloud Functions to generate and email a code.
- Sending notifications automatically (e.g. "your item is due tomorrow") needs
  Cloud Functions. For the demo, send a test message from
  Firebase Console -> Messaging.

## Editing and deleting a listing
Profile -> "My listings" -> tap an item -> edit screen, with **Save changes** and
**Delete listing** at the bottom. Delete asks for confirmation first.

Safety rule: once someone has requested or borrowed an item, it can no longer be
deleted and its price and type are locked, because deleting it would leave the
borrower's record pointing at a listing that no longer exists. The title,
description, category, condition and photo can still be edited. A note on the
screen explains this when it applies.

## Sending an updated APK to your group
`versionCode` is in `app/build.gradle.kts`. It is currently **2**. Increase it by
one every time you send a new APK, otherwise Android refuses to install over the
old copy. Everyone must also keep using APKs built on the same machine, since an
APK signed with a different debug key installs as a separate app rather than an
update.

## Demo flow (use 2 accounts / 2 emulators)
1. Account A posts an item as "Borrow".
2. Account B searches for it, opens it, taps Request to borrow, and chats with A.
3. Account A opens Borrowing -> "Requests to me" -> Approve. A due date appears.
4. A taps "Mark as returned" -> item goes back to AVAILABLE, B's record shows
   Returned, and A's landfill counter goes up by one.
