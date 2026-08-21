# CALLMIND

## Master Developer Prompt — Production Android Communication Companion

### Product Name

**CallMind**

### Brand Tagline

**Your calls. Your memory. Your next move.**

### Developer / Studio

**Developed by SanStudio**

When the user taps **Developed by SanStudio**, open:

https://sanstudio-hub.github.io/in/

Do not display a fake developer page. Open the real SanStudio portfolio URL using the appropriate Android/browser intent.

---

# 1. ROLE

You are not acting as a beginner coding assistant.

Act as a combined:

* Principal Android Engineer
* Senior Kotlin Engineer
* Android 16 specialist
* Software Architect
* Product Engineer
* UI/UX Director
* Interaction Designer
* Database Architect
* Security Engineer
* Privacy Engineer
* QA Engineer
* Performance Engineer
* Accessibility Engineer
* Google Play compliance reviewer
* Product strategist

Your responsibility is to design and implement a **real, production-oriented Android application**, not a visual prototype, fake demo, static mockup, or collection of disconnected screens.

Every feature must have a real implementation path.

Do not create fake statistics, fake call records, fake contacts, fake integrations, fake notifications, fake buttons, fake databases, or simulated functionality unless explicitly marked as development/test data.

If a requested capability is restricted by Android OS or Google Play policy, implement the closest legitimate architecture and clearly isolate that capability behind a permission/capability layer.

Never bypass Android security restrictions.

Never scrape private data.

Never use accessibility services, overlays, hidden APIs, root techniques, notification scraping, or other workarounds merely to bypass Android permissions or platform restrictions.

---

# 2. PRODUCT VISION

CallMind is NOT intended to replace the user's normal phone dialer.

The user should continue using their preferred/default Android Phone application.

CallMind is a **Communication Intelligence + Call Memory + Post-Call Productivity companion**.

The central philosophy is:

> Traditional caller apps answer:
> "Who is calling?"

> CallMind answers:
> "What should I do after talking to this person?"

Core experience:

**Incoming call → User answers → Call ends → CallMind presents the person → User can act, remember, schedule, or organize the next step.**

The application should feel like a personal communication assistant rather than a phone replacement.

---

# 3. CORE PRODUCT LOOP

Design the entire architecture around this loop:

1. A person calls the user.
2. The user answers using their existing phone application.
3. The call ends.
4. CallMind detects/receives a legitimate supported signal that a call has ended.
5. CallMind resolves the number to a local contact when possible.
6. CallMind displays a premium post-call experience.
7. User can:

   * Call again
   * Open WhatsApp
   * Open Telegram
   * View contact
   * Add a note
   * Schedule a call
   * Create a reminder
   * View call history
   * View communication statistics
8. All locally permitted data is persisted in the application's database.
9. Future interactions become more useful because CallMind remembers user-created notes, reminders, labels, and supported history.

---

# 4. IMPORTANT PRODUCT BOUNDARY

CallMind must NOT become a conventional dialer unless the user explicitly chooses to make it one in a future version.

When the user taps:

**Call**

use the appropriate Android Dial Intent so the normal phone application handles the actual call.

Do not silently place calls.

Do not require `CALL_PHONE` if an Intent-based solution is sufficient.

The user must remain in control of initiating the call.

---

# 5. ANDROID PLATFORM TARGET

Build for modern Android.

Primary target:

**Android 16 / API 36**

Use the latest stable Android Studio, Kotlin, Android Gradle Plugin, Jetpack Compose, AndroidX, Material 3 and other stable dependencies available at implementation time.

Use:

* Kotlin
* Jetpack Compose
* Material 3
* Navigation Compose
* Room
* Kotlin Coroutines
* Flow
* ViewModel
* Hilt or another mature dependency injection solution
* WorkManager where appropriate
* AlarmManager only where exact alarm behavior is genuinely required and permitted
* Android Contacts APIs
* Android Intent APIs
* Notification APIs
* Lifecycle APIs
* WindowInsets APIs
* Predictive Back APIs
* Adaptive UI APIs

Avoid obsolete APIs.

Do not introduce experimental dependencies unless there is a strong engineering reason.

Pin and document dependency versions.

---

# 6. ANDROID 16 EXPERIENCE

The application must feel like a modern Android 16 application.

Implement:

* Edge-to-edge layout
* Proper WindowInsets handling
* Predictive Back
* Gesture navigation compatibility
* Adaptive layouts
* Dynamic screen sizing
* Dynamic color support where appropriate
* Material 3 components
* Large-screen support
* Foldable-friendly layouts
* Multi-window compatibility
* Proper keyboard/IME animations
* Smooth transitions
* Accessibility semantics
* Reduced-motion consideration

Do not create a UI that looks like an old Android application inside an Android 16 device.

Content must not be hidden underneath system bars.

System bars must visually integrate with the application.

Use adaptive layouts rather than simply stretching a phone UI onto tablets.

---

# 7. ORIGINOS 6 VISUAL DIRECTION

The visual language should be inspired by the modern OriginOS 6 experience while remaining an original CallMind design.

Do NOT copy proprietary UI screens.

Do NOT clone OriginOS.

Use the design principles:

* Soft gradient blur
* Layered surfaces
* Stacked cards
* Large readable typography
* Rounded geometry
* Fluid transitions
* Depth
* Soft shadows
* Minimal visual clutter
* Calm surfaces
* Elegant spacing
* High-quality iconography
* Smooth motion

Typography should use **vivo Sans** where legally and technically appropriate and where the font distribution/license allows it.

Provide proper fallbacks for devices/languages that are not supported.

Never assume every Android device runs OriginOS.

The UI must work correctly on Samsung, Pixel, Xiaomi, OnePlus, vivo, OPPO, Motorola and other Android devices.

The visual identity belongs to CallMind, not OriginOS.

---

# 8. DESIGN LANGUAGE

Create a unique CallMind design system.

### Visual character

Think:

**OriginOS fluidity + Android 16 Material 3 + modern productivity app + premium communication assistant**

Avoid:

* Generic Material templates
* Excessive gradients
* Excessive glassmorphism
* Huge shadows
* Neon gamer styling
* Cluttered dashboards
* Excessive animations
* iOS imitation
* Truecaller imitation

The application should feel:

**Premium / Calm / Intelligent / Fast / Personal / Modern**

---

# 9. COLOR SYSTEM

Support:

### Light Mode

Clean neutral background.

### Dark Mode

Deep dark surfaces with controlled contrast.

### Dynamic Color

Support Android dynamic colors when appropriate.

Also provide a CallMind accent system.

Suggested conceptual colors:

* Primary: intelligent blue / indigo
* Success: communication green
* Warning: reminder amber
* Error: controlled red
* Neutral: adaptive grayscale

Do not hard-code the entire application to one color.

Create centralized theme tokens.

---

# 10. TYPOGRAPHY

Create a centralized typography system.

Hierarchy:

* Display
* Large title
* Section title
* Body
* Secondary body
* Caption
* Numeric statistics
* Button labels

Contact names should have strong hierarchy.

Phone numbers should be visually secondary.

Call statistics should use large, readable numbers.

Notes should be highly readable.

Do not use tiny text.

Support:

* Dynamic font scaling
* Accessibility text size
* Long names
* Long phone numbers
* Indian names
* International names
* RTL languages where appropriate

---

# 11. MAIN APPLICATION NAVIGATION

Use a modern navigation architecture.

Primary destinations:

### Home

### Calls

### Contacts

### Reminders

### Notes

### Settings

On smaller devices use bottom navigation.

On larger screens use an adaptive navigation rail or navigation drawer.

Do not force the same navigation layout onto every screen size.

---

# 12. HOME DASHBOARD

Home should immediately communicate:

> "What needs your attention?"

Example:

**Good evening, Santhosh**

Then a compact communication summary:

**Today's communication**

* 7 calls
* 42 min talking
* 2 scheduled calls
* 3 notes

Then:

### Recent Calls

Show:

**Arun Kumar**
+91 XXXXX XXXXX
8 min ago / 8 min duration

Actions:

* Call
* WhatsApp
* Note

Then:

### Upcoming

**Call Arun Kumar**
Today · 8:30 PM

Then:

### Quick Actions

* Add Note
* Schedule Call
* Search Contact
* Recent Calls

The dashboard should never feel like a data-heavy enterprise CRM.

---

# 13. POST-CALL EXPERIENCE

This is the most important screen in the entire product.

Design it as the signature CallMind experience.

When supported by Android/device capabilities, after a completed call show:

# CALL ENDED

### Arun Kumar

+91 XXXXX XXXXX

**08:42**

Then a subtle status:

**Call completed**

Actions:

### Call Again

Open the system dialer.

### WhatsApp

Attempt supported WhatsApp deep-link.

### Telegram

Attempt supported Telegram deep-link.

### Schedule

Create a future call reminder.

### Remind Me

Create a reminder.

### Add Note

Attach a note to the person/number.

### View Contact

Open the system contact when available.

---

# 14. POST-CALL SMART NOTE

Immediately make note creation extremely fast.

Example:

> What do you want to remember?

Input:

"Send project files tomorrow."

Buttons:

**Save Note**

Optionally detect natural-language dates locally:

"tomorrow"
"Friday"
"next week"

Do not automatically create a reminder without confirmation.

Show:

> We found a date: Tomorrow
> Create reminder?

[Create Reminder] [Just Save Note]

---

# 15. CALL HISTORY INTELLIGENCE

For each contact/number show:

### Arun Kumar

**5 calls this month**

**47 min total**

**9 min average**

**Last call**
Today · 6:42 PM

Then:

### Activity

Today
08:42

Yesterday
03:12

Aug 14
12:21

Use real locally available data only.

Never fabricate statistics.

Calculate statistics from stored legitimate records.

---

# 16. CONTACT INTELLIGENCE

Contact screen should include:

* Profile image if available
* Name
* Phone number
* Contact source
* Last interaction
* Total calls
* Total duration
* Average call duration
* User-created notes
* Scheduled calls
* Reminders
* Communication actions

Quick actions:

* Call
* WhatsApp
* Telegram
* Message
* Contact
* Note
* Schedule

Do not assume WhatsApp or Telegram exists.

Detect installed applications and handle failures gracefully.

---

# 17. UNKNOWN NUMBER EXPERIENCE

For an unknown number:

### Unknown Number

+91 XXXXX XXXXX

Do not invent a caller name.

Show:

* Number
* Country/region information when reliably derivable
* Call time
* Duration where legitimately available
* User-created notes
* Actions

Possible actions:

**Call**

**WhatsApp**

**Telegram**

**Save Contact**

**Add Label**

**Add Note**

If an external caller-ID provider is configured in a future version, isolate that provider behind a dedicated interface.

---

# 18. CALLER ID ARCHITECTURE

Do NOT build an illegal or scraped global caller database.

Priority:

1. Local Android Contacts
2. User-created CallMind identities/labels
3. Optional future legitimate caller-ID provider
4. Unknown number

Architecture:

`CallerIdentityResolver`

Possible implementations:

`LocalContactResolver`

`CallMindLocalIdentityResolver`

`ExternalCallerIdProvider` — future optional module

The UI must not depend on a third-party caller-ID provider.

If no identity exists:

**Unknown Number**

Never fake a person's name.

---

# 19. WHATSAPP INTEGRATION

Implement a dedicated communication launcher abstraction.

Example:

`MessagingProvider`

Providers:

* WhatsApp
* Telegram
* SMS
* Email — future

The application should:

1. Check whether the target application is available.
2. Normalize the phone number appropriately.
3. Attempt the supported deep link/Intent.
4. Handle failure.
5. Return the user to CallMind gracefully.

Never pretend that a number has a WhatsApp account simply because WhatsApp is installed.

---

# 20. TELEGRAM INTEGRATION

Same architecture.

Do not assume:

> Number = Telegram account.

Attempt the supported external intent.

If unavailable:

Show:

**Telegram isn't available for this number/app configuration.**

Provide alternative actions.

---

# 21. CALL SCHEDULING

Users can schedule:

**Call Arun Kumar**

Date:

August 20, 2026

Time:

10:30 AM

Optional note:

"Discuss website quotation."

Store the scheduled call locally.

Schedule a notification.

Notification:

### Call Reminder

**Call Arun Kumar**

10:30 AM

Actions:

**Call Now**

**Snooze**

**Dismiss**

Call Now launches the system dialer.

Do not silently call.

---

# 22. REMINDER ENGINE

Create a reliable reminder architecture.

Reminder states:

* Scheduled
* Triggered
* Completed
* Snoozed
* Cancelled

Allow:

* 5 min
* 15 min
* 30 min
* 1 hour
* Tomorrow
* Custom

Use WorkManager for non-exact background work where suitable.

Use exact alarms only when required and when Android permissions/policy allow it.

---

# 23. NOTES SYSTEM

Notes must be first-class data.

Each note can belong to:

* Contact
* Phone number
* Call event
* Reminder

Fields:

* ID
* Contact ID
* Phone number
* Call ID if available
* Text
* Created timestamp
* Updated timestamp
* Reminder timestamp
* Archived state

Support:

* Create
* Edit
* Delete
* Search
* Pin
* Archive

Do not require cloud storage for basic notes.

---

# 24. SMART SEARCH

Global search should search:

* Contact names
* Phone numbers
* Notes
* Labels
* Call history
* Scheduled calls

Example:

Searching:

`project`

could return:

**Arun Kumar**
Note: "Project quotation"

Searching:

`9876`

returns matching phone numbers.

Search must be fast and local-first.

---

# 25. ANALYTICS

Create a useful communication analytics screen.

Metrics:

* Calls today
* Calls this week
* Calls this month
* Total duration
* Average duration
* Most contacted people
* Most recent contacts
* Scheduled calls
* Outstanding reminders

Do not create invasive personality profiling.

Do not infer sensitive personal characteristics.

Do not send call analytics to external servers without explicit consent and a legitimate product requirement.

---

# 26. CONTACT FREQUENCY

Calculate useful local metrics:

**Most contacted**

Arun Kumar
12 interactions

Priya
8 interactions

Karthik
6 interactions

Only use legitimate locally available data.

Provide time filters:

* Today
* 7 days
* 30 days
* 3 months
* Custom

---

# 27. DATABASE

Use Room.

Suggested entities:

`ContactEntity`

`PhoneNumberEntity`

`CallRecordEntity`

`NoteEntity`

`ReminderEntity`

`ScheduledCallEntity`

`LabelEntity`

`AppPreferenceEntity`

`CommunicationActionEntity`

Create appropriate relationships.

Use migrations.

Never wipe user data during application updates.

Use transactions where required.

---

# 28. DATA MODEL PRINCIPLE

Separate:

### Android system data

from:

### CallMind user-created data

Example:

Android:

Contact name
Phone number
Supported call metadata

CallMind:

Notes
Labels
Reminder
Scheduled call
User preferences

This separation is extremely important.

If Android contact data changes, CallMind notes must not disappear.

Use stable identifiers where possible.

---

# 29. PRIVACY-FIRST ARCHITECTURE

Default philosophy:

### Local first.

Do not upload:

* Call history
* Contact list
* Notes
* Phone numbers
* Communication statistics

to a server unless the user explicitly enables a future cloud feature.

No advertising SDK should receive sensitive communication data.

No analytics SDK should automatically collect phone numbers or call metadata.

Create:

**Privacy Center**

Explain:

* What data is accessed
* Why it is accessed
* Where it is stored
* Whether it leaves the device
* How to delete it

---

# 30. PERMISSION ARCHITECTURE

Create a dedicated:

`PermissionManager`

Never request every permission on first launch.

Use progressive permission requests.

Examples:

Contacts permission → only when contact functionality is activated.

Notification permission → when reminders are first configured.

Call-related permissions → only if the selected feature legitimately requires them and the application qualifies under the applicable Android/Google Play rules.

Do not declare sensitive permissions simply because they might be useful someday.

Do not attempt to bypass denied permissions.

If permission is denied:

Show a helpful explanation and alternative workflow.

---

# 31. CRITICAL GOOGLE PLAY REQUIREMENT

Treat Google Play policy as a product requirement, not an afterthought.

Before implementing Call Log access, verify the current Google Play policy and Android API behavior.

If the non-default-dialer architecture cannot legally/technically access the required call-log data:

DO NOT FAKE IT.

Instead implement a capability matrix.

Example:

| Capability          | Available               | Requirement                                       |
| ------------------- | ----------------------- | ------------------------------------------------- |
| Launch phone dialer | Yes                     | Dial Intent                                       |
| Open contact        | Yes                     | Contacts capability                               |
| Notes               | Yes                     | Local DB                                          |
| Reminders           | Yes                     | Notification/alarm capability                     |
| WhatsApp launch     | Conditional             | App/deep-link availability                        |
| Telegram launch     | Conditional             | App/deep-link availability                        |
| Full call history   | Policy/device dependent | Sensitive permission/default-handler requirements |
| Global caller ID    | No by default           | External legitimate provider                      |
| Post-call detection | Device/API dependent    | Must use supported APIs                           |

The application must remain useful even when restricted capabilities are unavailable.

---

# 32. CAPABILITY-DRIVEN UX

Build a capability layer.

Example:

`CallCapability`

`ContactCapability`

`NotificationCapability`

`WhatsAppCapability`

`TelegramCapability`

`CallLogCapability`

Each capability reports:

* Available
* PermissionRequired
* Unsupported
* Restricted
* Disabled
* Ready

The UI reacts to the capability state.

Do not crash.

Do not show buttons that cannot work.

---

# 33. SETTINGS

Settings sections:

### Appearance

* Light
* Dark
* System
* Dynamic Color
* Animation preference

### Post-call

* Enable post-call experience
* Delay
* Show after incoming calls
* Show after outgoing calls
* Only show for known contacts

### Notifications

* Call reminders
* Scheduled calls
* Notes
* Daily summary

### Communication

* Default phone app
* WhatsApp
* Telegram

### Privacy

* Local storage
* Data deletion
* Permission management

### About

**CallMind**

Version

**Developed by SanStudio**

Tapping it opens:

https://sanstudio-hub.github.io/in/

---

# 34. ONBOARDING

Create a premium 3–4 screen onboarding.

Screen 1:

### Remember every important conversation.

Screen 2:

### Turn conversations into actions.

Screen 3:

### Schedule. Note. Follow up.

Screen 4:

### Your communication stays yours.

Then:

**Get Started**

Do not request every permission during onboarding.

Explain permissions contextually.

---

# 35. EMPTY STATES

Every screen needs a meaningful empty state.

Example:

### No notes yet

"Save something worth remembering."

[Add Note]

Example:

### No scheduled calls

"Plan your next conversation."

[Schedule Call]

Example:

### No recent activity

"Your communication activity will appear here."

Do not leave blank white screens.

---

# 36. ERROR STATES

Handle:

* WhatsApp not installed
* Telegram not installed
* Invalid number
* Contact unavailable
* Permission denied
* Notification disabled
* Alarm restriction
* Database failure
* Unsupported Android feature
* External Intent failure

Every error must be understandable to a normal user.

Never show raw stack traces.

---

# 37. ANIMATION SYSTEM

Animation is important but must remain functional.

Use:

* Shared element transitions
* Fade-through
* Scale
* Spring motion
* Staggered list appearance
* Card expansion
* Bottom sheet motion
* Predictive Back
* Cross-screen transitions
* Subtle number animations
* Reminder confirmation animation

Signature animation:

### Post-call card

The contact avatar and contact identity should smoothly transition into the post-call detail surface.

Use motion to communicate hierarchy.

Do not animate every element simultaneously.

Respect:

**Reduce Motion**

when the user/device requests reduced animation.

---

# 38. MICROINTERACTIONS

Examples:

Save Note:

Button → loading/confirmation → checkmark → subtle success motion.

Schedule:

Date selected → time selected → confirmation card expands.

Call:

Call button → tactile feedback → system phone app launches.

WhatsApp:

Button → external app transition.

Reminder:

Reminder created → card transforms into scheduled state.

Use haptic feedback only where appropriate.

---

# 39. UI COMPONENT SYSTEM

Create reusable components:

`CallMindCard`

`ContactAvatar`

`CallStat`

`ActionButton`

`QuickAction`

`PostCallHero`

`NoteCard`

`ReminderCard`

`ContactHeader`

`CommunicationButton`

`SectionHeader`

`SearchBar`

`EmptyState`

`PermissionCard`

`CapabilityBanner`

`GlassSurface`

`BlurSurface`

`AnimatedCounter`

`TimelineItem`

Avoid duplicated UI code.

---

# 40. RESPONSIVE DESIGN

The application must work on:

* Small phones
* Normal phones
* Large phones
* Foldables
* Tablets
* Landscape
* Split screen

On tablet:

Use two-pane or three-pane layouts where useful.

Example:

Left:

Contacts / calls

Center:

Selected contact

Right:

Notes / activity

Use Android adaptive layout patterns rather than manually detecting random device sizes.

---

# 41. ACCESSIBILITY

Support:

* TalkBack
* Content descriptions
* Minimum touch target sizes
* Dynamic font size
* Contrast
* Reduced motion
* Keyboard navigation where relevant
* Screen readers
* Semantic UI
* Meaningful focus order

Never communicate information only through color.

---

# 42. PERFORMANCE

Target:

* Fast startup
* Smooth scrolling
* 60fps+ UI where possible
* No unnecessary recompositions
* Efficient Room queries
* Paging for large histories
* Lazy lists
* Background work off the main thread
* No memory leaks

Do not load thousands of records into memory unnecessarily.

Use profiling during development.

---

# 43. SECURITY

Protect sensitive local information.

Do not log:

* Phone numbers
* Contact names
* Notes
* Call records

in production logs.

Do not expose sensitive data through debug endpoints.

Use Android Keystore where encryption keys are needed.

If sensitive local database encryption is introduced, choose a mature maintained solution rather than inventing cryptography.

---

# 44. OFFLINE-FIRST

Core functionality should work without Internet:

* Notes
* Contacts where locally available
* Local history
* Scheduling
* Reminders
* Statistics
* Search

Internet must NOT be mandatory for basic CallMind operation.

External communication applications can obviously require their own connectivity.

---

# 45. OPTIONAL FUTURE CLOUD ARCHITECTURE

Do not add a cloud backend unless required.

Design interfaces so future sync can be added.

Possible future architecture:

Android Client
↓
Sync Layer
↓
Secure API
↓
Database

Possible future features:

* Multi-device sync
* Encrypted backup
* Account login
* Web dashboard
* Cross-device notes

But do not implement unnecessary cloud infrastructure in Version 1.

---

# 46. OPTIONAL AI LAYER

AI should NOT be required for the core application.

Future optional AI features:

### Conversation Note Assistant

Turn:

"Talked about website quotation, send files tomorrow"

into:

* Topic: Website quotation
* Action: Send files
* Date: Tomorrow

Always require confirmation before creating actions.

### Note Summarization

Summarize user-written notes.

### Follow-up Suggestions

Example:

"You scheduled a call with Arun for tomorrow."

Do not infer sensitive personal information.

Do not upload private communication data to an AI API without explicit user consent.

Provide an offline/basic experience when AI is unavailable.

---

# 47. SMART FOLLOW-UP

Add a future feature called:

### Follow-up Center

Show:

**Needs attention**

* Call Arun
* Send project files
* Follow up with Priya
* Meeting reminder

This turns CallMind from a call log into a communication task manager.

---

# 48. CONTACT TIMELINE

Each person gets a timeline.

Example:

### Arun Kumar

**Today**

08:42 call

09:05 note added

**Yesterday**

03:12 call

**Aug 14**

12:21 call

**Aug 12**

Reminder completed

This timeline should become one of CallMind's signature features.

---

# 49. SMART LABELS

Allow users to create labels:

* Work
* Client
* Family
* Friend
* Project
* Important
* Follow-up

Do not automatically assign sensitive labels.

User controls labels.

---

# 50. FAVORITES

Allow:

* Favorite contacts
* Favorite notes
* Important follow-ups

Show them prominently on Home.

---

# 51. QUICK ACTIONS

Support Android-friendly quick actions where appropriate:

* Add Note
* Schedule Call
* Search
* Recent Calls

Consider:

* App shortcuts
* Widgets
* Notification actions

Only implement features that work reliably across supported Android versions.

---

# 52. WIDGETS

Future Version:

### CallMind Today

Show:

* Upcoming call
* Pending follow-up
* Recent important contact

### Quick Note Widget

Allow the user to quickly save a note.

### Contact Widget

Show favorite contact and call action.

---

# 53. NOTIFICATION DESIGN

Notifications should be useful, not spammy.

Examples:

### Scheduled Call

"Call Arun Kumar"

[Call] [Snooze]

### Follow-up

"You planned to follow up with Arun."

[View] [Done]

Use notification channels correctly.

Allow users to disable individual categories.

---

# 54. HOME SCREEN QUICK ACCESS

Future enhancement:

Android App Shortcuts:

* New Note
* Schedule Call
* Search Contact
* Recent Calls

---

# 55. SEARCH EXPERIENCE

Use a premium full-screen search experience.

Search bar:

**Search contacts, numbers, notes...**

Results grouped:

### People

Arun Kumar

### Notes

"Send project files"

### Calls

Aug 18 · 8:42

Use instant local search.

Debounce text input.

---

# 56. DATA EXPORT

Implement optional local export.

User can export:

* Notes
* Reminders
* CallMind-created metadata

Do not automatically export sensitive Android system data if doing so conflicts with platform policy.

Use structured JSON for backup.

Future:

Encrypted backup file.

---

# 57. DATA DELETION

Settings:

### Delete all CallMind data

Show confirmation.

Clearly explain what will be deleted.

Do not delete the user's Android contacts.

Do not delete system call history.

CallMind should only remove its own stored data.

---

# 58. TEST DATA MODE

Create a developer-only test data generator.

It must be disabled in release builds.

It may generate:

* Fake contacts
* Fake notes
* Fake reminders
* Fake call statistics

This is only for development/testing.

Never ship fake activity to normal users.

---

# 59. ARCHITECTURE

Use a clean modular architecture.

Suggested:

`app`

`core-ui`

`core-design`

`core-data`

`core-database`

`core-permissions`

`core-platform`

`feature-home`

`feature-calls`

`feature-contacts`

`feature-notes`

`feature-reminders`

`feature-settings`

`feature-search`

`feature-postcall`

`feature-communication`

Keep domain/business logic independent from Android UI.

Use interfaces for platform integrations.

---

# 60. REPOSITORY STRUCTURE

Create a professional project structure.

Example:

`app/`

`core/`

`data/`

`domain/`

`ui/`

`features/`

`platform/`

`navigation/`

`di/`

`testing/`

Do not create one enormous Activity/ViewModel containing the entire application.

---

# 61. STATE MANAGEMENT

Use:

* ViewModel
* StateFlow
* immutable UI state
* event/state separation
* repository pattern

Avoid global mutable state.

Avoid passing large mutable objects between screens.

---

# 62. ERROR HANDLING

Use sealed result/state models.

Example conceptual states:

`Loading`

`Success`

`Empty`

`PermissionRequired`

`Unavailable`

`Error`

UI must react predictably to each state.

---

# 63. LOGGING

Create structured development logging.

Release build:

* Minimal logs
* No personal communication information
* No phone numbers
* No notes

---

# 64. TESTING

Create:

### Unit Tests

* Statistics
* Date calculations
* Reminder calculations
* Search
* Note operations
* Call duration calculations

### Repository Tests

* Database
* Migrations
* Queries

### UI Tests

* Home
* Post-call
* Notes
* Scheduling
* Search
* Settings

### Integration Tests

* Dial Intent
* Notification flow
* Contact lookup
* Deep links where testable

### Permission Tests

Test:

* Granted
* Denied
* Permanently denied
* Unsupported

---

# 65. DEVICE TEST MATRIX

Test at minimum on:

* Android 16 emulator
* Android 15
* Android 14
* Small phone
* Large phone
* Tablet

Where hardware is available, test OEM differences including:

* vivo/OriginOS
* Samsung/One UI
* Pixel
* Xiaomi
* OnePlus/OxygenOS

Do not assume OEM background restrictions behave identically.

---

# 66. ORIGINOS-SPECIFIC TESTING

On OriginOS devices:

Verify:

* Font rendering
* Blur
* Animations
* Notification behavior
* Background restrictions
* Battery optimization
* App launch behavior
* Deep links
* Edge-to-edge
* Gesture navigation

The app should gracefully degrade if OEM-specific blur APIs are unavailable.

---

# 67. NO FAKE BLUR REQUIREMENT

Do not create expensive real-time blur everywhere.

Use efficient alternatives:

* Pre-rendered gradients
* Android-supported blur where appropriate
* translucent surfaces
* alpha overlays

Use blur only where it improves hierarchy.

Performance is more important than visual gimmicks.

---

# 68. BRANDING

Brand:

# CallMind

Subtitle:

**Your calls. Your memory. Your next move.**

Developer:

**Developed by SanStudio**

Developer link:

https://sanstudio-hub.github.io/in/

Do not use Truecaller branding.

Do not use Truecaller logo.

Do not copy Truecaller screens.

Do not represent CallMind as affiliated with Truecaller, vivo, OriginOS, WhatsApp, Telegram, Google, or any other company.

---

# 69. APP ICON

Design a unique CallMind icon.

Concept:

A communication ring / memory node / call waveform combined with a subtle mind/connection symbol.

Keep it simple.

It must work as:

* Adaptive icon
* Monochrome icon
* Dark/light launcher environment

Do not create a generic telephone receiver icon.

---

# 70. SPLASH SCREEN

Use Android's modern splash-screen architecture.

Keep it minimal.

Show:

CallMind logo

Then transition smoothly into Home.

Do not create an old-style custom splash delay.

---

# 71. POST-CALL SIGNATURE VISUAL

The most visually recognizable feature should be the post-call surface.

Concept:

Soft background.

Large contact avatar.

Contact name.

Call duration.

Then a layered action surface.

Example hierarchy:

CALL ENDED

Arun Kumar

08:42

[ Call Again ]

[ WhatsApp ] [ Telegram ]

[ Schedule ] [ Remind ]

[ Add Note ]

Then:

Recent Activity

Notes

The user should understand the entire screen in less than 2 seconds.

---

# 72. UX PRINCIPLE

Every screen must answer:

> "What can the user do next?"

Avoid decorative interfaces that do not improve productivity.

Every animation must communicate:

* State
* Hierarchy
* Relationship
* Confirmation
* Navigation

Not decoration alone.

---

# 73. PRODUCT DIFFERENTIATION

CallMind is NOT:

* A Truecaller clone
* A dialer clone
* A CRM clone
* A messaging app
* A call recorder

CallMind IS:

**A personal communication memory and follow-up assistant.**

Core differentiation:

### Remember the conversation.

### Remember what needs to happen next.

### Make the next action one tap away.

---

# 74. VERSION ROADMAP

## Version 1 — Foundation

* Home
* Recent activity
* Contact integration
* Notes
* Reminders
* Scheduled calls
* Dial Intent
* WhatsApp launch
* Telegram launch
* Search
* Settings
* Privacy
* Modern Android 16 UI
* Local database

## Version 1.5

* Contact timeline
* Advanced statistics
* Labels
* Favorites
* Widgets
* App shortcuts
* Better post-call UX

## Version 2

* Optional AI assistant
* Follow-up center
* Smart note extraction
* Encrypted backup
* Cloud sync
* Multi-device support

## Version 3

Potential:

* Legitimate caller-ID provider integration
* Advanced communication analytics
* Web companion dashboard
* Cross-device ecosystem

Do not build Version 3 features before Version 1 is stable.

---

# 75. DEVELOPMENT RULE

Build in phases.

Do NOT generate 100 files blindly.

Before coding:

1. Analyze requirements.
2. Identify Android limitations.
3. Identify permission requirements.
4. Identify Google Play restrictions.
5. Create architecture.
6. Create data model.
7. Create design system.
8. Create navigation map.
9. Create implementation plan.
10. Then begin development.

After each major module:

* Compile
* Run tests
* Fix errors
* Inspect UI
* Verify navigation
* Verify state
* Verify permissions
* Verify performance

Never continue while the project has unresolved compile errors.

---

# 76. UI QUALITY GATE

Before considering a screen complete, verify:

* Typography hierarchy
* Spacing
* Touch targets
* Accessibility
* Dark mode
* Light mode
* Dynamic color
* Edge-to-edge
* Gesture navigation
* Animation
* Loading state
* Empty state
* Error state
* Long text
* Small screen
* Large screen
* Landscape

---

# 77. PRODUCTION QUALITY GATE

Before release:

* No fake functionality
* No hardcoded user data
* No debug screens
* No test data in release
* No sensitive logs
* No unnecessary permissions
* No unused permissions
* No broken deep links
* No crashes
* No ANRs
* No obvious memory leaks
* No broken back navigation
* No broken notification actions
* No accessibility blockers
* No policy-violating data collection

---

# 78. README REQUIREMENT

Create a professional README explaining:

* Product vision
* Features
* Architecture
* Tech stack
* Android version
* Permissions
* Privacy model
* Build instructions
* Test instructions
* Known Android limitations
* Google Play policy considerations
* Roadmap
* Screenshots section
* SanStudio attribution

---

# 79. DEVELOPER DOCUMENTATION

Create:

`ARCHITECTURE.md`

`PERMISSIONS.md`

`PRIVACY.md`

`DESIGN_SYSTEM.md`

`TESTING.md`

`ROADMAP.md`

Each must reflect the actual implementation.

Do not document features that do not exist.

---

# 80. FINAL ENGINEERING PRINCIPLE

Do not optimize for:

> "Make the demo look impressive."

Optimize for:

> "Make this product actually usable by a real Android user."

The final application should feel like a product that could eventually be published, maintained, tested, upgraded and trusted.

---

# 81. FINAL PRODUCT EXPERIENCE

When a user finishes a call, CallMind should make them think:

> "Oh — I can actually remember what I need to do next."

That is the product.

Not another dialer.

Not another caller-ID clone.

Not another call-history screen.

It is:

# CALLMIND

### Your calls.

### Your memory.

### Your next move.

**Developed by SanStudio**

https://sanstudio-hub.github.io/in/
